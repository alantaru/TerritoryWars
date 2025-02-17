package com.alantaru.territorywars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;

public class CoreStructure {
    private final TerritoryWars plugin;
    private final Material material;
    private final int size;

    public CoreStructure(TerritoryWars plugin) {
        this.plugin = plugin;
        this.material = Material.valueOf(
            plugin.getConfig().getString("core.structure.material", "OBSIDIAN")
        );
        this.size = plugin.getConfig().getInt("core.structure.size", 2);
    }

    public List<Location> spawn(Location baseLocation) {
        List<Location> coreBlocks = new ArrayList<>();
        World world = baseLocation.getWorld();
        
        // Create a 2x2x2 cube of obsidian blocks
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

    public void remove(List<Location> coreBlocks) {
        // Remove all blocks in the core structure
        for (Location loc : coreBlocks) {
            loc.getBlock().setType(Material.AIR);
        }
    }

    public boolean isValidLocation(Location location, Territory territory) {
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

    public int getRequiredHits() {
        // Get the number of hits required to destroy the core
        return plugin.getConfig().getInt("core.required-hits", 50);
    }

    public double getResistanceMultiplier() {
        // Get how much more resistant the core is compared to normal obsidian
        return plugin.getConfig().getDouble("core.resistance-multiplier", 5.0);
    }

    public long getDamageInterval() {
        // Get the minimum time between hits in milliseconds
        return plugin.getConfig().getLong("core.damage-interval", 2000);
    }

    public int getDamagePerHit() {
        // Get how much damage each hit does
        return plugin.getConfig().getInt("core.damage-per-hit", 1);
    }

    public Material getMaterial() {
        return material;
    }

    public int getSize() {
        return size;
    }
}
