package dev.narek.pveauction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class GuiText {

    private GuiText() {}

    public static Component title(String text, NamedTextColor color) {
        return Component.text(text.toUpperCase(), color, TextDecoration.BOLD);
    }

    public static final Component TITLE_AUCTION = title("Аукцион", NamedTextColor.DARK_PURPLE);
    public static final Component TITLE_STORAGE = title("Хранилище", NamedTextColor.LIGHT_PURPLE);
    public static final Component TITLE_ADMIN = title("Админ-панель", NamedTextColor.RED);
}
