package dev.narek.pveauction.gui.trader;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.item.StorageKeyType;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TraderGuiListener implements Listener {

    private final PveAuctionPlugin plugin;

    public TraderGuiListener(PveAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (holder == null) {
            holder = top.getHolder(false);
        }
        if (!(holder instanceof TraderShopMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!player.getUniqueId().equals(menu.viewer().getUniqueId())) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= top.getSize()) {
            return;
        }
        if (raw == TraderShopMenu.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        StorageKeyType type = TraderShopMenu.typeForSlot(raw);
        if (type == null) {
            return;
        }
        int price = menu.priceFor(type);
        if (!CustomItems.takeSilver(plugin, player, price)) {
            Msg.server(player, Msg.err("Не хватает серебра (нужно " + price + ")."));
            return;
        }
        var leftover = player.getInventory().addItem(CustomItems.storageKey(plugin, type));
        if (!leftover.isEmpty()) {
            CustomItems.addSilver(plugin, player, price);
            Msg.server(player, Msg.err("Нет места в инвентаре."));
            return;
        }
        Msg.server(player, Msg.ok("Куплено: ")
                .append(type.displayName())
                .append(Msg.ok(" за " + price + " серебра")));
        TraderShopMenu.open(plugin, player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder == null) {
            holder = event.getView().getTopInventory().getHolder(false);
        }
        if (holder instanceof TraderShopMenu) {
            event.setCancelled(true);
        }
    }
}
