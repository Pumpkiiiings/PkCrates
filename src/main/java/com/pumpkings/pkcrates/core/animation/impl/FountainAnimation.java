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
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FountainAnimation implements AnimationPhase {

    private final int MAX_TICKS = 100; // 5 seconds
    private final List<FountainItem> items = new ArrayList<>();
    private ItemDisplay winnerDisplay;
    private final Random random = new Random();

    private static class FountainItem {
        ItemDisplay display;
        Vector velocity;
        Location currentLoc;
        boolean onGround = false;
        
        FountainItem(ItemDisplay display, Vector velocity, Location currentLoc) {
            this.display = display;
            this.velocity = velocity;
            this.currentLoc = currentLoc;
        }
    }

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.0, 0.5);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 2);

        List<IReward> allRewards = session.getCrate().getRewards();
        if (allRewards.isEmpty()) return;

        // Spawn fake items
        for (int i = 0; i < 15; i++) {
            IReward r = allRewards.get(random.nextInt(allRewards.size()));
            ItemDisplay display = createDisplay(center, r.getPreviewItem(), 0.5f);
            
            // Random velocity upwards and outwards
            double vx = (random.nextDouble() - 0.5) * 0.4;
            double vy = 0.5 + random.nextDouble() * 0.5;
            double vz = (random.nextDouble() - 0.5) * 0.4;
            
            items.add(new FountainItem(display, new Vector(vx, vy, vz), center.clone()));
        }

        // Spawn winner item
        winnerDisplay = createDisplay(center, session.getWonReward().getPreviewItem(), 1.2f);
        // Hide winner initially by putting it below ground
        winnerDisplay.teleport(center.clone().subtract(0, 2, 0));
    }
    
    private ItemDisplay createDisplay(Location loc, ItemStack item, float scale) {
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        if (item != null) {
            display.setItemStack(item);
        } else {
            display.setItemStack(new ItemStack(org.bukkit.Material.PAPER));
        }
        Transformation transform = display.getTransformation();
        transform.getScale().set(new Vector3f(scale, scale, scale));
        display.setTransformation(transform);
        display.setTeleportDuration(1);
        return display;
    }

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.0, 0.5);

        // Physics for fake items
        if (ticks < 70) {
            for (FountainItem fi : items) {
                if (fi.onGround) continue;
                
                fi.currentLoc.add(fi.velocity);
                fi.velocity.setY(fi.velocity.getY() - 0.05); // Gravity
                
                // Stop if it hits the ground (approx block level)
                if (fi.currentLoc.getY() <= session.getBlockLocation().getY() + 0.1) {
                    fi.currentLoc.setY(session.getBlockLocation().getY() + 0.1);
                    fi.onGround = true;
                }
                
                if (fi.display.isValid()) {
                    fi.display.teleport(fi.currentLoc);
                }
            }
        }
        
        if (ticks % 5 == 0 && ticks < 40) {
            center.getWorld().playSound(center, Sound.ENTITY_CHICKEN_EGG, 0.5f, 1.0f + random.nextFloat());
        }

        // At tick 70, winner emerges
        if (ticks == 70) {
            // Remove fake items
            for (FountainItem fi : items) {
                if (fi.display.isValid()) {
                    fi.display.remove();
                    center.getWorld().spawnParticle(Particle.POOF, fi.currentLoc, 3, 0.1, 0.1, 0.1, 0);
                }
            }
            items.clear();
            
            // Winner flies up
            if (winnerDisplay != null && winnerDisplay.isValid()) {
                winnerDisplay.setTeleportDuration(20); // Smooth float up
                winnerDisplay.teleport(center.clone().add(0, 1.5, 0));
            }
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        if (ticks > 70 && ticks < 95) {
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1.5, 0), 2, 0.3, 0.3, 0.3, 0);
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        for (FountainItem fi : items) {
            if (fi.display != null && fi.display.isValid()) fi.display.remove();
        }
        items.clear();
        
        if (winnerDisplay != null && winnerDisplay.isValid()) {
            winnerDisplay.remove();
        }
    }
}
