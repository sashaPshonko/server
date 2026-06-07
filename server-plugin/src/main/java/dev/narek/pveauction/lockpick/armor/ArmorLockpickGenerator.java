package dev.narek.pveauction.lockpick.armor;

import dev.narek.pveauction.lockpick.EnchantApply;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Алмаз — Защита III + минимум один зачар (не шипы). Незерит — ветки IV/V по ТЗ.
 */
public final class ArmorLockpickGenerator {

    private static final Material[] DIAMOND_PIECES = {
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS,
    };
    private static final Material[] NETHERITE_PIECES = {
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS,
    };

    private final double diamondChance;
    private final double thornsChance;
    private final double diamondExtraEnchantChance;
    private final double netheriteProt5Chance;
    private final double branchPickChance;

    public ArmorLockpickGenerator(
            double diamondChance,
            double thornsChance,
            double diamondExtraEnchantChance,
            double netheriteProt5Chance,
            double netheriteProt5BodyBonusChance,
            double netheriteThornedExtraChance,
            double branchPickChance
    ) {
        this.diamondChance = clamp01(diamondChance);
        this.thornsChance = clamp01(thornsChance);
        this.diamondExtraEnchantChance = clamp01(diamondExtraEnchantChance);
        this.netheriteProt5Chance = clamp01(netheriteProt5Chance);
        this.branchPickChance = clamp01(branchPickChance);
    }

    public ItemStack roll() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        boolean diamond = rnd.nextDouble() < diamondChance;
        Material[] pool = diamond ? DIAMOND_PIECES : NETHERITE_PIECES;
        ItemStack item = new ItemStack(pool[rnd.nextInt(pool.length)]);
        ArmorSlot slot = slotOf(item.getType());

