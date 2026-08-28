package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.massopening.MassOpeningOption;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MassOpeningService {

    CompletableFuture<CanOpenResult> canMassOpen(Player player, Crate crate, int amount);

    CompletableFuture<MassOpeningResult> startMassOpening(Player player, Crate crate, int amount);

    List<MassOpeningOption> getAvailableOptions(Player player, Crate crate, int userKeys);

    int resolveMaxAllowed(Player player, Crate crate);

    boolean isMassOpeningInProgress(Player player);

    void cancelMassOpening(Player player, String reason);
}
