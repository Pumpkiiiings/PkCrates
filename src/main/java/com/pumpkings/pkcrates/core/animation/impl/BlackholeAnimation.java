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

public class BlackholeAnimation implements AnimationPhase {

    private final int MAX_TICKS = 150; 
    private final List<OrbitItem> items = new ArrayList<>();
    private ItemDisplay winnerDisplay;
    private final Random random = new Random();

    private static class OrbitItem {
        ItemDisplay display;
        double angle;
        double radius;
        double heightOffset;
        double speedMult;
        
        OrbitItem(ItemDisplay display, double angle, double radius, double heightOffset, double speedMult) {
            this.display = display;
            this.angle = angle;
            this.radius = radius;
            this.heightOffset = heightOffset;
            this.speedMult = speedMult;
        }
    }

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.5f);

        List<IReward> allRewards = session.getCrate().getRewards();
        if (allRewards.isEmpty()) return;

        // Spawn fake items
        for (int i = 0; i < 12; i++) {
            IReward r = allRewards.get(random.nextInt(allRewards.size()));
            ItemDisplay display = createDisplay(center, r.getPreviewItem(), 0.4f);
            
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 1.0 + random.nextDouble() * 0.8;
            double heightOffset = (random.nextDouble() - 0.5) * 1.5;
            double speedMult = 0.5 + random.nextDouble() * 1.0;
            
            items.add(new OrbitItem(display, angle, radius, heightOffset, speedMult));
        }

        // Winner item will be spawned later in onTick
    }
    
    private ItemDisplay createDisplay(Location loc, ItemStack item, float scale) {
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        if (item != null) display.setItemStack(item);
        else display.setItemStack(new ItemStack(org.bukkit.Material.PAPER));
        
        Transformation transform = display.getTransformation();
        transform.getScale().set(new Vector3f(scale, scale, scale));
        display.setTransformation(transform);
        display.setTeleportDuration(1);
        return display;
    }

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);

        // Phase 1 & 2: Orbit and acceleration
        if (ticks < 100) {
            double globalSpeed = 0.1 + (ticks / 100.0) * 0.8; // Accelerates
            
            for (OrbitItem oi : items) {
                oi.angle += globalSpeed * oi.speedMult;
                
                // Slowly suck into center horizontally as time goes on
                double currentRadius = oi.radius * (1.0 - (ticks / 120.0));
                
                double x = Math.cos(oi.angle) * currentRadius;
                double z = Math.sin(oi.angle) * currentRadius;
                
                Location target = center.clone().add(x, oi.heightOffset, z);
                oi.display.teleport(target);
                
                if (random.nextDouble() < 0.2) {
                    center.getWorld().spawnParticle(Particle.PORTAL, target, 1, 0, 0, 0, 0);
                }
            }
            
            if (ticks % 10 == 0) {
                center.getWorld().playSound(center, Sound.ENTITY_MINECART_RIDING, 0.5f, 0.5f + (ticks / 50.0f));
            }
            if (ticks > 50 && ticks % 5 == 0) {
                center.getWorld().spawnParticle(Particle.SMOKE, center, 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
        
        // Phase 3: Collapse
        if (ticks >= 100 && ticks < 110) {
            double progress = (ticks - 100) / 10.0; // 0.0 to 1.0
            
            for (OrbitItem oi : items) {
                if (!oi.display.isValid()) continue;
                
                double currentRadius = oi.radius * (1.0 - progress);
                double x = Math.cos(oi.angle) * currentRadius;
                double z = Math.sin(oi.angle) * currentRadius;
                double currentHeight = oi.heightOffset * (1.0 - progress);
                
                Location target = center.clone().add(x, currentHeight, z);
                oi.display.teleport(target);
            }
            center.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 10, 0.5, 0.5, 0.5, 0.1);
        }
        
        // Phase 4: Explosion & Winner
        if (ticks == 110) {
            for (OrbitItem oi : items) {
                if (oi.display.isValid()) oi.display.remove();
            }
            items.clear();
            
            center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.8f);
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
            center.getWorld().spawnParticle(Particle.SCULK_SOUL, center, 50, 0.5, 0.5, 0.5, 0.2);
            
            winnerDisplay = createDisplay(center, session.getWonReward().getPreviewItem(), 1.5f);
            winnerDisplay.teleport(center);
            winnerDisplay.setTeleportDuration(20);
            winnerDisplay.teleport(center.clone().add(0, 1.0, 0));
        }
        
        if (ticks > 110 && ticks < 140) {
            if (ticks % 5 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1.0, 0), 3, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        for (OrbitItem oi : items) {
            if (oi.display != null && oi.display.isValid()) oi.display.remove();
        }
        items.clear();
        if (winnerDisplay != null && winnerDisplay.isValid()) winnerDisplay.remove();
    }
}
