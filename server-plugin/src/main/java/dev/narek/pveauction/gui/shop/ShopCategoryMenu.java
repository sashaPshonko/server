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

    public static final int SLOT_BACK = 49;

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
        ShopGuiLayout.fillChest54(inventory);

        Integer clanId = plugin.clans().repo().findMember(viewer.getUniqueId())
                .map(m -> m.clanId())
                .orElse(null);
        Optional<ShopCategory> focus = clanId != null ? shop.focusCategory(clanId) : Optional.empty();

        ShopCategory[] cats = ShopCategory.values();
        int[] gridSlots = ShopGuiGridLayout.slotsForCount(cats.length);
        for (int i = 0; i < cats.length && i < gridSlots.length; i++) {
            int slot = gridSlots[i];
            ShopCategory cat = cats[i];
            slotCategory.put(slot, cat);
            inventory.setItem(slot, categoryIcon(cat, clanId, focus));
        }

        inventory.setItem(4, GuiItems.button(Material.GOLD_INGOT,
                Component.text("Скупка ресурсов", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Выбери категорию", NamedTextColor.GRAY),
                Component.text("Shift+ЛКМ — бонус клана (владелец)", NamedTextColor.DARK_GRAY)));

        inventory.setItem(SLOT_BACK, GuiItems.button(Material.ARROW,
                Component.text("НАЗАД", NamedTextColor.RED, TextDecoration.BOLD)));
    }

    private ItemStack categoryIcon(ShopCategory cat, Integer clanId, Optional<ShopCategory> focus) throws SQLException {
        var lore = new java.util.ArrayList<Component>();
        lore.add(Component.empty());
        if (clanId != null) {
            ClanCategoryProgress p = shop.categoryProgress(clanId, cat);
            double mult = ShopLeveling.multiplier(plugin, p.level());
            if (focus.isPresent() && focus.get() == cat) {
                mult *= 1.1;
            }
            lore.add(Component.text("Уровень: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(p.level()), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)));
            lore.add(ShopLeveling.progressBar(plugin, p.level(), p.earnedCoins()));
            lore.add(ShopLeveling.progressLoreLine(plugin, p.level(), p.earnedCoins()));
            lore.add(Component.text("Множитель: ", NamedTextColor.GRAY)
                    .append(Component.text("x" + ShopLeveling.formatMultiplier(mult), NamedTextColor.GREEN, TextDecoration.BOLD)));
            lore.add(Component.text("След. ур.: x" + ShopLeveling.formatMultiplier(ShopLeveling.multiplier(plugin, p.level() + 1)),
                    NamedTextColor.YELLOW));
            if (focus.isPresent() && focus.get() == cat) {
                lore.add(Component.text("✦ Бонус клана +10%", NamedTextColor.AQUA, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("Владелец: Shift+ЛКМ — +10%", NamedTextColor.DARK_GRAY));
            }
        } else {
            lore.add(Component.text("Вступи в клан для прокачки", NamedTextColor.DARK_GRAY));
        }
        lore.add(Component.empty());
        lore.add(Component.text("ЛКМ — открыть", NamedTextColor.GREEN, TextDecoration.BOLD));
        ItemStack icon = GuiItems.button(cat.icon(),
                Component.text(cat.displayName(), NamedTextColor.WHITE, TextDecoration.BOLD),
                lore.toArray(Component[]::new));
        return ShopGuiTags.tagCategory(plugin, icon, cat);
    }

    public ShopCategory categoryAt(int slot) {
        return slotCategory.get(slot);
    }

    public Player viewer() {
        return viewer;
    }

    public void reload() throws SQLException {
        fill();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
