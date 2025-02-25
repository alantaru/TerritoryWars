package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a territory in the game.
 */
public class Territory {
    private final String id;
    private final int gridX;
    private final int gridZ;
    private final String worldName;
    private Clan owner;
    private int coreHealth;
    private Location coreLocation;
    private List<Location> coreBlocks;
    private ProtectionMode protectionMode;
    private long lastDamageTime;
    private long lastTributeTime;

    /**
     * Creates a new Territory.
     * @param id The unique ID of the territory
     * @param gridX The X coordinate in the grid
     * @param gridZ The Z coordinate in the grid
     * @param worldName The name of the world
     * @param owner The clan that owns the territory
     */
    public Territory(String id, int gridX, int gridZ, String worldName, Clan owner) {
        this.id = id;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.worldName = worldName;
        this.owner = owner;
        this.coreBlocks = new ArrayList<>();
        this.protectionMode = ProtectionMode.RAID_HOURS; // Default protection mode
        
        // Get core health from config or use default
        TerritoryWars plugin = (TerritoryWars) Bukkit.getPluginManager().getPlugin("TerritoryWars");
        if (plugin != null) {
            this.coreHealth = plugin.getCoreStructure().getCoreHealth();
        } else {
            this.coreHealth = 50; // Default value
        }
    }

    /**
     * Gets the unique ID of the territory.
     * @return The unique ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the X coordinate in the grid.
     * @return The X coordinate
     */
    public int getGridX() {
        return gridX;
    }

    /**
     * Gets the Z coordinate in the grid.
     * @return The Z coordinate
     */
    public int getGridZ() {
        return gridZ;
    }

    /**
     * Gets the name of the world.
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Gets the clan that owns the territory.
     * @return The owning clan
     */
    public Clan getOwner() {
        return owner;
    }

    /**
     * Sets the clan that owns the territory.
     * @param owner The new owning clan
     */
    public void setOwner(Clan owner) {
        this.owner = owner;
    }

    /**
     * Gets the core health.
     * @return The core health
     */
    public int getCoreHealth() {
        return coreHealth;
    }

    /**
     * Sets the core health.
     * @param coreHealth The new core health
     */
    public void setCoreHealth(int coreHealth) {
        this.coreHealth = coreHealth;
    }

    /**
     * Gets the location of the core.
     * @return The core location
     */
    public Location getCoreLocation() {
        return coreLocation;
    }

    /**
     * Sets the location of the core.
     * @param coreLocation The new core location
     */
    public void setCoreLocation(Location coreLocation) {
        this.coreLocation = coreLocation;
    }

    /**
     * Gets the blocks that make up the core.
     * @return The core blocks
     */
    public List<Location> getCoreBlocks() {
        return coreBlocks;
    }

    /**
     * Sets the blocks that make up the core.
     * @param coreBlocks The new core blocks
     */
    public void setCoreBlocks(List<Location> coreBlocks) {
        this.coreBlocks = coreBlocks;
    }

    /**
     * Gets the protection mode of the territory.
     * @return The protection mode
     */
    public ProtectionMode getProtectionMode() {
        return protectionMode;
    }

    /**
     * Sets the protection mode of the territory.
     * @param protectionMode The new protection mode
     */
    public void setProtectionMode(ProtectionMode protectionMode) {
        this.protectionMode = protectionMode;
    }

    /**
     * Gets the last time the core was damaged.
     * @return The last damage time
     */
    public long getLastDamageTime() {
        return lastDamageTime;
    }

    /**
     * Sets the last time the core was damaged.
     * @param lastDamageTime The new last damage time
     */
    public void setLastDamageTime(long lastDamageTime) {
        this.lastDamageTime = lastDamageTime;
    }

    /**
     * Gets the last time the territory received tribute.
     * @return The last tribute time
     */
    public long getLastTributeTime() {
        return lastTributeTime;
    }

    /**
     * Sets the last time the territory received tribute.
     * @param lastTributeTime The new last tribute time
     */
    public void setLastTributeTime(long lastTributeTime) {
        this.lastTributeTime = lastTributeTime;
    }

    /**
     * Checks if a location is inside this territory.
     * @param location The location to check
     * @return True if the location is inside this territory, false otherwise
     */
    public boolean isInside(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        // Get territory bounds
        TerritoryWars plugin = (TerritoryWars) Bukkit.getPluginManager().getPlugin("TerritoryWars");
        if (plugin == null) {
            return false;
        }
        
        int gridSize = plugin.getTerritoryManager().getGridSize();
        int blockSize = plugin.getTerritoryManager().getBlockSize();
        
        int minX = gridX * gridSize * blockSize;
        int minZ = gridZ * gridSize * blockSize;
        int maxX = minX + (gridSize * blockSize) - 1;
        int maxZ = minZ + (gridSize * blockSize) - 1;
        
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Places the core at the specified location.
     * @param location The location to place the core
     * @param plugin The TerritoryWars plugin
     * @return True if the core was placed successfully, false otherwise
     */
    public boolean placeCore(Location location, TerritoryWars plugin) {
        // Check if core is already placed
        if (coreLocation != null) {
            return false;
        }
        
        // Check if location is valid
        if (!isInside(location) || !plugin.getCoreStructure().isValidLocation(location, this)) {
            return false;
        }
        
        // Spawn core structure
        List<Location> blocks = plugin.getCoreStructure().spawn(location);
        
        // Update territory
        coreLocation = location;
        coreBlocks = blocks;
        coreHealth = plugin.getCoreStructure().getCoreHealth();
        
        return true;
    }

    /**
     * Removes the core.
     * @param plugin The TerritoryWars plugin
     */
    public void removeCore(TerritoryWars plugin) {
        if (coreLocation != null && !coreBlocks.isEmpty()) {
            plugin.getCoreStructure().remove(coreBlocks);
            coreLocation = null;
            coreBlocks.clear();
        }
    }

    /**
     * Damages the core.
     * @param damage The amount of damage to do
     */
    public void damageCore(int damage) {
        coreHealth -= damage;
        if (coreHealth < 0) {
            coreHealth = 0;
        }
    }

    /**
     * Checks if the core is destroyed.
     * @return True if the core is destroyed, false otherwise
     */
    public boolean isCoreDestroyed() {
        return coreHealth <= 0;
    }

    /**
     * Checks if a block is part of the core.
     * @param location The location to check
     * @return True if the block is part of the core, false otherwise
     */
    public boolean isBlockPartOfCore(Location location) {
        if (coreBlocks == null || coreBlocks.isEmpty()) {
            return false;
        }
        
        TerritoryWars plugin = (TerritoryWars) Bukkit.getPluginManager().getPlugin("TerritoryWars");
        if (plugin == null) {
            return false;
        }
        
        return plugin.getCoreStructure().isPartOfStructure(location, coreBlocks);
    }

    /**
     * Broadcasts an attack alert to all online members of the owning clan.
     * @param plugin The TerritoryWars plugin
     */
    public void broadcastAttackAlert(TerritoryWars plugin) {
        if (owner == null) {
            return;
        }
        
        String message = plugin.getMessage("territory_under_attack")
                .replace("%d", String.valueOf(gridX * plugin.getTerritoryManager().getGridSize() * plugin.getTerritoryManager().getBlockSize()))
                .replace("%d", String.valueOf(gridZ * plugin.getTerritoryManager().getGridSize() * plugin.getTerritoryManager().getBlockSize()));
        
        // Send message to all online clan members
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (owner.isMember(playerId)) {
                player.sendMessage(message);
            }
        }
    }
}
