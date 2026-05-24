package dev.narek.pveauction.shop;

import org.bukkit.Material;

import java.util.Set;

public record ShopEntry(Material material, long basePrice, Set<Material> accepts) {

    public ShopEntry(Material material, long basePrice) {
        this(material, basePrice, Set.of(material));
    }

    public boolean accepts(Material type) {
        return accepts.contains(type);
    }
}
