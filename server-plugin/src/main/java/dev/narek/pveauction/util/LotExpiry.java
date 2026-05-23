package dev.narek.pveauction.util;

import dev.narek.pveauction.model.AuctionLot;

public final class LotExpiry {

    private LotExpiry() {}

    public static long expiresAt(long createdAt, long expiryMs) {
        return createdAt + expiryMs;
    }

    public static boolean isExpired(AuctionLot lot, long expiryMs) {
        return System.currentTimeMillis() >= expiresAt(lot.createdAt(), expiryMs);
    }

    public static long remainingMs(AuctionLot lot, long expiryMs) {
        return Math.max(0, expiresAt(lot.createdAt(), expiryMs) - System.currentTimeMillis());
    }

    public static long minCreatedAtForListed(long expiryMs) {
        return System.currentTimeMillis() - expiryMs;
    }

    public static String formatRemaining(long ms) {
        long totalSec = (ms + 999) / 1000;
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) {
            return hours + "ч " + minutes + "м " + seconds + "с";
        }
        if (minutes > 0) {
            return minutes + "м " + seconds + "с";
        }
        return seconds + "с";
    }
}
