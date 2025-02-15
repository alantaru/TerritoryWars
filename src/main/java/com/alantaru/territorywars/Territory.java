package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.*;

public class Territory {
    private final UUID id;
    private final int gridX;
    private final int gridZ;
    private Clan owner;
    private Location coreLocation;
    private final double creationCost;
    private final double resistanceMultiplier;
    private ProtectionMode protectionMode;
    private int coreHealth;
    private long lastDamageTime;
    private long lastTributePaid;
    private String displayName;
    private String description;
    private String banner;
    private String name;
    private final Set<UUID> adjacentTerritories;
    private final List<Location> coreBlocks;

    public Territory(int gridX, int gridZ, Clan owner, Location coreLocation, double creationCost, double resistanceMultiplier) {
        this.id = UUID.randomUUID();
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.coreLocation = coreLocation;
        this.creationCost = creationCost;
        this.resistanceMultiplier = resistanceMultiplier;
        this.protectionMode = ProtectionMode.INFINITE_WAR;
        this.coreHealth = 50; // Default value, can be overridden by config
        this.lastDamageTime = 0;
        this.lastTributePaid = System.currentTimeMillis();
        this.displayName = "Território " + gridX + "," + gridZ;
        this.description = "Território pertencente a " + owner.getName();
        this.banner = "";
        this.adjacentTerritories = new HashSet<>();
        this.coreBlocks = new ArrayList<>();
    }

    public static int[] calculateGridCoordinates(Location location) {
        // Convert block coordinates to chunk coordinates
        // Example: Block X=1 -> Chunk X=0 (since 1/16 = 0)
        int chunkX = location.getBlockX() >> 4; // Divide by 16
        int chunkZ = location.getBlockZ() >> 4;
        
        // Convert chunk coordinates to grid coordinates (3x3 chunks per grid)
        // Example: If at block X=1, Z=1 (chunk 0,0), we want grid 0,0
        // Grid 0,0 contains chunks 0,0 to 2,2
        // Grid 0,1 contains chunks 0,3 to 2,5
        int gridX = Math.floorDiv(chunkX, 3);
        int gridZ = Math.floorDiv(chunkZ, 3);
        
        return new int[]{gridX, gridZ};
    }

    public boolean isInGrid(Location location) {
        // Get chunk coordinates
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        
        // Calculate grid bounds
        // Each grid is 3x3 chunks
        // Grid 0,0 contains chunks 0,0 to 2,2
        // Grid 0,1 contains chunks 0,3 to 2,5
        int startChunkX = gridX * 3;
        int startChunkZ = gridZ * 3;
        int endChunkX = startChunkX + 2; // +2 because it's 3 chunks wide (0,1,2)
        int endChunkZ = startChunkZ + 2;
        
        // Check if chunk is within grid bounds
        return chunkX >= startChunkX && chunkX <= endChunkX &&
               chunkZ >= startChunkZ && chunkZ <= endChunkZ;
    }

    public boolean isAdjacent(Territory other) {
        // Check if territories share a border
        // For example: Grid 0,0 is adjacent to 0,1 but not 0,2
        return Math.abs(this.gridX - other.gridX) <= 1 && 
               Math.abs(this.gridZ - other.gridZ) <= 1 &&
               !(this.gridX == other.gridX && this.gridZ == other.gridZ);
    }

    public boolean isBlockPartOfCore(Location location) {
        for (Location loc : coreBlocks) {
            if (loc.getBlockX() == location.getBlockX() &&
                loc.getBlockY() == location.getBlockY() &&
                loc.getBlockZ() == location.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public boolean canMoveCoreToLocation(Location newLocation) {
        if (!isInGrid(newLocation)) {
            return false;
        }

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    Location checkLoc = newLocation.clone().add(x, y, z);
                    if (!isInGrid(checkLoc)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public void spawnCoreStructure(TerritoryWars plugin) {
        for (Location loc : coreBlocks) {
            loc.getBlock().setType(org.bukkit.Material.AIR);
        }
        coreBlocks.clear();

        List<Location> newBlocks = plugin.getCoreStructure().spawn(coreLocation);
        coreBlocks.addAll(newBlocks);

        this.coreHealth = plugin.getConfig().getInt("core.required-hits", 50);
    }

    public void damageCore(int damage) {
        int reducedDamage = (int) Math.ceil(damage / resistanceMultiplier);
        coreHealth = Math.max(0, coreHealth - reducedDamage);
    }

    public boolean isCoreDestroyed() {
        return coreHealth <= 0;
    }

    public void broadcastAttackAlert(TerritoryWars plugin) {
        int blockX = gridX * 3 * 16;
        int blockZ = gridZ * 3 * 16;
        
        String message = String.format(
            "§c⚠ ALERTA: O território em X:%d Z:%d está sendo atacado!",
            blockX, blockZ
        );

        owner.getMembers().stream()
            .map(member -> plugin.getServer().getPlayer(member.getUniqueId()))
            .filter(player -> player != null && player.isOnline())
            .forEach(player -> {
                player.sendMessage(message);
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 1.0f);
            });

        plugin.debug(String.format("Territory at grid %d,%d under attack", gridX, gridZ));
    }

    public double calculateTribute(TerritoryWars plugin) {
        return creationCost * plugin.getConfig().getDouble("economy.tribute.per-territory", 0.1);
    }

    public void updateTributePayment() {
        this.lastTributePaid = System.currentTimeMillis();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public Clan getOwner() {
        return owner;
    }

    public void setOwner(Clan owner) {
        this.owner = owner;
    }

    public Location getCoreLocation() {
        return coreLocation;
    }

    public void setCoreLocation(Location coreLocation) {
        if (!canMoveCoreToLocation(coreLocation)) {
            throw new IllegalArgumentException("Invalid core location! Must be within territory bounds.");
        }
        this.coreLocation = coreLocation;
    }

    public double getCreationCost() {
        return creationCost;
    }

    public double getResistanceMultiplier() {
        return resistanceMultiplier;
    }

    public ProtectionMode getProtectionMode() {
        return protectionMode;
    }

    public void setProtectionMode(ProtectionMode protectionMode) {
        this.protectionMode = protectionMode;
    }

    public int getCoreHealth() {
        return coreHealth;
    }

    public void setCoreHealth(int coreHealth) {
        this.coreHealth = coreHealth;
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }

    public void setLastDamageTime(long lastDamageTime) {
        this.lastDamageTime = lastDamageTime;
    }

    public long getLastTributePaid() {
        return lastTributePaid;
    }

    public void setLastTributePaid(long lastTributePaid) {
        this.lastTributePaid = lastTributePaid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public Set<UUID> getAdjacentTerritories() {
        return new HashSet<>(adjacentTerritories);
    }

    public void addAdjacentTerritory(UUID territoryId) {
        adjacentTerritories.add(territoryId);
    }

    public List<Location> getCoreBlocks() {
        return Collections.unmodifiableList(coreBlocks);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
