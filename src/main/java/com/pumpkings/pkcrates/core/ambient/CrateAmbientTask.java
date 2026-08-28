package com.pumpkings.pkcrates.core.ambient;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runs every 4 ticks (5 times/s) and renders decorative ambient particle
 * effects around every placed crate block whose crate definition has an
 * {@code ambient-effect} value other than {@link AmbientEffect#NONE}.
 *
 * <p><b>Performance notes:</b></p>
 * <ul>
 *   <li>Uses the spatial map already maintained by {@link CrateLocationManager}
 *       — no extra data structures.</li>
 *   <li>Only renders if the chunk is loaded; skips silently otherwise.</li>
 *   <li>Effects are server-side particles, so bandwidth is minimal.</li>
 * </ul>
 */
public class CrateAmbientTask extends BukkitRunnable {

    // Period in server ticks between renders
    public static final long PERIOD_TICKS = 4L;

    private final CrateLocationManager locationMgr;
    private final CrateRegistry crateRegistry;
    private final Random random = new Random();

    // Internal tick counter used by effects that need time-based motion.
    private int tick = 0;

    public CrateAmbientTask(CrateLocationManager locationMgr, CrateRegistry crateRegistry) {
        this.locationMgr  = locationMgr;
        this.crateRegistry = crateRegistry;
    }

    // -----------------------------------------------------------------------

    @Override
    public void run() {
        tick++;

        for (Map.Entry<String, Map<Long, Map<BlockVector, String>>> worldEntry
                : locationMgr.getAllWorldEntries().entrySet()) {

            World world = Bukkit.getWorld(worldEntry.getKey());
            if (world == null) continue;

            for (Map<BlockVector, String> chunkMap : worldEntry.getValue().values()) {
                for (Map.Entry<BlockVector, String> blockEntry : chunkMap.entrySet()) {

                    BlockVector vec    = blockEntry.getKey();
                    String      crateId = blockEntry.getValue();

                    // Skip if chunk is not loaded — no need to load it just for particles.
                    if (!world.isChunkLoaded(vec.getBlockX() >> 4, vec.getBlockZ() >> 4)) continue;

                    Crate crate = crateRegistry.getCrate(crateId);
                    if (crate == null) continue;

                    AmbientEffect effect = crate.getAmbientEffect();
                    if (effect == AmbientEffect.NONE) continue;

                    // Centre of the crate block
                    Location center = new Location(world,
                            vec.getBlockX() + 0.5,
                            vec.getBlockY() + 0.5,
                            vec.getBlockZ() + 0.5);

                    renderEffect(effect, center, world);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Effect renderers
    // -----------------------------------------------------------------------

    private void renderEffect(AmbientEffect effect, Location center, World world) {
        switch (effect) {
            case ENCHANT    -> renderEnchant(center, world);
            case FLAME_RING -> renderFlameRing(center, world);
            case SNOWFALL   -> renderSnowfall(center, world);
            case PORTAL_RING-> renderPortalRing(center, world);
            case STAR_BURST -> renderStarBurst(center, world);
            case TOTEM      -> renderTotem(center, world);
            case RAINBOW    -> renderRainbow(center, world);
            case SOUL_FLAME -> renderSoulFlame(center, world);
            case VOID       -> renderVoid(center, world);
            case HEARTS     -> renderHearts(center, world);
            case CHERRY_BLOSSOM -> renderCherryBlossom(center, world);
            case SCULK      -> renderSculk(center, world);
            case ELECTRIC   -> renderElectric(center, world);
            case WATER      -> renderWater(center, world);
            case HONEY      -> renderHoney(center, world);
            default         -> {}
        }
    }

    // ── ENCHANT ─────────────────────────────────────────────────────────────
    // Orbiting enchantment glyphs in a slow lazy ring.
    private void renderEnchant(Location center, World world) {
        double angle = tick * 0.18;

        for (int i = 0; i < 4; i++) {
            double a  = angle + (i * Math.PI / 2.0);
            double hx = Math.cos(a) * 0.75;
            double hz = Math.sin(a) * 0.75;
            double hy = 0.6 + Math.sin(tick * 0.12 + i) * 0.25; // gentle bob

            world.spawnParticle(Particle.ENCHANT,
                    center.clone().add(hx, hy, hz),
                    1, 0, 0, 0, 0);
        }

        // Occasional upward glyph burst
        if (tick % 8 == 0) {
            world.spawnParticle(Particle.ENCHANT,
                    center.clone().add(0, 0.5, 0),
                    6, 0.3, 0.3, 0.3, 0.15);
        }
    }

    // ── FLAME RING ───────────────────────────────────────────────────────────
    // Spinning ring of flame + lava + smoke for a legendary feel.
    private void renderFlameRing(Location center, World world) {
        double angle = tick * 0.22;

        // 6 flame points in the ring
        for (int i = 0; i < 6; i++) {
            double a  = angle + (i * Math.PI / 3.0);
            double hx = Math.cos(a) * 0.85;
            double hz = Math.sin(a) * 0.85;
            world.spawnParticle(Particle.FLAME,
                    center.clone().add(hx, 0.15, hz),
                    1, 0.03, 0.05, 0.03, 0.005);
        }

        // Smoke puffs every 10 ticks
        if (tick % 10 == 0) {
            world.spawnParticle(Particle.SMOKE,
                    center.clone().add(0, 0.2, 0),
                    3, 0.4, 0.1, 0.4, 0.01);
        }

        // Lava drip occasionally
        if (tick % 20 == 0) {
            world.spawnParticle(Particle.LAVA,
                    center.clone().add(
                            (random.nextDouble() - 0.5) * 1.2,
                            0.1,
                            (random.nextDouble() - 0.5) * 1.2),
                    1, 0, 0, 0, 0);
        }
    }

    // ── SNOWFALL ─────────────────────────────────────────────────────────────
    // Gentle snowflakes raining down with a sparkle above.
    private void renderSnowfall(Location center, World world) {
        // Snowflakes spawned above, drift down naturally
        if (tick % 2 == 0) {
            for (int s = 0; s < 2; s++) {
                world.spawnParticle(Particle.SNOWFLAKE,
                        center.clone().add(
                                (random.nextDouble() - 0.5) * 1.5,
                                1.8,
                                (random.nextDouble() - 0.5) * 1.5),
                        1, 0, 0.005, 0, 0.008);
            }
        }

        // Frosty orbit ring
        double angle = tick * 0.15;
        for (int i = 0; i < 3; i++) {
            double a  = angle + (i * Math.PI * 2 / 3.0);
            double hx = Math.cos(a) * 0.65;
            double hz = Math.sin(a) * 0.65;
            world.spawnParticle(Particle.SNOWFLAKE,
                    center.clone().add(hx, 0.3, hz),
                    1, 0, 0, 0, 0);
        }

        // Sparkle
        if (tick % 12 == 0) {
            world.spawnParticle(Particle.END_ROD,
                    center.clone().add(0, 1.0, 0),
                    2, 0.35, 0.35, 0.35, 0.01);
        }
    }

    // ── PORTAL RING ──────────────────────────────────────────────────────────
    // Pulsing portal ring hovering above the crate.
    private void renderPortalRing(Location center, World world) {
        double angle = tick * 0.25;
        Location above = center.clone().add(0, 1.0, 0);

        for (int i = 0; i < 8; i++) {
            double a  = angle + (i * Math.PI / 4.0);
            double hx = Math.cos(a) * 0.7;
            double hz = Math.sin(a) * 0.7;
            world.spawnParticle(Particle.PORTAL,
                    above.clone().add(hx, 0, hz),
                    1, 0.02, 0.02, 0.02, 0);
        }

        // Inner witch sparkle
        if (tick % 6 == 0) {
            world.spawnParticle(Particle.WITCH,
                    above, 1, 0.3, 0.1, 0.3, 0);
        }

        // Reverse portal rising
        if (tick % 10 == 0) {
            world.spawnParticle(Particle.REVERSE_PORTAL,
                    center.clone().add(0, 0.3, 0),
                    2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    // ── STAR BURST ───────────────────────────────────────────────────────────
    // END_ROD stars shoot upward periodically — heroic / epic feel.
    private void renderStarBurst(Location center, World world) {
        // Gentle floating stars
        if (tick % 3 == 0) {
            world.spawnParticle(Particle.END_ROD,
                    center.clone().add(
                            (random.nextDouble() - 0.5) * 1.0,
                            0.3,
                            (random.nextDouble() - 0.5) * 1.0),
                    1, 0, 0, 0, 0.04);
        }

        // Rotating orbit of stars
        double angle = tick * 0.2;
        for (int i = 0; i < 3; i++) {
            double a  = angle + (i * Math.PI * 2 / 3.0);
            double hx = Math.cos(a) * 0.8;
            double hz = Math.sin(a) * 0.8;
            double hy = 0.4 + Math.sin(tick * 0.1 + i * 2) * 0.2;
            world.spawnParticle(Particle.END_ROD,
                    center.clone().add(hx, hy, hz),
                    1, 0, 0, 0, 0);
        }

        // Burst every 16 ticks
        if (tick % 16 == 0) {
            world.spawnParticle(Particle.END_ROD,
                    center.clone().add(0, 0.5, 0),
                    8, 0.4, 0.6, 0.4, 0.06);
        }
    }

    // ── TOTEM ────────────────────────────────────────────────────────────────
    // Rising totem columns — divine, god-tier feel.
    private void renderTotem(Location center, World world) {
        if (tick % 5 == 0) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING,
                    center.clone().add(
                            (random.nextDouble() - 0.5) * 0.8,
                            0.2,
                            (random.nextDouble() - 0.5) * 0.8),
                    3, 0.1, 0.3, 0.1, 0.08);
        }

        // Halo orbit
        double angle = tick * 0.2;
        for (int i = 0; i < 4; i++) {
            double a  = angle + (i * Math.PI / 2.0);
            double hx = Math.cos(a) * 0.7;
            double hz = Math.sin(a) * 0.7;
            world.spawnParticle(Particle.TOTEM_OF_UNDYING,
                    center.clone().add(hx, 0.5, hz),
                    1, 0, 0, 0, 0);
        }
    }

    // ── RAINBOW ──────────────────────────────────────────────────────────────
    // Slow rainbow DUST orbit — colorful, festive feel.
    private void renderRainbow(Location center, World world) {
        double angle = tick * 0.14;

        // 7 colored points cycling through hue
        for (int i = 0; i < 7; i++) {
            double a    = angle + (i * Math.PI * 2 / 7.0);
            double hx   = Math.cos(a) * 0.8;
            double hz   = Math.sin(a) * 0.8;
            double hy   = 0.3 + Math.sin(tick * 0.08 + i) * 0.2;

            // Hue cycles based on i + tick
            float hue = ((i * 51 + tick * 3) % 360) / 360.0f;
            java.awt.Color awtColor = java.awt.Color.getHSBColor(hue, 1.0f, 1.0f);
            Color dustColor = Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

            world.spawnParticle(Particle.DUST,
                    center.clone().add(hx, hy, hz),
                    1, new Particle.DustOptions(dustColor, 1.2f));
        }

        // Center burst every 20 ticks
        if (tick % 20 == 0) {
            for (int i = 0; i < 5; i++) {
                float hue = (i * 72) / 360.0f;
                java.awt.Color awtColor = java.awt.Color.getHSBColor(hue, 1.0f, 1.0f);
                Color dustColor = Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                world.spawnParticle(Particle.DUST,
                        center.clone().add(0, 0.8, 0),
                        3, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(dustColor, 1.5f));
            }
        }
    }

    // ── SOUL FLAME ───────────────────────────────────────────────────────────
    // Blue soul flame and smoke — spooky / underworld feel.
    private void renderSoulFlame(Location center, World world) {
        double angle = tick * 0.22;
        for (int i = 0; i < 6; i++) {
            double a  = angle + (i * Math.PI / 3.0);
            double hx = Math.cos(a) * 0.85;
            double hz = Math.sin(a) * 0.85;
            world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                    center.clone().add(hx, 0.15, hz),
                    1, 0.03, 0.05, 0.03, 0.005);
        }
        if (tick % 10 == 0) {
            world.spawnParticle(Particle.SOUL,
                    center.clone().add(0, 0.2, 0),
                    3, 0.4, 0.1, 0.4, 0.01);
        }
    }

    // ── VOID ─────────────────────────────────────────────────────────────────
    // End portal and dragon breath — void / dark magic feel.
    private void renderVoid(Location center, World world) {
        if (tick % 3 == 0) {
            world.spawnParticle(Particle.PORTAL,
                    center.clone().add(
                            (random.nextDouble() - 0.5) * 1.5,
                            (random.nextDouble() - 0.5) * 1.5,
                            (random.nextDouble() - 0.5) * 1.5),
                    1, 0, 0, 0, 0.05);
        }
        double angle = tick * 0.15;
        for (int i = 0; i < 3; i++) {
            double a  = angle + (i * Math.PI * 2 / 3.0);
            double hx = Math.cos(a) * 0.6;
            double hz = Math.sin(a) * 0.6;
            world.spawnParticle(Particle.DRAGON_BREATH,
                    center.clone().add(hx, 0, hz),
                    1, 0, 0, 0, 0.01);
        }
    }

    // ── HEARTS ───────────────────────────────────────────────────────────────
    // Floating hearts — romantic / cute feel.
    private void renderHearts(Location center, World world) {
        if (tick % 15 == 0) {
            world.spawnParticle(Particle.HEART,
                    center.clone().add(
                            (random.nextDouble() - 0.5) * 1.0,
                            0.2,
                            (random.nextDouble() - 0.5) * 1.0),
                    1, 0, 0, 0, 0);
        }
        double angle = tick * 0.12;
        for (int i = 0; i < 2; i++) {
            double a  = angle + (i * Math.PI);
            double hx = Math.cos(a) * 0.7;
            double hz = Math.sin(a) * 0.7;
            double hy = Math.sin(tick * 0.1 + i) * 0.3;
            world.spawnParticle(Particle.CHERRY_LEAVES,
                    center.clone().add(hx, hy, hz),
                    1, 0, 0, 0, 0);
        }
    }

    // ── CHERRY_BLOSSOM ──────────────────────────────────────────────────────────
    private void renderCherryBlossom(Location center, World world) {
        if (tick % 5 == 0) {
            double hx = ThreadLocalRandom.current().nextDouble(-1.2, 1.2);
            double hy = ThreadLocalRandom.current().nextDouble(1.0, 2.5);
            double hz = ThreadLocalRandom.current().nextDouble(-1.2, 1.2);
            world.spawnParticle(Particle.CHERRY_LEAVES,
                    center.clone().add(hx, hy, hz),
                    1, 0, 0, 0, 0);
        }
    }

    // ── SCULK ───────────────────────────────────────────────────────────────
    private void renderSculk(Location center, World world) {
        if (tick % 6 == 0) {
            double hx = ThreadLocalRandom.current().nextDouble(-0.7, 0.7);
            double hz = ThreadLocalRandom.current().nextDouble(-0.7, 0.7);
            world.spawnParticle(Particle.SCULK_SOUL,
                    center.clone().add(hx, 0.2, hz),
                    1, 0.1, 0.3, 0.1, 0.05);
        }
        if (tick % 20 == 0) {
            world.spawnParticle(Particle.SCULK_CHARGE,
                    center.clone().add(0, 0.5, 0),
                    1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    // ── ELECTRIC ────────────────────────────────────────────────────────────
    private void renderElectric(Location center, World world) {
        if (tick % 3 == 0) {
            double hx = ThreadLocalRandom.current().nextDouble(-0.8, 0.8);
            double hy = ThreadLocalRandom.current().nextDouble(0.0, 1.5);
            double hz = ThreadLocalRandom.current().nextDouble(-0.8, 0.8);
            world.spawnParticle(Particle.ELECTRIC_SPARK,
                    center.clone().add(hx, hy, hz),
                    2, 0.0, 0.0, 0.0, 0.1);
        }
    }

    // ── WATER ───────────────────────────────────────────────────────────────
    private void renderWater(Location center, World world) {
        if (tick % 4 == 0) {
            double hx = ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
            double hz = ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP,
                    center.clone().add(hx, 0.1, hz),
                    1, 0, 0.5, 0, 0.1);
            world.spawnParticle(Particle.SPLASH,
                    center.clone().add(hx, 1.5, hz),
                    3, 0.2, 0, 0.2, 0);
        }
    }

    // ── HONEY ───────────────────────────────────────────────────────────────
    private void renderHoney(Location center, World world) {
        if (tick % 8 == 0) {
            double hx = ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
            double hy = ThreadLocalRandom.current().nextDouble(1.0, 1.8);
            double hz = ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
            world.spawnParticle(Particle.FALLING_HONEY,
                    center.clone().add(hx, hy, hz),
                    1, 0, 0, 0, 0);
        }
    }
}
