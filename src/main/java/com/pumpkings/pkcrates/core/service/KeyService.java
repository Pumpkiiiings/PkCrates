package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.model.key.IKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import com.pumpkings.pkcrates.infrastructure.database.DatabaseManager;
import java.util.concurrent.CompletableFuture;

public class KeyService {

    private final NamespacedKey pdcKey;
    private final DatabaseManager databaseManager;

    public KeyService(Plugin plugin, DatabaseManager databaseManager) {
        this.pdcKey = new NamespacedKey(plugin, "pkcrates_key_id");
        this.databaseManager = databaseManager;
    }

    /**
     * Gives a key to a player. If physical, injects the invisible ID into the PDC.
     */
    public void giveKey(Player player, IKey key, int amount) {
        if (key.isVirtual()) {
            databaseManager.addVirtualKeys(player.getUniqueId(), key.getId(), amount).thenRun(() -> {
                player.sendMessage("§aYou received " + amount + " virtual keys of " + key.getId());
            });
            return;
        }

        ItemStack item = key.getBaseItem();
        if (item == null) return;

        item.setAmount(amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Inject the inviolable signature using Bukkit/Paper native PersistentDataContainer
            meta.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, key.getId());
            item.setItemMeta(meta);
        }

        // Give to inventory or drop to floor if full
        player.getInventory().addItem(item).values().forEach(leftover -> 
            player.getWorld().dropItem(player.getLocation(), leftover)
        );
    }

    /**
     * Extracts the key ID from a physical ItemStack safely.
     * @return The key ID or null if the item is not a plugin key.
     */
    public @Nullable String extractKeyId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(pdcKey, PersistentDataType.STRING);
    }

    /**
     * Asynchronously checks if the player has the required key.
     */
    public CompletableFuture<Boolean> hasKey(Player player, IKey key) {
        if (key.isVirtual()) {
            return databaseManager.getVirtualKeys(player.getUniqueId(), key.getId())
                    .thenApply(amount -> amount > 0);
        }

        String handKeyId = extractKeyId(player.getInventory().getItemInMainHand());
        return CompletableFuture.completedFuture(key.getId().equals(handKeyId));
    }

    /**
     * Consumes (deducts) a key from the player asynchronously.
     * Returns true if successfully consumed.
     */
    public CompletableFuture<Boolean> consumeKey(Player player, IKey key) {
        if (key.isVirtual()) {
            return databaseManager.takeVirtualKeys(player.getUniqueId(), key.getId(), 1);
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        String handKeyId = extractKeyId(handItem);

        if (key.getId().equals(handKeyId)) {
            handItem.setAmount(handItem.getAmount() - 1);
            return CompletableFuture.completedFuture(true);
        }
        
        return CompletableFuture.completedFuture(false);
    }
}
