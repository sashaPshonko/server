package dev.narek.pveauction.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public enum StorageKeyType {

    SPHERES(
            "spheres",
            Component.text("◆ ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("Отмычка к сферам", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)),
            List.of(
                    Component.empty(),
                    Component.text("Отмычка к хранилищу сфер", NamedTextColor.GRAY),
                    Component.text("ПКМ у точки хранилища сфер", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("§5[§dотмычка§5]", NamedTextColor.DARK_GRAY)
            )
    ),
    WEAPONS(
            "weapons",
            Component.text("» ", NamedTextColor.RED)
                    .append(Component.text("Отмычка к оружию", NamedTextColor.GOLD, TextDecoration.BOLD)),
            List.of(
                    Component.empty(),
                    Component.text("Отмычка к хранилищу оружия", NamedTextColor.GRAY),
                    Component.text("ПКМ у точки хранилища оружия", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("§c[§6отмычка§c]", NamedTextColor.DARK_GRAY)
            )
    ),
    ARMOR(
            "armor",
            Component.text("» ", NamedTextColor.AQUA)
                    .append(Component.text("Отмычка к броне", NamedTextColor.BLUE, TextDecoration.BOLD)),
            List.of(
                    Component.empty(),
                    Component.text("Случайная алмазная или незеритовая броня", NamedTextColor.GRAY),
                    Component.text("ПКМ по кузнечному столу на спавне", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("§b[§9отмычка§b]", NamedTextColor.DARK_GRAY)
            )
    ),
    TOOLS(
            "tools",
            Component.text("» ", NamedTextColor.YELLOW)
                    .append(Component.text("Отмычка к инструментам", NamedTextColor.GOLD, TextDecoration.BOLD)),
            List.of(
                    Component.empty(),
                    Component.text("Отмычка к хранилищу инструментов", NamedTextColor.GRAY),
                    Component.text("ПКМ у точки хранилища инструментов", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("§e[§6отмычка§e]", NamedTextColor.DARK_GRAY)
            )
    ),
    ENCHANTMENTS(
            "enchantments",
            Component.text("✦ ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("Отмычка к зачарованиям", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)),
            List.of(
                    Component.empty(),
                    Component.text("Отмычка к хранилищу зачарований", NamedTextColor.GRAY),
                    Component.text("ПКМ у точки хранилища зачарований", NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("§d[§5отмычка§d]", NamedTextColor.DARK_GRAY)
            )
    );

    private final String id;
    private final Component displayName;
    private final List<Component> lore;

    StorageKeyType(String id, Component displayName, List<Component> lore) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return displayName;
    }

    public List<Component> lore() {
        return lore;
    }

    public static StorageKeyType byId(String id) {
        if (id == null) {
            return null;
        }
        for (StorageKeyType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }
}
