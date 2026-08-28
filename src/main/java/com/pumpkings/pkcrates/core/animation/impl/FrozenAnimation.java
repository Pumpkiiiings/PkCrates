package com.pumpkings.pkcrates.core.animation.impl;

import com.pumpkings.pkcrates.core.animation.AnimationPhase;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.session.CrateSession;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * FrozenAnimation — cristales de hielo rodean la caja congelando todo,
 * el tiempo se detiene con una esfera de hielo y el winner emerge rompiendo
 * el bloque desde adentro.
 *
 * Fases:
 *  0–29   : ítems flotando hacia afuera mientras se forman cristales de hielo
 *  30–59  : ítems congelados en una esfera, lluvia de copos (SNOWFLAKE)
 *  60–69  : la esfera se aprieta (freeze final), todo cruje
 *  70     : explosión de hielo (SNOWFLAKE masivo + cracks) y winner emerge
 *  71–130 : winner flota rodeado de copos de nieve suaves
 */
public class FrozenAnimation implements AnimationPhase {

    private static final int MAX_TICKS = 130;

    private final List<IceItem> iceItems = new ArrayList<>();
    private ItemDisplay winnerDisplay;
    private final Random random = new Random();

    /** Ítem congelado: posición fija en la esfera de hielo. */
    private static class IceItem {
        ItemDisplay display;
        double theta; // latitud
        double phi;   // longitud
        double radius;

        IceItem(ItemDisplay display, double theta, double phi, double radius) {
            this.display = display;
            this.theta   = theta;
            this.phi     = phi;
            this.radius  = radius;
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        center.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 1.0f, 0.5f);
        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.3f);

        List<IReward> all = session.getCrate().getRewards();
        if (all.isEmpty()) return;

        // 9 ítems distribuidos en coordenadas esféricas
        int count = 9;
        for (int i = 0; i < count; i++) {
            IReward r = all.get(random.nextInt(all.size()));
            ItemDisplay d = spawnDisplay(center, r.getPreviewItem(), 0.35f);

            // Distribución uniforme en una esfera (Fibonacci sphere)
            double theta = Math.acos(1 - 2.0 * (i + 0.5) / count);
            double phi   = Math.PI * (1 + Math.sqrt(5)) * i;
            double radius = 1.3 + random.nextDouble() * 0.3;

            iceItems.add(new IceItem(d, theta, phi, radius));
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);

        /* ── Fase 1 (0-29): ítems salen del centro expandiéndose ─────── */
        if (ticks < 30) {
            double progress = ticks / 29.0; // 0..1

            for (IceItem ii : iceItems) {
                double r = ii.radius * progress;
                double x = Math.sin(ii.theta) * Math.cos(ii.phi) * r;
                double y = Math.cos(ii.theta) * r;
                double z = Math.sin(ii.theta) * Math.sin(ii.phi) * r;
                ii.display.teleport(center.clone().add(x, y, z));

                // Cristales de hielo emanando
                if (ticks % 3 == 0) {
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            center.clone().add(x, y, z), 2, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Viento helado
            if (ticks % 8 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.7f);
                center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 8, 1.5, 0.5, 1.5, 0.05);
            }
        }

        /* ── Fase 2 (30-59): ítems congelados, lluvia de cristales ────── */
        if (ticks >= 30 && ticks < 60) {
            // Ítems estáticos en la esfera (ya están posicionados)
            // Pequeño balanceo ≈ 0.05 bloques para que parezca "vivo"
            for (IceItem ii : iceItems) {
                double wobble = Math.sin(ticks * 0.3 + ii.phi) * 0.05;
                double x = Math.sin(ii.theta) * Math.cos(ii.phi) * (ii.radius + wobble);
                double y = Math.cos(ii.theta) * (ii.radius + wobble);
                double z = Math.sin(ii.theta) * Math.sin(ii.phi) * (ii.radius + wobble);
                ii.display.teleport(center.clone().add(x, y, z));

                if (ticks % 4 == 0) {
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            center.clone().add(x, y, z), 1, 0.05, 0.05, 0.05, 0);
                }
            }

