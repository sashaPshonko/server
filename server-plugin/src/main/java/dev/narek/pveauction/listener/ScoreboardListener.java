package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.nametag.NameTagService;
import dev.narek.pveauction.scoreboard.ScoreboardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;

public final class ScoreboardListener implements Listener {

    private final PveAuctionPlugin plugin;
    private final ScoreboardService scoreboards;
    private final NameTagService nameTags;

    public ScoreboardListener(PveAuctionPlugin plugin, ScoreboardService scoreboards, NameTagService nameTags) {
        this.plugin = plugin;
        this.scoreboards = scoreboards;
        this.nameTags = nameTags;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.auth() != null && !plugin.auth().isLoggedIn(event.getPlayer())) {
            return;
        }
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quit = event.getPlayer();
        nameTags.removeFromAllBoards(quit);
        NameTagService.reset(quit);
        scoreboards.clear(quit);
    }

    /** Профиль, донат, клан, nametags — при входе и смене ранга/клана/доната. */
    public void refresh(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = plugin.players().getOrCreate(player.getUniqueId(), player.getName());
                var primary = plugin.donates().primaryActive(player.getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    nameTags.updateCache(player, profile, primary);
                    nameTags.applyToPlayerEntity(player, profile, primary);
                    scoreboards.show(player, profile, primary, true);
                    nameTags.propagateToOtherBoards(player);
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Профиль игрока: " + e.getMessage());
            }
        });
    }

    /** Только монеты на sidebar — без пересборки nametag у всех онлайн. */
    public void refreshCoins(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = plugin.players().getOrCreate(player.getUniqueId(), player.getName());
                var primary = plugin.donates().primaryActive(player.getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        scoreboards.show(player, profile, primary, false);
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Профиль игрока: " + e.getMessage());
            }
        });
    }

    public void refreshCoins(Player first, Player second) {
        refreshCoins(first);
        if (!first.getUniqueId().equals(second.getUniqueId())) {
            refreshCoins(second);
        }
    }
}
