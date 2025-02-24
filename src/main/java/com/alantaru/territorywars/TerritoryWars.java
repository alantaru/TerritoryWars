package com.alantaru.territorywars;

import net.milkbowl.vault.economy.Economy;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;

public class TerritoryWars extends JavaPlugin {
    private static TerritoryWars instance;
    private TerritoryManager territoryManager;

    public static TerritoryWars getInstance() {
        return instance;
    }

    private CoreStructure coreStructure;
    private DynmapManager dynmapManager;
    private TributeManager tributeManager;
    private DatabaseManager databaseManager;
    private SimpleClans clans;
    private Economy economy;
    private FileConfiguration messagesConfig;
    private WorldGuardPlugin worldGuard;

   @Override
    public void onEnable() {
        instance = this;
        try {
            // Save default config
            saveDefaultConfig();

            // Setup WorldGuard
            if (!setupWorldGuard()) {
                getLogger().severe(getMessage("worldguard_not_found"));
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            // Load messages config
            loadMessagesConfig();

            // Setup dependencies
            if (!setupDependencies()) {
                getLogger().severe(getMessage("failed_dependencies"));
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            // Initialize database first
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.initializeDatabase();

            // Initialize components in correct order
            // Core structure must be initialized first as it's needed by Territory
            this.coreStructure = new CoreStructure(this);

            // Territory manager is needed by most other components
            this.territoryManager = new TerritoryManager(this);

            // Tribute manager depends on territory manager
            this.tributeManager = new TributeManager(this, territoryManager);

            // Register command
            registerCommand();

            // Register event listeners
            getServer().getPluginManager().registerEvents(new CoreBreakListener(territoryManager, clans, this), this);
            getServer().getPluginManager().registerEvents(new TerritoryListener(this, territoryManager, clans), this);
            getServer().getPluginManager().registerEvents(new TerritoryProtectionListener(this, territoryManager), this);
            getServer().getPluginManager().registerEvents(tributeManager, this);
            getServer().getPluginManager().registerEvents(new TransactionListener(this), this);


            // Setup Dynmap if enabled
            if (getConfig().getBoolean("dynmap.enabled", true)) {
                setupDynmap();
            }

            getLogger().info(getMessage("plugin_enabled"));
        } catch (Exception e) {
            getLogger().severe("An error occurred during plugin initialization: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

   @Override
    public void onDisable() {
        try {
            if (territoryManager != null) {
                territoryManager.saveTerritories();
            }

   if (tributeManager != null) {
                tributeManager.disable();
            }

   if (dynmapManager != null) {
                dynmapManager.disable();
            }
            dynmapManager = null;

   if (databaseManager != null) {
                databaseManager.closeConnection();
            }

            getLogger().info(getMessage("plugin_disabled"));
        } catch (Exception e) {
            getLogger().severe("An error occurred during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerCommand() {
        try {
            TerritoryWarsCommand commandExecutor = new TerritoryWarsCommand(this, territoryManager);
            getCommand("tw").setExecutor(commandExecutor);
            getCommand("tw").setTabCompleter(commandExecutor);
        } catch (Exception e) {
            getLogger().severe("Error registering command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean setupDependencies() {
        // Setup SimpleClans
        clans = (SimpleClans) Bukkit.getPluginManager().getPlugin("SimpleClans");
        if (clans == null) {
            getLogger().severe(getMessage("simpleclans_not_found"));
            return false;
        }

        // Setup Vault Economy
        if (!setupEconomy()) {
            getLogger().severe(getMessage("vault_economy_not_found"));
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
            getLogger().warning(getMessage("dynmap_not_found"));
            return;
        }

        this.dynmapManager = new DynmapManager(this, territoryManager);
        if (!dynmapManager.isEnabled()) {
            getLogger().warning(getMessage("dynmap_failed_init"));
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

    public WorldGuardPlugin getWorldGuard() {
        return worldGuard;
    }

    public DynmapManager getDynmapManager() {
        return dynmapManager;
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load default messages from the resource
        try (InputStream defConfigStream = getResource("messages.yml")) {
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                messagesConfig.setDefaults(defConfig);
            }
        } catch (Exception e) {
            getLogger().severe("Could not load default messages.yml: " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&',
            Objects.requireNonNull(messagesConfig.getString(key, "&cMissing message key: " + key)));
    }

    private boolean setupWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin instanceof WorldGuardPlugin) {
            worldGuard = (WorldGuardPlugin) plugin;
            getLogger().info("WorldGuard found and enabled.");
        } else {
            getLogger().severe("WorldGuard not found!");
            return false;
        }
        return true;
    }
}