        if (diamond) {
            boolean thorns = rnd.nextDouble() < thornsChance;
            rollDiamond(item, slot, thorns, rnd);
        } else {
            rollNetherite(item, slot, rnd);
        }
        return item;
    }

    private void rollNetherite(ItemStack item, ArmorSlot slot, ThreadLocalRandom rnd) {
        boolean protection5 = rnd.nextDouble() < netheriteProt5Chance;
        boolean thorns = rnd.nextDouble() < thornsChance;

        if (!protection5) {
            rollNetheriteProtection4(item, slot, thorns, rnd);
        } else {
            rollNetheriteProtection5(item, slot, thorns, rnd);
        }
    }

    /** Защита IV */
    private void rollNetheriteProtection4(ItemStack item, ArmorSlot slot, boolean thorns, ThreadLocalRandom rnd) {
        EnchantApply.set(item, Enchantment.PROTECTION, 4);
        if (thorns) {
            EnchantApply.set(item, Enchantment.THORNS, rnd.nextInt(1, 4));
            EnchantApply.set(item, Enchantment.UNBREAKING, 4);
            return;
        }
        applyNetheriteProt4NoThornsEnchants(item, slot);
    }

    private void applyNetheriteProt4NoThornsEnchants(ItemStack item, ArmorSlot slot) {
        EnchantApply.set(item, Enchantment.UNBREAKING, 4);
        switch (slot) {
            case HELMET -> EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 4);
            case CHEST, LEGGINGS -> {
                EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 4);
                EnchantApply.set(item, Enchantment.FIRE_PROTECTION, 4);
            }
            case BOOTS -> {
                EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 4);
                EnchantApply.set(item, Enchantment.FEATHER_FALLING, 4);
            }
        }
    }

    /** Защита V: доп. зачары по слоту только без шипов */
    private void rollNetheriteProtection5(ItemStack item, ArmorSlot slot, boolean thorns, ThreadLocalRandom rnd) {
        EnchantApply.set(item, Enchantment.PROTECTION, 5);
        if (thorns) {
            EnchantApply.set(item, Enchantment.THORNS, rnd.nextInt(1, 4));
            EnchantApply.set(item, Enchantment.UNBREAKING, pickBranch(rnd) ? 4 : 5);
            return;
        }
        EnchantApply.set(item, Enchantment.UNBREAKING, pickBranch(rnd) ? 4 : 5);
        applyNetheriteProt5SlotExtras(item, slot, rnd);
    }

    private void applyNetheriteProt5SlotExtras(ItemStack item, ArmorSlot slot, ThreadLocalRandom rnd) {
        switch (slot) {
            case CHEST, LEGGINGS -> {
                int body = rnd.nextInt(3);
                if (body == 0) {
                    return;
                }
                if (body == 1) {
                    EnchantApply.set(item, Enchantment.FIRE_PROTECTION, 5);
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 5);
                } else {
                    EnchantApply.set(item, Enchantment.FIRE_PROTECTION, 4);
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 4);
                }
            }
            case BOOTS -> {
                if (pickBranch(rnd)) {
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 5);
                } else {
                    EnchantApply.set(item, Enchantment.DEPTH_STRIDER, rnd.nextInt(1, 4));
                }
            }
            case HELMET -> {
                if (pickBranch(rnd)) {
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, 5);
                } else {
                    EnchantApply.set(item, Enchantment.AQUA_AFFINITY, 1);
                    EnchantApply.set(item, Enchantment.RESPIRATION, 2);
                }
            }
        }
    }

    private void rollDiamond(ItemStack item, ArmorSlot slot, boolean thorns, ThreadLocalRandom rnd) {
        EnchantApply.set(item, Enchantment.PROTECTION, 3);
        if (thorns) {
            EnchantApply.set(item, Enchantment.THORNS, rnd.nextInt(1, 4));
        }
        if (rnd.nextBoolean()) {
            EnchantApply.set(item, Enchantment.UNBREAKING, rnd.nextInt(1, 6));
        }
        rollDiamondExtras(item, slot, rnd);
        ensureDiamondMandatoryNonThornsEnchant(item, slot, rnd);
    }

    private void rollDiamondExtras(ItemStack item, ArmorSlot slot, ThreadLocalRandom rnd) {
        switch (slot) {
            case HELMET -> {
                maybe(item, rnd, Enchantment.PROJECTILE_PROTECTION, 1, 4);
                maybe(item, rnd, Enchantment.FIRE_PROTECTION, 1, 4);
                if (rnd.nextDouble() < diamondExtraEnchantChance) {
                    if (pickBranch(rnd)) {
                        EnchantApply.set(item, Enchantment.AQUA_AFFINITY, 1);
                    } else {
                        EnchantApply.set(item, Enchantment.RESPIRATION, rnd.nextInt(1, 4));
                    }
                }
            }
            case CHEST, LEGGINGS -> {
                maybe(item, rnd, Enchantment.PROJECTILE_PROTECTION, 1, 4);
                maybe(item, rnd, Enchantment.FIRE_PROTECTION, 1, 4);
            }
            case BOOTS -> {
                maybe(item, rnd, Enchantment.PROJECTILE_PROTECTION, 1, 4);
                maybe(item, rnd, Enchantment.FIRE_PROTECTION, 1, 4);
                maybe(item, rnd, Enchantment.FEATHER_FALLING, 1, 4);
                if (rnd.nextDouble() < diamondExtraEnchantChance) {
                    EnchantApply.set(item, Enchantment.DEPTH_STRIDER, rnd.nextInt(1, 4));
                }
            }
        }
    }

    /** Минимум один зачар кроме Защиты III; обязательный — не шипы */
    private void ensureDiamondMandatoryNonThornsEnchant(ItemStack item, ArmorSlot slot, ThreadLocalRandom rnd) {
        if (hasNonThornsEnchantBesidesProtection(item)) {
            return;
        }
        switch (slot) {
            case HELMET -> {
                if (pickBranch(rnd)) {
                    EnchantApply.set(item, Enchantment.UNBREAKING, rnd.nextInt(1, 4));
                } else {
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, rnd.nextInt(1, 4));
                }
            }
            case CHEST, LEGGINGS -> {
                if (pickBranch(rnd)) {
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, rnd.nextInt(1, 4));
                } else {
                    EnchantApply.set(item, Enchantment.FIRE_PROTECTION, rnd.nextInt(1, 4));
                }
            }
            case BOOTS -> {
                if (pickBranch(rnd)) {
                    EnchantApply.set(item, Enchantment.PROJECTILE_PROTECTION, rnd.nextInt(1, 4));
                } else {
                    EnchantApply.set(item, Enchantment.FEATHER_FALLING, rnd.nextInt(1, 4));
                }
            }
        }
    }

    private void maybe(ItemStack item, ThreadLocalRandom rnd, Enchantment ench, int minLvl, int maxLvl) {
        if (rnd.nextDouble() < diamondExtraEnchantChance) {
            EnchantApply.set(item, ench, rnd.nextInt(minLvl, maxLvl));
        }
    }

    private boolean hasNonThornsEnchantBesidesProtection(ItemStack item) {
        for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) {
            Enchantment key = e.getKey();
            if (key == Enchantment.PROTECTION || key == Enchantment.THORNS) {
                continue;
            }
            if (e.getValue() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean pickBranch(ThreadLocalRandom rnd) {
        return rnd.nextDouble() < branchPickChance;
    }

    private enum ArmorSlot {
        HELMET,
        CHEST,
        LEGGINGS,
        BOOTS
    }

    private static ArmorSlot slotOf(Material type) {
        return switch (type) {
            case DIAMOND_HELMET, NETHERITE_HELMET -> ArmorSlot.HELMET;
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> ArmorSlot.CHEST;
            case DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> ArmorSlot.LEGGINGS;
            case DIAMOND_BOOTS, NETHERITE_BOOTS -> ArmorSlot.BOOTS;
            default -> ArmorSlot.CHEST;
        };
    }

    private static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }
}
