package dev.narek.pveauction.gui.trader;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.item.StorageKeyType;
import dev.narek.pveauction.gui.shop.ShopGuiLayout;
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

public final class TraderShopMenu implements InventoryHolder {

    public static final int SLOT_INFO = 4;
    public static final int SLOT_SPHERES = 20;
    public static final int SLOT_WEAPONS = 21;
    public static final int SLOT_ARMOR = 22;
    public static final int SLOT_TOOLS = 23;
    public static final int SLOT_ENCHANTS = 24;
    public static final int SLOT_CLOSE = 49;

    private final PveAuctionPlugin plugin;
    private final Player viewer;
    private Inventory inventory;

    private TraderShopMenu(PveAuctionPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    public static void open(PveAuctionPlugin plugin, Player player) {
        TraderShopMenu menu = new TraderShopMenu(plugin, player);
        menu.inventory = Bukkit.createInventory(menu, 54, GuiText.title("Торгаш", NamedTextColor.GOLD));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() {
        ShopGuiLayout.fillChest54(inventory);
        int silver = CustomItems.countSilver(plugin, viewer);
        inventory.setItem(SLOT_INFO, GuiItems.button(Material.IRON_NUGGET,
                Component.text("Серебро", NamedTextColor.GRAY, TextDecoration.BOLD),
                Component.text("У тебя: ", NamedTextColor.GRAY)
                        .append(Component.text(silver + " шт.", NamedTextColor.WHITE, TextDecoration.BOLD)),
                Component.empty(),
                Component.text("Плати серебром за отмычки", NamedTextColor.DARK_GRAY)));

        inventory.setItem(SLOT_SPHERES, shopOffer(StorageKeyType.SPHERES, Material.ENDER_PEARL));
        inventory.setItem(SLOT_WEAPONS, shopOffer(StorageKeyType.WEAPONS, Material.IRON_SWORD));
        inventory.setItem(SLOT_ARMOR, shopOffer(StorageKeyType.ARMOR, Material.IRON_CHESTPLATE));
        inventory.setItem(SLOT_TOOLS, shopOffer(StorageKeyType.TOOLS, Material.IRON_PICKAXE));
        inventory.setItem(SLOT_ENCHANTS, shopOffer(StorageKeyType.ENCHANTMENTS, Material.ENCHANTED_BOOK));

        inventory.setItem(SLOT_CLOSE, GuiItems.button(Material.BARRIER,
                Component.text("Закрыть", NamedTextColor.RED)));
    }

    private ItemStack shopOffer(StorageKeyType type, Material icon) {
        int price = plugin.getConfig().getInt("trader.prices." + type.id(), 10);
        ItemStack display = new ItemStack(icon);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.displayName(type.displayName());
            List<Component> lore = new ArrayList<>(type.lore());
            lore.add(Component.empty());
            lore.add(Component.text("Цена: ", NamedTextColor.GRAY)
                    .append(Component.text(price + " серебра", NamedTextColor.WHITE, TextDecoration.BOLD)));
            lore.add(Component.text("ЛКМ — купить", NamedTextColor.GREEN));
            meta.lore(lore);
            GuiItems.decorateMeta(meta);
            display.setItemMeta(meta);
        }
        return display;
    }

    public int priceFor(StorageKeyType type) {
        return plugin.getConfig().getInt("trader.prices." + type.id(), 10);
    }

    public static StorageKeyType typeForSlot(int rawSlot) {
        return switch (rawSlot) {
            case SLOT_SPHERES -> StorageKeyType.SPHERES;
            case SLOT_WEAPONS -> StorageKeyType.WEAPONS;
            case SLOT_ARMOR -> StorageKeyType.ARMOR;
            case SLOT_TOOLS -> StorageKeyType.TOOLS;
            case SLOT_ENCHANTS -> StorageKeyType.ENCHANTMENTS;
            default -> null;
        };
    }

    public Player viewer() {
        return viewer;
    }

    public PveAuctionPlugin plugin() {
        return plugin;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
