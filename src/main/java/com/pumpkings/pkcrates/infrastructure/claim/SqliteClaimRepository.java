package com.pumpkings.pkcrates.infrastructure.claim;

import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-backed implementation of {@link ClaimRepository}.
 *
 * <p><strong>Status: PREPARED — not active in this release.</strong></p>
 *
 * <p>This class skeleton is included so that switching from YAML to SQLite
 * requires only changing one line in
 * {@link com.pumpkings.pkcrates.PkCratesPlugin} (the constructor argument
 * passed to {@link com.pumpkings.pkcrates.core.service.ClaimServiceImpl}).
 * No other class needs to change.</p>
 *
 * <p>To activate: implement the method bodies using the existing
 * {@link com.pumpkings.pkcrates.infrastructure.database.DatabaseManager},
 * add a {@code player_claims} table, and register this class instead of
 * {@link YamlClaimRepository} in the plugin bootstrap.</p>
 *
 * <h3>Suggested DDL</h3>
 * <pre>{@code
 * CREATE TABLE IF NOT EXISTS player_claims (
 *   id           VARCHAR(36)  NOT NULL,
 *   player_uuid  VARCHAR(36)  NOT NULL,
 *   crate_id     VARCHAR(100) NOT NULL,
 *   reward_id    VARCHAR(100) NOT NULL,
 *   preview_item TEXT,
 *   items_data   TEXT         NOT NULL DEFAULT '[]',
 *   commands     TEXT         NOT NULL DEFAULT '[]',
 *   reason       VARCHAR(50)  NOT NULL,
 *   stored_at    BIGINT       NOT NULL,
 *   PRIMARY KEY (id)
 * );
 * CREATE INDEX IF NOT EXISTS idx_claims_player ON player_claims (player_uuid);
 * }</pre>
 */
public class SqliteClaimRepository implements ClaimRepository {

    // Future constructor: inject DatabaseManager

    @Override
    public void save(ClaimedReward claim) {
        throw new UnsupportedOperationException(
                "SqliteClaimRepository is not yet implemented. Use YamlClaimRepository.");
    }

    @Override
    public void delete(UUID playerUuid, UUID claimId) {
        throw new UnsupportedOperationException(
                "SqliteClaimRepository is not yet implemented. Use YamlClaimRepository.");
    }

    @Override
    public List<ClaimedReward> findByPlayer(UUID playerUuid) {
        throw new UnsupportedOperationException(
                "SqliteClaimRepository is not yet implemented. Use YamlClaimRepository.");
    }

    @Override
    public void deleteAll(UUID playerUuid) {
        throw new UnsupportedOperationException(
                "SqliteClaimRepository is not yet implemented. Use YamlClaimRepository.");
    }

    @Override
    public void deleteAllPlayers() {
        throw new UnsupportedOperationException(
                "SqliteClaimRepository is not yet implemented. Use YamlClaimRepository.");
    }
}
