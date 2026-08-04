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
            globals.put(trigger, compile(lines, "config.yml effects." + trigger.getConfigKey()));
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

        List<EffectSpec> specs = (crateSpecs != null && !crateSpecs.isEmpty())
                ? crateSpecs
                : globals.get(trigger);

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