            // Lluvia de copos desde arriba
            if (ticks % 5 == 0) {
                for (int s = 0; s < 6; s++) {
                    Location snow = center.clone().add(
                            (random.nextDouble() - 0.5) * 3,
                            3.0,
                            (random.nextDouble() - 0.5) * 3);
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, snow, 2, 0.2, 0.1, 0.2, 0.005);
                }
            }

            // Crujidos de hielo
            if (ticks % 12 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f + random.nextFloat() * 0.5f);
            }
        }

        /* ── Fase 3 (60-69): freeze final, esfera se contrae ──────────── */
        if (ticks >= 60 && ticks < 70) {
            double squeeze = 1.0 - ((ticks - 60) / 9.0) * 0.4; // 1.0 → 0.6

            for (IceItem ii : iceItems) {
                double r = ii.radius * squeeze;
                double x = Math.sin(ii.theta) * Math.cos(ii.phi) * r;
                double y = Math.cos(ii.theta) * r;
                double z = Math.sin(ii.theta) * Math.sin(ii.phi) * r;
                ii.display.teleport(center.clone().add(x, y, z));
            }

            // Crujido intenso
            if (ticks % 4 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 12, 0.8, 0.8, 0.8, 0.05);
            }
        }

        /* ── Fase 4 (70): estallido de hielo y reveal ─────────────────── */
        if (ticks == 70) {
            for (IceItem ii : iceItems) {
                if (ii.display.isValid()) ii.display.remove();
            }
            iceItems.clear();

            // Explosión de hielo
            center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 200, 1.5, 1.5, 1.5, 0.2);
            center.getWorld().spawnParticle(Particle.ITEM,
                    center, 80, 0.6, 0.6, 0.6, 0.3,
                    new ItemStack(Material.ICE));
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);

            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.8f);
            center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

            // Winner surge del corazón de hielo
            Location spawn = center.clone().add(0, -1.5, 0); // debajo
            winnerDisplay = spawnDisplay(center, session.getWonReward().getPreviewItem(), 0f);
            winnerDisplay.teleport(spawn);

            Transformation t = winnerDisplay.getTransformation();
            t.getScale().set(new Vector3f(1.6f, 1.6f, 1.6f));
            winnerDisplay.setTransformation(t);
            winnerDisplay.setTeleportDuration(28);
            winnerDisplay.teleport(center.clone().add(0, 0.5, 0));
        }

        /* ── Fase 5 (71-130): winner flotando en nieve suave ──────────── */
        if (ticks > 70 && ticks < MAX_TICKS) {
            if (winnerDisplay == null || !winnerDisplay.isValid()) return;

            Location winLoc = winnerDisplay.getLocation();

            // Anillo de copos girando
            if (ticks % 2 == 0) {
                double haloA = ticks * 0.22;
                for (int k = 0; k < 5; k++) {
                    double a  = haloA + (k * Math.PI * 2 / 5.0);
                    double hx = Math.cos(a) * 0.55;
                    double hz = Math.sin(a) * 0.55;
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            winLoc.clone().add(hx, 0, hz), 1, 0, 0, 0, 0);
                }
            }

            // Partículas de cristal flotando hacia arriba
            if (ticks % 6 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD,
                        winLoc, 2, 0.25, 0.15, 0.25, 0.01);
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
        for (IceItem ii : iceItems) {
            if (ii.display != null && ii.display.isValid()) ii.display.remove();
        }
        iceItems.clear();
        if (winnerDisplay != null && winnerDisplay.isValid()) winnerDisplay.remove();
    }

    // -----------------------------------------------------------------------

    private ItemDisplay spawnDisplay(Location loc, ItemStack item, float scale) {
        ItemDisplay d = (ItemDisplay) loc.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        d.setBillboard(Display.Billboard.CENTER);
        d.setItemStack(item != null ? item : new ItemStack(Material.PAPER));
        Transformation t = d.getTransformation();
        t.getScale().set(new Vector3f(scale, scale, scale));
        d.setTransformation(t);
        d.setTeleportDuration(1);
        return d;
    }
}
