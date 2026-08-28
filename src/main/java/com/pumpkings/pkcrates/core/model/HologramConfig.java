package com.pumpkings.pkcrates.core.model;

import org.bukkit.entity.Display.Billboard;

import java.util.List;

public record HologramConfig(
        List<String> content,
        Billboard billboard,
        String backgroundColor,
        boolean shadowText,
        float scale
) {
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
}
