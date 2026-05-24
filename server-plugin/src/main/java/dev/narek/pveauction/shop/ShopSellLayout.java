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
 * Мясник: сырое над жареным по колонкам.
 * Остальные: плотная сетка без пустых ячеек под каждым предметом.
 * Рыбак: пары слева, одиночные справа в тех же двух рядах.
 */
public final class ShopSellLayout {

    private static final int WIDTH = 9;
    private static final int CONTENT_ROWS = 4;
    private static final int CONTENT_ROW_START = 1;

    private ShopSellLayout() {}

    public static Map<Material, Integer> slotsFor(List<ShopEntry> entries) {
        List<Placement> pairs = new ArrayList<>();
        List<Material> singles = new ArrayList<>();
        splitPlacements(entries, pairs, singles);

        Map<Material, Integer> slots = new HashMap<>();
        if (pairs.isEmpty() && singles.isEmpty()) {
            return slots;
        }
        if (!pairs.isEmpty() && singles.isEmpty()) {
            layoutPairsOnly(pairs, slots);
        } else if (pairs.isEmpty()) {
            layoutSinglesOnly(singles, slots);
        } else {
            layoutMixed(pairs, singles, slots);
        }
        return slots;
    }

    private static void layoutPairsOnly(List<Placement> pairs, Map<Material, Integer> slots) {
        int cols = pairs.size();
        int colOffset = (WIDTH - cols) / 2;
        int firstRow = CONTENT_ROW_START + (CONTENT_ROWS - 2) / 2;

        for (int c = 0; c < cols; c++) {
            Placement p = pairs.get(c);
            int top = (firstRow) * WIDTH + colOffset + c;
            int bottom = (firstRow + 1) * WIDTH + colOffset + c;
            slots.put(p.raw(), top);
            slots.put(p.cooked(), bottom);
        }
    }

    private static void layoutSinglesOnly(List<Material> singles, Map<Material, Integer> slots) {
        int rows = rowsForCount(singles.size());
        int firstRow = CONTENT_ROW_START + (CONTENT_ROWS - rows) / 2;
        placeSinglesGrid(singles, firstRow, rows, 0, WIDTH, slots);
    }

    private static void layoutMixed(List<Placement> pairs, List<Material> singles, Map<Material, Integer> slots) {
        int pairCols = pairs.size();
        int firstRow = CONTENT_ROW_START + (CONTENT_ROWS - 2) / 2;

        for (int c = 0; c < pairCols; c++) {
            Placement p = pairs.get(c);
            slots.put(p.raw(), firstRow * WIDTH + c);
            slots.put(p.cooked(), (firstRow + 1) * WIDTH + c);
        }

        placeSinglesGrid(singles, firstRow, 2, pairCols, WIDTH - pairCols, slots);
    }

    /**
     * Лишние предметы — в верхние ряды; каждый ряд по центру своей зоны.
     */
    private static void placeSinglesGrid(
            List<Material> singles,
            int firstRow,
            int rows,
            int areaStartCol,
            int areaWidth,
            Map<Material, Integer> slots
    ) {
        int count = singles.size();
        if (count == 0) {
            return;
        }
        int base = count / rows;
        int extra = count % rows;
        int maxInRow = 0;
        for (int r = 0; r < rows; r++) {
            maxInRow = Math.max(maxInRow, base + (r < extra ? 1 : 0));
        }
        int blockOffset = areaStartCol + Math.max(0, (areaWidth - maxInRow) / 2);

        int index = 0;
        for (int r = 0; r < rows; r++) {
            int inRow = base + (r < extra ? 1 : 0);
            for (int c = 0; c < inRow; c++) {
                slots.put(singles.get(index++), (firstRow + r) * WIDTH + blockOffset + c);
            }
        }
    }

    /** Сколько рядов занять под N одиночных предметов (макс. 4). */
    private static int rowsForCount(int count) {
        if (count <= 9) {
            return 1;
        }
        if (count <= 18) {
            return 2;
        }
        if (count <= 27) {
            return 3;
        }
        return 4;
    }

    private static void splitPlacements(List<ShopEntry> entries, List<Placement> pairs, List<Material> singles) {
        Set<Material> used = new HashSet<>();
        for (ShopEntry entry : entries) {
            Material mat = entry.material();
            if (used.contains(mat)) {
                continue;
            }
            Optional<Material> cooked = cookedVersion(mat);
            if (cooked.isPresent() && contains(entries, cooked.get())) {
                used.add(mat);
                used.add(cooked.get());
                pairs.add(new Placement(mat, cooked.get()));
                continue;
            }
            if (isCooked(mat) && contains(entries, rawVersion(mat).orElse(null))) {
                continue;
            }
            used.add(mat);
            singles.add(mat);
        }
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

    private record Placement(Material raw, Material cooked) {}
}
