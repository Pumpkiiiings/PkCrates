package com.pumpkings.pkcrates.core.animation.impl;

import com.pumpkings.pkcrates.core.animation.AnimationPhase;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.session.CrateSession;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GalaxyAnimation — ítems viajan en brazos espirales galácticos que rotan,
 * luego la galaxia colapsa en una nova y el winner nace de la explosión
 * de supernova.
 *
 * Fases:
 *  0–79   : ítems en brazos espirales (ángulo + radio creciente → curva aritmédica)
 *  80–99  : colapso rápido hacia el núcleo con partículas de polvo estelar
 *  100    : supernova (EXPLOSION_EMITTER + SCULK_SOUL + TOTEM_OF_UNDYING) y winner emerge
 *  101–150: winner flota en nebulosa de partículas pastel
 */
public class GalaxyAnimation implements AnimationPhase {

    private static final int MAX_TICKS = 155;

    private final List<ArmItem> armItems = new ArrayList<>();
    private ItemDisplay winnerDisplay;
    private final Random random = new Random();

    /** Un ítem en el brazo espiral de la galaxia. */
    private static class ArmItem {
        ItemDisplay display;
        double baseAngle;   // ángulo inicial en el brazo
        double armOffset;   // desplazamiento de brazo (0 o PI)
        double radius;      // radio al que pertenece
        double vertOff;     // altura relativa al centro

        ArmItem(ItemDisplay display, double baseAngle, double armOffset, double radius, double vertOff) {
            this.display   = display;
            this.baseAngle = baseAngle;
            this.armOffset = armOffset;
            this.radius    = radius;
            this.vertOff   = vertOff;
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.3f);

        List<IReward> all = session.getCrate().getRewards();
        if (all.isEmpty()) return;

        // 10 ítems: 5 por brazo, radios escalonados (espiral aritmédica)
        for (int arm = 0; arm < 2; arm++) {
            for (int i = 0; i < 5; i++) {
                IReward r = all.get(random.nextInt(all.size()));
                ItemDisplay d = spawnDisplay(center, r.getPreviewItem(), 0.38f);

                double armOffset = arm * Math.PI;
                double t         = (i + 1) / 5.0;            // 0.2..1.0
                double radius    = 0.4 + t * 1.4;             // 0.6..1.8
                double baseAngle = t * Math.PI * 1.2 + armOffset; // espiral

                double vertOff = (random.nextDouble() - 0.5) * 0.4; // plano fino

                armItems.add(new ArmItem(d, baseAngle, armOffset, radius, vertOff));
            }
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);

        /* ── Fase 1 (0-79): rotación galáctica ────────────────────────── */
        if (ticks < 80) {
            double globalRotation = ticks * 0.04;  // rotación lenta y constante

            for (ArmItem ai : armItems) {
                double a = ai.baseAngle + globalRotation;
                double x = Math.cos(a) * ai.radius;
                double z = Math.sin(a) * ai.radius;
                Location pos = center.clone().add(x, ai.vertOff, z);
                ai.display.teleport(pos);

                // Polvo estelar dorado
                if (ticks % 3 == 0) {
                    center.getWorld().spawnParticle(Particle.DUST,
                            pos, 1,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 200, 80), 0.8f));
                }
            }

            // Núcleo de la galaxia pulsando
            if (ticks % 5 == 0) {
                center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 4, 0.15, 0.05, 0.15, 0.02);
            }

            // Música cósmica
            if (ticks % 15 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.5f, 0.5f + (ticks / 80.0f));
            }
        }

        /* ── Fase 2 (80-99): colapso hacia el núcleo ──────────────────── */
        if (ticks >= 80 && ticks < 100) {
            double progress = (ticks - 80) / 20.0; // 0.0..1.0

            for (ArmItem ai : armItems) {
                double a      = ai.baseAngle + ticks * 0.04;
                double shrink = ai.radius * (1.0 - progress);
                double x      = Math.cos(a) * shrink;
                double z      = Math.sin(a) * shrink;
                double y      = ai.vertOff * (1.0 - progress);
                ai.display.teleport(center.clone().add(x, y, z));

                // Trazas de colapso
                center.getWorld().spawnParticle(Particle.DUST,
                        center.clone().add(x, y, z), 2,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 80, 255), 0.6f));
            }

            // Pitido de colapso
            if (ticks % 7 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.4f + (float) progress * 2.0f);
            }
        }

        /* ── Fase 3 (100): supernova ───────────────────────────────────── */
        if (ticks == 100) {
            for (ArmItem ai : armItems) {
                if (ai.display.isValid()) ai.display.remove();
            }
            armItems.clear();

            // Supernova visual
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
            center.getWorld().spawnParticle(Particle.SCULK_SOUL, center, 80, 1.0, 1.0, 1.0, 0.25);
            center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 100, 0.8, 0.8, 0.8, 0.4);
            center.getWorld().spawnParticle(Particle.DUST,
                    center, 60,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 200, 80), 2.0f));

            center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.3f);
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

            // Winner nace del centro
            winnerDisplay = spawnDisplay(center, session.getWonReward().getPreviewItem(), 0f);
            Transformation t = winnerDisplay.getTransformation();
            t.getScale().set(new Vector3f(1.7f, 1.7f, 1.7f));
            winnerDisplay.setTransformation(t);
            winnerDisplay.setTeleportDuration(30);
            winnerDisplay.teleport(center.clone().add(0, 0.6, 0));
        }

        /* ── Fase 4 (101-155): winner en nebulosa ─────────────────────── */
        if (ticks > 100 && ticks < MAX_TICKS) {
            if (winnerDisplay == null || !winnerDisplay.isValid()) return;

            Location winLoc = winnerDisplay.getLocation();

            // Nebulosa en anillo alrededor del winner
            double nebAngle = ticks * 0.18;
            for (int k = 0; k < 6; k++) {
                double a  = nebAngle + (k * Math.PI / 3.0);
                double hx = Math.cos(a) * 0.55;
                double hz = Math.sin(a) * 0.55;
                // Colores alternos morado/dorado
                org.bukkit.Color c = k % 2 == 0
                        ? org.bukkit.Color.fromRGB(180, 80, 255)
                        : org.bukkit.Color.fromRGB(255, 200, 80);
                center.getWorld().spawnParticle(Particle.DUST,
                        winLoc.clone().add(hx, 0, hz), 1,
                        new Particle.DustOptions(c, 1.0f));
            }

            // Chispas doradas flotantes
            if (ticks % 4 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD,
                        winLoc, 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        for (ArmItem ai : armItems) {
            if (ai.display != null && ai.display.isValid()) ai.display.remove();
        }
        armItems.clear();
        if (winnerDisplay != null && winnerDisplay.isValid()) winnerDisplay.remove();
    }

    // -----------------------------------------------------------------------

    private ItemDisplay spawnDisplay(Location loc, ItemStack item, float scale) {
        ItemDisplay d = (ItemDisplay) loc.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        d.setBillboard(Display.Billboard.CENTER);
        d.setItemStack(item != null ? item : new ItemStack(org.bukkit.Material.PAPER));
        Transformation t = d.getTransformation();
        t.getScale().set(new Vector3f(scale, scale, scale));
        d.setTransformation(t);
        d.setTeleportDuration(1);
        return d;
    }
}
