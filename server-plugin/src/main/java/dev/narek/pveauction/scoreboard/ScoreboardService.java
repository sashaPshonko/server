package dev.narek.pveauction.scoreboard;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.util.RankColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardService {

    private static final Component BORDER = Component.text("▌ ", NamedTextColor.RED);

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
        String titleText = plugin.getConfig().getString("scoreboard.title", "4нарек");
        NamedTextColor rankColor = RankColors.parse(profile.rankColor());

        Component header = Component.text("⚡ ", NamedTextColor.GOLD)
                .append(Component.text(titleText, NamedTextColor.RED, TextDecoration.BOLD));

        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("==========", NamedTextColor.RED, TextDecoration.STRIKETHROUGH));
        lines.add(bordered(Component.text("K", NamedTextColor.RED)
                .append(Component.text("| ", NamedTextColor.RED))
                .append(Component.text(player.getName(), NamedTextColor.GOLD))));
        lines.add(bordered(Component.text("★ ", NamedTextColor.AQUA)
                .append(Component.text("Ранг: ", NamedTextColor.WHITE))
                .append(Component.text(profile.rankDisplayName(), rankColor))));
        lines.add(bordered(Component.text("$ ", NamedTextColor.YELLOW)
                .append(Component.text("Монет: ", NamedTextColor.WHITE))
                .append(Component.text(formatCoins(coins), NamedTextColor.YELLOW))));
        lines.add(bordered(Component.text("☀ ", NamedTextColor.GREEN)
                .append(Component.text("Токенов: ", NamedTextColor.WHITE))
                .append(Component.text(formatTokens(profile.tokens()), NamedTextColor.GREEN))));

        applySidebar(player, header, lines);
    }

    public void clear(Player player) {
        boards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private static Component bordered(Component content) {
        return BORDER.append(content);
    }

    private static String formatCoins(double amount) {
        return String.format(Locale.US, "%,.0f", amount);
    }

    private static String formatTokens(long amount) {
        return String.format(Locale.US, "%,d", amount);
    }

    private void applySidebar(Player player, Component title, List<Component> lines) {
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id ->
                Bukkit.getScoreboardManager().getNewScoreboard());

        for (Team team : new ArrayList<>(board.getTeams())) {
            team.unregister();
        }
        Objective old = board.getObjective("pve_sb");
        if (old != null) {
            old.unregister();
        }

        Objective objective = board.registerNewObjective("pve_sb", Criteria.DUMMY, title);
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
