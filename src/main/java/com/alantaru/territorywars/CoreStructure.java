package com.alantaru.territorywars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the core structure of a territory.
 */
public class CoreStructure {
    private final TerritoryWars plugin;
    private final Material material;
    private final int size;
    private final double resistanceMultiplier;
    private final long damageInterval;
    private final int damagePerHit;
    private final int coreHealth;

    /**
     * Constructor for the CoreStructure.
     * @param plugin The TerritoryWars plugin.
     */
    public CoreStructure(TerritoryWars plugin) {
        this.plugin = plugin;
        Material mat;
        int size;
        double resistanceMultiplier;
        long damageInterval;
        int damagePerHit;
        int coreHealth;

        try {
            mat = Material.valueOf(plugin.getConfig().getString("core.structure.material", "OBSIDIAN"));
            size = plugin.getConfig().getInt("core.structure.size", 2);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("Invalid core material specified in config.yml. Using default material: OBSIDIAN.");
            mat = Material.OBSIDIAN; // Default material
            size = 2; // Default size
        }
        this.material = mat;
        this.size = size;
        this.resistanceMultiplier = plugin.getConfig().getDouble("core.resistance-multiplier", 5.0);
        this.damageInterval = plugin.getConfig().getLong("core.damage-interval", 2000);
        this.damagePerHit = plugin.getConfig().getInt("core.damage-per-hit", 1);
        this.coreHealth = plugin.getConfig().getInt("core.required-hits", 50);
    }

    /**
     * Spawns the core structure at the given location.
     * @param baseLocation The location to spawn the core structure at.
     * @return A list of locations of the blocks that make up the core structure.
     */
    public List<Location> spawn(Location baseLocation) {
        List<Location> coreBlocks = new ArrayList<>();
        World world = baseLocation.getWorld();

        // Create a size x size x size cube of obsidian blocks
        // The base location is the bottom-front-left corner
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    Location blockLoc = baseLocation.clone().add(x, y, z);
                    Block block = world.getBlockAt(blockLoc);
                    block.setType(material);
                    coreBlocks.add(blockLoc);
                }
            }
        }

        return coreBlocks;
    }

    /**
     * Removes the core structure from the world.
     * @param coreBlocks A list of locations of the blocks that make up the core structure.
     */
    public void remove(List<Location> coreBlocks) {
        // Remove all blocks in the core structure
        for (Location loc : coreBlocks) {
            loc.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Checks if the given location is a valid location for the core structure.
     * @param location The location to check.
     * @param territory The territory the core structure is in.
     * @return True if the location is valid, false otherwise.
     */
    public boolean isValidLocation(Location location, Territory territory) {
        // Check if the base location is within the territory bounds
        if (!territory.isInside(location)) {
            return false;
        }

        // Check if the entire core structure would be within the territory bounds
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    Location checkLoc = location.clone().add(x, y, z);

                    // Check if location is within territory bounds
                    if (!territory.isInside(checkLoc)) {
                        return false;
                    }

                    // Check if there's enough space (no solid blocks)
                    Block block = checkLoc.getBlock();
                    if (block.getType().isSolid() && block.getType() != material) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Checks if the given location is part of the core structure.
     * @param location The location to check.
     * @param coreBlocks A list of locations of the blocks that make up the core structure.
     * @return True if the location is part of the core structure, false otherwise.
     */
    public boolean isPartOfStructure(Location location, List<Location> coreBlocks) {
        // Check if a block location is part of the core structure
        for (Location loc : coreBlocks) {
            if (loc.getBlockX() == location.getBlockX() &&
                loc.getBlockY() == location.getBlockY() &&
                loc.getBlockZ() == location.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Gets the number of hits required to destroy the core.
	 * @return The number of hits required to destroy the core.
	 */
	public int getCoreHealth() {
		return coreHealth;
	}

	/**
	 * Gets how much more resistant the core is compared to normal obsidian.
	 * @return How much more resistant the core is compared to normal obsidian.
	 */
	public double getResistanceMultiplier() {
		return resistanceMultiplier;
	}

	/**
	 * Gets the minimum time between hits in milliseconds.
	 * @return The minimum time between hits in milliseconds.
	 */
	public long getDamageInterval() {
		return damageInterval;
	}

	/**
	 * Gets how much damage each hit does.
	 * @return How much damage each hit does.
	 */
	public int getDamagePerHit() {
		return damagePerHit;
	}

	/**
	 * Gets the material of the core structure.
	 * @return The material of the core structure.
	 */
	public Material getMaterial() {
		return material;
	}

	/**
	 * Gets the size of the core structure.
	 * @return The size of the core structure.
	 */
	public int getSize() {
		return size;
	}
}
