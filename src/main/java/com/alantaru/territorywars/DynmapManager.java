package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynmapManager {
    private final TerritoryWars plugin;
    private final TerritoryManager territoryManager;
    private DynmapAPI dynmap;
    private MarkerSet markerSet;
    private final Map<UUID, AreaMarker> territoryMarkers;
    private static final int CHUNK_SIZE = 16;
    private static final int GRID_SIZE = 3;
    private boolean enabled;

    public DynmapManager(TerritoryWars plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.territoryMarkers = new HashMap<>();
        this.enabled = setupDynmap();
    }

    private boolean setupDynmap() {
        Plugin dynmapPlugin = plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapPlugin == null) {
            return false;
        }

        dynmap = (DynmapAPI) dynmapPlugin;
        MarkerAPI markerAPI = dynmap.getMarkerAPI();
        if (markerAPI == null) {
            return false;
        }

        // Remove existing marker set if any
        markerSet = markerAPI.getMarkerSet("territorywars.territories");
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
        }

        // Create new marker set
        String layerName = plugin.getConfig().getString("dynmap.layer-name", "Territórios");
        markerSet = markerAPI.createMarkerSet("territorywars.territories", layerName, null, true);
        markerSet.setLayerPriority(10);
        markerSet.setHideByDefault(false);

        return true;
    }

    public void updateTerritory(Territory territory) {
        if (!enabled) return;

        // Remove old marker if exists
        AreaMarker oldMarker = territoryMarkers.remove(territory.getId());
        if (oldMarker != null) {
            oldMarker.deleteMarker();
        }

        // Calculate territory bounds
        int gridX = territory.getGridX();
        int gridZ = territory.getGridZ();

        
        double x1 = gridX * GRID_SIZE * CHUNK_SIZE;
        double z1 = gridZ * GRID_SIZE * CHUNK_SIZE;
        double x2 = x1 + (GRID_SIZE * CHUNK_SIZE);
        double z2 = z1 + (GRID_SIZE * CHUNK_SIZE);

        // Create marker
        String markerId = "territory_" + territory.getId().toString();
        String label = territory.getDisplayName();
        String description = String.format(
            "<div>%s</div><div>Clã: %s</div>",
            territory.getDescription(),
            territory.getOwner().getName()
        );

        Location coreLoc = territory.getCoreLocation();
        AreaMarker marker = markerSet.createAreaMarker(
            markerId, label, true,
            coreLoc.getWorld().getName(),
            new double[]{x1, x2, x2, x1},
            new double[]{z1, z1, z2, z2},
            true
        );

        // Set marker style
        String fillColor = plugin.getConfig().getString("dynmap.marker-style.fill-color", "#FF0000");
        double fillOpacity = plugin.getConfig().getDouble("dynmap.marker-style.fill-opacity", 0.3);
        String strokeColor = plugin.getConfig().getString("dynmap.marker-style.stroke-color", "#000000");
        int strokeWeight = plugin.getConfig().getInt("dynmap.marker-style.stroke-weight", 3);

        try {
            marker.setFillStyle(fillOpacity, Integer.decode(fillColor));
            marker.setLineStyle(strokeWeight, 1.0, Integer.decode(strokeColor));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid color format in dynmap configuration: " + e.getMessage());
        }

        marker.setDescription(description);

        territoryMarkers.put(territory.getId(), marker);
    }   

    public void removeTerritory(Territory territory) {
        if (!enabled) return;

        // Remove marker
        AreaMarker marker = territoryMarkers.remove(territory.getId());
        if (marker != null) {
            marker.deleteMarker();
        }
    }

    public void updateAllTerritories() {
        if (!enabled) return;

        // Clear all markers
        territoryMarkers.values().forEach(marker -> marker.deleteMarker());
        territoryMarkers.clear();

        // Update all territories
        territoryManager.getTerritories().values().forEach(this::updateTerritory);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reload() {
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
        }
        this.enabled = setupDynmap();
        updateAllTerritories();
        updateAllClanTerritories();
    }

    public void updateAllClanTerritories() {
        if (!enabled) return;

        // Clear all markers
        territoryMarkers.values().forEach(marker -> marker.deleteMarker());
        territoryMarkers.clear();

        // Get all clans with territories
        Set<Clan> clansWithTerritories = new HashSet<>();
        for (Territory territory : territoryManager.getTerritories().values()) {
            clansWithTerritories.add(territory.getOwner());
        }

        for (Clan clan : clansWithTerritories) {
            List<Location> boundary = calculateClanTerritoryBoundary(clan);

            if (boundary.isEmpty()) {
                continue;
            }

            double[] x = new double[boundary.size()];
            double[] z = new double[boundary.size()];
            for (int i = 0; i < boundary.size(); i++) {
                x[i] = boundary.get(i).getX();
                z[i] = boundary.get(i).getZ();
            }

            // Create marker
            String markerId = "clan_" + clan.getTag();
            String label = clan.getName();
            String description = "Território de " + clan.getName();

            AreaMarker marker = markerSet.createAreaMarker(
                    markerId, label, true,
                    boundary.get(0).getWorld().getName(),
                    x,
                    z,
                    true
            );

            // Set marker style
            String fillColor = plugin.getConfig().getString("dynmap.marker-style.fill-color", "#FF0000");
            double fillOpacity = plugin.getConfig().getDouble("dynmap.marker-style.fill-opacity", 0.3);
            String strokeColor = plugin.getConfig().getString("dynmap.marker-style.stroke-color", "#000000");
            int strokeWeight = plugin.getConfig().getInt("dynmap.marker-style.stroke-weight", 3);

            try {
                marker.setFillStyle(fillOpacity, Integer.decode(fillColor));
                marker.setLineStyle(strokeWeight, 1.0, Integer.decode(strokeColor));
            }
            catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid color format in dynmap configuration: " + e.getMessage());
            }
            
            marker.setDescription(description);

            territoryMarkers.put(UUID.nameUUIDFromBytes(clan.getTag().getBytes()), marker);
        }
    }

    private List<Location> calculateClanTerritoryBoundary(Clan clan) {
        List<Location> boundary = new ArrayList<>();
        List<Territory> clanTerritories = territoryManager.getTerritories().values().stream()
                .filter(t -> t.getOwner().equals(clan))
                .collect(Collectors.toList());

        if (clanTerritories.isEmpty()) {
            return boundary;
        }

       int chunkSize = 16;
        int gridSize = 3;

        for (Territory territory : clanTerritories) {
            int gridX = territory.getGridX();
            int gridZ = territory.getGridZ();

            double x1 = gridX * GRID_SIZE * CHUNK_SIZE;
            double z1 = gridZ * GRID_SIZE * CHUNK_SIZE;
            double x2 = x1 + (GRID_SIZE * CHUNK_SIZE);
            double z2 = z1 + (GRID_SIZE * CHUNK_SIZE);

            Location corner1 = new Location(territory.getCoreLocation().getWorld(), x1, territory.getCoreLocation().getY(), z1);
            Location corner2 = new Location(territory.getCoreLocation().getWorld(), x2, territory.getCoreLocation().getY(), z1);
            Location corner3 = new Location(territory.getCoreLocation().getWorld(), x2, territory.getCoreLocation().getY(), z2);
            Location corner4 = new Location(territory.getCoreLocation().getWorld(), x1, territory.getCoreLocation().getY(), z2);

            if (!hasTerritoryAtGridBelongingToClan(territory, clan, gridX - 1, gridZ)) {
                boundary.add(corner1);
                boundary.add(corner4);
            }
            if (!hasTerritoryAtGridBelongingToClan(territory, clan, gridX + 1, gridZ)) {
                boundary.add(corner2);
                boundary.add(corner3);
            }
            if (!hasTerritoryAtGridBelongingToClan(territory, clan, gridX, gridZ - 1)) {
                boundary.add(corner1);
                boundary.add(corner2);
            }
            if (!hasTerritoryAtGridBelongingToClan(territory, clan, gridX, gridZ + 1)) {
                boundary.add(corner3);
                boundary.add(corner4);
            }
        }

       return boundary;
    }

   private boolean hasTerritoryAtGridBelongingToClan(Territory territory, Clan clan, int gridX, int gridZ) {
        return territoryManager.getTerritoryAtGrid(gridX, gridZ) != null &&
               territoryManager.getTerritoryAtGrid(gridX, gridZ).getOwner().equals(clan);
    }

    public void disable() {
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
            markerSet = null;
        }
        if (dynmap != null) {
            dynmap = null;
        }
        enabled = false;
    }
}
