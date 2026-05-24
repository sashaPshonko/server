package dev.narek.pveauction.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Сырое над жареным в одной колонке, строки вплотную.
 * Вся сетка по центру сундука (9 колонок × 4 ряда под товары).
 */
public final class ShopSellLayout {

    private static final int WIDTH = 9;
    private static final int MAX_COLS = 9;
    /** Ряды 1–4 сундука (под шапкой, над низом). */
    private static final int CONTENT_ROWS = 4;
    private static final int CONTENT_ROW_START = 1;

    private ShopSellLayout() {}

    public static Map<Material, Integer> slotsFor(List<ShopEntry> entries) {
        List<Placement> placements = buildPlacements(entries);
        Map<Material, Integer> slots = new HashMap<>();
        if (placements.isEmpty()) {
            return slots;
        }

        int bandsNeeded = (placements.size() + MAX_COLS - 1) / MAX_COLS;
        int rowPairs = bandsNeeded * 2;
        int firstRow = CONTENT_ROW_START + Math.max(0, (CONTENT_ROWS - rowPairs) / 2);

        int index = 0;
        for (int band = 0; band < bandsNeeded && index < placements.size(); band++) {
            int count = Math.min(MAX_COLS, placements.size() - index);
            int colOffset = (WIDTH - count) / 2;
            int topRow = firstRow + band * 2;
            int bottomRow = topRow + 1;
            int topBase = topRow * WIDTH;
            int bottomBase = bottomRow * WIDTH;

            for (int c = 0; c < count; c++) {
                Placement placement = placements.get(index++);
                int top = topBase + colOffset + c;
                int bottom = bottomBase + colOffset + c;
                if (placement.cooked() != null) {
                    slots.put(placement.raw(), top);
                    slots.put(placement.cooked(), bottom);
                } else {
                    slots.put(placement.material(), top);
                }
            }
        }
        return slots;
    }

    private static List<Placement> buildPlacements(List<ShopEntry> entries) {
        Set<Material> used = new HashSet<>();
        List<Placement> result = new ArrayList<>();
        for (ShopEntry entry : entries) {
            Material mat = entry.material();
            if (used.contains(mat)) {
                continue;
            }
            Optional<Material> cooked = cookedVersion(mat);
            if (cooked.isPresent() && contains(entries, cooked.get())) {
                used.add(mat);
                used.add(cooked.get());
                result.add(new Placement(mat, cooked.get()));
                continue;
            }
            if (isCooked(mat) && contains(entries, rawVersion(mat).orElse(null))) {
                continue;
            }
            used.add(mat);
            result.add(new Placement(mat, null));
        }
        return result;
    }

    private static boolean contains(List<ShopEntry> entries, Material material) {
        if (material == null) {
            return false;
        }
        for (ShopEntry e : entries) {
            if (e.material() == material) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Material> cookedVersion(Material raw) {
        if (isCooked(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Material.valueOf("COOKED_" + raw.name()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<Material> rawVersion(Material cooked) {
        if (!isCooked(cooked)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Material.valueOf(cooked.name().substring("COOKED_".length())));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static boolean isCooked(Material material) {
        return material.name().startsWith("COOKED_");
    }

    private record Placement(Material raw, Material cooked) {
        Material material() {
            return raw;
        }
    }
}
