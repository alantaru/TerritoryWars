package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *  Listens for block break events to handle core breaking logic.
 */
public class CoreBreakListener implements Listener {
    private static final String ATTACK_ADJACENT_TERRITORIES_ONLY = "attack_adjacent_territories_only";

    private final TerritoryManager territoryManager;
    private final SimpleClans clans;
    private final TerritoryWars plugin;
    private final Map<String, Long> lastDamageTime;
    private Date raidStartTime;
    private Date raidEndTime;
	private final double onlinePercentage;

    /**
     * Constructor for the CoreBreakListener.
     * @param territoryManager The territory manager.
     * @param clans The SimpleClans plugin.
     * @param plugin The TerritoryWars plugin.
     */
    public CoreBreakListener(TerritoryManager territoryManager, SimpleClans clans, TerritoryWars plugin) {
        this.territoryManager = territoryManager;
        this.clans = clans;
        this.plugin = plugin;
        this.lastDamageTime = new HashMap<>();
        loadRaidHours();
		this.onlinePercentage = plugin.getConfig().getDouble("protection.minimum-players.online-percentage", 25.0);
    }

    /**
     * Loads the raid hours from the plugin configuration.
     */
    private void loadRaidHours() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        try {
            this.raidStartTime = format.parse(plugin.getConfig().getString("protection.raid-hours.start-time"));
            this.raidEndTime = format.parse(plugin.getConfig().getString("protection.raid-hours.end-time"));
        } catch (ParseException e) {
            plugin.getLogger().severe(plugin.getMessage("error_parsing_raid_hours") + e.getMessage());
            this.raidStartTime = null;
            this.raidEndTime = null;
        }
    }

    /**
     * Handles the block break event.
     * @param event The block break event.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        // Check if the block is part of the core
        if (!territory.isBlockPartOfCore(event.getBlock().getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        Clan attackerClan = clans.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());

        // Check if the player belongs to a clan
        if (attackerClan == null) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage(ATTACK_ADJACENT_TERRITORIES_ONLY));
            return;
        }

        // Check if player has permission
        if (!attackerClan.isLeader(player.getUniqueId()) &&
            !plugin.getClans().getPermissionsManager().has(player, "simpleclans.leader.can-attack")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage("leader_needed_to_attack"));
            return;
        }

        // Check if attacking own territory
        if (attackerClan.equals(territory.getOwner())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage("cannot_attack_own_territory"));
            return;
        }

        // Check if territory is adjacent to attacker's territory
        if (!territoryManager.hasAdjacentTerritory(territory, attackerClan)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage(ATTACK_ADJACENT_TERRITORIES_ONLY));
            return;
        }

        // Check protection mode
        switch (territory.getProtectionMode()) {
            case RAID_HOURS:
                if (!isWithinRaidHours()) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessage("raid_hours_only")
                        .replace("%s", plugin.getRaidStartTime())
                        .replace("%s", plugin.getRaidEndTime()));
                    return;
                }
                break;

            case MINIMUM_ONLINE_PLAYERS:
                double requiredPercentage = onlinePercentage / 100.0;
                int onlineCount = (int) territory.getOwner().getMembers().stream()
                    .map(member -> TerritoryWarsUtils.getOnlinePlayer(territory.getOwner(), plugin, member))
                    .filter(p -> p != null)
                    .count();
                int totalCount = territory.getOwner().getMembers().size();

                if (onlineCount < totalCount * requiredPercentage) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getMessage("minimum_online_percentage")
                        .replace("%d%%", String.valueOf((int) (requiredPercentage * 100))));
                    return;
                }
                break;

            case INFINITE_WAR:
                // Always allowed
                break;
        }

        // Check damage interval
        String key = player.getUniqueId() + ":" + territory.getId();
        long now = System.currentTimeMillis();
        Long lastDamage = lastDamageTime.get(key);
        
        if (lastDamage != null && now - lastDamage < plugin.getCoreStructure().getDamageInterval()) {
            event.setCancelled(true);
            return;
        }

        // Apply damage
        event.setCancelled(true); // Cancel vanilla break
        lastDamageTime.put(key, now);
        territory.damageCore(plugin.getCoreStructure().getDamagePerHit());
        territory.setLastDamageTime(now);

        // Broadcast attack alert
        territory.broadcastAttackAlert(plugin);

        // Check if core is destroyed
        if (territory.isCoreDestroyed()) {
            handleCoreDestroyed(territory, attackerClan);
        }
    }

    /**
     * Checks if the current time is within raid hours.
     * @return True if the current time is within raid hours, false otherwise.
     */
    private boolean isWithinRaidHours() {
        if (raidStartTime == null || raidEndTime == null) {
            return true;
        }

        Calendar now = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        start.setTime(raidStartTime);
        end.setTime(raidEndTime);

        start.set(Calendar.YEAR, now.get(Calendar.YEAR));
        start.set(Calendar.MONTH, now.get(Calendar.MONTH));
        start.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        end.set(Calendar.YEAR, now.get(Calendar.YEAR));
        end.set(Calendar.MONTH, now.get(Calendar.MONTH));
        end.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        return now.after(start) && now.before(end);
    }

    /**
     * Handles the core destroyed event.
     * @param territory The territory that was destroyed.
     * @param attackerClan The clan that destroyed the core.
     */
    private void handleCoreDestroyed(Territory territory, Clan attackerClan) {
        // Broadcast conquest message
       String message = plugin.getMessage("territory_conquered")
            .replace("%d", String.valueOf(territory.getGridX() * plugin.getTerritoryManager().getGridSize() * plugin.getTerritoryManager().getBlockSize()))
            .replace("%d", String.valueOf(territory.getGridZ() * plugin.getTerritoryManager().getGridSize() * plugin.getTerritoryManager().getBlockSize()))
            .replace("%s", attackerClan.getName());

        // Notify both clans
        sendMessageToClanMembers(territory.getOwner(), message);
        sendMessageToClanMembers(attackerClan, message);

        // Transfer ownership
        territory.setOwner(attackerClan);
        territory.setCoreHealth(plugin.getCoreStructure().getRequiredHits());

        // Update Dynmap
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
        }

        // Save changes
        territoryManager.saveTerritories();
    }

    /**
     * Sends a message to all online members of a clan.
     * @param clan The clan to send the message to.
     * @param message The message to send.
     */
    private void sendMessageToClanMembers(Clan clan, String message) {
        if (clan == null) return;
        clan.getMembers().stream()
            .map(member -> TerritoryWarsUtils.getOnlinePlayer(clan, plugin, member))
            .filter(p -> p != null)
            .forEach(p -> p.sendMessage(message));
    }
}
