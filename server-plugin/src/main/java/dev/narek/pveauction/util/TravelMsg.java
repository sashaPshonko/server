package dev.narek.pveauction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class TravelMsg {

    private TravelMsg() {}

    public static void send(Player player, Component body) {
        player.sendMessage(body);
    }

    public static Component ok(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    public static Component err(String text) {
        return Component.text(text, NamedTextColor.RED);
    }
}
