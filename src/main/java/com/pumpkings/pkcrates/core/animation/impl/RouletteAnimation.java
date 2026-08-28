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
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RouletteAnimation implements AnimationPhase {

    private final int MAX_TICKS = 140; // 7 seconds total
    private final List<ItemDisplay> displays = new ArrayList<>();
    private final List<IReward> displayRewards = new ArrayList<>();
    private final double RADIUS = 1.2;
    private double currentAngle = 0;
    private double speed = Math.PI / 4; // Starts fast (quarter circle per tick)
    private final double DECELERATION = 0.96; // Friction per tick
    
    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        
        List<IReward> allRewards = session.getCrate().getRewards();
        if (allRewards.isEmpty()) return;

        // Choose 8 rewards to display in the circle
        for (int i = 0; i < 8; i++) {
            IReward r;
            if (i == 0) {
                // Guarantee the winning reward is exactly at index 0
                r = session.getWonReward();
            } else {
                r = allRewards.get((int) (Math.random() * allRewards.size()));
            }
            displayRewards.add(r);

            ItemDisplay display = (ItemDisplay) center.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            
            ItemStack preview = r.getPreviewItem();
            if (preview != null) {
                display.setItemStack(preview);
            } else {
                display.setItemStack(new ItemStack(org.bukkit.Material.PAPER));
            }

            // Small scale for the ring items
            Transformation transform = display.getTransformation();
            transform.getScale().set(new Vector3f(0.5f, 0.5f, 0.5f));
            display.setTransformation(transform);
            
            // Set 1 tick teleport duration for smooth movement client-side
            display.setTeleportDuration(1);
            
            displays.add(display);
        }
    }

    @Override
    public void onTick(CrateSession session) {
        if (displays.isEmpty()) {
            session.setFinished(true);
            return;
        }

        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        int ticks = session.getTicksLived();

        // Slow down the speed
        if (ticks > 20) {
            speed *= DECELERATION;
        }
        
        // Prevent complete stop too early
        if (ticks > 120 && speed < 0.01) {
            speed = 0;
        }

        currentAngle += speed;

        for (int i = 0; i < displays.size(); i++) {
            ItemDisplay display = displays.get(i);
            
            // Calculate angle for this specific item (evenly spaced)
            double offsetAngle = currentAngle + (i * (Math.PI * 2 / displays.size()));
            
            double x = Math.cos(offsetAngle) * RADIUS;
            double z = Math.sin(offsetAngle) * RADIUS;
            
            Location targetLoc = center.clone().add(x, 0, z);
            
            // If it's the winner (index 0) and the roulette is almost stopping (speed < 0.05), move it to center
            if (i == 0 && speed < 0.05) {
                // Lerp towards center
                double lerpFactor = (0.05 - speed) / 0.05; // 0 to 1
                double lerpX = x * (1 - lerpFactor);
                double lerpZ = z * (1 - lerpFactor);
                targetLoc = center.clone().add(lerpX, lerpFactor * 0.5, lerpZ); // Goes slightly up
                
                Transformation transform = display.getTransformation();
                float scale = 0.5f + (float)(lerpFactor * 0.5f); // Grows to 1.0f
                transform.getScale().set(new Vector3f(scale, scale, scale));
                display.setTransformation(transform);
                
                // Sound logic for winner slowing down
                if (ticks % 10 == 0 && speed > 0.01) {
                    center.getWorld().playSound(center, Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                }
            }
            
            display.teleport(targetLoc);
            
            // Particle trail
            if (speed > 0.1 && i == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 1, 0, 0, 0, 0);
            }
        }

        // Ticking sound fast at first
        if (speed > 0.1 && ticks % 3 == 0) {
            center.getWorld().playSound(center, Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        
        // Final celebration!
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0, 0.5, 0), 50, 0.5, 0.5, 0.5, 0.1);
        
        for (ItemDisplay display : displays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
    }
}
