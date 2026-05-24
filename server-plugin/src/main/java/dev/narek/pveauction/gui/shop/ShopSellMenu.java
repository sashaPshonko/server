package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.SellAmountMode;
import dev.narek.pveauction.shop.ShopCategory;
import dev.narek.pveauction.shop.ShopEntry;
import dev.narek.pveauction.shop.ShopService;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.GuiText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShopSellMenu implements InventoryHolder {

    public static final int SLOT_BACK = 49;
    public static final int SLOT_MODE = 53;
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final PveAuctionPlugin plugin;
    private final ShopService shop;
    private final Player viewer;
    private final ShopCategory category;
    private final double multiplier;
    private Inventory inventory;
    private final Map<Integer, ShopEntry> slotEntry = new HashMap<>();

    private ShopSellMenu(
            PveAuctionPlugin plugin,
            ShopService shop,
            Player viewer,
            ShopCategory category,
            double multiplier
    ) {
        this.plugin = plugin;
        this.shop = shop;
        this.viewer = viewer;
        this.category = category;
        this.multiplier = multiplier;
    }

    public static void open(PveAuctionPlugin plugin, ShopService shop, Player player, ShopCategory category, double mult) {
        ShopSellMenu menu = new ShopSellMenu(plugin, shop, player, category, mult);
        menu.inventory = Bukkit.createInventory(menu, 54,
                GuiText.title("Скупка: " + category.displayName(), NamedTextColor.GREEN));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        inventory.clear();
        slotEntry.clear();
        ShopGuiLayout.fillChest54(inventory);

        inventory.setItem(4, GuiItems.button(category.icon(),
                Component.text(category.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Множитель: x" + String.format("%.2f", multiplier), NamedTextColor.GREEN)));

        int idx = 0;
        for (ShopEntry entry : category.entries()) {
            if (idx >= ITEM_SLOTS.length) {
                break;
            }
            int slot = ITEM_SLOTS[idx++];
            slotEntry.put(slot, entry);
            long unit = Math.max(1, Math.round(entry.basePrice() * multiplier));
            int inInv = shop.sellMode(viewer).resolveCount(viewer, entry.material());
            inventory.setItem(slot, sellIcon(entry, unit, inInv));
        }

        inventory.setItem(SLOT_BACK, GuiItems.button(org.bukkit.Material.ARROW,
                Component.text("НАЗАД", NamedTextColor.RED, TextDecoration.BOLD)));
        fillModeButton();
    }

    private void fillModeButton() {
        SellAmountMode mode = shop.sellMode(viewer);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("[ЛКМ] настроить", NamedTextColor.GOLD));
        lines.add(Component.empty());
        for (SellAmountMode m : SellAmountMode.values()) {
            NamedTextColor c = m == mode ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY;
            String prefix = m == mode ? ">> " : "   ";
            lines.add(Component.text(prefix + "Продать " + m.label(), c));
        }
        inventory.setItem(SLOT_MODE, GuiItems.button(org.bukkit.Material.HOPPER,
                Component.text("Сдача: " + mode.label(), NamedTextColor.YELLOW, TextDecoration.BOLD),
                lines.toArray(Component[]::new)));
    }

    private ItemStack sellIcon(ShopEntry entry, long unitPrice, int inInv) {
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
                    Component.empty(),
                    Component.text("ЛКМ — сдать", NamedTextColor.GREEN, TextDecoration.BOLD)
            ));
            GuiItems.decorateMeta(meta);
            display.setItemMeta(meta);
        }
        return ShopGuiTags.tagMaterial(plugin, display, entry.material());
    }

    public ShopEntry entryAt(int slot) {
        return slotEntry.get(slot);
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
