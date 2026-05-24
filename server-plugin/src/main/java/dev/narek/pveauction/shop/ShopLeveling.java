package dev.narek.pveauction.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.util.GuiItems;

public final class ShopLeveling {

    private ShopLeveling() {}

    public static long baseCoins(PveAuctionPlugin plugin) {
        return plugin.getConfig().getLong("shop.level-base-coins", 50L);
    }

    public static double bonusPerLevel(PveAuctionPlugin plugin) {
        return plugin.getConfig().getDouble("shop.bonus-per-level", 0.1);
    }

    /** Монет для перехода с текущего уровня на следующий (геом. прогрессия). */
    public static long coinsToNextLevel(PveAuctionPlugin plugin, int currentLevel) {
        long base = baseCoins(plugin);
        return base * (1L << Math.max(0, currentLevel - 1));
    }

    public static double multiplier(PveAuctionPlugin plugin, int level) {
        if (level < 1) {
            level = 1;
        }
        return 1.0 + (level - 1) * bonusPerLevel(plugin);
    }

    public static LevelUpResult addEarned(PveAuctionPlugin plugin, int level, long earnedCoins, long payment) {
        long progress = earnedCoins + payment;
        int newLevel = level;
        while (true) {
            long need = coinsToNextLevel(plugin, newLevel);
            if (progress < need) {
                break;
            }
            progress -= need;
            newLevel++;
        }
        return new LevelUpResult(newLevel, progress, newLevel > level);
    }

    public record LevelUpResult(int level, long earnedCoins, boolean leveledUp) {}

    public static String progressText(PveAuctionPlugin plugin, int level, long earnedCoins) {
        long need = coinsToNextLevel(plugin, level);
        return GuiItems.formatPrice(earnedCoins) + "/" + GuiItems.formatPrice(need) + " $";
    }
}
