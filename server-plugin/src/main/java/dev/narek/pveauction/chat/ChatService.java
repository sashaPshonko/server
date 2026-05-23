package dev.narek.pveauction.chat;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.util.RankColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChatService {

    private final PveAuctionPlugin plugin;

    public ChatService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public int localRadius() {
        return plugin.getConfig().getInt("chat.local-radius", 100);
    }

    public Component formatLine(PlayerProfile profile, String playerName, boolean global, String message) {
        String scope = global ? "Мир" : "Локал";
        NamedTextColor scopeColor = global ? NamedTextColor.GOLD : NamedTextColor.GRAY;
        return Component.text("[" + scope + "] ", scopeColor, TextDecoration.BOLD)
                .append(rankTag(profile))
                .append(Component.text(playerName, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public Component formatClanLine(PlayerProfile profile, ClanMemberParts parts, String message) {
        Component line = Component.text("[Клан] ", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                .append(Component.text(parts.clanName() + " ", NamedTextColor.AQUA))
                .append(rankTag(profile));
        if (parts.owner()) {
            line = line.append(Component.text("★ ", NamedTextColor.GOLD, TextDecoration.BOLD));
        }
        return line
                .append(Component.text(parts.playerName(), parts.owner() ? NamedTextColor.GOLD : NamedTextColor.AQUA,
                        TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component rankTag(PlayerProfile profile) {
        NamedTextColor color = RankColors.parse(profile.rankColor());
        return Component.text("[" + profile.rankDisplayName() + "] ", color);
    }

    public void broadcastGlobal(Component line) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(line);
        }
    }

    public void broadcastLocal(Player sender, Component line) {
        int radius = localRadius();
        double radiusSq = (double) radius * radius;
        var world = sender.getWorld();
        for (Player online : world.getPlayers()) {
            if (online.getLocation().distanceSquared(sender.getLocation()) <= radiusSq) {
                online.sendMessage(line);
            }
        }
    }

    public record ClanMemberParts(String clanName, String playerName, boolean owner) {}
}
