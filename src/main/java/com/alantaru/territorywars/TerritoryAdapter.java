package com.alantaru.territorywars;

import com.google.gson.*;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TerritoryAdapter implements JsonSerializer<Territory>, JsonDeserializer<Territory> {
    private final TerritoryWars plugin;

    public TerritoryAdapter(TerritoryWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public JsonElement serialize(Territory territory, Type type, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("id", territory.getId().toString());
        obj.addProperty("gridX", territory.getGridX());
        obj.addProperty("gridZ", territory.getGridZ());
        obj.addProperty("owner", territory.getOwner() != null ? territory.getOwner().getTag() : null);
        obj.add("coreLocation", context.serialize(territory.getCoreLocation()));
        obj.addProperty("creationCost", territory.getCreationCost());
        obj.addProperty("resistanceMultiplier", territory.getResistanceMultiplier());
        obj.addProperty("protectionMode", territory.getProtectionMode().name());
        obj.addProperty("coreHealth", territory.getCoreHealth());
        obj.addProperty("lastDamageTime", territory.getLastDamageTime());
        obj.addProperty("lastTributePaid", territory.getLastTributePaid());
        obj.addProperty("displayName", territory.getDisplayName());
        obj.addProperty("description", territory.getDescription());
        obj.addProperty("banner", territory.getBanner());
        obj.addProperty("name", territory.getName());

        JsonArray adjacentTerritories = new JsonArray();
        for (UUID adjacentId : territory.getAdjacentTerritories()) {
            adjacentTerritories.add(adjacentId.toString());
        }
        obj.add("adjacentTerritories", adjacentTerritories);

        JsonArray coreBlocks = new JsonArray();
        for (Location loc : territory.getCoreBlocks()) {
            coreBlocks.add(context.serialize(loc));
        }
        obj.add("coreBlocks", coreBlocks);

        return obj;
    }

    @Override
    public Territory deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        int gridX = obj.get("gridX").getAsInt();
        int gridZ = obj.get("gridZ").getAsInt();
        ClanManager clanManager = plugin.getClans().getClanManager();
		String clanTag = obj.get("owner").getAsString();
        Clan owner =  clanManager.getClan(clanTag);
        Location coreLocation = context.deserialize(obj.get("coreLocation"), Location.class);
        double creationCost = obj.get("creationCost").getAsDouble();
        double resistanceMultiplier = obj.get("resistanceMultiplier").getAsDouble();
        String name = obj.get("name").getAsString();

        Territory territory = new Territory(gridX, gridZ, owner, coreLocation, creationCost, resistanceMultiplier, name);

        // Use reflection to set the id field since there's no setter
        try {
            java.lang.reflect.Field idField = Territory.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(territory, UUID.fromString(obj.get("id").getAsString()));
        } catch (Exception e) {
            throw new JsonParseException("Failed to set territory ID: " + e.getMessage());
        }
        territory.setProtectionMode(ProtectionMode.valueOf(obj.get("protectionMode").getAsString()));
        territory.setCoreHealth(obj.get("coreHealth").getAsInt());
        territory.setLastDamageTime(obj.get("lastDamageTime").getAsLong());
        territory.setLastTributePaid(obj.get("lastTributePaid").getAsLong());
        territory.setDisplayName(obj.get("displayName").getAsString());
        territory.setDescription(obj.get("description").getAsString());
        territory.setBanner(obj.get("banner").getAsString());

        Set<UUID> adjacentTerritories = new HashSet<>();
        JsonArray adjacentTerritoriesArray = obj.get("adjacentTerritories").getAsJsonArray();
        for (JsonElement element : adjacentTerritoriesArray) {
            adjacentTerritories.add(UUID.fromString(element.getAsString()));
        }
        territory.setAdjacentTerritories(adjacentTerritories);

        List<Location> coreBlocks = new ArrayList<>();
        JsonArray coreBlocksArray = obj.get("coreBlocks").getAsJsonArray();
        for (JsonElement element : coreBlocksArray) {
            coreBlocks.add(context.deserialize(element, Location.class));
        }
        // No need to manually add core blocks, as they are already part of the Territory object

        return territory;
    }
}