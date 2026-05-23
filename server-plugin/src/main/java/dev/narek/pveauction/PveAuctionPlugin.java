package dev.narek.pveauction;

import dev.narek.pveauction.clan.ClanService;
import dev.narek.pveauction.command.AhAdminCommand;
import dev.narek.pveauction.command.AhCommand;
import dev.narek.pveauction.command.ClanCommand;
import dev.narek.pveauction.command.HomeCommand;
import dev.narek.pveauction.command.PayCommand;
import dev.narek.pveauction.command.RtpCommand;
import dev.narek.pveauction.command.SetHomeCommand;
import dev.narek.pveauction.command.SpawnCommand;
import dev.narek.pveauction.db.ClanRepository;
import dev.narek.pveauction.db.LotRepository;
import dev.narek.pveauction.db.PlayerRepository;
import dev.narek.pveauction.gui.clan.ClanGuiListener;
import dev.narek.pveauction.listener.ScoreboardListener;
import dev.narek.pveauction.scoreboard.ScoreboardService;
import dev.narek.pveauction.economy.EconomyService;
import dev.narek.pveauction.economy.EconomyHookListener;
import dev.narek.pveauction.gui.GuiListener;
import dev.narek.pveauction.listener.CommandWhitelistListener;
import dev.narek.pveauction.listener.SpawnWorldListener;
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
    private EconomyService economyService;
    private WorldTravelService worldTravelService;
    private ScoreboardService scoreboardService;
    private ScoreboardListener scoreboardListener;
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

        clanRepository = new ClanRepository(this);
        clanRepository.init();
        clanService = new ClanService(this, clanRepository);

        scoreboardService = new ScoreboardService(this);
        scoreboardListener = new ScoreboardListener(this, scoreboardService);

        worldTravelService = new WorldTravelService(this);
        worldTravelService.ensureRtpWorld();
        worldTravelService.ensureSpawnNight();
        worldTravelService.refreshLocations();
        getServer().getScheduler().runTaskTimer(this, worldTravelService::ensureSpawnNight, 100L, 200L);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new ClanGuiListener(this, clanService), this);
        getServer().getPluginManager().registerEvents(new CommandWhitelistListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnWorldListener(this), this);
        getServer().getPluginManager().registerEvents(scoreboardListener, this);

        long sbTicks = getConfig().getLong("scoreboard.update-ticks", 40L);
        getServer().getScheduler().runTaskTimer(this, scoreboardListener::refreshAll, sbTicks, sbTicks);

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

        getLogger().info("PveAuction: аукцион, кланы, /pay, дом; лимит " + maxActiveLots() + " лотов.");
    }

    @Override
    public void onDisable() {
        if (lotRepository != null) {
            lotRepository.close();
        }
        if (playerRepository != null) {
            playerRepository.close();
        }
        if (clanRepository != null) {
            clanRepository.close();
        }
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

    public EconomyService economy() {
        return economyService;
    }

    public int maxActiveLots() {
        return getConfig().getInt("max-active-lots", 5);
    }

    public long auctionExpiryMs() {
        return getConfig().getLong("auction-expiry-hours", 12) * 3_600_000L;
    }

    public long maxAuctionPrice() {
        return getConfig().getLong("auction-max-price", 100_000_000L);
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
