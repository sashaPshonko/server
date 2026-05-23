package dev.narek.pveauction.listener;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.PlayerProfile;
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

    public ScoreboardListener(PveAuctionPlugin plugin, ScoreboardService scoreboards) {
        this.plugin = plugin;
        this.scoreboards = scoreboards;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboards.clear(event.getPlayer());
    }

    public void refresh(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = plugin.players().getOrCreate(player.getUniqueId(), player.getName());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        scoreboards.show(player, profile);
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("Профиль игрока: " + e.getMessage());
            }
        });
    }

    public void refreshAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refresh(player);
        }
    }
}
