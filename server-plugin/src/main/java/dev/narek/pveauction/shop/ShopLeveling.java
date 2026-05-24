package dev.narek.pveauction.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.util.GuiItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class ShopLeveling {

    private static final int BAR_LENGTH = 16;

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

    public static int progressPercent(PveAuctionPlugin plugin, int level, long earnedCoins) {
        long need = coinsToNextLevel(plugin, level);
        if (need <= 0) {
            return 100;
        }
        return (int) Math.min(100, (earnedCoins * 100L) / need);
    }

    public static Component progressBar(PveAuctionPlugin plugin, int level, long earnedCoins) {
        long need = coinsToNextLevel(plugin, level);
        int filled = need <= 0
                ? BAR_LENGTH
                : (int) Math.min(BAR_LENGTH, (earnedCoins * BAR_LENGTH) / need);
        Component bar = Component.text("[", NamedTextColor.DARK_GRAY);
        for (int i = 0; i < BAR_LENGTH; i++) {
            bar = bar.append(Component.text(
                    i < filled ? "█" : "░",
                    i < filled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY
            ));
        }
        int pct = progressPercent(plugin, level, earnedCoins);
        return bar.append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(pct + "%", NamedTextColor.YELLOW, TextDecoration.BOLD));
    }

    public static Component progressLoreLine(PveAuctionPlugin plugin, int level, long earnedCoins) {
        return Component.text("До ур. " + (level + 1) + ": ", NamedTextColor.GRAY)
                .append(Component.text(progressText(plugin, level, earnedCoins), NamedTextColor.WHITE));
    }

    public static String formatMultiplier(double mult) {
        return String.format("%.2f", mult);
    }
}
