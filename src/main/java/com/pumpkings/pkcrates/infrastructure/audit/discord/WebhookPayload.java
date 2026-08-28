package com.pumpkings.pkcrates.infrastructure.audit.discord;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the root JSON payload sent to a Discord webhook.
 * Designed to be serialized by Gson.
 */
public class WebhookPayload {

    private String username;
    private String avatar_url;
    private final List<DiscordEmbed> embeds = new ArrayList<>();

    public WebhookPayload(String username, String avatarUrl) {
        this.username = username;
        this.avatar_url = avatarUrl;
    }

    public void addEmbed(DiscordEmbed embed) {
        this.embeds.add(embed);
    }
}
