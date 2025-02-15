package com.alantaru.territorywars;

import net.milkbowl.vault.economy.Economy;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class TerritoryWars extends JavaPlugin {
    private TerritoryManager territoryManager;
    private CoreStructure coreStructure;
    private DynmapManager dynmapManager;
    private TributeManager tributeManager;
    private SimpleClans clans;
    private Economy economy;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Setup dependencies
        if (!setupDependencies()) {
            getLogger().severe("Failed to setup required dependencies! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize components in correct order
        try {
            // Core structure must be initialized first as it's needed by Territory
            this.coreStructure = new CoreStructure(this);
            
            // Territory manager is needed by most other components
            this.territoryManager = new TerritoryManager(this);
            
            // Tribute manager depends on territory manager
            this.tributeManager = new TributeManager(this, territoryManager);

            // Register commands
            TerritoryWarsCommand commandExecutor = new TerritoryWarsCommand(this, territoryManager);
            getCommand("tw").setExecutor(commandExecutor);
            getCommand("tw").setTabCompleter(commandExecutor);

            // Register event listeners
            getServer().getPluginManager().registerEvents(
                new CoreBreakListener(territoryManager, clans, this), this);
            getServer().getPluginManager().registerEvents(
                new TerritoryProtectionListener(this, territoryManager), this);
            getServer().getPluginManager().registerEvents(tributeManager, this);

            // Setup Dynmap if enabled
            if (getConfig().getBoolean("dynmap.enabled", true)) {
                setupDynmap();
            }

            getLogger().info("TerritoryWars has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin components!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (territoryManager != null) {
            territoryManager.saveTerritories();
        }
        
        if (tributeManager != null) {
            tributeManager.disable();
        }
        
        if (dynmapManager != null) {
            // Dynmap will handle cleanup of markers when plugin is disabled
            dynmapManager = null;
        }

        getLogger().info("TerritoryWars has been disabled!");
    }

    private boolean setupDependencies() {
        // Setup SimpleClans
        clans = (SimpleClans) Bukkit.getPluginManager().getPlugin("SimpleClans");
        if (clans == null) {
            getLogger().severe("SimpleClans not found!");
            return false;
        }

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Vault Economy not found!");
            return false;
        }

        return true;
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
            .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupDynmap() {
        if (Bukkit.getPluginManager().getPlugin("dynmap") == null) {
            getLogger().warning("Dynmap not found, territory visualization disabled.");
            return;
        }

        this.dynmapManager = new DynmapManager(this, territoryManager);
        if (!dynmapManager.isEnabled()) {
            getLogger().warning("Failed to initialize Dynmap integration.");
            dynmapManager = null;
        }
    }

    public void updateDynmapTerritory(Territory territory) {
        if (dynmapManager != null) {
            dynmapManager.updateTerritory(territory);
        }
    }

    public void updateAllDynmapTerritories() {
        if (dynmapManager != null) {
            dynmapManager.updateAllTerritories();
        }
    }

    public void removeDynmapTerritory(Territory territory) {
        if (dynmapManager != null) {
            dynmapManager.removeTerritory(territory);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        if (tributeManager != null) {
            tributeManager.reload();
        }
        if (dynmapManager != null) {
            dynmapManager.updateAllTerritories();
        }
    }

    public double getFirstTerritoryCost() {
        return getConfig().getDouble("economy.first-territory-cost", 10000.0);
    }

    public double getTerritoryCost() {
        return getConfig().getDouble("economy.territory-cost", 5000.0);
    }

    public double getTaxRate() {
        return getConfig().getDouble("economy.tribute.per-territory", 2000.0);
    }

    public int getTaxInterval() {
        return getConfig().getInt("economy.tribute.interval", 1440);
    }

    public String getRaidStartTime() {
        return getConfig().getString("protection.raid-hours.start-time", "18:00");
    }

    public String getRaidEndTime() {
        return getConfig().getString("protection.raid-hours.end-time", "23:00");
    }

    public double getOnlinePercentage() {
        return getConfig().getDouble("protection.minimum-players.online-percentage", 30.0);
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().log(Level.INFO, "[Debug] " + message);
        }
    }

    public SimpleClans getClans() {
        return clans;
    }

    public Economy getEconomy() {
        return economy;
    }

    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }

    public CoreStructure getCoreStructure() {
        return coreStructure;
    }

    public TributeManager getTributeManager() {
        return tributeManager;
    }

    public DynmapManager getDynmapManager() {
        return dynmapManager;
    }
}
