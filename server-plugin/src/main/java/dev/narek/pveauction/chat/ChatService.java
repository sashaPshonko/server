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
        NamedTextColor main = global ? NamedTextColor.GRAY : NamedTextColor.GREEN;
        return rankTag(profile, global)
                .append(Component.text(playerName, main, TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, main));
    }

    /** Тело сообщения для {@link dev.narek.pveauction.util.Msg#clan}. */
    public Component formatClanChatBody(PlayerProfile profile, ClanMemberParts parts, String message) {
        Component body = Component.text(parts.clanName() + " ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(rankTag(profile, false));
        if (parts.owner()) {
            body = body.append(Component.text("★ ", NamedTextColor.GOLD, TextDecoration.BOLD));
        }
        return body
                .append(Component.text(parts.playerName(), parts.owner() ? NamedTextColor.GOLD : NamedTextColor.AQUA,
                        TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component rankTag(PlayerProfile profile) {
        return rankTag(profile, false);
    }

    private static Component rankTag(PlayerProfile profile, boolean global) {
        if (global) {
            return Component.text("[" + profile.rankDisplayName() + "] ", NamedTextColor.GRAY);
        }
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
