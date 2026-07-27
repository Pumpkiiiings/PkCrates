package com.pumpkings.pkcrates.core.model.rarity;

import java.util.HashMap;
import java.util.Map;

public class Rarity {

    private final String id;
    private String displayName;
    private String description;
    private int priority;
    private boolean enabled;
    
    private RarityChanceMode chanceMode;
    private double weight;
    
    private String color;
    private String miniMessageFormat;
    private String icon;
    private boolean glow;
    
    private String particle;
    private String sound;
    private String fireworkColor;
    
    private boolean broadcastEnabled;
    private String announcementTemplate;
    
    private String defaultAnimation;
    private String permission;
    
    private Map<String, String> placeholders;

    public Rarity(String id) {
        this.id = id;
        this.placeholders = new HashMap<>();
    }

    public String getId() { return id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public RarityChanceMode getChanceMode() { return chanceMode; }
    public void setChanceMode(RarityChanceMode chanceMode) { this.chanceMode = chanceMode; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getMiniMessageFormat() { return miniMessageFormat; }
    public void setMiniMessageFormat(String miniMessageFormat) { this.miniMessageFormat = miniMessageFormat; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isGlow() { return glow; }
    public void setGlow(boolean glow) { this.glow = glow; }

    public String getParticle() { return particle; }
    public void setParticle(String particle) { this.particle = particle; }

    public String getSound() { return sound; }
    public void setSound(String sound) { this.sound = sound; }

    public String getFireworkColor() { return fireworkColor; }
    public void setFireworkColor(String fireworkColor) { this.fireworkColor = fireworkColor; }

    public boolean isBroadcastEnabled() { return broadcastEnabled; }
    public void setBroadcastEnabled(boolean broadcastEnabled) { this.broadcastEnabled = broadcastEnabled; }

    public String getAnnouncementTemplate() { return announcementTemplate; }
    public void setAnnouncementTemplate(String announcementTemplate) { this.announcementTemplate = announcementTemplate; }

    public String getDefaultAnimation() { return defaultAnimation; }
    public void setDefaultAnimation(String defaultAnimation) { this.defaultAnimation = defaultAnimation; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public Map<String, String> getPlaceholders() { return placeholders; }
    public void setPlaceholders(Map<String, String> placeholders) { 
        this.placeholders = placeholders != null ? placeholders : new HashMap<>(); 
    }
}
