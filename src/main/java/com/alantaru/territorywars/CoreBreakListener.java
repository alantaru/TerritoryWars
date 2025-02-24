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

public class CoreBreakListener implements Listener {
    private final TerritoryManager territoryManager;
    private final SimpleClans clans;
    private final TerritoryWars plugin;
    private final Map<String, Long> lastDamageTime;

    public CoreBreakListener(TerritoryManager territoryManager, SimpleClans clans, TerritoryWars plugin) {
        this.territoryManager = territoryManager;
        this.clans = clans;
        this.plugin = plugin;
        this.lastDamageTime = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        // Verifica se o bloco é parte do núcleo
        if (!territory.isBlockPartOfCore(event.getBlock().getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        Clan attackerClan = clans.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());

        // Verifica se o jogador pertence a um clã
        if (attackerClan == null) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessage("attack_adjacent_territories_only"));
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
            player.sendMessage(plugin.getMessage("attack_adjacent_territories_only"));
            return;
        }

        // Check protection mode
        switch (territory.getProtectionMode()) {
            case RAID_HOURS:
                if (!isWithinRaidHours()) {
                    event.setCancelled(true);
                    player.sendMessage(String.format(
                        plugin.getMessage("raid_hours_only"),
                        plugin.getRaidStartTime(),
                        plugin.getRaidEndTime()
                    ));
                    return;
                }
                break;

            case MINIMUM_ONLINE_PLAYERS:
                double requiredPercentage = plugin.getOnlinePercentage() / 100.0;
                int onlineCount = (int) territory.getOwner().getMembers().stream()
                    .map(member -> plugin.getServer().getPlayer(member.getUniqueId()))
                    .filter(p -> p != null && p.isOnline())
                    .count();
                int totalCount = territory.getOwner().getMembers().size();
                
                if (onlineCount < totalCount * requiredPercentage) {
                    event.setCancelled(true);
                    player.sendMessage(String.format(
                        plugin.getMessage("minimum_online_percentage"),
                        (int) (requiredPercentage * 100)
                    ));
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

    private boolean isWithinRaidHours() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        try {
            Date startTime = format.parse(plugin.getRaidStartTime());
            Date endTime = format.parse(plugin.getRaidEndTime());

            Calendar now = Calendar.getInstance();
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();

            start.setTime(startTime);
            end.setTime(endTime);

            start.set(Calendar.YEAR, now.get(Calendar.YEAR));
            start.set(Calendar.MONTH, now.get(Calendar.MONTH));
            start.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            end.set(Calendar.YEAR, now.get(Calendar.YEAR));
            end.set(Calendar.MONTH, now.get(Calendar.MONTH));
            end.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            return now.after(start) && now.before(end);
        } catch (ParseException e) {
            plugin.getLogger().warning(plugin.getMessage("error_parsing_raid_hours") + e.getMessage());
            return false;
        }
    }

    private void handleCoreDestroyed(Territory territory, Clan attackerClan) {
        // Broadcast conquest message
       String message = String.format(
            plugin.getMessage("territory_conquered"),
            territory.getGridX() * 3 * 16,
            territory.getGridZ() * 3 * 16,
            attackerClan.getName()
        );

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

    private void sendMessageToClanMembers(Clan clan, String message) {
        if (clan == null) return;
        clan.getMembers().stream()
            .map(member -> plugin.getServer().getPlayer(member.getUniqueId()))
            .filter(p -> p != null && p.isOnline())
            .forEach(p -> p.sendMessage(message));
    }
}
