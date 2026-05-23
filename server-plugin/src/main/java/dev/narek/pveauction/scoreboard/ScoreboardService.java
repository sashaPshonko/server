package dev.narek.pveauction.scoreboard;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.RankColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PveAuctionPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();

    public ScoreboardService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void show(Player player, PlayerProfile profile) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            return;
        }

        double coins = plugin.economy().getBalance(player);
        String title = plugin.getConfig().getString("scoreboard.title", "4NAREK");

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        NamedTextColor rankColor = RankColors.parse(profile.rankColor());
        lines.add(Component.text("[", rankColor)
                .append(Component.text(profile.rankDisplayName(), rankColor))
                .append(Component.text("] ", rankColor))
                .append(Component.text(player.getName(), NamedTextColor.WHITE)));
        lines.add(Component.text("Клан: ", NamedTextColor.GRAY)
                .append(Component.text(profile.clanDisplay(), NamedTextColor.WHITE)));
        lines.add(Component.text("Монет: ", NamedTextColor.GRAY)
                .append(Component.text(GuiItems.formatPrice((long) coins), NamedTextColor.GOLD)));
        lines.add(Component.text("Токенов: ", NamedTextColor.GRAY)
                .append(Component.text(GuiItems.formatPrice(profile.tokens()), NamedTextColor.AQUA)));
        lines.add(Component.empty());

        applySidebar(player, title, lines);
    }

    public void clear(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void applySidebar(Player player, String title, List<Component> lines) {
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id ->
                Bukkit.getScoreboardManager().getNewScoreboard());

        for (Team team : new ArrayList<>(board.getTeams())) {
            team.unregister();
        }
        Objective old = board.getObjective("pve_sb");
        if (old != null) {
            old.unregister();
        }

        Objective objective = board.registerNewObjective(
                "pve_sb",
                Criteria.DUMMY,
                LEGACY.deserialize("§6§l" + title)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        int index = 0;
        for (Component line : lines) {
            String teamId = "pve" + index;
            String entry = uniqueEntry(index);
            Team team = board.registerNewTeam(teamId);
            team.addEntry(entry);
            team.prefix(line);
            objective.getScore(entry).setScore(score--);
            index++;
        }

        player.setScoreboard(board);
    }

    private static String uniqueEntry(int index) {
        StringBuilder sb = new StringBuilder();
        String hex = Integer.toHexString(index);
        for (int i = 0; i < hex.length(); i++) {
            sb.append('§').append(hex.charAt(i));
        }
        sb.append('§').append('r');
        return sb.toString();
    }
}
