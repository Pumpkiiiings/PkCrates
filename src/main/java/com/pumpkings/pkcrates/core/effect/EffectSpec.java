package com.pumpkings.pkcrates.core.effect;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * One configured effect: a particle pattern, a sound, or a firework.
 *
 * <h3>Config syntax</h3>
 * <p>Space-separated {@code key:value} pairs, in any order. Unknown keys are ignored and
 * missing keys fall back to a sensible default, so a line stays valid as the format grows:</p>
 * <pre>
 * particle:FLAME shape:HELIX radius:0.8 height:2.5 count:60
 * particle:DUST color:#FF0055 size:1.5 shape:CIRCLE radius:1.2
 * sound:BLOCK_BEACON_ACTIVATE volume:1.0 pitch:1.4
 * firework:FFAA00 type:BALL_LARGE
 * </pre>
 *
 * <p>Values are validated once at parse time. An invalid particle or sound name yields a
 * {@code null} spec with a reason, so the problem is reported when the config loads rather
 * than spamming the console on every crate opening.</p>
 */
public final class EffectSpec {

    /**
     * Upper bound on particles per effect.
     *
     * <p>A mistyped {@code count:60000} would otherwise stall the server for a visible
     * fraction of a second on every opening.</p>
     */
    private static final int MAX_PARTICLES = 500;

    private enum Kind { PARTICLE, SOUND, FIREWORK }

    private final Kind kind;

    // Particle
    private final Particle particle;
    private final EffectShape shape;
    private final int count;
    private final double radius;
    private final double height;
    private final double speed;
    private final Color color;
    private final float size;

    // Sound
    private final Sound sound;
    private final float volume;
    private final float pitch;

    // Firework
    private final Color fireworkColor;
    private final FireworkEffect.Type fireworkType;

    private EffectSpec(Kind kind, Particle particle, EffectShape shape, int count, double radius,
                       double height, double speed, Color color, float size, Sound sound,
                       float volume, float pitch, Color fireworkColor, FireworkEffect.Type fireworkType) {
        this.kind = kind;
        this.particle = particle;
        this.shape = shape;
        this.count = count;
        this.radius = radius;
        this.height = height;
        this.speed = speed;
        this.color = color;
        this.size = size;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.fireworkColor = fireworkColor;
        this.fireworkType = fireworkType;
    }

    /**
     * Result of parsing one config line.
     *
     * @param spec  The parsed effect, or {@code null} when the line was invalid.
     * @param error Why parsing failed; {@code null} on success.
     */
    public record ParseResult(@Nullable EffectSpec spec, @Nullable String error) {
        public boolean isValid() {
            return spec != null;
        }
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    /**
     * Parses one effect line.
     *
     * @param line Raw config line, e.g. {@code "particle:FLAME shape:HELIX count:40"}.
     * @return The parsed spec, or a result carrying the reason it could not be parsed.
     */
    public static ParseResult parse(String line) {
        if (line == null || line.isBlank()) {
            return new ParseResult(null, "empty line");
        }

        Args args = Args.of(line);

        if (args.has("particle")) return parseParticle(args);
        if (args.has("sound")) return parseSound(args);
        if (args.has("firework")) return parseFirework(args);

        return new ParseResult(null, "line must start with particle:, sound: or firework: — got '" + line + "'");
    }

    private static ParseResult parseParticle(Args args) {
        String name = args.get("particle", "");
        Particle particle = resolveParticle(name);
        if (particle == null) {
            return new ParseResult(null, "unknown particle '" + name + "'");
        }

        int count = Math.min(MAX_PARTICLES, Math.max(1, args.getInt("count", 20)));
        Color color = args.getColor("color");

        // Coloured particles need DustOptions; without a colour they would render grey.
        if (color == null && requiresColor(particle)) {
            color = Color.WHITE;
        }

        return new ParseResult(new EffectSpec(
                Kind.PARTICLE,
                particle,
                EffectShape.parse(args.get("shape", "POINT")),
                count,
                args.getDouble("radius", 0.6),
                args.getDouble("height", 1.0),
                args.getDouble("speed", 0.0),
                color,
                (float) args.getDouble("size", 1.0),
                null, 0f, 0f, null, null), null);
    }

    private static ParseResult parseSound(Args args) {
        String name = args.get("sound", "");
        Sound sound;
        try {
            sound = Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return new ParseResult(null, "unknown sound '" + name + "'");
        }

        return new ParseResult(new EffectSpec(
                Kind.SOUND, null, null, 0, 0, 0, 0, null, 0f,
                sound,
                (float) args.getDouble("volume", 1.0),
                (float) args.getDouble("pitch", 1.0),
                null, null), null);
    }

