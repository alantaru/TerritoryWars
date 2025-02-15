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
    private final Map<UUID, PolyLineMarker> adjacencyLines;
    private boolean enabled;

    public DynmapManager(TerritoryWars plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.territoryMarkers = new HashMap<>();
        this.adjacencyLines = new HashMap<>();
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
        //int gridX = territory.getGridX();
        //int gridZ = territory.getGridZ();
        //int chunkSize = 16;
        //int gridSize = 3;
        
        //double x1 = gridX * gridSize * chunkSize;
        //double z1 = gridZ * gridSize * chunkSize;
        //double x2 = x1 + (gridSize * chunkSize);
        //double z2 = z1 + (gridSize * chunkSize);

        // Create marker
        //String markerId = "territory_" + territory.getId().toString();
        //String label = territory.getDisplayName();
        //String description = String.format(
        //    "<div>%s</div><div>Clã: %s</div>",
        //    territory.getDescription(),
        //    territory.getOwner().getName()
        //);

        //Location coreLoc = territory.getCoreLocation();
        //AreaMarker marker = markerSet.createAreaMarker(
        //    markerId, label, true,
        //    coreLoc.getWorld().getName(),
        //    new double[]{x1, x2, x2, x1},
        //    new double[]{z1, z1, z2, z2},
        //    true
        //);

        // Set marker style
        //String fillColor = plugin.getConfig().getString("dynmap.marker-style.fill-color", "#FF0000");
        //double fillOpacity = plugin.getConfig().getDouble("dynmap.marker-style.fill-opacity", 0.3);
        //String strokeColor = plugin.getConfig().getString("dynmap.marker-style.stroke-color", "#000000");
        //int strokeWeight = plugin.getConfig().getInt("dynmap.marker-style.stroke-weight", 3);

        //marker.setFillStyle(fillOpacity, Integer.parseInt(fillColor.substring(1), 16));
        //marker.setLineStyle(strokeWeight, 1.0, Integer.parseInt(strokeColor.substring(1), 16));
        //marker.setDescription(description);

        //territoryMarkers.put(territory.getId(), marker);

        // Update adjacency lines
        //if (plugin.getConfig().getBoolean("dynmap.show-adjacency", true)) {
        //    updateAdjacencyLines(territory);
        //}
    }

    public void updateAdjacencyLines(Territory territory) {
        // Remove old lines
        //PolyLineMarker oldLine = adjacencyLines.remove(territory.getId());
        //if (oldLine != null) {
        //    oldLine.deleteMarker();
        //}

        // Create lines to adjacent territories
        //for (UUID adjacentId : territory.getAdjacentTerritories()) {
        //    Territory adjacent = territoryManager.getTerritories().get(adjacentId);
        //    if (adjacent == null) continue;

        //    // Calculate center points
        //    double x1 = (territory.getGridX() * 3 * 16) + (3 * 16 / 2.0);
        //    double z1 = (territory.getGridZ() * 3 * 16) + (3 * 16 / 2.0);
        //    double x2 = (adjacent.getGridX() * 3 * 16) + (3 * 16 / 2.0);
        //    double z2 = (adjacent.getGridZ() * 3 * 16) + (3 * 16 / 2.0);

        //    // Create line
        //    String lineId = "adjacency_" + territory.getId() + "_" + adjacentId;
        //    String lineColor = plugin.getConfig().getString("dynmap.adjacency-color", "#FF0000");

        //    PolyLineMarker line = markerSet.createPolyLineMarker(
        //        lineId, "", true,
        //        territory.getCoreLocation().getWorld().getName(),
        //        new double[]{x1, x2},
        //        new double[]{territory.getCoreLocation().getY(), territory.getCoreLocation().getY()},
        //        new double[]{z1, z2},
        //        true
        //    );

        //    line.setLineStyle(2, 1.0, Integer.parseInt(lineColor.substring(1), 16));
        //    adjacencyLines.put(territory.getId(), line);
        //}
    }

    public void removeTerritory(Territory territory) {
        if (!enabled) return;

        // Remove marker
        AreaMarker marker = territoryMarkers.remove(territory.getId());
        if (marker != null) {
            marker.deleteMarker();
        }

        // Remove adjacency lines
        //PolyLineMarker line = adjacencyLines.remove(territory.getId());
        //if (line != null) {
        //    line.deleteMarker();
        //}
    }

    public void updateAllTerritories() {
        if (!enabled) return;

        // Clear all markers
        territoryMarkers.values().forEach(marker -> marker.deleteMarker());
        territoryMarkers.clear();
        //adjacencyLines.values().forEach(line -> line.deleteMarker());
        //adjacencyLines.clear();

        // Update all territories
        territoryManager.getTerritories().values().forEach(this::updateTerritory);
    }

    public boolean isEnabled() {
        return enabled;
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
            String label = clan.getName(); // Display clan name as label
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

            marker.setFillStyle(fillOpacity, Integer.parseInt(fillColor.substring(1), 16));
            marker.setLineStyle(strokeWeight, 1.0, Integer.parseInt(strokeColor.substring(1), 16));
            marker.setDescription(description);

            territoryMarkers.put(UUID.randomUUID(), marker); // Use a random UUID as the key
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

            double x1 = gridX * gridSize * chunkSize;
            double z1 = gridZ * gridSize * chunkSize;
            double x2 = x1 + (gridSize * chunkSize);
            double z2 = z1 + (gridSize * chunkSize);

            Location corner1 = new Location(territory.getCoreLocation().getWorld(), x1, territory.getCoreLocation().getY(), z1);
            Location corner2 = new Location(territory.getCoreLocation().getWorld(), x2, territory.getCoreLocation().getY(), z1);
            Location corner3 = new Location(territory.getCoreLocation().getWorld(), x2, territory.getCoreLocation().getY(), z2);
            Location corner4 = new Location(territory.getCoreLocation().getWorld(), x1, territory.getCoreLocation().getY(), z2);

            if (!isAdjacentToClanTerritory(territory, clan, gridX - 1, gridZ)) {
                boundary.add(corner1);
                boundary.add(corner4);
            }
            if (!isAdjacentToClanTerritory(territory, clan, gridX + 1, gridZ)) {
                boundary.add(corner2);
                boundary.add(corner3);
            }
            if (!isAdjacentToClanTerritory(territory, clan, gridX, gridZ - 1)) {
                boundary.add(corner1);
                boundary.add(corner2);
            }
            if (!isAdjacentToClanTerritory(territory, clan, gridX, gridZ + 1)) {
                boundary.add(corner3);
                boundary.add(corner4);
            }
        }

        return boundary;
    }

    private boolean isAdjacentToClanTerritory(Territory territory, Clan clan, int gridX, int gridZ) {
        return territoryManager.getTerritoryAtGrid(gridX, gridZ) != null &&
               territoryManager.getTerritoryAtGrid(gridX, gridZ).getOwner().equals(clan);
    }
}
