package dev.narek.pveauction.lockpick.armor;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.auth.AuthService;
import dev.narek.pveauction.item.CustomItems;
import dev.narek.pveauction.item.StorageKeyType;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Активация отмычки к броне на кузнечном столе спавна. */
public final class ArmorLockpickService {

    public enum Result {
        SUCCESS,
        NOT_ARMOR_KEY,
        NOT_SMITHING_TABLE,
        NOT_SPAWN_WORLD,
        NEED_AUTH
    }

    private final PveAuctionPlugin plugin;
    private final WorldTravelService worlds;
    private final AuthService auth;
    private final ArmorLockpickGenerator generator;
    private final Map<UUID, Long> recentSuccessMs = new ConcurrentHashMap<>();

    public ArmorLockpickService(PveAuctionPlugin plugin) {
        this.plugin = plugin;
        this.worlds = plugin.worlds();
        this.auth = plugin.auth();
        var cfg = plugin.getConfig();
        this.generator = new ArmorLockpickGenerator(
                cfg.getDouble("lockpick.armor.diamond-chance", 0.55),
                cfg.getDouble("lockpick.armor.thorns-chance", 0.35),
                cfg.getDouble("lockpick.armor.diamond-extra-enchant-chance", 0.45),
                cfg.getDouble("lockpick.armor.netherite-protection-5-chance", 0.5),
                cfg.getDouble("lockpick.armor.netherite-prot5-body-bonus-chance", 0.5),
                cfg.getDouble("lockpick.armor.netherite-thorned-extra-chance", 0.45),
                cfg.getDouble("lockpick.armor.branch-pick-chance", 0.5)
        );
    }

    public static boolean isSmithingStation(Block block) {
        return block != null && block.getType() == Material.SMITHING_TABLE;
    }

    public boolean isSpawnSmithing(Block block) {
        return block != null && worlds.isSpawnWorld(block.getWorld()) && isSmithingStation(block);
    }

    public Result tryActivate(Player player, Block block) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = recentSuccessMs.get(id);
        if (last != null && now - last < 400) {
            return Result.SUCCESS;
        }
        if (!isSmithingStation(block)) {
            return Result.NOT_SMITHING_TABLE;
        }
        if (!worlds.isSpawnWorld(block.getWorld())) {
            return Result.NOT_SPAWN_WORLD;
        }
        if (auth != null && !auth.isLoggedIn(player)) {
            return Result.NEED_AUTH;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (CustomItems.keyType(plugin, hand) != StorageKeyType.ARMOR) {
            return Result.NOT_ARMOR_KEY;
        }

        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }

        ItemStack reward = generator.roll();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                block.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        Msg.server(player, Msg.ok("Отмычка сработала — получена броня."));
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1f);
        recentSuccessMs.put(id, now);
        return Result.SUCCESS;
    }

    public void sendFailure(Player player, Result result) {
        switch (result) {
            case NEED_AUTH -> Msg.server(player, Msg.err("Сначала войди: /login или /reg"));
            case NOT_SPAWN_WORLD -> Msg.server(player, Msg.err("Кузнечный стол для этой отмычки только на спавне."));
            case NOT_ARMOR_KEY -> Msg.server(
                    player,
                    Msg.err("Нужна отмычка к броне с торгаша (крюк с подписью)."));
            case NOT_SMITHING_TABLE -> {
            }
            default -> {
            }
        }
    }
}
