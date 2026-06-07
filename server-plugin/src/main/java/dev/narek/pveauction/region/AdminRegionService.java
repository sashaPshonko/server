package dev.narek.pveauction.region;

import dev.narek.pveauction.PveAuctionPlugin;
import dev.narek.pveauction.db.AdminRegionRepository;
import dev.narek.pveauction.model.AdminRegion;
import dev.narek.pveauction.model.SavedLocation;
import dev.narek.pveauction.util.Msg;
import dev.narek.pveauction.world.WorldTeleportService;
import dev.narek.pveauction.world.WorldTravelService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminRegionService {

    private final NamespacedKey wandKey;
    private final PveAuctionPlugin plugin;
    private final AdminRegionRepository repo;
    private final WorldTravelService worlds;
    private final Map<UUID, CuboidSelection> selections = new ConcurrentHashMap<>();

    public AdminRegionService(PveAuctionPlugin plugin, AdminRegionRepository repo, WorldTravelService worlds) {
        this.plugin = plugin;
        this.repo = repo;
        this.worlds = worlds;
        this.wandKey = new NamespacedKey(plugin, "apriv_wand");
    }

    public AdminRegionRepository repo() {
        return repo;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("admin-regions.enabled", true);
    }

    public boolean canManage(Player player) {
        return player.hasPermission("pveauction.apriv") || player.hasPermission("pveauction.admin");
    }

    public boolean canBuildInRegions(Player player) {
        return canManage(player);
    }

    public Material wandMaterial() {
        String raw = plugin.getConfig().getString("admin-regions.wand-material", "WOODEN_AXE");
        Material mat = Material.matchMaterial(raw);
        return mat != null ? mat : Material.WOODEN_AXE;
    }

    public long maxHorizontalArea() {
        return plugin.getConfig().getLong("admin-regions.max-horizontal-area", 0L);
    }

    public long maxVolume() {
        return plugin.getConfig().getLong("admin-regions.max-volume", 0L);
    }

    /** 0 = без лимита (для всех с apriv.unlimited или admin) */
    public int maxRegionsPerPlayer() {
        return plugin.getConfig().getInt("admin-regions.max-regions-per-player", 0);
    }

    public boolean hasUnlimitedRegions(Player player) {
        return player.hasPermission("pveauction.apriv.unlimited")
                || player.hasPermission("pveauction.admin");
    }

    /** Сундуки, двери, кнопки, рычаги, плиты и прочие блоки с ПКМ/нажатием. */
    public boolean isMemberInteractBlock(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (!type.isBlock()) {
            return false;
        }
        return type.isInteractable()
                || Tag.PRESSURE_PLATES.isTagged(type)
                || Tag.BUTTONS.isTagged(type);
    }

    public boolean protectsRtpWorldOnly() {
        return plugin.getConfig().getBoolean("admin-regions.rtp-world-only", true);
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null || stack.getType() != wandMaterial()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public ItemStack createWand() {
        ItemStack stack = new ItemStack(wandMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Топор привата", NamedTextColor.GOLD));
            meta.lore(List.of(
                    Component.text("ЛКМ — точка 1", NamedTextColor.GRAY),
                    Component.text("ПКМ — точка 2", NamedTextColor.GRAY),
                    Component.text("/expand <N> up|down", NamedTextColor.GRAY),
                    Component.text("/claim <имя>", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void giveWand(Player player) {
        if (!canManage(player)) {
            Msg.server(player, Msg.err("Нет доступа."));
            return;
        }
        player.getInventory().addItem(createWand());
        Msg.server(player, Msg.ok("Топор выдан. ЛКМ/ПКМ — две точки, /expand, /claim <имя>."));
    }

    public void setPos1(Player player, Location loc) {
        CuboidSelection sel = selections.computeIfAbsent(player.getUniqueId(), u -> new CuboidSelection());
        sel.setPos1(loc);
        Msg.server(player, Msg.ok("Поз.1: ").append(Component.text(formatBlock(loc), NamedTextColor.WHITE)));
        if (sel.isComplete()) {
            Msg.server(player, Msg.info("Выделение: " + sel.boundsSummary()));
        }
    }

    public void setPos2(Player player, Location loc) {
        CuboidSelection sel = selections.computeIfAbsent(player.getUniqueId(), u -> new CuboidSelection());
        sel.setPos2(loc);
        Msg.server(player, Msg.ok("Поз.2: ").append(Component.text(formatBlock(loc), NamedTextColor.WHITE)));
        if (sel.isComplete()) {
            Msg.server(player, Msg.info("Выделение: " + sel.boundsSummary()));
        }
    }

    public String expand(Player player, int amount, String directionRaw) {
        if (amount <= 0) {
            return "Число должно быть > 0.";
        }
        String dir = directionRaw == null ? "" : directionRaw.toLowerCase(Locale.ROOT);
        CuboidSelection sel = selections.get(player.getUniqueId());
        if (sel == null || !sel.isComplete()) {
            return "Сначала отметь две точки топором (/wand или //wand).";
        }
        if (protectsRtpWorldOnly()) {
            World w = player.getWorld();
            if (!worlds.isRtpWorld(w)) {
                return "Расширять можно только на анархии (" + worlds.rtpWorldName() + ").";
            }
        }

        switch (dir) {
            case "up", "вверх" -> sel.expandUp(amount);
            case "down", "вниз" -> sel.expandDown(amount);
            default -> {
                return "Направление: up или down (например /expand 20 up).";
            }
        }

        Msg.server(player, Msg.ok("Расширено на " + amount + " " + dir + ". ")
                .append(Component.text(sel.boundsSummary(), NamedTextColor.WHITE)));
        return null;
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
        Msg.server(player, Msg.info("Выделение сброшено."));
    }

    public Optional<CuboidSelection> selection(Player player) {
        CuboidSelection sel = selections.get(player.getUniqueId());
        if (sel == null || !sel.isComplete()) {
            return Optional.empty();
        }
        return Optional.of(sel);
    }

    public String createRegion(Player player, String rawName) throws SQLException {
        if (!enabled()) {
            return "Модуль приватов выключен.";
        }
        String name = normalizeName(rawName);
        if (name == null) {
            return "Имя: 3–24 символа, a-z, цифры, дефис.";
        }
        CuboidSelection sel = selection(player).orElse(null);
        if (sel == null) {
            return "Нет выделения: /wand → две точки → /expand up|down → /claim <имя>.";
        }
        if (protectsRtpWorldOnly() && !worlds.isRtpWorld(player.getWorld())) {
            return "Создавать приват только на анархии (" + worlds.rtpWorldName() + ").";
        }
        if (!sel.worldName().equalsIgnoreCase(player.getWorld().getName())) {
            return "Создай приват в том же мире, где выделял.";
        }

        long area = (long) (sel.maxX() - sel.minX() + 1) * (sel.maxZ() - sel.minZ() + 1);
        long maxArea = maxHorizontalArea();
        if (maxArea > 0 && area > maxArea) {
            return "Слишком большая площадь по X×Z: " + area;
        }

        long volume = (long) (sel.maxX() - sel.minX() + 1)
                * (sel.maxY() - sel.minY() + 1)
                * (sel.maxZ() - sel.minZ() + 1);
        long maxVol = maxVolume();
        if (maxVol > 0 && volume > maxVol) {
            return "Слишком большой объём: " + volume;
        }

        if (repo.findByName(name).isPresent()) {
            return "Регион «" + name + "» уже есть.";
        }

        int maxRegions = maxRegionsPerPlayer();
        if (maxRegions > 0 && !hasUnlimitedRegions(player)) {
            int owned = repo.countByCreator(player.getUniqueId());
            if (owned >= maxRegions) {
                return "Лимит приватов: " + maxRegions + " (нужно pveauction.apriv.unlimited).";
            }
        }

        AdminRegion candidate = new AdminRegion(
                0, name, sel.worldName(),
                sel.minX(), sel.maxX(), sel.minY(), sel.maxY(), sel.minZ(), sel.maxZ(),
                player.getUniqueId(), player.getName(), System.currentTimeMillis(),
                false, null, null, null, null, null, null
        );
        for (AdminRegion existing : repo.listAll()) {
            if (candidate.overlaps(existing)) {
                return "Пересечение с «" + existing.name() + "».";
            }
        }

        repo.insert(
                name, sel.worldName(),
                sel.minX(), sel.maxX(), sel.minY(), sel.maxY(), sel.minZ(), sel.maxZ(),
                player.getUniqueId(), player.getName()
        );
        selections.remove(player.getUniqueId());
        return null;
    }

    public Optional<AdminRegion> regionAt(Location loc) throws SQLException {
        if (!enabled() || loc.getWorld() == null) {
            return Optional.empty();
        }
        if (!plugin.getConfig().getBoolean("admin-regions.protect-spawn-world", false)
                && worlds.isSpawnWorld(loc.getWorld())) {
            return Optional.empty();
        }
        if (protectsRtpWorldOnly() && !worlds.isRtpWorld(loc.getWorld())) {
            return Optional.empty();
        }
        return repo.findAt(
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    public boolean isProtected(Location loc) throws SQLException {
        return regionAt(loc).isPresent();
    }

    public String setRegionHome(Player player, String rawName) throws SQLException {
        String name = normalizeName(rawName);
        if (name == null) {
            return "Имя: 3–24 символа, a-z, цифры, дефис.";
        }
        var region = repo.findByName(name).orElse(null);
        if (region == null) {
            return "Регион «" + name + "» не найден.";
        }
        if (!region.contains(player.getLocation())) {
            return "Встань внутри региона «" + name + "».";
        }
        Location loc = player.getLocation();
        repo.setHome(name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        return null;
    }

    public String clearRegionHome(String rawName) throws SQLException {
        String name = normalizeName(rawName);
        if (name == null) {
            return "Имя: 3–24 символа, a-z, цифры, дефис.";
        }
        if (repo.findByName(name).isEmpty()) {
            return "Регион не найден.";
        }
        repo.clearHome(name);
        return null;
    }

    public String teleportRegionHome(Player player, String rawName) throws SQLException {
        String name = normalizeName(rawName);
        if (name == null) {
            return "Имя: 3–24 символа, a-z, цифры, дефис.";
        }
        var region = repo.findByName(name).orElse(null);
        if (region == null) {
            return "Регион «" + name + "» не найден.";
        }
        if (!region.hasHome()) {
            return "У региона нет home. /apriv sethome " + name;
        }
        SavedLocation saved = region.homeLocation();
        Location target = saved != null ? saved.toLocation() : null;
        if (target == null) {
            return "Мир home не загружен.";
        }
        WorldTeleportService.teleport(plugin, player, target, success -> {
            if (success) {
                Msg.server(player, Msg.ok("Телепорт в «" + name + "»."));
            } else {
                Msg.server(player, Msg.err("Не удалось телепортироваться."));
            }
        });
        return null;
    }

    public String setMemberInteract(Player player, String rawName, Boolean enable) throws SQLException {
        String name = normalizeName(rawName);
        if (name == null) {
            return "Имя: 3–24 символа, a-z, цифры, дефис.";
        }
        var region = repo.findByName(name).orElse(null);
        if (region == null) {
            return "Регион «" + name + "» не найден.";
        }
        boolean allowed = enable != null ? enable : !region.allowMemberInteract();
        repo.setAllowMemberInteract(name, allowed);
        Msg.server(player, Msg.ok("«" + name + "»: использование блоков для игроков — "
                + (allowed ? "включено" : "выключено") + "."));
        return null;
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim().toLowerCase(Locale.ROOT);
        if (name.length() < 3 || name.length() > 24) {
            return null;
        }
        if (!name.matches("[a-z0-9][a-z0-9-]*")) {
            return null;
        }
        return name;
    }

    private static String formatBlock(Location loc) {
        return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }
}
