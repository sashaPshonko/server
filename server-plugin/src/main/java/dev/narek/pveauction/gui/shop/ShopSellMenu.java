package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ClanCategoryProgress;
import dev.narek.pveauction.shop.SellAmountMode;
import dev.narek.pveauction.shop.ShopCatalog;
import dev.narek.pveauction.shop.ShopCategory;
import dev.narek.pveauction.shop.ShopEntry;
import dev.narek.pveauction.shop.ShopLeveling;
import dev.narek.pveauction.shop.ShopSellLayout;
import dev.narek.pveauction.shop.ShopService;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.GuiText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ShopSellMenu implements InventoryHolder {

    /** Меняй при деплое — видно в книге в слоте 4, если JAR обновился. */
    public static final String LAYOUT_BUILD = "0.1.6";

    public static final int SLOT_BACK = 49;
    public static final int SLOT_INFO = 4;

    private final PveAuctionPlugin plugin;
    private final ShopService shop;
    private final Player viewer;
    private final ShopCategory category;
    private Inventory inventory;

    private ShopSellMenu(PveAuctionPlugin plugin, ShopService shop, Player viewer, ShopCategory category) {
        this.plugin = plugin;
        this.shop = shop;
        this.viewer = viewer;
        this.category = category;
    }

    public static void open(PveAuctionPlugin plugin, ShopService shop, Player player, ShopCategory category) {
        ShopSellMenu menu = new ShopSellMenu(plugin, shop, player, category);
        menu.inventory = Bukkit.createInventory(menu, 54,
                GuiText.title("Скупка [" + LAYOUT_BUILD + "]: " + category.displayName(), NamedTextColor.GREEN));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        ShopGuiLayout.fillChest54Sell(inventory);

        double multiplier = 1.0;
        ClanCategoryProgress clanProgress = null;
        boolean clanFocusHere = false;
        try {
            var member = plugin.clans().repo().findMember(viewer.getUniqueId());
            if (member.isPresent()) {
                int clanId = member.get().clanId();
                multiplier = shop.effectiveMultiplier(clanId, category);
                clanProgress = shop.categoryProgress(clanId, category);
                clanFocusHere = shop.focusCategory(clanId).map(c -> c == category).orElse(false);
            }
        } catch (SQLException ignored) {
        }

        inventory.setItem(SLOT_INFO, headerItem(multiplier, clanProgress, clanFocusHere));
        placeItemsManual(multiplier);
        inventory.setItem(SLOT_BACK, GuiItems.button(Material.ARROW,
                Component.text("НАЗАД", NamedTextColor.RED, TextDecoration.BOLD)));
    }

    /** Ручные слоты — без автосетки. */
    private void placeItemsManual(double multiplier) {
        switch (category) {
            case FARMER -> placeFarmer(multiplier);
            case FOOD -> placeFood(multiplier);
            case FISHER -> placeFisher(multiplier);
            case LOOT -> placeLoot(multiplier);
            case MINER -> placeMiner(multiplier);
            case BUTCHER -> placeButcher(multiplier);
            default -> placeAuto(multiplier);
        }
    }

    private void placeFarmer(double multiplier) {
        for (ShopEntry entry : category.entries()) {
            int slot = switch (entry.material()) {
                case COCOA_BEANS -> 21;
                case NETHER_WART -> 22;
                case CHORUS_FRUIT -> 23;
                case SUGAR_CANE -> 30;
                case CACTUS -> 31;
                case OAK_LOG -> 32;
                default -> -1;
            };
            put(entry, slot, multiplier);
        }
    }

    private void placeFood(double multiplier) {
        for (ShopEntry entry : category.entries()) {
            int slot = switch (entry.material()) {
                case CARROT -> 20;
                case POTATO -> 21;
                case BEETROOT -> 22;
                case SWEET_BERRIES -> 23;
                case GLOW_BERRIES -> 29;
                case WHEAT -> 30;
                case MELON_SLICE -> 31;
                case PUMPKIN -> 32;
                default -> -1;
            };
            put(entry, slot, multiplier);
        }
    }

    private void placeFisher(double multiplier) {
        for (ShopEntry entry : category.entries()) {
            int slot = switch (entry.material()) {
                case COD -> 20;
                case COOKED_COD -> 29;
                case SALMON -> 21;
                case COOKED_SALMON -> 30;
                case TROPICAL_FISH -> 22;
                case PUFFERFISH -> 23;
                case NAUTILUS_SHELL -> 24;
                case LILY_PAD -> 25;
                case LEATHER -> 31;
                case INK_SAC -> 32;
                case SADDLE -> 33;
                case NAME_TAG -> 34;
                default -> -1;
            };
            put(entry, slot, multiplier);
        }
    }

    private void placeLoot(double multiplier) {
        for (ShopEntry entry : category.entries()) {
            int slot = switch (entry.material()) {
                case SPIDER_EYE -> 20;
                case ROTTEN_FLESH -> 21;
                case BONE -> 22;
                case STRING -> 23;
                case BLAZE_ROD -> 24;
                case BREEZE_ROD -> 25;
                case SLIME_BALL -> 29;
                case MAGMA_CREAM -> 30;
                case GUNPOWDER -> 31;
                case ENDER_PEARL -> 32;
                case PRISMARINE_SHARD -> 33;
                case SHULKER_SHELL -> 34;
                default -> -1;
            };
            put(entry, slot, multiplier);
        }
    }

    private void placeMiner(double multiplier) {
        for (ShopEntry entry : category.entries()) {
            int slot = switch (entry.material()) {
                case COAL -> 13;
                case LAPIS_LAZULI -> 20;
                case REDSTONE -> 21;
                case AMETHYST_SHARD -> 22;
                case QUARTZ -> 23;
                case IRON_INGOT -> 24;
                case COPPER_INGOT -> 29;
                case GOLD_INGOT -> 30;
                case DIAMOND -> 31;
                case EMERALD -> 32;
                case NETHERITE_INGOT -> 33;
                default -> -1;
            };
            put(entry, slot, multiplier);
        }
    }

    private void placeButcher(double multiplier) {
        for (ShopEntry entry : category.entries()) {
            int slot = switch (entry.material()) {
                case PORKCHOP -> 20;
                case COOKED_PORKCHOP -> 29;
                case BEEF -> 21;
                case COOKED_BEEF -> 30;
                case CHICKEN -> 22;
                case COOKED_CHICKEN -> 31;
                case MUTTON -> 23;
                case COOKED_MUTTON -> 32;
                case RABBIT -> 24;
                case COOKED_RABBIT -> 33;
                default -> -1;
            };
            put(entry, slot, multiplier);
        }
    }

    private void placeAuto(double multiplier) {
        var slots = ShopSellLayout.slotsFor(category.entries());
        for (ShopEntry entry : category.entries()) {
            Integer slot = slots.get(entry.material());
            if (slot != null) {
                put(entry, slot, multiplier);
            }
        }
    }

    private void put(ShopEntry entry, int slot, double multiplier) {
        if (slot < 0) {
            plugin.getLogger().warning("Shop: нет слота для " + entry.material() + " в " + category.id());
            return;
        }
        long unit = Math.max(1, Math.round(entry.basePrice() * multiplier));
        SellAmountMode mode = shop.sellMode(viewer, entry.material());
        int inInv = SellAmountMode.countInInventory(viewer, entry);
        inventory.setItem(slot, sellIcon(entry, unit, inInv, mode, slot));
    }

    private ItemStack headerItem(double multiplier, ClanCategoryProgress clanProgress, boolean clanFocusHere) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Сборка: " + LAYOUT_BUILD, NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        lore.add(Component.text("Множитель: x" + ShopLeveling.formatMultiplier(multiplier), NamedTextColor.GREEN, TextDecoration.BOLD));
        lore.add(Component.empty());
        if (clanProgress != null) {
            int level = clanProgress.level();
            long earned = clanProgress.earnedCoins();
            double nextMult = ShopLeveling.multiplier(plugin, level + 1);
            lore.add(Component.text("Уровень: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(level), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
            lore.add(ShopLeveling.progressBar(plugin, level, earned));
            lore.add(ShopLeveling.progressLoreLine(plugin, level, earned));
            lore.add(Component.text("След. множитель: x" + ShopLeveling.formatMultiplier(nextMult), NamedTextColor.YELLOW));
            if (clanFocusHere) {
                lore.add(Component.text("✦ Бонус клана +10%", NamedTextColor.AQUA, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("Владелец: Shift+ЛКМ в категориях — +10%", NamedTextColor.DARK_GRAY));
            }
        } else {
            lore.add(Component.text("Вступи в клан для прокачки", NamedTextColor.DARK_GRAY));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Сырое над жареным", NamedTextColor.GRAY));
        return GuiItems.button(Material.BOOK,
                Component.text(category.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                lore.toArray(Component[]::new));
    }

    private ItemStack sellIcon(ShopEntry entry, long unitPrice, int inInv, SellAmountMode mode, int slotIndex) {
        ItemStack display = new ItemStack(entry.material());
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            Component name = ShopCatalog.isWoodEntry(entry)
                    ? Component.text("Древесина", NamedTextColor.WHITE, TextDecoration.BOLD)
                    .append(Component.text(" (все виды)", NamedTextColor.GRAY))
                    : Component.translatable(entry.material().translationKey())
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(name);
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Цена: ", NamedTextColor.GRAY)
                            .append(Component.text(GuiItems.formatPrice(unitPrice) + " $ / шт", NamedTextColor.GOLD)),
                    Component.text("У тебя: ", NamedTextColor.GRAY)
                            .append(Component.text(inInv + " шт", NamedTextColor.AQUA)),
                    Component.text("Сдача: ", NamedTextColor.GRAY)
                            .append(Component.text(mode.label(), NamedTextColor.YELLOW, TextDecoration.BOLD)),
                    Component.text("Слот GUI: " + slotIndex, NamedTextColor.DARK_GRAY),
                    Component.empty(),
                    Component.text("ЛКМ — продать", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("ПКМ — сменить кол-во", NamedTextColor.GOLD)
            ));
            GuiItems.decorateMeta(meta);
            display.setItemMeta(meta);
        }
        return ShopGuiTags.tagMaterial(plugin, display, entry.material());
    }

    public ShopCategory category() {
        return category;
    }

    public Player viewer() {
        return viewer;
    }

    public void refresh() {
        fill();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
