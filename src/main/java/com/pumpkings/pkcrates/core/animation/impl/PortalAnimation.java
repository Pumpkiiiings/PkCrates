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

public class PortalAnimation implements AnimationPhase {

    private final int MAX_TICKS = 120;
    private ItemDisplay winnerDisplay;
    private final List<ItemDisplay> junkItems = new ArrayList<>();
    private final Random random = new Random();
    private Location portalLoc;

    @Override
    public void onStart(CrateSession session) {
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);
        portalLoc = center.clone().add(0, 3.0, 0);
        
        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);

        // Hide winner
        winnerDisplay = (ItemDisplay) center.getWorld().spawnEntity(portalLoc, EntityType.ITEM_DISPLAY);
        winnerDisplay.setBillboard(Display.Billboard.CENTER);
        ItemStack winnerItem = session.getWonReward().getPreviewItem();
        winnerDisplay.setItemStack(winnerItem != null ? winnerItem : new ItemStack(org.bukkit.Material.PAPER));
        
        Transformation t = winnerDisplay.getTransformation();
        t.getScale().set(new Vector3f(0f, 0f, 0f)); // Invisible scale
        winnerDisplay.setTransformation(t);
        
        // Spawn junk items inside the crate
        List<IReward> allRewards = session.getCrate().getRewards();
        if (!allRewards.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                IReward r = allRewards.get(random.nextInt(allRewards.size()));
                ItemDisplay junk = (ItemDisplay) center.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);
                junk.setBillboard(Display.Billboard.CENTER);
                junk.setItemStack(r.getPreviewItem() != null ? r.getPreviewItem() : new ItemStack(org.bukkit.Material.DIRT));
                
                Transformation jt = junk.getTransformation();
                jt.getScale().set(new Vector3f(0.5f, 0.5f, 0.5f));
                junk.setTransformation(jt);
                junk.setTeleportDuration(20);
                junkItems.add(junk);
            }
        }
    }

    @Override
    public void onTick(CrateSession session) {
        int ticks = session.getTicksLived();
        Location center = session.getBlockLocation().clone().add(0.5, 1.2, 0.5);

        // Draw portal ring
        if (ticks < 100) {
            double angle = (ticks * 0.2) % (Math.PI * 2);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            
            Location p1 = portalLoc.clone().add(x, 0, z);
            Location p2 = portalLoc.clone().add(-x, 0, -z);
            
            Particle particle = ticks < 60 ? Particle.PORTAL : Particle.FLAME;
            
            center.getWorld().spawnParticle(particle, p1, 2, 0, 0, 0, 0);
            center.getWorld().spawnParticle(particle, p2, 2, 0, 0, 0, 0);
            center.getWorld().spawnParticle(Particle.WITCH, portalLoc, 1, 1.5, 0.1, 1.5, 0);
        }
        
        // Phase 2: Junk sucks into portal
        if (ticks == 20) {
            for (ItemDisplay junk : junkItems) {
                if (junk.isValid()) {
                    // Slight random offset in the portal
                    Location target = portalLoc.clone().add(
                            (random.nextDouble() - 0.5), 
                            0, 
                            (random.nextDouble() - 0.5));
                    junk.teleport(target);
                }
            }
            center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
        
        // Remove junk as it "enters" portal
        if (ticks == 40) {
            for (ItemDisplay junk : junkItems) {
                if (junk.isValid()) junk.remove();
            }
            junkItems.clear();
        }
        
        // Phase 3: Portal turns red and lightning strikes
        if (ticks == 60) {
            center.getWorld().strikeLightningEffect(portalLoc);
            center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }
        
        // Phase 4: Winner descends
        if (ticks == 70) {
            if (winnerDisplay.isValid()) {
                Transformation t = winnerDisplay.getTransformation();
                t.getScale().set(new Vector3f(1.5f, 1.5f, 1.5f));
                winnerDisplay.setTransformation(t);
                
                winnerDisplay.setTeleportDuration(30);
                winnerDisplay.teleport(center.clone().add(0, 0.5, 0));
                
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
            }
        }
    }

    @Override
    public boolean isFinished(CrateSession session) {
        return session.getTicksLived() >= MAX_TICKS;
    }

    @Override
    public void onEnd(CrateSession session) {
        for (ItemDisplay junk : junkItems) {
            if (junk != null && junk.isValid()) junk.remove();
        }
        junkItems.clear();
        
        if (winnerDisplay != null && winnerDisplay.isValid()) {
            winnerDisplay.remove();
        }
    }
}
