package dev.narek.pveauction.gui.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.shop.ClanCategoryProgress;
import dev.narek.pveauction.shop.ShopCategory;
import dev.narek.pveauction.shop.ShopLeveling;
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ShopCategoryMenu implements InventoryHolder {

    public static final int SLOT_BACK = 45;
    public static final int SLOT_MODE = 49;
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15};

    private final PveAuctionPlugin plugin;
    private final ShopService shop;
    private final Player viewer;
    private Inventory inventory;
    private final Map<Integer, ShopCategory> slotCategory = new HashMap<>();

    private ShopCategoryMenu(PveAuctionPlugin plugin, ShopService shop, Player viewer) {
        this.plugin = plugin;
        this.shop = shop;
        this.viewer = viewer;
    }

    public static void open(PveAuctionPlugin plugin, ShopService shop, Player player) throws SQLException {
        ShopCategoryMenu menu = new ShopCategoryMenu(plugin, shop, player);
        menu.inventory = Bukkit.createInventory(menu, 54, GuiText.title("Скупка ресурсов", NamedTextColor.GREEN));
        menu.fill();
        player.openInventory(menu.inventory);
    }

    private void fill() throws SQLException {
        inventory.clear();
        slotCategory.clear();
        var glass = GuiItems.glassFill();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        Integer clanId = plugin.clans().repo().findMember(viewer.getUniqueId())
                .map(m -> m.clanId())
                .orElse(null);
        Optional<ShopCategory> focus = clanId != null ? shop.focusCategory(clanId) : Optional.empty();

        ShopCategory[] cats = ShopCategory.values();
        for (int i = 0; i < cats.length && i < CATEGORY_SLOTS.length; i++) {
            int slot = CATEGORY_SLOTS[i];
            ShopCategory cat = cats[i];
            slotCategory.put(slot, cat);
            inventory.setItem(slot, categoryIcon(cat, clanId, focus));
        }

        inventory.setItem(SLOT_BACK, GuiItems.button(Material.ARROW,
                Component.text("НАЗАД", NamedTextColor.GREEN, TextDecoration.BOLD)));
        fillModeButton();
    }

    private void fillModeButton() {
        var mode = shop.sellMode(viewer);
        inventory.setItem(SLOT_MODE, GuiItems.button(Material.HOPPER,
                Component.text("Сдача: " + mode.label(), NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text("[ПКМ] сменить режим", NamedTextColor.GOLD),
                Component.text(">> " + mode.label(), NamedTextColor.GREEN)));
    }

    private ItemStack categoryIcon(ShopCategory cat, Integer clanId, Optional<ShopCategory> focus) throws SQLException {
        var lore = new java.util.ArrayList<Component>();
        lore.add(Component.empty());
        if (clanId != null) {
            ClanCategoryProgress p = shop.categoryProgress(clanId, cat);
            double mult = ShopLeveling.multiplier(plugin, p.level());
            lore.add(Component.text("★ Уровень: ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(String.valueOf(p.level()), NamedTextColor.WHITE)));
            lore.add(Component.text("Прогресс: ", NamedTextColor.GOLD)
                    .append(Component.text(ShopLeveling.progressText(plugin, p.level(), p.earnedCoins()), NamedTextColor.WHITE)));
            lore.add(Component.text("Множитель: ", NamedTextColor.GREEN)
                    .append(Component.text("x" + String.format("%.2f", mult), NamedTextColor.WHITE)));
            if (focus.isPresent() && focus.get() == cat) {
                lore.add(Component.text("✦ Бонус клана активен", NamedTextColor.AQUA, TextDecoration.BOLD));
            } else if (focus.isEmpty()) {
                lore.add(Component.text("Владелец: Shift+ЛКМ — бонус клана", NamedTextColor.GRAY));
            }
        } else {
            lore.add(Component.text("В клане — прокачка категорий", NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        lore.add(Component.text("ЛКМ — открыть", NamedTextColor.GREEN));
        return GuiItems.button(cat.icon(),
                Component.text(cat.displayName(), NamedTextColor.WHITE, TextDecoration.BOLD),
                lore.toArray(Component[]::new));
    }

    public ShopCategory categoryAt(int slot) {
        return slotCategory.get(slot);
    }

    public Player viewer() {
        return viewer;
    }

    public void refreshMode() {
        fillModeButton();
    }

    public void reload() throws SQLException {
        fill();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
