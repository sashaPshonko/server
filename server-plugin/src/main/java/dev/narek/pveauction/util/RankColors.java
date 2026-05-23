package dev.narek.pveauction.util;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

public final class RankColors {

    private RankColors() {}

    public static NamedTextColor parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NamedTextColor.GREEN;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (key) {
            case "BLACK" -> NamedTextColor.BLACK;
            case "DARK_BLUE" -> NamedTextColor.DARK_BLUE;
            case "DARK_GREEN" -> NamedTextColor.DARK_GREEN;
            case "DARK_AQUA", "DARK_CYAN" -> NamedTextColor.DARK_AQUA;
            case "DARK_RED" -> NamedTextColor.DARK_RED;
            case "DARK_PURPLE" -> NamedTextColor.DARK_PURPLE;
            case "GOLD", "ORANGE" -> NamedTextColor.GOLD;
            case "GRAY", "GREY" -> NamedTextColor.GRAY;
            case "DARK_GRAY", "DARK_GREY" -> NamedTextColor.DARK_GRAY;
            case "BLUE" -> NamedTextColor.BLUE;
            case "GREEN" -> NamedTextColor.GREEN;
            case "AQUA", "CYAN" -> NamedTextColor.AQUA;
            case "RED" -> NamedTextColor.RED;
            case "LIGHT_PURPLE", "PINK", "MAGENTA" -> NamedTextColor.LIGHT_PURPLE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            case "WHITE" -> NamedTextColor.WHITE;
            default -> NamedTextColor.GREEN;
        };
    }
}
