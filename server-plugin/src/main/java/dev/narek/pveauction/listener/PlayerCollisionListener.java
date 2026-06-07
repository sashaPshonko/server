package dev.narek.pveauction.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Отключает столкновения игроков друг с другом. */
public final class PlayerCollisionListener implements Listener {

    private static final String TEAM_NAME = "pve_no_collision";

    private final Team noCollisionTeam;

    public PlayerCollisionListener() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team existing = board.getTeam(TEAM_NAME);
        if (existing != null) {
            noCollisionTeam = existing;
        } else {
            noCollisionTeam = board.registerNewTeam(TEAM_NAME);
        }
        noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        noCollisionTeam.removeEntry(event.getPlayer().getName());
    }

    private void apply(Player player) {
        player.setCollidable(false);
        String name = player.getName();
        if (!noCollisionTeam.hasEntry(name)) {
            noCollisionTeam.addEntry(name);
        }
    }
}
