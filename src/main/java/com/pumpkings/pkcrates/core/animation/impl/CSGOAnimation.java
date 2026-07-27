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

import java.util.List;

public class CSGOAnimation implements AnimationPhase {

    private final int MAX_TICKS = 100; // 5 seconds
    private ItemDisplay display;
    private int cycleDelay = 2; // Starts fast (change every 2 ticks)
    private int nextCycleTick = 0;
    
    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        
        display = (ItemDisplay) center.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        
        Transformation transform = display.getTransformation();
        transform.getScale().set(new Vector3f(0.8f, 0.8f, 0.8f));
        display.setTransformation(transform);
        display.setTeleportDuration(1);
        
        center.getWorld().playSound(center, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    @Override
    public void onTick(CrateSession session) {
        if (display == null || display.isDead()) {
            session.setFinished(true);
            return;
        }

        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        List<IReward> allRewards = session.getCrate().getRewards();

        if (allRewards.isEmpty()) {
            session.setFinished(true);
            return;
        }

        // Increase the delay (visual scroll friction) gradually
        if (ticks > 40) cycleDelay = 4;
        if (ticks > 70) cycleDelay = 8;
        if (ticks > 85) cycleDelay = 15;

        // Animate item change
        if (ticks >= nextCycleTick && ticks < 90) { // Continues changing until tick 90
            IReward randomReward = allRewards.get((int) (Math.random() * allRewards.size()));
            ItemStack preview = randomReward.getPreviewItem();
            
            if (preview != null) {
                display.setItemStack(preview);
            }
            
            // CSGO style click sound
            center.getWorld().playSound(center, Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
            
            // Move slightly up and release (bounce effect)
            Location up = center.clone().add(0, 0.2, 0);
            display.teleport(up);
            org.bukkit.Bukkit.getScheduler().runTaskLater(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                if (display != null && !display.isDead()) {
                    display.teleport(center);
                }
            }, 1L);
            
            nextCycleTick = ticks + cycleDelay;
        }

        // Right at stopping, set the final winning item
        if (ticks == 90) {
            ItemStack winnerPreview = session.getWonReward().getPreviewItem();
            if (winnerPreview != null) {
                display.setItemStack(winnerPreview);
            }
            
            Transformation transform = display.getTransformation();
            transform.getScale().set(new Vector3f(1.2f, 1.2f, 1.2f)); // Grows a bit
            display.setTransformation(transform);
            
            center.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, 0.5, 0), 1);
        
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }
}
