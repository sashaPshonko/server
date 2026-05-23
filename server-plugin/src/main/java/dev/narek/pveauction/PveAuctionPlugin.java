package dev.narek.pveauction;

import dev.narek.pveauction.command.AhAdminCommand;
import dev.narek.pveauction.command.AhCommand;
import dev.narek.pveauction.command.RtpCommand;
import dev.narek.pveauction.command.SpawnCommand;
import dev.narek.pveauction.db.LotRepository;
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
    private EconomyService economyService;
    private WorldTravelService worldTravelService;
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

        worldTravelService = new WorldTravelService(this);
        worldTravelService.ensureRtpWorld();
        worldTravelService.refreshLocations();

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandWhitelistListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnWorldListener(this), this);

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

        getLogger().info("PveAuction: /ah, /admin, /rtp, /spawn; лимит " + maxActiveLots() + " лотов.");
    }

    @Override
    public void onDisable() {
        if (lotRepository != null) {
            lotRepository.close();
        }
    }

    public WorldTravelService worlds() {
        return worldTravelService;
    }

    public LotRepository lots() {
        return lotRepository;
    }

    public EconomyService economy() {
        return economyService;
    }

    public int maxActiveLots() {
        return getConfig().getInt("max-active-lots", 5);
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
