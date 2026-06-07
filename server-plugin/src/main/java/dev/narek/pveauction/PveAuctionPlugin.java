package dev.narek.pveauction;

import dev.narek.pveauction.auth.AuthRepository;
import dev.narek.pveauction.auth.AuthService;
import dev.narek.pveauction.chat.ChatService;
import dev.narek.pveauction.command.LoginCommand;
import dev.narek.pveauction.command.RegisterCommand;
import dev.narek.pveauction.listener.AuthListener;
import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.listener.ChatListener;
import dev.narek.pveauction.command.AdminPrivCommand;
import dev.narek.pveauction.command.ClaimCommand;
import dev.narek.pveauction.command.ExpandCommand;
import dev.narek.pveauction.command.WandCommand;
import dev.narek.pveauction.command.AhAdminCommand;
import dev.narek.pveauction.command.AhCommand;
import dev.narek.pveauction.command.ClanCommand;
import dev.narek.pveauction.command.HomeCommand;
import dev.narek.pveauction.command.PayCommand;
import dev.narek.pveauction.command.RtpCommand;
import dev.narek.pveauction.command.SetHomeCommand;
import dev.narek.pveauction.command.DonateCommand;
import dev.narek.pveauction.command.GiveSilverCommand;
import dev.narek.pveauction.db.DonateRepository;
import dev.narek.pveauction.donate.DonateService;
import dev.narek.pveauction.command.ShopCommand;
import dev.narek.pveauction.command.SpawnCommand;
import dev.narek.pveauction.command.TpAcceptCommand;
import dev.narek.pveauction.command.TpCommand;
import dev.narek.pveauction.command.TpDenyCommand;
import dev.narek.pveauction.gui.trader.TraderGuiListener;
import dev.narek.pveauction.listener.ArmorLockpickListener;
import dev.narek.pveauction.listener.StorageKeyListener;
import dev.narek.pveauction.lockpick.armor.ArmorLockpickService;
import dev.narek.pveauction.listener.TraderNpcListener;
import dev.narek.pveauction.trader.TraderNpcService;
import dev.narek.pveauction.db.AdminRegionRepository;
import dev.narek.pveauction.db.ClanRepository;
import dev.narek.pveauction.db.ShopRepository;
import dev.narek.pveauction.gui.shop.ShopGuiListener;
import dev.narek.pveauction.gui.shop.ShopSellMenu;
import dev.narek.pveauction.shop.ShopService;
import dev.narek.pveauction.db.LotRepository;
import dev.narek.pveauction.db.PlayerRepository;
import dev.narek.pveauction.gui.clan.ClanGuiListener;
import dev.narek.pveauction.listener.ScoreboardListener;
import dev.narek.pveauction.nametag.NameTagService;
import dev.narek.pveauction.scoreboard.ScoreboardService;
import dev.narek.pveauction.economy.EconomyService;
import dev.narek.pveauction.economy.EconomyHookListener;
import dev.narek.pveauction.gui.GuiListener;
import dev.narek.pveauction.listener.CommandWhitelistListener;
import dev.narek.pveauction.listener.SpawnItemCleanupTask;
import dev.narek.pveauction.listener.AdminRegionProtectListener;
import dev.narek.pveauction.listener.AdminRegionWandListener;
import dev.narek.pveauction.listener.PlayerCollisionListener;
import dev.narek.pveauction.listener.SpawnWorldListener;
import dev.narek.pveauction.listener.TpRequestListener;
import dev.narek.pveauction.region.AdminRegionService;
import dev.narek.pveauction.travel.TeleportRequestService;
import dev.narek.pveauction.world.WorldTravelService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PveAuctionPlugin extends JavaPlugin {

    private LotRepository lotRepository;
    private PlayerRepository playerRepository;
    private ClanRepository clanRepository;
    private ClanService clanService;
    private ChatService chatService;
    private ShopRepository shopRepository;
    private ShopService shopService;
    private EconomyService economyService;
    private WorldTravelService worldTravelService;
    private ScoreboardService scoreboardService;
    private ScoreboardListener scoreboardListener;
    private NameTagService nameTagService;
    private TraderNpcService traderNpcService;
    private TeleportRequestService teleportRequestService;
    private DonateRepository donateRepository;
    private DonateService donateService;
    private AuthRepository authRepository;
    private AuthService authService;
    private AdminRegionRepository adminRegionRepository;
    private AdminRegionService adminRegionService;
    private final Map<UUID, Long> lastRelistAt = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastAuctionPage = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        economyService = new EconomyService();
        getServer().getPluginManager().registerEvents(new EconomyHookListener(this), this);
        economyService.hook();
        logEconomyState();

        lotRepository = new LotRepository(this);
        lotRepository.init();

        playerRepository = new PlayerRepository(this);
        playerRepository.init();

        authRepository = new AuthRepository(this);
        authRepository.init();
        authService = new AuthService(this, authRepository);

        donateRepository = new DonateRepository(this);
        donateRepository.init();
        donateService = new DonateService(this, donateRepository);
        getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                donateService::purgeExpiredAndRefresh,
                20L * 60,
                20L * 60
        );

        clanRepository = new ClanRepository(this);
        clanRepository.init();
        clanService = new ClanService(this, clanRepository);
        chatService = new ChatService(this);

        shopRepository = new ShopRepository(this);
        shopRepository.init();
        shopService = new ShopService(this, shopRepository);

        nameTagService = new NameTagService(this);
        scoreboardService = new ScoreboardService(this, nameTagService);
        scoreboardListener = new ScoreboardListener(this, scoreboardService, nameTagService);

        worldTravelService = new WorldTravelService(this);
        worldTravelService.ensureRtpWorld();
        worldTravelService.ensureSpawnNight();
        worldTravelService.refreshLocations();
        getServer().getScheduler().runTaskTimer(this, worldTravelService::ensureSpawnNight, 100L, 200L);

        adminRegionRepository = new AdminRegionRepository(this);
        adminRegionRepository.init();
        adminRegionService = new AdminRegionService(this, adminRegionRepository, worldTravelService);

        long itemDespawnSec = getConfig().getLong("spawn-item-despawn-seconds", 60L);
        if (itemDespawnSec > 0) {
            int despawnSec = (int) Math.min(itemDespawnSec, Integer.MAX_VALUE / 20);
            getServer().getScheduler().runTaskTimer(
                    this,
                    new SpawnItemCleanupTask(this, despawnSec),
                    40L,
                    40L
            );
        }

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanGuiListener(this, clanService), this);
        getServer().getPluginManager().registerEvents(new CommandWhitelistListener(authService), this);
        getServer().getPluginManager().registerEvents(new PlayerCollisionListener(), this);
        var armorLockpickService = new ArmorLockpickService(this);
        getServer().getPluginManager().registerEvents(
                new ArmorLockpickListener(this, armorLockpickService), this);
        getServer().getPluginManager().registerEvents(
                new AuthListener(this, authService, armorLockpickService), this);
        getServer().getPluginManager().registerEvents(new SpawnWorldListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminRegionWandListener(adminRegionService), this);
        getServer().getPluginManager().registerEvents(
                new AdminRegionProtectListener(this, adminRegionService, armorLockpickService), this);

        teleportRequestService = new TeleportRequestService(this);
        getServer().getPluginManager().registerEvents(new TpRequestListener(teleportRequestService), this);
        getServer().getPluginManager().registerEvents(scoreboardListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this, chatService), this);
        getServer().getPluginManager().registerEvents(new ShopGuiListener(this, shopService), this);

        traderNpcService = new TraderNpcService(this);
        traderNpcService.start();
        getServer().getPluginManager().registerEvents(new TraderNpcListener(this, traderNpcService), this);
        getServer().getPluginManager().registerEvents(new TraderGuiListener(this), this);
        getServer().getPluginManager().registerEvents(new StorageKeyListener(this, armorLockpickService), this);
        getLogger().info(
                "Отмычка к броне: ПКМ по кузнечному столу (SMITHING_TABLE) в мире "
                        + getConfig().getString("spawn-world", "world")
        );

        var ah = new AhCommand(this);
        var ahCmd = getCommand("ah");
        if (ahCmd != null) {
            ahCmd.setExecutor(ah);
            ahCmd.setTabCompleter(ah);
        }

        var admin = new AhAdminCommand(this);
        var adminCmd = getCommand("admin");
        if (adminCmd != null) {
            adminCmd.setExecutor(admin);
            adminCmd.setTabCompleter(admin);
        }

        var rtpCmd = getCommand("rtp");
        if (rtpCmd != null) {
            rtpCmd.setExecutor(new RtpCommand(this));
        }

        var spawnCmd = getCommand("spawn");
        if (spawnCmd != null) {
            spawnCmd.setExecutor(new SpawnCommand(this));
        }

        var tpaCmd = getCommand("tpa");
        if (tpaCmd != null) {
            TpCommand tp = new TpCommand(this, teleportRequestService);
            tpaCmd.setExecutor(tp);
            tpaCmd.setTabCompleter(tp);
        }
        var tpAcceptCmd = getCommand("tpaccept");
        if (tpAcceptCmd != null) {
            tpAcceptCmd.setExecutor(new TpAcceptCommand(teleportRequestService));
        }
        var tpDenyCmd = getCommand("tpdeny");
        if (tpDenyCmd != null) {
            tpDenyCmd.setExecutor(new TpDenyCommand(teleportRequestService));
        }

        registerCmd("sethome", new SetHomeCommand(this, clanService, worldTravelService));
        registerCmd("home", new HomeCommand(this, clanService));
        var payCmd = getCommand("pay");
        if (payCmd != null) {
            payCmd.setExecutor(new PayCommand(this, clanService));
            payCmd.setTabCompleter(new PayCommand(this, clanService));
        }
        var clanCmd = getCommand("clan");
        if (clanCmd != null) {
            clanCmd.setExecutor(new ClanCommand(this, clanService));
            clanCmd.setTabCompleter(new ClanCommand(this, clanService));
        }

        registerCmd("shop", new ShopCommand(this));

        var regCmd = getCommand("register");
        if (regCmd != null) {
            regCmd.setExecutor(new RegisterCommand(authService));
        }
        var loginCmd = getCommand("login");
        if (loginCmd != null) {
            loginCmd.setExecutor(new LoginCommand(authService));
        }
        var giveSilver = new GiveSilverCommand(this);
        var giveSilverCmd = getCommand("givesilver");
        if (giveSilverCmd != null) {
            giveSilverCmd.setExecutor(giveSilver);
            giveSilverCmd.setTabCompleter(giveSilver);
        }

        var donateCmd = new DonateCommand(this, donateService);
        var donate = getCommand("donate");
        if (donate != null) {
            donate.setExecutor(donateCmd);
            donate.setTabCompleter(donateCmd);
        }

        var apriv = new AdminPrivCommand(adminRegionService);
        var aprivCmd = getCommand("apriv");
        if (aprivCmd != null) {
            aprivCmd.setExecutor(apriv);
            aprivCmd.setTabCompleter(apriv);
        }

        var wand = new WandCommand(adminRegionService);
        var wandCmd = getCommand("wand");
        if (wandCmd != null) {
            wandCmd.setExecutor(wand);
        }

        var expand = new ExpandCommand(adminRegionService);
        var expandCmd = getCommand("expand");
        if (expandCmd != null) {
            expandCmd.setExecutor(expand);
            expandCmd.setTabCompleter(expand);
        }

        registerCmd("claim", new ClaimCommand(adminRegionService));

        getLogger().info("PveAuction v" + getPluginMeta().getVersion()
                + " | auth=" + getConfig().getBoolean("auth.enabled", true));

        getLogger().info("PveAuction: аукцион, кланы, донаты, /pay; база лотов " + maxActiveLotsBase() + "+донат.");
        logWorldEditStatus();
    }

    private void logWorldEditStatus() {
        var pm = getServer().getPluginManager();
        if (pm.getPlugin("FastAsyncWorldEdit") != null) {
            getLogger().info("WorldEdit: FastAsyncWorldEdit — команды // только с pveauction.worldedit / OP");
        } else if (pm.getPlugin("WorldEdit") != null) {
            getLogger().info("WorldEdit: WorldEdit — команды // только с pveauction.worldedit / OP");
        } else {
            getLogger().info("WorldEdit: не установлен (bash install-worldedit.sh в папке server)");
        }
    }

    @Override
    public void onDisable() {
        if (traderNpcService != null) {
            traderNpcService.stop();
        }
        if (lotRepository != null) {
            lotRepository.close();
        }
        if (playerRepository != null) {
            playerRepository.close();
        }
        if (clanRepository != null) {
            clanRepository.close();
        }
        if (shopRepository != null) {
            shopRepository.close();
        }
        if (donateRepository != null) {
            donateRepository.close();
        }
        if (authRepository != null) {
            authRepository.close();
        }
        if (adminRegionRepository != null) {
            adminRegionRepository.close();
        }
    }

    public AdminRegionService adminRegions() {
        return adminRegionService;
    }

    public AuthService auth() {
        return authService;
    }

    public ShopService shop() {
        return shopService;
    }

    private void registerCmd(String name, org.bukkit.command.CommandExecutor executor) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        }
    }

    public WorldTravelService worlds() {
        return worldTravelService;
    }

    public LotRepository lots() {
        return lotRepository;
    }

    public PlayerRepository players() {
        return playerRepository;
    }

    public ScoreboardService scoreboards() {
        return scoreboardService;
    }

    public ScoreboardListener scoreboardListener() {
        return scoreboardListener;
    }

    public ClanService clans() {
        return clanService;
    }

    public ChatService chat() {
        return chatService;
    }

    public EconomyService economy() {
        return economyService;
    }

    public int maxActiveLotsBase() {
        return getConfig().getInt("max-active-lots", 5);
    }

    public int maxActiveLots(java.util.UUID playerId) throws java.sql.SQLException {
        return donateService.maxActiveLots(playerId);
    }

    public DonateService donates() {
        return donateService;
    }

    public long auctionExpiryMs() {
        return getConfig().getLong("auction-expiry-hours", 12) * 3_600_000L;
    }

    public long maxAuctionPrice() {
        return getConfig().getLong("auction-max-price", 100_000_000L);
    }

    /** Лимит для /pay, /clan invest, /clan withdraw */
    public long maxMoneyAmount() {
        return getConfig().getLong("money-max-amount", maxAuctionPrice());
    }

    public long relistCooldownMs() {
        return getConfig().getLong("relist-cooldown-seconds", 60) * 1000L;
    }

    public long relistCooldownLeftMs(UUID playerId) {
        Long last = lastRelistAt.get(playerId);
        if (last == null) {
            return 0;
        }
        long left = relistCooldownMs() - (System.currentTimeMillis() - last);
        return Math.max(0, left);
    }

    public void markRelistUsed(UUID playerId) {
        lastRelistAt.put(playerId, System.currentTimeMillis());
    }

    public int lastAuctionPage(UUID playerId) {
        return lastAuctionPage.getOrDefault(playerId, 0);
    }

    public void setLastAuctionPage(UUID playerId, int page) {
        lastAuctionPage.put(playerId, Math.max(0, page));
    }

    public void retryEconomyHook() {
        economyService.hook();
        logEconomyState();
    }

    private void logEconomyState() {
        if (economyService.isEnabled()) {
            getLogger().info("Экономика Vault подключена.");
        } else if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault не установлен — ./install-economy.sh в папке server/");
        } else {
            getLogger().warning("Vault есть, но провайдер экономики нет — поставь EssentialsX.");
        }
    }
}
