package com.pumpkings.pkcrates.infrastructure.audit.discord;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single Discord embed structure.
 * Designed to be serialized by Gson.
 */
public class DiscordEmbed {

    private String title;
    private String description;
    private Integer color;
    private String timestamp;
    private Author author;
    private Footer footer;
    private final List<EmbedField> fields = new ArrayList<>();

    public DiscordEmbed setTitle(String title) {
        this.title = title;
        return this;
    }

    public DiscordEmbed setDescription(String description) {
        this.description = description;
        return this;
    }

    public DiscordEmbed setColor(Integer color) {
        this.color = color;
        return this;
    }

    public DiscordEmbed setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public DiscordEmbed setAuthor(String name, String iconUrl) {
        this.author = new Author(name, iconUrl);
        return this;
    }

    public DiscordEmbed setFooter(String text) {
        this.footer = new Footer(text);
        return this;
    }

    public DiscordEmbed addField(String name, String value, boolean inline) {
        this.fields.add(new EmbedField(name, value, inline));
        return this;
    }

    // ── Nested DTOs ─────────────────────────────────────────────────────────

    private static class Author {
        private final String name;
        private final String icon_url;

        public Author(String name, String iconUrl) {
            this.name = name;
            this.icon_url = iconUrl;
        }
    }

    private static class Footer {
        private final String text;

        public Footer(String text) {
            this.text = text;
        }
    }
}
