package com.pumpkings.pkcrates.infrastructure.audit.sink;

import com.google.gson.Gson;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditRecord;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditSink;
import com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig;
import com.pumpkings.pkcrates.infrastructure.audit.discord.DiscordEmbed;
import com.pumpkings.pkcrates.infrastructure.audit.discord.WebhookPayload;
import com.pumpkings.pkcrates.infrastructure.audit.impl.AuditRetryWorker;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Sends audit records to a Discord webhook as rich embedded messages.
 */
public class DiscordWebhookAuditSink implements AuditSink {

    private final LoggerConfig config;
    private final AuditRetryWorker retryWorker;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneId.of("UTC"));

    public DiscordWebhookAuditSink(Plugin plugin, LoggerConfig config, AuditRetryWorker retryWorker) {
        this.config = config;
        this.retryWorker = retryWorker;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // Discord webhooks sometimes act weird with HTTP/2
                .build();
    }

    @Override
    public String getId() {
        return "discord";
    }

    @Override
    public boolean isEnabled() {
        return config.isDiscordEnabled();
    }

    @Override
    public boolean accepts(AuditRecord record) {
        return isEnabled() && record.getPriority().isAtLeast(config.getDiscordMinPriority());
    }

    @Override
    public void write(AuditRecord record) {
        WebhookPayload payload = buildPayload(record);
        String json = gson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getDiscordUrl()))
                .header("Content-Type", "application/json")
                .header("User-Agent", "PkCrates-AuditSystem")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Discord returns 200, 204 for success. 429 for rate limit.
            if (response.statusCode() >= 400) {
                throw new RuntimeException("Discord HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            // If retry is enabled, hand it off to the retry worker instead of throwing up to the main AuditWorker
            if (config.isDiscordRetryEnabled() && retryWorker != null) {
                retryWorker.enqueue(record, this, config.getDiscordRetryAttempts(), config.getDiscordRetryDelaySeconds());
            } else {
                throw new RuntimeException("Discord webhook failed and retries are disabled/unavailable", e);
            }
        }
    }

    private WebhookPayload buildPayload(AuditRecord record) {
        WebhookPayload payload = new WebhookPayload(config.getDiscordUsername(), config.getDiscordAvatarUrl());

        String emoji = config.getDiscordEmoji(record.getCategory());
        String title = emoji + " " + formatEnum(record.getEvent().name());
        
        String description = "Player **" + record.getActor() + "** performed action on **" + record.getTarget() + "**";
        if (record.getTarget() == null || record.getTarget().isEmpty()) {
            description = "Actor **" + record.getActor() + "** triggered an event.";
        }

        DiscordEmbed embed = new DiscordEmbed()
                .setTitle(title)
                .setDescription(description)
                .setColor(config.getDiscordColor(record.getPriority()))
                .setTimestamp(timeFormatter.format(record.getTimestamp()))
                .setFooter(config.getDiscordServerName());

        // Dynamic fields
        embed.addField("Actor", record.getActor(), true);
        if (record.getTarget() != null && !record.getTarget().isEmpty()) {
            embed.addField("Target", record.getTarget(), true);
        }
        
        if (record.getWorldName() != null && !record.getWorldName().isEmpty()) {
            embed.addField("World", record.getWorldName(), true);
        }

        for (Map.Entry<String, String> entry : record.getData().entrySet()) {
            // Capitalize first letter of key for aesthetics
            String key = entry.getKey();
            if (!key.isEmpty()) {
                key = key.substring(0, 1).toUpperCase() + key.substring(1);
            }
            embed.addField(key, entry.getValue(), true);
        }

        payload.addEmbed(embed);
        return payload;
    }

    private String formatEnum(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(p.substring(0, 1).toUpperCase()).append(p.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
