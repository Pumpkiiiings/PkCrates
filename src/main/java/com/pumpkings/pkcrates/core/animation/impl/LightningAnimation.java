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
 * LightningAnimation — relámpagos caen sobre la caja, ítems orbitan
 * electrizados, columna de luz revela al ganador.
 *
 * Fases:
 *  0–39   : ítems orbitan con chispas eléctricas, truenos aleatorios
 *  40–59  : relámpagos consecutivos (strikeLightningEffect) en la caja
 *  60     : todos los ítems explotan y colapsan, sonido épico
 *  61–120 : columna de END_ROD + winner aparece flotando con halo eléctrico
 */
public class LightningAnimation implements AnimationPhase {

    private static final int MAX_TICKS = 130;

    private final List<ChainItem> orbitItems = new ArrayList<>();
    private ItemDisplay winnerDisplay;
    private final Random random = new Random();

    /** Representa un ítem orbitando con velocidad y offset propios. */
    private static class ChainItem {
        ItemDisplay display;
        double angle;
        double radius;
        double height;
        double speed;

        ChainItem(ItemDisplay display, double angle, double radius, double height, double speed) {
            this.display  = display;
            this.angle    = angle;
            this.radius   = radius;
            this.height   = height;
            this.speed    = speed;
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);

        List<IReward> all = session.getCrate().getRewards();
        if (all.isEmpty()) return;

        // 8 ítems orbitando en dos anillos concéntricos
        for (int i = 0; i < 8; i++) {
            IReward r = all.get(random.nextInt(all.size()));
            ItemDisplay d = spawnDisplay(center, r.getPreviewItem(), 0.35f);

            double angle  = (Math.PI * 2 / 8.0) * i;
            double radius = i % 2 == 0 ? 1.1 : 1.6;
            double height = i % 2 == 0 ? 0.2 : 0.9;
            double speed  = 0.07 + random.nextDouble() * 0.05;

            orbitItems.add(new ChainItem(d, angle, radius, height, speed));
        }
    }

    // -----------------------------------------------------------------------

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);

        /* ── Fase 1 (0-39): órbita eléctrica ─────────────────────────── */
        if (ticks < 40) {
            for (ChainItem ci : orbitItems) {
                ci.angle += ci.speed;
                double x = Math.cos(ci.angle) * ci.radius;
                double z = Math.sin(ci.angle) * ci.radius;
                Location pos = center.clone().add(x, ci.height, z);
                ci.display.teleport(pos);

                // Chispa eléctrica cada 2 ticks
                if (ticks % 2 == 0) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.05, 0.05, 0.05, 0);
                }
            }

            // Trueno de advertencia
            if (ticks % 13 == 0) {
                center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.5f + random.nextFloat() * 0.5f);
            }

            // Partículas de tormenta encima
            if (ticks % 4 == 0) {
                Location sky = center.clone().add(0, 8, 0);
                center.getWorld().spawnParticle(Particle.CLOUD, sky, 3, 1.5, 0.2, 1.5, 0);
            }
        }

        /* ── Fase 2 (40-59): relámpagos al crate ──────────────────────── */
        if (ticks >= 40 && ticks < 60) {
            // Velocidad orbital aumenta con el caos
            for (ChainItem ci : orbitItems) {
                ci.angle += ci.speed * 3.5;
                double x = Math.cos(ci.angle) * ci.radius;
                double z = Math.sin(ci.angle) * ci.radius;
                ci.display.teleport(center.clone().add(x, ci.height, z));
            }

            // Relámpago cada 5 ticks
            if (ticks % 5 == 0) {
                // Pequeña variación alrededor del centro
                Location strikePos = center.clone().add(
                        (random.nextDouble() - 0.5) * 0.8, 0,
                        (random.nextDouble() - 0.5) * 0.8);
                center.getWorld().strikeLightningEffect(strikePos);
                center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.2f);
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 25, 0.6, 0.6, 0.6, 0.1);
            }
        }

        /* ── Fase 3 (60): colapso y reveal ────────────────────────────── */
        if (ticks == 60) {
            // Destruir todos los ítems con explosión de chispas
            for (ChainItem ci : orbitItems) {
                if (ci.display.isValid()) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ci.display.getLocation(), 15, 0.1, 0.1, 0.1, 0.2);
                    ci.display.remove();
                }
            }
            orbitItems.clear();

            // Mega explosión eléctrica
            center.getWorld().strikeLightningEffect(center);
            center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 0.5f);
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center, 150, 1.0, 1.5, 1.0, 0.3);
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);

            // Winner emerge desde arriba con columna de luz
            Location above = center.clone().add(0, 5, 0);
            winnerDisplay = spawnDisplay(center, session.getWonReward().getPreviewItem(), 0f);
            winnerDisplay.teleport(above);

            // Escala de 0 → 1.8 suavemente
            Transformation t = winnerDisplay.getTransformation();
            t.getScale().set(new Vector3f(1.8f, 1.8f, 1.8f));
            winnerDisplay.setTransformation(t);
            winnerDisplay.setTeleportDuration(25);
            winnerDisplay.teleport(center.clone().add(0, 0.6, 0));
        }

        /* ── Fase 4 (61-130): halo eléctrico alrededor del winner ──────── */
        if (ticks > 60 && ticks < MAX_TICKS) {
            if (winnerDisplay == null || !winnerDisplay.isValid()) return;

            Location winLoc = winnerDisplay.getLocation();

            // Columna de luz (END_ROD)
            if (ticks % 3 == 0) {
                for (int y = 0; y < 6; y++) {
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            center.clone().add(0, y * 0.5, 0), 1, 0.1, 0, 0.1, 0.02);
                }
            }

            // Chispas orbitando al winner
            double haloAngle = ticks * 0.25;
            for (int k = 0; k < 3; k++) {
                double a = haloAngle + (k * Math.PI * 2 / 3.0);
                double hx = Math.cos(a) * 0.6;
                double hz = Math.sin(a) * 0.6;
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        winLoc.clone().add(hx, 0, hz), 1, 0, 0, 0, 0);
            }

            // Sonido eléctrico suave cada 20 ticks
            if (ticks % 20 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 2.0f);
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
        for (ChainItem ci : orbitItems) {
            if (ci.display != null && ci.display.isValid()) ci.display.remove();
        }
        orbitItems.clear();
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
