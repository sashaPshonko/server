package dev.narek.pveauction.donate;

import java.util.Locale;
import java.util.Optional;

/** Справочник донатов (цвет — ключ для {@link dev.narek.pveauction.util.RankColors}). */
public enum DonateType {

    GERTSOG("gertsog", "Герцог", "GOLD", 1, 1),
    GORBATY("gorbaty", "Горбатый", "DARK_GREEN", 2, 1),
    VENOZNY("venozny", "Венозный", "DARK_RED", 3, 1),
    DYRAVY("dyravy", "Дырявый", "LIGHT_PURPLE", 4, 1),
    SMAZLIVY("smazlivy", "Смазливый", "YELLOW", 5, 1),
    SLYUNYAVY("slyunyavy", "Слюнявый", "AQUA", 6, 1),
    CHORNY("chorny", "Чорный", "DARK_GRAY", 7, 1),
    TEHNAR("tehnar", "Технарь", "BLUE", 8, 1),
    HAN_PERSIDSKIY("han_persidskiy", "Хан Персидский", "DARK_PURPLE", 9, 1),
    GRAFYA("grafya", "Графья с Помойки", "GOLD", 10, 1),
    GOTKA("gotka", "Готка", "MAROON", 11, 1);

    private final String id;
    private final String displayName;
    private final String color;
    private final int sortOrder;
    private final int defaultAhSlots;

    DonateType(String id, String displayName, String color, int sortOrder, int defaultAhSlots) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.sortOrder = sortOrder;
        this.defaultAhSlots = defaultAhSlots;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String color() {
        return color;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public int defaultAhSlots() {
        return defaultAhSlots;
    }

    public static Optional<DonateType> byId(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (DonateType type : values()) {
            if (type.id.equals(key)) {
                return Optional.of(type);
            }
        }
        for (DonateType type : values()) {
            if (type.displayName.equalsIgnoreCase(raw.trim())) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
