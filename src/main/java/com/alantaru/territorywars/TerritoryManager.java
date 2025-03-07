package com.alantaru.territorywars;

import com.google.gson.*;
import net.milkbowl.vault.economy.Economy;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class TerritoryManager {
    private final TerritoryWars plugin;
    private final Map<UUID, Territory> territories;
    private final File territoriesFile;
    private final Gson gson;

    public TerritoryManager(TerritoryWars plugin) {
        this.plugin = plugin;
        this.territories = new HashMap<>();
        this.territoriesFile = new File(plugin.getDataFolder(), "territories.json");

        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Location.class, new LocationAdapter())
                .registerTypeAdapter(Territory.class, new TerritoryAdapter(plugin));

        this.gson = builder.create();
        loadTerritories();
    }

    public Territory createTerritory(Player player, Location location, String territoryName) {
        Clan clan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§cVocê precisa fazer parte de um clã para criar territórios!");
            return null;
        }

        if (!plugin.getClans().getPermissionsManager().has(player, "simpleclans.leader.can-create")) {
            player.sendMessage("§cApenas líderes e membros autorizados podem criar territórios!");
            return null;
        }

        int[] gridCoords = Territory.calculateGridCoordinates(location);
        int gridX = gridCoords[0];
        int gridZ = gridCoords[1];

        if (getTerritoryAtGrid(gridX, gridZ) != null) {
            player.sendMessage("§cJá existe um território neste local!");
            return null;
        }

        boolean isFirstTerritory = !hasTerritory(clan);
        double cost = isFirstTerritory ? plugin.getFirstTerritoryCost() : plugin.getTerritoryCost();

        Economy economy = plugin.getEconomy();
        if (!economy.has(player, cost)) {
            player.sendMessage(String.format(
                    "§cVocê precisa de §f%.2f §cpara criar um território!",
                    cost
            ));
            return null;
        }

        if (!isFirstTerritory && !hasAdjacentTerritoryAtGrid(clan, gridX, gridZ)) {
            player.sendMessage("§cVocê só pode criar territórios adjacentes aos seus!");
            return null;
        }

        Territory territory = new Territory(gridX, gridZ, clan, location, cost,
                plugin.getCoreStructure().getResistanceMultiplier(), territoryName);

       if (!plugin.getCoreStructure().isValidLocation(location, territory)) {
            player.sendMessage("§cLocalização inválida para o núcleo!");
            return null;
        }

        economy.withdrawPlayer(player, cost);
        player.sendMessage(String.format(
            "§aTerritório criado com sucesso! Custo: §f%.2f",
            cost
        ));

        territory.spawnCoreStructure(plugin);
        territories.put(territory.getId(), territory);
        updateAdjacencies(territory);
        saveTerritories();

        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
            plugin.getDynmapManager().updateAllClanTerritories();
        }

        return territory;
    }

    public void removeTerritory(UUID territoryId, boolean abandoned) {
        Territory territory = territories.remove(territoryId);
        if (territory == null) return;

        Economy economy = plugin.getEconomy();
        Clan clan = territory.getOwner();

        if (abandoned) {
            double refundPercentage = plugin.getConfig().getDouble("economy.abandon.refund-percentage", 0.5);
            double refundAmount = territory.getCreationCost() * refundPercentage;

            // Attempt to deposit into the clan's bank account, if possible
            if (clan.getBalance() >= 0) { // Assuming getBalance() exists and a negative value indicates an error
                clan.deposit(refundAmount, null); // Assuming deposit() exists and takes a CommandSender (or null)
                // Broadcast message to online clan members
                String refundMessage = "§aSeu clã abandonou um território e recebeu um reembolso de §f" + String.format("%.2f", refundAmount);
                clan.getMembers().forEach(member -> {
                    Player p = Bukkit.getPlayer(member.getUniqueId());
                    if (p != null && p.isOnline()){
                        p.sendMessage(refundMessage);
                    }
                });
            } else {
                // Fallback: Give to online leaders
                for (net.sacredlabyrinth.phaed.simpleclans.ClanPlayer cp : clan.getLeaders()) {
                    Player player = Bukkit.getPlayer(cp.getUniqueId());
                    if (player != null && player.isOnline()) {
                        economy.depositPlayer(player, refundAmount);
                        player.sendMessage(String.format(
                            "§aSeu clã abandonou um território e recebeu um reembolso de §f%.2f",
                            refundAmount
                        ));
                    }
                }
            }
        }

        territory.getCoreBlocks().forEach(loc -> loc.getBlock().setType(org.bukkit.Material.AIR));

        for (UUID adjacentId : territory.getAdjacentTerritories()) {
            Territory adjacent = territories.get(adjacentId);
            if (adjacent != null) {
                updateAdjacencies(adjacent);
            }
        }

        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.removeDynmapTerritory(territory);
            plugin.getDynmapManager().updateAllClanTerritories();
        }

        saveTerritories();
    }

    public Territory getTerritoryAt(Location location) {
        int[] gridCoords = Territory.calculateGridCoordinates(location);
        return getTerritoryAtGrid(gridCoords[0], gridCoords[1]);
    }

    public Territory getTerritoryAtGrid(int gridX, int gridZ) {
        return territories.values().stream()
                .filter(t -> t.getGridX() == gridX && t.getGridZ() == gridZ)
                .findFirst()
                .orElse(null);
    }

    public boolean hasTerritory(Clan clan) {
        return territories.values().stream()
                .anyMatch(t -> t.getOwner().equals(clan));
    }

    public boolean hasAdjacentTerritory(Territory territory, Clan clan) {
        return territory.getAdjacentTerritories().stream()
                .map(territories::get)
                .filter(Objects::nonNull)
                .anyMatch(t -> t.getOwner().equals(clan));
    }

    private boolean hasAdjacentTerritoryAtGrid(Clan clan, int gridX, int gridZ) {
        return territories.values().stream()
                .filter(t -> t.getOwner().equals(clan))
                .anyMatch(t -> Math.abs(t.getGridX() - gridX) <= 1 && 
                          Math.abs(t.getGridZ() - gridZ) <= 1 &&
                          !(t.getGridX() == gridX && t.getGridZ() == gridZ));
    }

    private void updateAdjacencies(Territory territory) {
        territory.getAdjacentTerritories().clear();

        territories.values().stream()
                .filter(t -> t.isAdjacent(territory))
                .forEach(t -> {
                    territory.addAdjacentTerritory(t.getId());
                    t.addAdjacentTerritory(territory.getId());
                });
    }

    public void loadTerritories() {
        territories.clear();

        if (!territoriesFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(territoriesFile)) {
            Territory[] loadedTerritories = gson.fromJson(reader, Territory[].class);
            if (loadedTerritories != null) {
                for (Territory territory : loadedTerritories) {
                    territories.put(territory.getId(), territory);
                    updateAdjacencies(territory); // Update adjacencies after loading
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load territories: " + e.getMessage());
        }
    }

    public void saveTerritories() {
        try (FileWriter writer = new FileWriter(territoriesFile)) {
            gson.toJson(territories.values(), writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save territories: " + e.getMessage());
        }
    }

    public Map<UUID, Territory> getTerritories() {
        return Collections.unmodifiableMap(territories);
    }

    public void reload() {
        loadTerritories();
    }

    public Territory getTerritoryByName(String name) {
        for (Territory territory : territories.values()) {
            if (territory.getDisplayName().equalsIgnoreCase(name)) {
                return territory;
            }
        }
        return null;
    }

    public Clan getDominantClan() {
        Clan dominantClan = null;
        int maxTerritories = 0;

        Map<Clan, Integer> clanTerritoryCounts = new HashMap<>();

        for (Territory territory : territories.values()) {
            Clan clan = territory.getOwner();
            if (clan != null) {
                clanTerritoryCounts.put(clan, clanTerritoryCounts.getOrDefault(clan, 0) + 1);
                if (clanTerritoryCounts.get(clan) > maxTerritories) {
                    maxTerritories = clanTerritoryCounts.get(clan);
                    dominantClan = clan;
                }
            }
        }

        return dominantClan;
    }
    
    public void save() {
        saveTerritories();
    }
}