package dev.narek.pveauction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class Msg {

    private static final Component PREFIX = Component.text("✦ ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text("АУКЦИОН ", NamedTextColor.AQUA, TextDecoration.BOLD))
            .append(Component.text("» ", NamedTextColor.DARK_GRAY));

    private Msg() {}

    public static void send(Player player, Component body) {
        player.sendMessage(PREFIX.append(body));
    }

    public static void send(CommandSender sender, Component body) {
        if (sender instanceof Player player) {
            send(player, body);
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
