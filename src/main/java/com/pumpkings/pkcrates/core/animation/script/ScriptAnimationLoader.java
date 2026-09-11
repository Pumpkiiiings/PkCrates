package com.pumpkings.pkcrates.core.animation.script;

import com.pumpkings.pkcrates.core.animation.AnimationRegistry;
import com.pumpkings.pkcrates.core.effect.EffectEngine;
import com.pumpkings.pkcrates.core.effect.EffectSpec;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loads and validates scripted animations from {@code animations/*.yml}. */
public final class ScriptAnimationLoader {

    private static final int HARD_MAX_DURATION = 1200;
    private static final int HARD_MAX_ACTIONS = 500;

    private final Plugin plugin;
    private final EffectEngine effects;
    private final AnimationRegistry registry;
    private final Set<String> registered = new HashSet<>();
    private final Map<String, List<EffectSpec>> components = new HashMap<>();

    public ScriptAnimationLoader(Plugin plugin, EffectEngine effects, AnimationRegistry registry) {
        this.plugin = plugin;
        this.effects = effects;
        this.registry = registry;
    }

    public int loadAll() {
        registered.forEach(registry::unregister);
        registered.clear();
        components.clear();
        ensureDefaults();
        loadComponents();

        File folder = new File(plugin.getDataFolder(), "animations");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return 0;

        for (File file : files) {
            try {
                ScriptAnimationDefinition definition = parse(file);
                if (registry.getRegisteredAnimations().contains(definition.id())) {
                    throw new IllegalArgumentException("id '" + definition.id()
                            + "' collides with a built-in animation; choose a unique id");
                }
                registry.register(definition.id(), () -> new ScriptedAnimationPhase(definition, plugin));
                registered.add(definition.id());
                plugin.getLogger().info("Loaded scripted animation '" + definition.id() + "' ("
                        + definition.actions().size() + " actions, " + definition.duration() + " ticks).");
            } catch (Exception e) {
                plugin.getLogger().severe("Cannot load animation " + file.getName() + ": " + e.getMessage());
            }
        }
        return registered.size();
    }

    private void ensureDefaults() {
        File animations = new File(plugin.getDataFolder(), "animations");
        if (!animations.exists()) animations.mkdirs();
        saveResourceIfMissing("animations/scripted_example.yml");
        saveResourceIfMissing("effects.yml");
    }

    private void saveResourceIfMissing(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists()) plugin.saveResource(path, false);
    }

    private void loadComponents() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "effects.yml"));
        ConfigurationSection section = config.getConfigurationSection("components");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            List<String> lines = section.getStringList(id + ".actions");
            if (lines.isEmpty()) lines = section.getStringList(id);
            components.put(id.toLowerCase(Locale.ROOT), effects.compile(lines, "effects.yml component '" + id + "'"));
        }
    }

    private ScriptAnimationDefinition parse(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("id", file.getName().replaceFirst("(?i)\\.yml$", ""));
        if (!id.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("invalid id '" + id + "'");
        int duration = yaml.getInt("duration", 100);
        if (duration < 1 || duration > HARD_MAX_DURATION) {
            throw new IllegalArgumentException("duration must be between 1 and " + HARD_MAX_DURATION);
        }

        Map<String, Double> variables = new LinkedHashMap<>();
        ConfigurationSection variableSection = yaml.getConfigurationSection("variables");
        if (variableSection != null) {
            for (String key : variableSection.getKeys(false)) {
                variables.put(key, variableSection.getDouble(key));
            }
        }

        List<Map<?, ?>> rawActions = new ArrayList<>();
        rawActions.addAll(yaml.getMapList("timeline"));
        ConfigurationSection tracks = yaml.getConfigurationSection("tracks");
        if (tracks != null) {
            for (String track : tracks.getKeys(false)) rawActions.addAll(tracks.getMapList(track));
        }
        if (rawActions.size() > HARD_MAX_ACTIONS) throw new IllegalArgumentException("more than " + HARD_MAX_ACTIONS + " actions");

        List<ScriptAction> actions = new ArrayList<>();
        int index = 0;
        for (Map<?, ?> raw : rawActions) {
            actions.add(parseAction(raw, file.getName() + " action #" + (++index), duration));
        }

        int maxActionsPerTick = clamp(yaml.getInt("limits.max-actions-per-tick", 100), 1, 200);
        int maxDisplays = clamp(yaml.getInt("limits.max-displays", 20), 1, 50);
        validatePeak(actions, duration, maxActionsPerTick);

        long seed = yaml.getLong("seed", id.hashCode());
        return new ScriptAnimationDefinition(id.toUpperCase(Locale.ROOT), duration, seed,
                Map.copyOf(variables), List.copyOf(actions), maxActionsPerTick, maxDisplays);
    }

    private ScriptAction parseAction(Map<?, ?> raw, String source, int duration) {
        Map<String, Object> map = stringMap(raw);
        int from;
        int to;
        int every = Math.max(1, integer(map.get("every"), 1));
        if (map.containsKey("during")) {
            String[] range = String.valueOf(map.get("during")).split("\\.\\.", 2);
            if (range.length != 2) throw new IllegalArgumentException(source + ": during must be START..END");
            from = Integer.parseInt(range[0].trim());
            to = Integer.parseInt(range[1].trim());
        } else {
            from = integer(map.getOrDefault("from", map.get("at")), 0);
            to = integer(map.getOrDefault("to", from), from);
        }
        if (map.containsKey("repeat")) {
            int repeats = clamp(integer(map.get("repeat"), 1), 1, 100);
            every = Math.max(1, integer(map.get("interval"), every));
            to = from + (repeats - 1) * every;
        }
        if (from < 0 || to < from || to > duration) throw new IllegalArgumentException(source + ": invalid tick range " + from + ".." + to);

        ScriptAction.Type type;
        Map<String, Object> arguments = new LinkedHashMap<>();
        List<EffectSpec> compiled = List.of();

        if (map.containsKey("use") || map.containsKey("effect") || map.containsKey("particle")
                || map.containsKey("sound") || map.containsKey("firework")) {
            type = ScriptAction.Type.EFFECT;
            compiled = compileEffects(map, source);
        } else if (map.containsKey("spawn")) {
            type = ScriptAction.Type.SPAWN_DISPLAY;
            arguments = nestedMap(map.get("spawn"));
        } else if (map.containsKey("animate")) {
            type = ScriptAction.Type.ANIMATE_DISPLAY;
            arguments = nestedMap(map.get("animate"));
        } else if (map.containsKey("remove")) {
            type = ScriptAction.Type.REMOVE_DISPLAY;
            Object remove = map.get("remove");
            arguments.put("id", remove instanceof Map<?, ?> ? nestedMap(remove).get("id") : remove);
        } else if (map.containsKey("title")) {
            type = ScriptAction.Type.TITLE;
            arguments = nestedMap(map.get("title"));
        } else {
            throw new IllegalArgumentException(source + ": unknown action; expected use/effect/particle/sound/firework/spawn/animate/remove/title");
        }

        return new ScriptAction(source, from, to, every, map.get("if"), type,
                Map.copyOf(arguments), List.copyOf(compiled));
    }

    private List<EffectSpec> compileEffects(Map<String, Object> map, String source) {
        Object reference = map.containsKey("use") ? map.get("use") : map.get("effect");
        if (reference instanceof String id && !id.contains(":")) {
            List<EffectSpec> preset = components.get(id.toLowerCase(Locale.ROOT));
            if (preset == null) throw new IllegalArgumentException(source + ": unknown effect component '" + id + "'");
            return preset;
        }

        List<String> lines = new ArrayList<>();
        if (reference instanceof String line && line.contains(":")) lines.add(line);
        for (String kind : List.of("particle", "sound", "firework")) {
            if (!map.containsKey(kind)) continue;
            Object value = map.get(kind);
            if (value instanceof String text) {
                lines.add(kind + ":" + text);
            } else {
                Map<String, Object> values = nestedMap(value);
                String name = String.valueOf(kind.equals("firework")
                        ? values.getOrDefault("color", "")
                        : values.getOrDefault("type", ""));
                StringBuilder line = new StringBuilder(kind).append(':').append(name);
                values.forEach((key, item) -> {
                    boolean identity = key.equals("type") || (kind.equals("firework") && key.equals("color"));
                    if (!identity) line.append(' ').append(key).append(':').append(item);
                });
                lines.add(line.toString());
            }
        }
        List<EffectSpec> result = effects.compile(lines, source);
        if (result.isEmpty()) throw new IllegalArgumentException(source + ": effect contains no valid entries");
        return result;
    }

    private void validatePeak(List<ScriptAction> actions, int duration, int maximum) {
        for (int tick = 0; tick <= duration; tick++) {
            int count = 0;
            for (ScriptAction action : actions) if (action.runsAt(tick)) count++;
            if (count > maximum) throw new IllegalArgumentException("tick " + tick + " runs " + count
                    + " actions, over limits.max-actions-per-tick=" + maximum);
        }
    }

    private static Map<String, Object> stringMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static Map<String, Object> nestedMap(Object value) {
        return value instanceof Map<?, ?> map ? stringMap(map) : new LinkedHashMap<>();
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException e) { return fallback; }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
