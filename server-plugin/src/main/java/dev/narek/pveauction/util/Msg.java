package dev.narek.pveauction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Msg {

    private static final Component PREFIX_AUCTION = prefix("АУКЦИОН", NamedTextColor.AQUA);
    private static final Component PREFIX_CLAN = prefix("КЛАН", NamedTextColor.GOLD);
    private static final Component PREFIX_PAY = prefix("ПЕРЕВОД", NamedTextColor.GREEN);
    private static final Component PREFIX_SERVER = prefix("4NAREK", NamedTextColor.YELLOW);

    private Msg() {}

    private static Component prefix(String label, NamedTextColor labelColor) {
        return Component.text("✦ ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(label + " ", labelColor, TextDecoration.BOLD))
                .append(Component.text("» ", NamedTextColor.DARK_GRAY));
    }

    /** Сообщения аукциона (/ah, GUI лотов). */
    public static void send(Player player, Component body) {
        auction(player, body);
    }

    public static void auction(Player player, Component body) {
        player.sendMessage(PREFIX_AUCTION.append(body));
    }

    public static void clan(Player player, Component body) {
        player.sendMessage(PREFIX_CLAN.append(body));
    }

    public static void pay(Player player, Component body) {
        player.sendMessage(PREFIX_PAY.append(body));
    }

    public static void server(Player player, Component body) {
        player.sendMessage(PREFIX_SERVER.append(body));
    }

    public static void send(CommandSender sender, Component body) {
        if (sender instanceof Player player) {
            auction(player, body);
        } else {
            sender.sendMessage(Component.text("[Аукцион] ").append(body));
        }
    }

    public static Component ok(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    public static Component info(String text) {
        return Component.text(text, NamedTextColor.AQUA);
    }

    public static Component warn(String text) {
        return Component.text(text, NamedTextColor.GOLD);
    }

    public static Component err(String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    public static Component money(long amount) {
        return Component.text(GuiItems.formatPrice(amount) + " $", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    public static Component money(double amount) {
        return Component.text(GuiItems.formatPrice((long) amount) + " $", NamedTextColor.GOLD, TextDecoration.BOLD);
    }
}
