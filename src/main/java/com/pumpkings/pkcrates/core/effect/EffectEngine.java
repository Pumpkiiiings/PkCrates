package com.pumpkings.pkcrates.core.effect;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parses configured effect bundles and plays them at the right moment.
 *
 * <h3>Resolution order</h3>
 * <p>A crate may define its own bundle for a trigger. When it does, it replaces the global
 * one rather than adding to it — an operator writing crate-specific effects means those
 * effects, not those plus whatever the defaults happened to be.</p>
 *
 * <h3>Validation</h3>
 * <p>Lines are parsed once, when the config loads, and bad lines are reported with their
 * reason and then skipped. Nothing is re-parsed per opening, and a typo cannot spam the
 * console every time a player uses a crate.</p>
 */
public class EffectEngine {

    private final Plugin plugin;

    /** Global fallbacks from {@code config.yml}. */
    private final Map<EffectTrigger, List<EffectSpec>> globals = new EnumMap<>(EffectTrigger.class);

    public EffectEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Reloads the global bundles from the {@code effects} section of {@code config.yml}.
     */
    public void loadGlobals(@Nullable ConfigurationSection effectsSection) {
        globals.clear();
        if (effectsSection == null) return;

        for (EffectTrigger trigger : EffectTrigger.values()) {
            List<String> lines = effectsSection.getStringList(trigger.getConfigKey());
            if (lines.isEmpty()) continue;
            String source = "config.yml effects." + trigger.getConfigKey();
            globals.put(trigger, trigger == EffectTrigger.AMBIENT
                    ? compileAmbient(lines, source)
                    : compile(lines, source));
        }
    }

    /**
     * Turns raw config lines into playable specs, logging and dropping the invalid ones.
     *
     * @param lines  Raw effect lines.
     * @param source Where they came from, used in warnings.
     * @return The specs that parsed cleanly; never {@code null}.
     */
    public List<EffectSpec> compile(List<String> lines, String source) {
        List<EffectSpec> specs = new ArrayList<>();
        if (lines == null) return specs;

        for (String line : lines) {
            EffectSpec.ParseResult result = EffectSpec.parse(line);
            if (result.isValid()) {
                specs.add(result.spec());
            } else {
                plugin.getLogger().warning("Ignoring effect in " + source + ": " + result.error());
            }
        }
        return specs;
    }

    /**
     * Compiles a repeating ambient bundle and rejects effects that would be unsafe to
     * execute five times per second (sounds and entity-spawning fireworks).
     */
    public List<EffectSpec> compileAmbient(List<String> lines, String source) {
        List<EffectSpec> specs = compile(lines, source);
        List<EffectSpec> safe = new ArrayList<>();
        for (EffectSpec spec : specs) {
            if (spec.isAmbientSafe()) {
                safe.add(spec);
            } else {
                plugin.getLogger().warning("Ignoring non-particle ambient effect in " + source
                        + ": ambient bundles run repeatedly and only accept particle lines");
            }
        }
        return safe;
    }

    /**
     * Plays the bundle for a trigger.
     *
     * <p>Must be called from the main thread — it spawns particles and entities.</p>
     *
     * @param trigger      Which bundle to play.
     * @param crateSpecs   The crate's own bundle, or {@code null} to use the global one.
     * @param origin       Where to centre the effect.
     * @param viewer       Player to play sounds for; {@code null} plays them to everyone in range.
     */
    public void play(EffectTrigger trigger, @Nullable List<EffectSpec> crateSpecs,
                     Location origin, @Nullable Player viewer) {

        // null means "no crate override". An explicitly configured bundle that compiles
        // to an empty list remains empty instead of unexpectedly falling back to globals.
        List<EffectSpec> specs = crateSpecs != null ? crateSpecs : globals.get(trigger);

        if (specs == null || specs.isEmpty()) return;

        for (EffectSpec spec : specs) {
            try {
                spec.play(origin, viewer);
            } catch (Exception e) {
                // A single bad effect must not abort the opening it decorates.
                plugin.getLogger().warning("Effect failed on " + trigger.getConfigKey() + ": " + e.getMessage());
            }
        }
    }

    /**
     * @return {@code true} when any bundle is configured for the trigger, globally.
     */
    public boolean hasGlobal(EffectTrigger trigger) {
        List<EffectSpec> specs = globals.get(trigger);
        return specs != null && !specs.isEmpty();
    }
}
