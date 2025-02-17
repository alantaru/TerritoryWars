package com.alantaru.territorywars;

import com.google.gson.*;
import net.milkbowl.vault.economy.Economy;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
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
            .registerTypeAdapter(Territory.class, new TerritoryAdapter());
        
        this.gson = builder.create();
        loadTerritories();
    }

    public Territory createTerritory(Player player, Location location, String territoryName) {
        Clan clan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage("§cVocê precisa fazer parte de um clã para criar territórios!");
            return null;
        }

        if (!clan.isLeader(player.getUniqueId()) && 
            !plugin.getClans().getPermissionsManager().has(player, "simpleclans.leader.can-create")) {
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

        territory.setName(gridX + "," + gridZ);

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

            // Find a leader to give the money to
            Player leader = Bukkit.getPlayer(clan.getLeaders().get(0).getUniqueId());
             if (leader != null) {
                economy.depositPlayer(leader, refundAmount);
                leader.sendMessage(String.format(
                    "§aSeu clã abandonou um território e recebeu um reembolso de §f%.2f",
                    refundAmount
                ));
            } else {
                 plugin.getLogger().warning("Could not find leader to refund territory abandonment cost to.");
            }
        }

        territory.getCoreBlocks().forEach(loc ->
            loc.getBlock().setType(org.bukkit.Material.AIR)
        );

        for (UUID adjacentId : territory.getAdjacentTerritories()) {
            Territory adjacent = territories.get(adjacentId);
            if (adjacent != null) {
                updateAdjacencies(adjacent);
            }
        }

        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.removeDynmapTerritory(territory);
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
            if (territory.getName().equalsIgnoreCase(name)) {
                return territory;
            }
        }
        return null;
    }

    public void save() {
        saveTerritories();
    }

    private static class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
        @Override
        public JsonElement serialize(Location location, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("world", location.getWorld().getName());
            json.addProperty("x", location.getX());
            json.addProperty("y", location.getY());
            json.addProperty("z", location.getZ());
            json.addProperty("yaw", location.getYaw());
            json.addProperty("pitch", location.getPitch());
            return json;
        }

        @Override
        public Location deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            World world = Bukkit.getWorld(json.get("world").getAsString());
            if (world == null) {
                throw new JsonParseException("World not found");
            }
            double x = json.get("x").getAsDouble();
            double y = json.get("y").getAsDouble();
            double z = json.get("z").getAsDouble();
            float yaw = json.get("yaw").getAsFloat();
            float pitch = json.get("pitch").getAsFloat();
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    private class TerritoryAdapter implements JsonSerializer<Territory>, JsonDeserializer<Territory> {
        @Override
        public JsonElement serialize(Territory territory, Type type, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty("id", territory.getId().toString());
            json.addProperty("gridX", territory.getGridX());
            json.addProperty("gridZ", territory.getGridZ());
            json.addProperty("clanId", territory.getOwner().getTag());
            json.add("coreLocation", context.serialize(territory.getCoreLocation(), Location.class));
            json.addProperty("creationCost", territory.getCreationCost());
            json.addProperty("resistanceMultiplier", territory.getResistanceMultiplier());
            json.addProperty("protectionMode", territory.getProtectionMode().name());
            json.addProperty("coreHealth", territory.getCoreHealth());
            json.addProperty("lastDamageTime", territory.getLastDamageTime());
            json.addProperty("lastTributePaid", territory.getLastTributePaid());
            json.addProperty("displayName", territory.getDisplayName());
            json.addProperty("description", territory.getDescription());
            json.addProperty("banner", territory.getBanner());
            return json;
        }

        @Override
        public Territory deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = element.getAsJsonObject();
            int gridX = json.get("gridX").getAsInt();
            int gridZ = json.get("gridZ").getAsInt();
            
            String clanTag = json.get("clanId").getAsString();
            ClanManager clanManager = plugin.getClans().getClanManager();
            Clan clan = clanManager.getClan(clanTag);
            if (clan == null) {
                throw new JsonParseException("Clan not found: " + clanTag);
            }

            Location coreLoc = context.deserialize(json.get("coreLocation"), Location.class);
            double creationCost = json.get("creationCost").getAsDouble();
            double resistanceMultiplier = json.get("resistanceMultiplier").getAsDouble();

            Territory territory = new Territory(gridX, gridZ, clan, coreLoc, creationCost, resistanceMultiplier, "dummy");
            territory.setProtectionMode(ProtectionMode.valueOf(json.get("protectionMode").getAsString()));
            territory.setCoreHealth(json.get("coreHealth").getAsInt());
            territory.setLastDamageTime(json.get("lastDamageTime").getAsLong());
            territory.setLastTributePaid(json.get("lastTributePaid").getAsLong());
            territory.setDisplayName(json.get("displayName").getAsString());
            territory.setDescription(json.get("description").getAsString());
            territory.setBanner(json.get("banner").getAsString());

            return territory;
        }
    }
}
