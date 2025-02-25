package com.alantaru.territorywars;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all territories in the plugin.
 */
public class TerritoryManager {
    private final TerritoryWars plugin;
    private final Map<String, Territory> territories;
    private final int gridSize;
    private final int blockSize;
    private final Gson gson;

    /**
     * Creates a new TerritoryManager.
     * @param plugin The TerritoryWars plugin
     */
    public TerritoryManager(TerritoryWars plugin) {
        this.plugin = plugin;
        this.territories = new ConcurrentHashMap<>();
        this.gridSize = plugin.getConfig().getInt("territory.grid_size", 16);
        this.blockSize = plugin.getConfig().getInt("territory.block_size", 16);
        
        // Setup Gson with adapters
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .registerTypeAdapter(Territory.class, new TerritoryAdapter(plugin))
                .setPrettyPrinting()
                .create();
        
        // Load territories
        loadTerritories();
    }

    /**
     * Loads territories from storage.
     */
    private void loadTerritories() {
        if (plugin.getConfig().getBoolean("storage.use_database", false)) {
            loadTerritoriesFromDatabase();
        } else {
            loadTerritoriesFromFile();
        }
        
        // Update Dynmap with all territories
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            updateDynmapMarkers();
        }
    }

    /**
     * Updates Dynmap markers for all territories.
     */
    private void updateDynmapMarkers() {
        // Implement Dynmap integration if available
        if (Bukkit.getPluginManager().getPlugin("dynmap") != null) {
            // For each territory, add a marker to Dynmap
            // This is a placeholder for actual implementation
            plugin.getLogger().info("Updating Dynmap markers for all territories");
        }
    }

    /**
     * Loads territories from the database.
     */
    private void loadTerritoriesFromDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM territories")) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String id = rs.getString("id");
                int gridX = rs.getInt("grid_x");
                int gridZ = rs.getInt("grid_z");
                String worldName = rs.getString("world");
                String clanTag = rs.getString("owner_clan");
                
                Clan owner = null;
                if (clanTag != null && !clanTag.isEmpty()) {
                    owner = plugin.getSimpleClans().getClanManager().getClan(clanTag);
                }
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Failed to load territory " + id + ": World " + worldName + " does not exist");
                    continue;
                }
                
                Territory territory = new Territory(id, gridX, gridZ, worldName, owner);
                
                // Load additional data
                territory.setCoreHealth(rs.getInt("core_health"));
                territory.setProtectionMode(ProtectionMode.valueOf(rs.getString("protection_mode")));
                territory.setLastDamageTime(rs.getLong("last_damage_time"));
                territory.setLastTributeTime(rs.getLong("last_tribute_time"));
                
                // Load core location if exists
                String coreLocJson = rs.getString("core_location");
                if (coreLocJson != null && !coreLocJson.isEmpty()) {
                    Location coreLoc = gson.fromJson(coreLocJson, Location.class);
                    territory.setCoreLocation(coreLoc);
                }
                
                // Load core blocks if exist
                String coreBlocksJson = rs.getString("core_blocks");
                if (coreBlocksJson != null && !coreBlocksJson.isEmpty()) {
                    Type listType = new TypeToken<ArrayList<Location>>(){}.getType();
                    List<Location> coreBlocks = gson.fromJson(coreBlocksJson, listType);
                    territory.setCoreBlocks(coreBlocks);
                }
                
                territories.put(id, territory);
            }
            
            plugin.getLogger().info("Loaded " + territories.size() + " territories from database");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load territories from database", e);
        }
    }

    /**
     * Loads territories from the territories.json file.
     */
    private void loadTerritoriesFromFile() {
        File file = new File(plugin.getDataFolder(), "territories.json");
        
        if (!file.exists()) {
            plugin.getLogger().info("No territories.json file found. Creating a new one.");
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            Type mapType = new TypeToken<Map<String, Territory>>(){}.getType();
            Map<String, Territory> loadedTerritories = gson.fromJson(reader, mapType);
            
            if (loadedTerritories != null) {
                territories.putAll(loadedTerritories);
                plugin.getLogger().info("Loaded " + territories.size() + " territories from file");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load territories from file", e);
        }
    }

    /**
     * Saves territories to storage.
     */
    public void saveTerritories() {
        if (plugin.getConfig().getBoolean("storage.use_database", false)) {
            saveTerrritoriesToDatabase();
        } else {
            saveTerrritoriesToFile();
        }
    }

    /**
     * Saves territories to the database.
     */
    private void saveTerrritoriesToDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // First clear all territories
            try (PreparedStatement clearStmt = conn.prepareStatement("DELETE FROM territories")) {
                clearStmt.executeUpdate();
            }
            
            // Then insert all territories
            String sql = "INSERT INTO territories (id, grid_x, grid_z, world, owner_clan, core_health, " +
                         "protection_mode, last_damage_time, last_tribute_time, core_location, core_blocks) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                for (Territory territory : territories.values()) {
                    insertStmt.setString(1, territory.getId());
                    insertStmt.setInt(2, territory.getGridX());
                    insertStmt.setInt(3, territory.getGridZ());
                    insertStmt.setString(4, territory.getWorldName());
                    insertStmt.setString(5, territory.getOwner() != null ? territory.getOwner().getTag() : null);
                    insertStmt.setInt(6, territory.getCoreHealth());
                    insertStmt.setString(7, territory.getProtectionMode().name());
                    insertStmt.setLong(8, territory.getLastDamageTime());
                    insertStmt.setLong(9, territory.getLastTributeTime());
                    
                    // Serialize complex objects
                    Location coreLoc = territory.getCoreLocation();
                    insertStmt.setString(10, coreLoc != null ? gson.toJson(coreLoc) : null);
                    
                    List<Location> coreBlocks = territory.getCoreBlocks();
                    insertStmt.setString(11, coreBlocks != null && !coreBlocks.isEmpty() ? 
                                             gson.toJson(coreBlocks) : null);
                    
                    insertStmt.addBatch();
                }
                
                insertStmt.executeBatch();
            }
            
            plugin.getLogger().info("Saved " + territories.size() + " territories to database");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save territories to database", e);
        }
    }

    /**
     * Saves territories to the territories.json file.
     */
    private void saveTerrritoriesToFile() {
        File file = new File(plugin.getDataFolder(), "territories.json");
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(territories, writer);
            plugin.getLogger().info(plugin.getMessage("territories_saved_file"));
        } catch (IOException e) {
            plugin.getLogger().severe(plugin.getMessage("error_saving_territories") + e.getMessage());
        }
    }

    /**
     * Creates a new territory.
     * @param gridX The X coordinate in the grid
     * @param gridZ The Z coordinate in the grid
     * @param world The world the territory is in
     * @param owner The clan that owns the territory
     * @return The new territory or null if creation failed
     */
    public Territory createTerritory(int gridX, int gridZ, World world, Clan owner) {
        String id = generateTerritoryId(gridX, gridZ, world.getName());
        
        // Check if territory already exists
        if (territories.containsKey(id)) {
            return null;
        }
        
        // Create new territory
        Territory territory = new Territory(id, gridX, gridZ, world.getName(), owner);
        territories.put(id, territory);
        
        // Update dynmap
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
        }
        
        // Save territories
        saveTerritories();
        
        return territory;
    }

    /**
     * Gets a territory by its grid coordinates and world.
     * @param gridX The X coordinate in the grid
     * @param gridZ The Z coordinate in the grid
     * @param worldName The name of the world
     * @return The territory or null if not found
     */
    public Territory getTerritory(int gridX, int gridZ, String worldName) {
        String id = generateTerritoryId(gridX, gridZ, worldName);
        return territories.get(id);
    }

    /**
     * Gets a territory by its ID.
     * @param id The ID of the territory
     * @return The territory or null if not found
     */
    public Territory getTerritory(String id) {
        return territories.get(id);
    }

    /**
     * Gets the territory at a specific location.
     * @param location The location
     * @return The territory or null if not found
     */
    public Territory getTerritoryAt(Location location) {
        int gridX = (int) Math.floor(location.getX() / (gridSize * blockSize));
        int gridZ = (int) Math.floor(location.getZ() / (gridSize * blockSize));
        String worldName = location.getWorld().getName();
        
        return getTerritory(gridX, gridZ, worldName);
    }

    /**
     * Gets all territories.
     * @return A collection of all territories
     */
    public Collection<Territory> getAllTerritories() {
        return territories.values();
    }

    /**
     * Gets all territories owned by a specific clan.
     * @param clan The clan
     * @return A list of territories owned by the clan
     */
    public List<Territory> getTerritoriesByClan(Clan clan) {
        List<Territory> result = new ArrayList<>();
        
        for (Territory territory : territories.values()) {
            if (territory.getOwner() != null && territory.getOwner().equals(clan)) {
                result.add(territory);
            }
        }
        
        return result;
    }

    /**
     * Checks if a clan has a territory adjacent to the given territory.
     * @param territory The territory to check
     * @param clan The clan
     * @return True if the clan has an adjacent territory, false otherwise
     */
    public boolean hasAdjacentTerritory(Territory territory, Clan clan) {
        // If adjacency check is disabled, always return true
        if (!plugin.getConfig().getBoolean("territory.require_adjacency", true)) {
            return true;
        }
        
        int gridX = territory.getGridX();
        int gridZ = territory.getGridZ();
        String worldName = territory.getWorldName();
        
        // Check all adjacent territories (north, east, south, west)
        Territory north = getTerritory(gridX, gridZ - 1, worldName);
        Territory east = getTerritory(gridX + 1, gridZ, worldName);
        Territory south = getTerritory(gridX, gridZ + 1, worldName);
        Territory west = getTerritory(gridX - 1, gridZ, worldName);
        
        return (north != null && north.getOwner() != null && north.getOwner().equals(clan)) ||
               (east != null && east.getOwner() != null && east.getOwner().equals(clan)) ||
               (south != null && south.getOwner() != null && south.getOwner().equals(clan)) ||
               (west != null && west.getOwner() != null && west.getOwner().equals(clan));
    }

    /**
     * Generates a territory ID from grid coordinates and world name.
     * @param gridX The X coordinate in the grid
     * @param gridZ The Z coordinate in the grid
     * @param worldName The name of the world
     * @return The territory ID
     */
    private String generateTerritoryId(int gridX, int gridZ, String worldName) {
        return worldName + "_" + gridX + "_" + gridZ;
    }

    /**
     * Gets the grid size.
     * @return The grid size
     */
    public int getGridSize() {
        return gridSize;
    }

    /**
     * Gets the block size.
     * @return The block size
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Deletes a territory.
     * @param territory The territory to delete
     */
    public void deleteTerritory(Territory territory) {
        territories.remove(territory.getId());
        saveTerritories();
    }
    
    /**
     * Reloads the territory manager.
     */
    public void reload() {
        territories.clear();
        loadTerritories();
    }
    
    /**
     * Gets a territory by its name.
     * @param name The name of the territory
     * @return The territory or null if not found
     */
    public Territory getTerritoryByName(String name) {
        for (Territory territory : territories.values()) {
            if (territory.getName() != null && territory.getName().equalsIgnoreCase(name)) {
                return territory;
            }
        }
        return null;
    }
    
    /**
     * Gets all territories.
     * @return A map of all territories
     */
    public Map<String, Territory> getTerritories() {
        return territories;
    }
    
    /**
     * Creates a territory at the specified location.
     * @param player The player creating the territory
     * @param location The location to create the territory at
     * @param name The name of the territory
     * @return The new territory or null if creation failed
     */
    public Territory createTerritory(Player player, Location location, String name) {
        // Implementation would go here
        return null;
    }
    
    /**
     * Removes a territory by its ID.
     * @param id The ID of the territory
     * @param refund Whether to refund the creation cost
     * @return True if successful, false otherwise
     */
    public boolean removeTerritory(UUID id, boolean refund) {
        // Implementation would go here
        return false;
    }
    
    /**
     * Gets a territory at the specified grid coordinates.
     * @param gridX The X coordinate in the grid
     * @param gridZ The Z coordinate in the grid
     * @return The territory or null if not found
     */
    public Territory getTerritoryAtGrid(int gridX, int gridZ) {
        for (Territory territory : territories.values()) {
            if (territory.getGridX() == gridX && territory.getGridZ() == gridZ) {
                return territory;
            }
        }
        return null;
    }
    
    /**
     * Saves the territories.
     */
    public void save() {
        saveTerritories();
    }
}