    private static ParseResult parseFirework(Args args) {
        Color color = args.getColor("firework");
        if (color == null) {
            return new ParseResult(null, "firework needs a hex colour, e.g. firework:FFAA00");
        }

        FireworkEffect.Type type = FireworkEffect.Type.BALL;
        String typeName = args.get("type", null);
        if (typeName != null) {
            try {
                type = FireworkEffect.Type.valueOf(typeName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return new ParseResult(null, "unknown firework type '" + typeName + "'");
            }
        }

        return new ParseResult(new EffectSpec(
                Kind.FIREWORK, null, null, 0, 0, 0, 0, null, 0f,
                null, 0f, 0f, color, type), null);
    }

    /**
     * Resolves a particle name, tolerating the 1.20.5 renames so configs written against
     * either naming keep working.
     */
    private static @Nullable Particle resolveParticle(String name) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // Fall through to the alias table.
        }

        String alias = switch (normalized) {
            case "REDSTONE" -> "DUST";
            case "DUST" -> "REDSTONE";
            case "SPELL_WITCH", "WITCH_SPELL" -> "WITCH";
            case "TOTEM" -> "TOTEM_OF_UNDYING";
            case "TOTEM_OF_UNDYING" -> "TOTEM";
            case "SMOKE_NORMAL" -> "SMOKE";
            case "SMOKE" -> "SMOKE_NORMAL";
            case "VILLAGER_HAPPY" -> "HAPPY_VILLAGER";
            case "HAPPY_VILLAGER" -> "VILLAGER_HAPPY";
            default -> null;
        };
        if (alias == null) return null;

        try {
            return Particle.valueOf(alias);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** @return {@code true} for particles whose data type is {@link Particle.DustOptions}. */
    private static boolean requiresColor(Particle particle) {
        return particle.getDataType() == Particle.DustOptions.class;
    }

    // -------------------------------------------------------------------------
    // Playback
    // -------------------------------------------------------------------------

    /**
     * Plays this effect. Must be called from the main thread.
     *
     * @param origin The location the effect is centred on.
     * @param viewer Player to play sounds for; when {@code null} the sound is played to
     *               everyone in range instead.
     */
    public void play(Location origin, @Nullable Player viewer) {
        if (origin.getWorld() == null) return;

        switch (kind) {
            case PARTICLE -> playParticle(origin);
            case SOUND -> {
                if (viewer != null) {
                    viewer.playSound(origin, sound, volume, pitch);
                } else {
                    origin.getWorld().playSound(origin, sound, volume, pitch);
                }
            }
            case FIREWORK -> spawnFirework(origin);
        }
    }

    private void playParticle(Location origin) {
        List<Vector> offsets = shape.offsets(count, radius, height);
        Object data = color != null && requiresColor(particle)
                ? new Particle.DustOptions(color, size)
                : null;

        for (Vector offset : offsets) {
            Location point = origin.clone().add(offset);
            // One particle per point: the shape already positions them, so letting the
            // client scatter a bundle would smear the pattern.
            origin.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, speed, data);
        }
    }

    private void spawnFirework(Location origin) {
        Firework firework = origin.getWorld().spawn(origin, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(fireworkColor)
                .with(fireworkType)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        // Power 0 still takes a moment to burst; detonating now keeps it at the origin.
        firework.detonate();
    }

    // -------------------------------------------------------------------------
    // Tiny key:value parser
    // -------------------------------------------------------------------------

    /**
     * Splits {@code key:value key:value} into a lookup, tolerating extra whitespace.
     */
    private record Args(java.util.Map<String, String> values) {

        static Args of(String line) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            for (String token : line.trim().split("\\s+")) {
                int colon = token.indexOf(':');
                if (colon <= 0) continue;
                map.put(token.substring(0, colon).toLowerCase(Locale.ROOT), token.substring(colon + 1));
            }
            return new Args(map);
        }

        boolean has(String key) {
            return values.containsKey(key);
        }

        String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        int getInt(String key, int fallback) {
            try {
                return Integer.parseInt(values.getOrDefault(key, String.valueOf(fallback)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        double getDouble(String key, double fallback) {
            try {
                return Double.parseDouble(values.getOrDefault(key, String.valueOf(fallback)));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        @Nullable Color getColor(String key) {
            String raw = values.get(key);
            if (raw == null || raw.isBlank()) return null;
            String hex = raw.startsWith("#") ? raw.substring(1) : raw;
            try {
                return Color.fromRGB(Integer.parseInt(hex, 16));
            } catch (IllegalArgumentException e) {
                // Covers both an unparseable hex string and an out-of-range RGB value.
                return null;
            }
        }
    }
}
