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

public class SpiralAnimation implements AnimationPhase {

    private final int MAX_TICKS = 120; // 6 seconds total
    private final List<ItemDisplay> displays = new ArrayList<>();
    private ItemDisplay winnerDisplay;
    private final Random random = new Random();
    private double currentAngle = 0;
    
    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 0.5, 0.5);
        center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

        List<IReward> allRewards = session.getCrate().getRewards();
        if (allRewards.isEmpty()) return;

        // Create 3 spiral items
        for (int i = 0; i < 3; i++) {
            IReward r = allRewards.get(random.nextInt(allRewards.size()));
            ItemDisplay display = createDisplay(center, r.getPreviewItem(), 0.5f);
            displays.add(display);
        }

        // Winner item will be spawned later in onTick
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

        if (ticks < 90) {
            // Speed of rotation increases then decreases
            double speed = 0.2 + (Math.sin(ticks * Math.PI / 90.0) * 0.4);
            currentAngle += speed;
            
            // Height goes up slowly
            double heightOffset = (ticks / 90.0) * 2.0; 
            
            for (int i = 0; i < displays.size(); i++) {
                ItemDisplay display = displays.get(i);
                if (!display.isValid()) continue;
                
                double angleOffset = currentAngle + (i * (Math.PI * 2 / displays.size()));
                // Radius expands and then contracts slightly
                double radius = 1.0 - (ticks / 90.0) * 0.5;
                
                double x = Math.cos(angleOffset) * radius;
                double z = Math.sin(angleOffset) * radius;
                
                Location target = center.clone().add(x, heightOffset, z);
                display.teleport(target);
                
                if (ticks % 2 == 0) {
                    center.getWorld().spawnParticle(Particle.ENCHANT, target, 1, 0, 0, 0, 0);
                }
            }
            
            if (ticks % 10 == 0) {
                center.getWorld().playSound(center, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.3f, 1.0f + (ticks / 90.0f));
            }
        }
        
        // Tick 90: merge them and show winner
        if (ticks == 90) {
            Location highCenter = center.clone().add(0, 2.0, 0);
            for (ItemDisplay display : displays) {
                if (display.isValid()) display.remove();
            }
            displays.clear();
            
            center.getWorld().spawnParticle(Particle.EXPLOSION, highCenter, 1);
            center.getWorld().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.5f);
            
            winnerDisplay = createDisplay(center, session.getWonReward().getPreviewItem(), 1.0f);
            winnerDisplay.teleport(highCenter);
            
            // Animate winner coming down slowly
            winnerDisplay.setTeleportDuration(20);
            winnerDisplay.teleport(center.clone().add(0, 0.5, 0));
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        for (ItemDisplay display : displays) {
            if (display != null && display.isValid()) display.remove();
        }
        displays.clear();
        
        if (winnerDisplay != null && winnerDisplay.isValid()) {
            winnerDisplay.remove();
        }
    }
}
