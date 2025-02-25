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
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;

/**
 * Main class for the TerritoryWars plugin.
 */
public class TerritoryWars extends JavaPlugin {
    private static TerritoryWars instance;
    private Logger logger;
    private FileConfiguration config;
    private Economy economy;
    private SimpleClans simpleClans;
    private TerritoryManager territoryManager;
    private DatabaseManager databaseManager;
    private CoreStructure coreStructure;
    private MessageManager messageManager;
    private DynmapManager dynmapManager;

    public static TerritoryWars getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // Load configuration
        saveDefaultConfig();
        config = getConfig();
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        
        // Setup hooks
        if (!setupEconomy()) {
            logger.warning("Vault not found! Economy features will be disabled.");
        }
        
        if (!setupSimpleClans()) {
            logger.severe("SimpleClans not found! This plugin requires SimpleClans to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Setup database if enabled
        if (config.getBoolean("storage.use_database", false)) {
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                logger.severe("Failed to initialize database! Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        
        // Initialize core structure
        coreStructure = new CoreStructure(this);
        
        // Initialize territory manager
        territoryManager = new TerritoryManager(this);

		// Initialize Dynmap manager if Dynmap is available
		if (getConfig().getBoolean("dynmap.enabled", true) && getServer().getPluginManager().getPlugin("dynmap") != null) {
			dynmapManager = new DynmapManager(this, territoryManager);
			logger.info("Dynmap integration enabled.");
		}
        
        // Register command handlers
        Objects.requireNonNull(getCommand("territory")).setExecutor(new TerritoryWarsCommand(this));
        
        // Register event handlers
        getServer().getPluginManager().registerEvents(new TerritoryListener(this), this);
        
        logger.info("TerritoryWars has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all territories
        if (territoryManager != null) {
            territoryManager.saveTerritories();
        }
        
        // Close database connection if open
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        logger.info("TerritoryWars has been disabled!");
    }
    
    /**
     * Sets up the economy hook with Vault.
     * @return True if successful, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Sets up the SimpleClans hook.
     * @return True if successful, false otherwise
     */
    private boolean setupSimpleClans() {
        if (getServer().getPluginManager().getPlugin("SimpleClans") == null) {
            return false;
        }
        simpleClans = (SimpleClans) getServer().getPluginManager().getPlugin("SimpleClans");
        return simpleClans != null;
    }
    
    /**
     * Gets the SimpleClans API.
     * @return The SimpleClans API
     */
    public SimpleClans getSimpleClans() {
        return simpleClans;
    }
    
    /**
     * Gets the territory manager.
     * @return The territory manager
     */
    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }
    
    /**
     * Gets the database manager.
     * @return The database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Gets the core structure.
     * @return The core structure
     */
    public CoreStructure getCoreStructure() {
        return coreStructure;
    }
    
    /**
     * Gets the economy provider.
     * @return The economy provider
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * Gets a localized message.
     * @param key The message key
     * @return The localized message
     */
    public String getMessage(String key) {
        return messageManager.getMessage(key);
    }
    
    /**
     * Gets the DynmapManager instance.
     * @return The DynmapManager instance
     */
    public DynmapManager getDynmapManager() {
        return dynmapManager;
    }
    
    /**
     * Updates a territory on the Dynmap.
     * @param territory The territory to update
     */
    public void updateDynmapTerritory(Territory territory) {
        if (dynmapManager != null) {
            dynmapManager.updateTerritory(territory);
        }
    }
}
