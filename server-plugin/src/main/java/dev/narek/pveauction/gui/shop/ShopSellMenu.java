package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ClanCategoryProgress;
import dev.narek.pveauction.shop.SellAmountMode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopSellMenu implements InventoryHolder {

    public static final int SLOT_BACK = 49;
    public static final int SLOT_INFO = 4;

    private final PveAuctionPlugin plugin;
    private final ShopService shop;
    private final Player viewer;
    private final ShopCategory category;
    private final double multiplier;
    private final ClanCategoryProgress clanProgress;
    private final boolean clanFocusHere;
    private Inventory inventory;

    private ShopSellMenu(
            PveAuctionPlugin plugin,
            ShopService shop,
            Player viewer,
            ShopCategory category,
            double multiplier,
            ClanCategoryProgress clanProgress,
            boolean clanFocusHere
    ) {
        this.plugin = plugin;
        this.shop = shop;
        this.viewer = viewer;
        this.category = category;
        this.multiplier = multiplier;
        this.clanProgress = clanProgress;
        this.clanFocusHere = clanFocusHere;
    }

    public static void open(
            PveAuctionPlugin plugin,
            ShopService shop,
            Player player,
            ShopCategory category,
            double mult,
            ClanCategoryProgress clanProgress,
            boolean clanFocusHere
    ) {
        ShopSellMenu menu = new ShopSellMenu(
                plugin, shop, player, category, mult, clanProgress, clanFocusHere
        );
        menu.inventory = Bukkit.createInventory(menu, 54,
                GuiText.title("Скупка: " + category.displayName(), NamedTextColor.GREEN));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        ShopGuiLayout.fillChest54(inventory);
        inventory.setItem(SLOT_INFO, headerItem());

        Map<Material, Integer> slots = ShopSellLayout.slotsFor(category.entries());
        for (ShopEntry entry : category.entries()) {
            Integer slot = slots.get(entry.material());
            if (slot == null) {
                continue;
            }
            long unit = Math.max(1, Math.round(entry.basePrice() * multiplier));
            SellAmountMode mode = shop.sellMode(viewer, entry.material());
            int inInv = mode.resolveCount(viewer, entry.material());
            inventory.setItem(slot, sellIcon(entry, unit, inInv, mode));
        }

        inventory.setItem(SLOT_BACK, GuiItems.button(Material.ARROW,
                Component.text("НАЗАД", NamedTextColor.RED, TextDecoration.BOLD)));
    }

    private ItemStack headerItem() {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Множитель: x" + String.format("%.2f", multiplier), NamedTextColor.GREEN));
        lore.add(Component.empty());
        if (clanProgress != null) {
            int level = clanProgress.level();
            long earned = clanProgress.earnedCoins();
            lore.add(Component.text("Уровень клана: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(level), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
            lore.add(ShopLeveling.progressBar(plugin, level, earned));
            lore.add(ShopLeveling.progressLoreLine(plugin, level, earned));
            if (clanFocusHere) {
                lore.add(Component.text("✦ Бонус клана активен", NamedTextColor.AQUA, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("Бонус на другой категории", NamedTextColor.DARK_GRAY));
            }
        } else {
            lore.add(Component.text("Вступи в клан для прокачки", NamedTextColor.DARK_GRAY));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Сырое — сверху, жареное — снизу", NamedTextColor.GRAY));
        return GuiItems.button(category.icon(),
                Component.text(category.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                lore.toArray(Component[]::new));
    }

    private ItemStack sellIcon(ShopEntry entry, long unitPrice, int inInv, SellAmountMode mode) {
        ItemStack display = new ItemStack(entry.material());
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.translatable(entry.material().translationKey())
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    Component.text("Цена: ", NamedTextColor.GRAY)
                            .append(Component.text(GuiItems.formatPrice(unitPrice) + " $ / шт", NamedTextColor.GOLD)),
                    Component.text("У тебя: ", NamedTextColor.GRAY)
                            .append(Component.text(inInv + " шт", NamedTextColor.AQUA)),
                    Component.text("Сдача: ", NamedTextColor.GRAY)
                            .append(Component.text(mode.label(), NamedTextColor.YELLOW, TextDecoration.BOLD)),
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
