package com.pumpkings.pkcrates.infrastructure.audit.discord;

/**
 * Represents a single field within a Discord embed.
 */
public class EmbedField {
    private String name;
    private String value;
    private boolean inline;

    public EmbedField(String name, String value, boolean inline) {
        this.name = name;
        // Discord API limit: 1024 chars for field value.
        this.value = (value != null && value.length() > 1000) ? value.substring(0, 1000) + "..." : value;
        if (this.value == null || this.value.isEmpty()) this.value = "-"; // Cannot be empty
        this.inline = inline;
    }
}
