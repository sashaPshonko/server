package dev.narek.pveauction.shop;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

public final class ShopCatalog {

    public static final Set<Material> WOOD_LOGS = EnumSet.of(
            Material.OAK_LOG,
            Material.SPRUCE_LOG,
            Material.BIRCH_LOG,
            Material.JUNGLE_LOG,
            Material.ACACIA_LOG,
            Material.DARK_OAK_LOG,
            Material.CHERRY_LOG,
            Material.MANGROVE_LOG,
            Material.CRIMSON_STEM,
            Material.WARPED_STEM
    );

    private ShopCatalog() {}

    public static boolean isWoodEntry(ShopEntry entry) {
        return entry.accepts().size() > 1 && entry.accepts().containsAll(WOOD_LOGS);
    }
}
