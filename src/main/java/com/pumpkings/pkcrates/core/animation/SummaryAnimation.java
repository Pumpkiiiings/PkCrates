package com.pumpkings.pkcrates.core.animation;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;

public class SummaryAnimation {

    public static void play(Player player, Crate crate, int totalOpened, List<IReward> rewards) {
        if (player == null || !player.isOnline()) return;

        try {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } catch (Exception ignored) {}

        Title title = Title.title(
                TextUtil.parse("<gradient:#4287f5:#42d4f5><bold>MASS OPENING COMPLETE</bold></gradient>"),
                TextUtil.parse("<gray>Opened <white>" + totalOpened + "x " + crate.getName() + " <gray>crates!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
        );
        player.showTitle(title);
    }
}
