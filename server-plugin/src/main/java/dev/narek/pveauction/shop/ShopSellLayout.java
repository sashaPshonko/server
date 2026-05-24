package dev.narek.pveauction.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Раскладка: в колонке сверху сырое, снизу жареное. */
public final class ShopSellLayout {

    private static final int[][] BANDS = {
            {10, 19},
            {28, 37}
    };

    private ShopSellLayout() {}

    public static Map<Material, Integer> slotsFor(List<ShopEntry> entries) {
        List<Placement> placements = buildPlacements(entries);
        Map<Material, Integer> slots = new HashMap<>();
        int band = 0;
        int col = 0;
        for (Placement placement : placements) {
            if (band >= BANDS.length) {
                break;
            }
            if (col > 6) {
                col = 0;
                band++;
                if (band >= BANDS.length) {
                    break;
                }
            }
            int top = BANDS[band][0] + col;
            int bottom = BANDS[band][1] + col;
            if (placement.cooked() != null) {
                slots.put(placement.raw(), top);
                slots.put(placement.cooked(), bottom);
            } else {
                slots.put(placement.material(), top);
            }
            col++;
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
