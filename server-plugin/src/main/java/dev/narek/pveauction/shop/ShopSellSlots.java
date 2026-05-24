package dev.narek.pveauction.shop;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ручная раскладка слотов (индексы сундука 54).
 * Ряд 2 = 18–26, ряд 3 = 27–35, «лишний» сверху = 13 (центр ряда 1).
 */
public final class ShopSellSlots {

    private ShopSellSlots() {}

    public static Map<Material, Integer> forCategory(ShopCategory category, List<ShopEntry> entries) {
        return switch (category) {
            case FARMER -> farmer();
            case FOOD -> food();
            case FISHER -> fisher();
            case LOOT -> loot();
            case MINER -> miner();
            case BUTCHER -> butcher();
            default -> ShopSellLayout.slotsFor(entries);
        };
    }

    /** 6 предметов: 3 + 3 по центру (ряды 2–3, колонки 3–5). */
    private static Map<Material, Integer> farmer() {
        Map<Material, Integer> s = new LinkedHashMap<>();
        s.put(Material.COCOA_BEANS, 21);
        s.put(Material.NETHER_WART, 22);
        s.put(Material.CHORUS_FRUIT, 23);
        s.put(Material.SUGAR_CANE, 30);
        s.put(Material.CACTUS, 31);
        s.put(Material.OAK_LOG, 32);
        return s;
    }

    /** 8 предметов: 4 + 4 (колонки 2–5). */
    private static Map<Material, Integer> food() {
        Map<Material, Integer> s = new LinkedHashMap<>();
        s.put(Material.CARROT, 20);
        s.put(Material.POTATO, 21);
        s.put(Material.BEETROOT, 22);
        s.put(Material.SWEET_BERRIES, 23);
        s.put(Material.GLOW_BERRIES, 29);
        s.put(Material.WHEAT, 30);
        s.put(Material.MELON_SLICE, 31);
        s.put(Material.PUMPKIN, 32);
        return s;
    }

    /**
     * Слева треска/лосось (сырое/жареное), справа остальное 5 + 5.
     * Пары: колонки 2–3; одиночные: колонки 4–8.
     */
    private static Map<Material, Integer> fisher() {
        Map<Material, Integer> s = new LinkedHashMap<>();
        s.put(Material.COD, 20);
        s.put(Material.COOKED_COD, 29);
        s.put(Material.NAUTILUS_SHELL, 21);
        s.put(Material.SALMON, 30);
        s.put(Material.COOKED_SALMON, 22);
        s.put(Material.TROPICAL_FISH, 31);
        s.put(Material.NAME_TAG, 23);
        s.put(Material.PUFFERFISH, 32);
        s.put(Material.LILY_PAD, 24);
        s.put(Material.LEATHER, 33);
        s.put(Material.INK_SAC, 25);
        s.put(Material.SADDLE, 34);
        return s;
    }

    /** 12: 6 + 6 по центру. */
    private static Map<Material, Integer> loot() {
        Map<Material, Integer> s = new LinkedHashMap<>();
        s.put(Material.BREEZE_ROD, 20);
        s.put(Material.ROTTEN_FLESH, 29);
        s.put(Material.SPIDER_EYE, 21);
        s.put(Material.BONE, 30);
        s.put(Material.BLAZE_ROD, 22);
        s.put(Material.SLIME_BALL, 31);
        s.put(Material.SHULKER_SHELL, 23);
        s.put(Material.STRING, 32);
        s.put(Material.GUNPOWDER, 24);
        s.put(Material.MAGMA_CREAM, 33);
        s.put(Material.PRISMARINE_SHARD, 25);
        s.put(Material.ENDER_PEARL, 34);
        return s;
    }

    /** 11: уголь сверху по центру, под ним 5 + 5. */
    private static Map<Material, Integer> miner() {
        Map<Material, Integer> s = new LinkedHashMap<>();
        s.put(Material.COAL, 13);
        s.put(Material.LAPIS_LAZULI, 20);
        s.put(Material.REDSTONE, 21);
        s.put(Material.AMETHYST_SHARD, 22);
        s.put(Material.QUARTZ, 23);
        s.put(Material.IRON_INGOT, 24);
        s.put(Material.COPPER_INGOT, 29);
        s.put(Material.GOLD_INGOT, 30);
        s.put(Material.DIAMOND, 31);
        s.put(Material.EMERALD, 32);
        s.put(Material.NETHERITE_INGOT, 33);
        return s;
    }

    /** Мясник: 5 колонок по центру, сырое над жареным. */
    private static Map<Material, Integer> butcher() {
        Map<Material, Integer> s = new LinkedHashMap<>();
        s.put(Material.PORKCHOP, 20);
        s.put(Material.COOKED_PORKCHOP, 29);
        s.put(Material.BEEF, 21);
        s.put(Material.COOKED_BEEF, 30);
        s.put(Material.CHICKEN, 22);
        s.put(Material.COOKED_CHICKEN, 31);
        s.put(Material.MUTTON, 23);
        s.put(Material.COOKED_MUTTON, 32);
        s.put(Material.RABBIT, 24);
        s.put(Material.COOKED_RABBIT, 33);
        return s;
    }
}
