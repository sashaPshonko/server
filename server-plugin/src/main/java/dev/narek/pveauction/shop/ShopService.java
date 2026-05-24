package dev.narek.pveauction.shop;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.db.ShopRepository;
import dev.narek.pveauction.model.ClanMember;
import dev.narek.pveauction.util.GuiItems;
import dev.narek.pveauction.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopService {

    private final PveAuctionPlugin plugin;
    private final ShopRepository shopDb;
    private final Map<UUID, Map<Material, SellAmountMode>> sellModes = new ConcurrentHashMap<>();

    public ShopService(PveAuctionPlugin plugin, ShopRepository shopDb) {
        this.plugin = plugin;
        this.shopDb = shopDb;
    }

    public ShopRepository repo() {
        return shopDb;
    }

    public SellAmountMode sellMode(Player player, Material material) {
        Map<Material, SellAmountMode> perItem = sellModes.get(player.getUniqueId());
        if (perItem == null) {
            return SellAmountMode.ONE;
        }
        return perItem.getOrDefault(material, SellAmountMode.ONE);
    }

    public SellAmountMode cycleSellMode(Player player, Material material) {
        Map<Material, SellAmountMode> perItem = sellModes.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new ConcurrentHashMap<>()
        );
        SellAmountMode next = sellMode(player, material).next();
        perItem.put(material, next);
        return next;
    }

    /** Множитель по уровню прокачки этой категории (сдаёшь мясника — растёт мясник). */
    public double clanMultiplier(int clanId, ShopCategory category) throws SQLException {
        ClanCategoryProgress progress = shopDb.getProgress(clanId, category);
        return ShopLeveling.multiplier(plugin, progress.level());
    }

    /** Доп. множитель, если владелец выбрал эту категорию бонусом клана (+10% к базовому). */
    public double focusBonusMultiplier(int clanId, ShopCategory category) throws SQLException {
        Optional<String> focus = shopDb.findFocusCategory(clanId);
        if (focus.isPresent() && focus.get().equals(category.id())) {
            return 1.1;
        }
        return 1.0;
    }

    public double effectiveMultiplier(int clanId, ShopCategory category) throws SQLException {
        return clanMultiplier(clanId, category) * focusBonusMultiplier(clanId, category);
    }

    public ClanCategoryProgress categoryProgress(int clanId, ShopCategory category) throws SQLException {
        return shopDb.getProgress(clanId, category);
    }

    public Optional<ShopCategory> focusCategory(int clanId) throws SQLException {
        return shopDb.findFocusCategory(clanId).map(ShopCategory::byId);
    }

    public SellResult sell(Player player, ShopCategory category, Material material, long basePrice) throws SQLException {
        if (!plugin.economy().isEnabled()) {
            return SellResult.fail("Экономика не подключена.");
        }

        SellAmountMode mode = sellMode(player, material);
        int count = mode.resolveCount(player, material);
        if (count <= 0) {
            return SellResult.fail("Нет предметов в инвентаре.");
        }

        double mult = 1.0;
        Integer clanId = null;
        Optional<ClanMember> member = plugin.clans().repo().findMember(player.getUniqueId());
        if (member.isPresent()) {
            clanId = member.get().clanId();
            mult = effectiveMultiplier(clanId, category);
        }

        long unitPrice = Math.max(1, Math.round(basePrice * mult));
        long total = unitPrice * count;

        if (!removeFromInventory(player, material, count)) {
            return SellResult.fail("Не удалось снять предметы.");
        }
        if (!plugin.economy().deposit(player, total)) {
            player.getInventory().addItem(new ItemStack(material, count));
            return SellResult.fail("Не удалось выдать деньги.");
        }

        boolean leveled = false;
        if (clanId != null) {
            ClanCategoryProgress before = shopDb.getProgress(clanId, category);
            ShopLeveling.LevelUpResult up = ShopLeveling.addEarned(plugin, before.level(), before.earnedCoins(), total);
            shopDb.saveProgress(clanId, category, new ClanCategoryProgress(up.level(), up.earnedCoins()));
            leveled = up.leveledUp();
            if (leveled) {
                notifyClanLevelUp(clanId, category, up.level());
            }
        }

        return SellResult.ok(count, unitPrice, total, mult, leveled);
    }

    private void notifyClanLevelUp(int clanId, ShopCategory category, int level) {
        double mult;
        try {
            mult = effectiveMultiplier(clanId, category);
        } catch (SQLException e) {
            mult = ShopLeveling.multiplier(plugin, level);
        }
        Component body = Msg.ok("Категория «" + category.displayName() + "» — уровень " + level
                + " (x" + ShopLeveling.formatMultiplier(mult) + ")");
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                for (var m : plugin.clans().repo().listMembers(clanId)) {
                    Player online = Bukkit.getPlayer(m.playerUuid());
                    if (online != null) {
                        Msg.shop(online, body);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe(e.getMessage());
            }
        });
    }

    public void setClanFocus(Player player, ShopCategory category) throws SQLException {
        ClanMember member = plugin.clans().repo().findMember(player.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("Ты не в клане."));
        if (!member.isOwner()) {
            throw new IllegalStateException("Только владелец выбирает категорию клана.");
        }
        shopDb.setFocusCategory(member.clanId(), category);
    }

    private static boolean removeFromInventory(Player player, Material material, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }
            int take = Math.min(left, stack.getAmount());
            int remaining = stack.getAmount() - take;
            if (remaining <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(remaining);
            }
            left -= take;
            if (left <= 0) {
                player.getInventory().setStorageContents(contents);
                return true;
            }
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (left > 0 && off.getType() == material) {
            int take = Math.min(left, off.getAmount());
            int remaining = off.getAmount() - take;
            if (remaining <= 0) {
                player.getInventory().setItemInOffHand(null);
            } else {
                off.setAmount(remaining);
            }
            left -= take;
        }
        if (left > 0) {
            return false;
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    public record SellResult(
            boolean success,
            String error,
            int count,
            long unitPrice,
            long total,
            double multiplier,
            boolean clanLeveled
    ) {
        static SellResult ok(int count, long unitPrice, long total, double mult, boolean leveled) {
            return new SellResult(true, null, count, unitPrice, total, mult, leveled);
        }

        static SellResult fail(String error) {
            return new SellResult(false, error, 0, 0, 0, 1.0, false);
        }

        public void send(Player player) {
            if (!success) {
                Msg.shop(player, Msg.err(error));
                return;
            }
            Msg.shop(player, Msg.ok("Сдано " + count + " шт. за ")
                    .append(Msg.money(total))
                    .append(Msg.ok(multiplier > 1.001 ? " (x" + String.format("%.2f", multiplier) + ")" : "")));
            if (clanLeveled) {
                Msg.shop(player, Msg.info("Клан прокачал категорию!"));
            }
        }
    }
}
