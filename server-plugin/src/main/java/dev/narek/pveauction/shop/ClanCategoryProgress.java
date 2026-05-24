package dev.narek.pveauction.shop;

public record ClanCategoryProgress(int level, long earnedCoins) {
    public static final ClanCategoryProgress DEFAULT = new ClanCategoryProgress(1, 0);
}
