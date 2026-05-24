package dev.narek.pveauction.shop;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public enum SellAmountMode {
    ONE(1, "1 шт"),
    STACK(16, "16 шт"),
    STACK64(64, "64 шт"),
    ALL(-1, "ВСЁ");

    private final int amount;
    private final String label;

    SellAmountMode(int amount, String label) {
        this.amount = amount;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public SellAmountMode next() {
        return switch (this) {
            case ONE -> STACK;
            case STACK -> STACK64;
            case STACK64 -> ALL;
            case ALL -> ONE;
        };
    }

    public int resolveCount(Player player, Material material) {
        int available = countInInventory(player, material);
        if (available <= 0) {
            return 0;
        }
        if (this == ALL) {
            return available;
        }
        return Math.min(amount, available);
    }

    public static int countInInventory(Player player, Material material) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == material) {
            total += off.getAmount();
        }
        return total;
    }
}
