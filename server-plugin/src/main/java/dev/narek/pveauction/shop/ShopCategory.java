package dev.narek.pveauction.shop;

import org.bukkit.Material;

import java.util.List;
import java.util.Optional;

public enum ShopCategory {
    FARMER("Фермер", Material.WHEAT, List.of(
            entry(Material.COCOA_BEANS, 4),
            entry(Material.NETHER_WART, 10),
            entry(Material.CHORUS_FRUIT, 7),
            entry(Material.SUGAR_CANE, 3),
            entry(Material.CACTUS, 3),
            entryWood(5)
    )),
    FOOD("Еда", Material.CARROT, List.of(
            entry(Material.CARROT, 4),
            entry(Material.POTATO, 4),
            entry(Material.BEETROOT, 4),
            entry(Material.SWEET_BERRIES, 5),
            entry(Material.GLOW_BERRIES, 9),
            entry(Material.WHEAT, 3),
            entry(Material.MELON_SLICE, 2),
            entry(Material.PUMPKIN, 14)
    )),
    BUTCHER("Мясник", Material.COOKED_BEEF, List.of(
            entry(Material.PORKCHOP, 6),
            entry(Material.COOKED_PORKCHOP, 7),
            entry(Material.BEEF, 7),
            entry(Material.COOKED_BEEF, 8),
            entry(Material.CHICKEN, 5),
            entry(Material.COOKED_CHICKEN, 6),
            entry(Material.MUTTON, 6),
            entry(Material.COOKED_MUTTON, 7),
            entry(Material.RABBIT, 5),
            entry(Material.COOKED_RABBIT, 6)
    )),
    MINER("Шахтёр", Material.DIAMOND, List.of(
            entry(Material.COAL, 4),
            entry(Material.LAPIS_LAZULI, 6),
            entry(Material.REDSTONE, 5),
            entry(Material.AMETHYST_SHARD, 8),
            entry(Material.QUARTZ, 7),
            entry(Material.IRON_INGOT, 12),
            entry(Material.COPPER_INGOT, 8),
            entry(Material.GOLD_INGOT, 18),
            entry(Material.DIAMOND, 80),
            entry(Material.EMERALD, 45),
            entry(Material.NETHERITE_INGOT, 350)
    )),
    LOOT("Лут", Material.SPIDER_EYE, List.of(
            entry(Material.SPIDER_EYE, 5),
            entry(Material.ROTTEN_FLESH, 2),
            entry(Material.BONE, 4),
            entry(Material.BLAZE_ROD, 14),
            entry(Material.BREEZE_ROD, 16),
            entry(Material.SLIME_BALL, 10),
            entry(Material.MAGMA_CREAM, 12),
            entry(Material.GUNPOWDER, 8),
            entry(Material.ENDER_PEARL, 18),
            entry(Material.PRISMARINE_SHARD, 7),
            entry(Material.SHULKER_SHELL, 120)
    )),
    FISHER("Рыбак", Material.FISHING_ROD, List.of(
            entry(Material.COD, 5),
            entry(Material.COOKED_COD, 6),
            entry(Material.SALMON, 6),
            entry(Material.COOKED_SALMON, 7),
            entry(Material.TROPICAL_FISH, 8),
            entry(Material.PUFFERFISH, 6),
            entry(Material.NAUTILUS_SHELL, 45),
            entry(Material.LILY_PAD, 3),
            entry(Material.BOWL, 1),
            entry(Material.LEATHER, 4),
            entry(Material.STRING, 3),
            entry(Material.INK_SAC, 4),
            entry(Material.SADDLE, 55),
            entry(Material.NAME_TAG, 80)
    ));

    private final String displayName;
    private final Material icon;
    private final List<ShopEntry> entries;

    ShopCategory(String displayName, Material icon, List<ShopEntry> entries) {
        this.displayName = displayName;
        this.icon = icon;
        this.entries = entries;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public List<ShopEntry> entries() {
        return entries;
    }

    public String id() {
        return name();
    }

    public static ShopCategory byId(String id) {
        return ShopCategory.valueOf(id);
    }

    public Optional<ShopEntry> findEntry(Material material) {
        for (ShopEntry entry : entries) {
            if (entry.accepts(material)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    private static ShopEntry entry(Material material, long basePrice) {
        return new ShopEntry(material, basePrice);
    }

    private static ShopEntry entryWood(long basePrice) {
        return new ShopEntry(Material.OAK_LOG, basePrice, ShopCatalog.WOOD_LOGS);
    }
}
