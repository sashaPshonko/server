package dev.narek.pveauction.chat;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.donate.DonateService;
import dev.narek.pveauction.model.PlayerDonate;
import dev.narek.pveauction.model.PlayerProfile;
import dev.narek.pveauction.util.RankColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class ChatService {

    private final PveAuctionPlugin plugin;

    public ChatService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public int localRadius() {
        return plugin.getConfig().getInt("chat.local-radius", 100);
    }

    public Component formatLine(
            PlayerProfile profile,
            Optional<PlayerDonate> primaryDonate,
            String playerName,
            boolean global,
            String message
    ) {
        NamedTextColor textColor = global
                ? NamedTextColor.GRAY
                : NamedTextColor.GREEN;
        return chatPrefix(profile, primaryDonate)
                .append(Component.text(playerName, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, textColor));
    }

    public Component formatClanChatBody(ClanMemberParts parts, String message) {
        Component body = Component.empty();
        if (parts.owner()) {
            body = body.append(Component.text("★ ", NamedTextColor.GOLD, TextDecoration.BOLD));
        }
        return body
                .append(Component.text(parts.playerName(), parts.owner() ? NamedTextColor.GOLD : NamedTextColor.AQUA,
                        TextDecoration.BOLD))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    /** Действующий донат (высший приоритет) или зелёный [Игрок]. */
    public static Component chatPrefix(PlayerProfile profile, Optional<PlayerDonate> primaryDonate) {
        if (primaryDonate.isPresent()) {
            return DonateService.donateTag(primaryDonate.get());
        }
        return rankTag(profile);
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

    public record ClanMemberParts(String playerName, boolean owner) {}
}
