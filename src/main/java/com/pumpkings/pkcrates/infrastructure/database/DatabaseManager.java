package com.pumpkings.pkcrates.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/database.db");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("PkCrates-SQLitePool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(60000);
        config.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_virtual_keys (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "key_id VARCHAR(100) NOT NULL, " +
                "amount INT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (uuid, key_id)" +
                ");";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create database tables: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public CompletableFuture<Integer> getVirtualKeys(UUID uuid, String keyId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT amount FROM player_virtual_keys WHERE uuid = ? AND key_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, keyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("amount");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting virtual keys: " + e.getMessage());
            }
            return 0;
        });
    }

    public CompletableFuture<Void> addVirtualKeys(UUID uuid, String keyId, int amount) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_virtual_keys (uuid, key_id, amount) VALUES (?, ?, ?) " +
                    "ON CONFLICT(uuid, key_id) DO UPDATE SET amount = amount + ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, keyId);
                ps.setInt(3, amount);
                ps.setInt(4, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error adding virtual keys: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Boolean> takeVirtualKeys(UUID uuid, String keyId, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            String getSql = "SELECT amount FROM player_virtual_keys WHERE uuid = ? AND key_id = ?";
            String updateSql = "UPDATE player_virtual_keys SET amount = amount - ? WHERE uuid = ? AND key_id = ?";
            
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement getPs = conn.prepareStatement(getSql)) {
                    getPs.setString(1, uuid.toString());
                    getPs.setString(2, keyId);
                    
                    int currentAmount = 0;
                    try (ResultSet rs = getPs.executeQuery()) {
                        if (rs.next()) {
                            currentAmount = rs.getInt("amount");
                        }
                    }
                    
                    if (currentAmount >= amount) {
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                            updatePs.setInt(1, amount);
                            updatePs.setString(2, uuid.toString());
                            updatePs.setString(3, keyId);
                            updatePs.executeUpdate();
                        }
                        conn.commit();
                        return true;
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error taking virtual keys: " + e.getMessage());
            }
            return false;
        });
    }
    /**
     * Sets the virtual key amount for a player to an exact value.
     * Replaces whatever they currently hold.
     */
    public CompletableFuture<Void> setVirtualKeys(UUID uuid, String keyId, int amount) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_virtual_keys (uuid, key_id, amount) VALUES (?, ?, ?) " +
                    "ON CONFLICT(uuid, key_id) DO UPDATE SET amount = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, keyId);
                ps.setInt(3, amount);
                ps.setInt(4, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error setting virtual keys: " + e.getMessage());
            }
        });
    }

    /**
     * Returns all virtual key entries for a player as a map of keyId → amount.
     */
    public CompletableFuture<java.util.Map<String, Integer>> getAllVirtualKeys(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.Map<String, Integer> keys = new java.util.LinkedHashMap<>();
            String sql = "SELECT key_id, amount FROM player_virtual_keys WHERE uuid = ? AND amount > 0";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        keys.put(rs.getString("key_id"), rs.getInt("amount"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error fetching all virtual keys: " + e.getMessage());
            }
            return keys;
        });
    }
}
