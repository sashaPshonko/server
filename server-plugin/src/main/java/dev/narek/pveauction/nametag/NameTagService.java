package dev.narek.pveauction.nametag;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.chat.ChatService;
import dev.narek.pveauction.model.PlayerDonate;
import dev.narek.pveauction.model.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Префикс доната над головой: team на scoreboard каждого зрителя (иначе при личном sidebar не видно).
 */
public final class NameTagService {

    private final PveAuctionPlugin plugin;
    private final Map<UUID, Component> prefixCache = new ConcurrentHashMap<>();

    public NameTagService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateCache(Player target, PlayerProfile profile, Optional<PlayerDonate> primaryDonate) {
        if (!enabled()) {
            prefixCache.remove(target.getUniqueId());
            return;
        }
        prefixCache.put(target.getUniqueId(), ChatService.chatPrefix(profile, primaryDonate));
    }

    public void applyToPlayerEntity(Player target, PlayerProfile profile, Optional<PlayerDonate> primaryDonate) {
        if (!enabled()) {
            reset(target);
            return;
        }
        Component prefix = ChatService.chatPrefix(profile, primaryDonate);
        Component label = prefix.append(Component.text(target.getName(), NamedTextColor.WHITE));
        target.customName(label);
        target.setCustomNameVisible(true);
        target.displayName(label);
        target.playerListName(label);
    }

    public void applyOnBoard(Scoreboard board, Player target) {
        if (!enabled() || board == null) {
            return;
        }
        Component prefix = prefixCache.get(target.getUniqueId());
        if (prefix == null) {
            prefix = Component.text("[Игрок] ", NamedTextColor.GREEN);
        }

        String teamId = teamId(target);
        Team team = board.getTeam(teamId);
        if (team == null) {
            team = board.registerNewTeam(teamId);
        }
        team.prefix(prefix);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    public void removeFromBoard(Scoreboard board, Player target) {
        if (board == null) {
            return;
        }
        Team team = board.getTeam(teamId(target));
        if (team != null) {
            team.unregister();
        }
    }

    public void syncAllOnBoard(Scoreboard board) {
        if (!enabled() || board == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            applyOnBoard(board, online);
        }
    }

    public void propagateToOtherBoards(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            Scoreboard board = plugin.scoreboards().getBoard(viewer);
            if (board != null) {
                applyOnBoard(board, target);
            }
        }
    }

    public void removeFromAllBoards(Player target) {
        prefixCache.remove(target.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            removeFromBoard(plugin.scoreboards().getBoard(viewer), target);
        }
    }

    public static void reset(Player player) {
        player.customName(null);
        player.setCustomNameVisible(false);
        Component plain = Component.text(player.getName(), NamedTextColor.WHITE);
        player.displayName(plain);
        player.playerListName(plain);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("nametag.enabled", true);
    }

    private static String teamId(Player player) {
        String hex = player.getUniqueId().toString().replace("-", "");
        return "nt" + hex.substring(0, 14);
    }
}
