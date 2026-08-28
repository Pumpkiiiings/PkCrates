package com.pumpkings.pkcrates.core.animation.impl;

import com.pumpkings.pkcrates.core.animation.AnimationPhase;
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

public class MeteorAnimation implements AnimationPhase {

    private final int MAX_TICKS = 100;
    private ItemDisplay meteorDisplay;
    private ItemDisplay winnerDisplay;

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        
        // Spawn winner hidden
        winnerDisplay = (ItemDisplay) center.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);
        winnerDisplay.setBillboard(Display.Billboard.CENTER);
        ItemStack winnerItem = session.getWonReward().getPreviewItem();
        winnerDisplay.setItemStack(winnerItem != null ? winnerItem : new ItemStack(org.bukkit.Material.PAPER));
        
        Transformation t = winnerDisplay.getTransformation();
        t.getScale().set(new Vector3f(1.5f, 1.5f, 1.5f));
        winnerDisplay.setTransformation(t);
        winnerDisplay.teleport(center.clone().subtract(0, 3, 0)); // Hidden
        
        // Meteor high in the sky
        Location meteorLoc = center.clone().add(0, 20, 0);
        meteorDisplay = (ItemDisplay) center.getWorld().spawnEntity(meteorLoc, EntityType.ITEM_DISPLAY);
        meteorDisplay.setBillboard(Display.Billboard.CENTER);
        meteorDisplay.setItemStack(new ItemStack(org.bukkit.Material.MAGMA_BLOCK));
        
        Transformation mt = meteorDisplay.getTransformation();
        mt.getScale().set(new Vector3f(2.5f, 2.5f, 2.5f));
        meteorDisplay.setTransformation(mt);
        meteorDisplay.setTeleportDuration(1);
    }

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);

        // Phase 1: Warning
        if (ticks < 40) {
            if (ticks % 10 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                // Draw laser
                for (int y = 0; y < 20; y++) {
                    center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, y, 0), 2, 0.1, 0.1, 0.1, 0);
                }
            }
        }
        
        // Phase 2: Falling
        if (ticks >= 40 && ticks < 60) {
            double progress = (ticks - 40) / 20.0; // 0.0 to 1.0
            double currentY = 20 - (20 * progress);
            Location target = center.clone().add(0, currentY, 0);
            
            if (meteorDisplay.isValid()) {
                meteorDisplay.teleport(target);
                // Meteor trail
                center.getWorld().spawnParticle(Particle.LAVA, target, 5, 0.5, 0.5, 0.5, 0);
                center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, target, 2, 0.5, 0.5, 0.5, 0);
            }
            
            if (ticks % 5 == 0) {
                center.getWorld().playSound(center, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.5f);
            }
        }
        
        // Phase 3: Impact
        if (ticks == 60) {
            if (meteorDisplay.isValid()) meteorDisplay.remove();
            
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f); // Deep boom
            center.getWorld().playSound(center, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
            
            center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 2);
            center.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 50, 2.0, 1.0, 2.0, 0.05);
            center.getWorld().spawnParticle(Particle.FLAME, center, 50, 1.0, 1.0, 1.0, 0.2);
            
            // Bring winner up
            if (winnerDisplay.isValid()) {
                winnerDisplay.teleport(center);
                winnerDisplay.setTeleportDuration(20);
                winnerDisplay.teleport(center.clone().add(0, 0.5, 0));
            }
        }
        
        // Phase 4: Sizzling
        if (ticks > 60 && ticks < 100) {
            if (ticks % 10 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.0f);
            }
            center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 0.5, 0), 2, 0.5, 0.5, 0.5, 0);
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        if (meteorDisplay != null && meteorDisplay.isValid()) meteorDisplay.remove();
        if (winnerDisplay != null && winnerDisplay.isValid()) winnerDisplay.remove();
    }
}
