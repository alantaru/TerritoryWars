package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TerritoryListener implements Listener {

    private final TerritoryWars plugin;
    private final TerritoryManager territoryManager;
    private final SimpleClans clans;

    public TerritoryListener(TerritoryWars plugin, TerritoryManager territoryManager, SimpleClans clans) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.clans = clans;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        // Only check if the player moved to a different block
        if (to.getBlockX() == from.getBlockX() && to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        Territory fromTerritory = territoryManager.getTerritoryAt(from);
        Territory toTerritory = territoryManager.getTerritoryAt(to);

        Clan playerClan = clans.getClanManager().getClanByPlayerUniqueId(player.getUniqueId());

        // Player moved FROM a territory TO another territory
        if (fromTerritory != null && toTerritory != null) {
            if (fromTerritory == toTerritory) {
                return;
            }
            // Both territories belong to clans
            if (fromTerritory.getOwner() != null && toTerritory.getOwner() != null) {
                // From own territory to enemy territory
                if (fromTerritory.getOwner().equals(playerClan) && !toTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, plugin.getMessage("enemy_territory_title"), "§7" + toTerritory.getDisplayName());
                    return;
                //From enemy territory to own territory
                } else if (!fromTerritory.getOwner().equals(playerClan) && toTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, plugin.getMessage("ally_territory_title"), "§7" + toTerritory.getDisplayName());
                    return;
                //From enemy to another enemy
                } else if (!fromTerritory.getOwner().equals(playerClan) && !toTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, plugin.getMessage("enemy_territory_title"), "§7" + toTerritory.getDisplayName());
                    return;
                }
                //From own to own
                else if (fromTerritory.getOwner().equals(playerClan) && toTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, plugin.getMessage("ally_territory_title"), "§7" + toTerritory.getDisplayName());
                    return;
                }
            }

        }
        // Player moved FROM a territory TO the wilderness
        if (fromTerritory != null && toTerritory == null) {
            //From own territory to wilderness
            if (fromTerritory.getOwner() != null) {
                if (fromTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, plugin.getMessage("wildlands_title"), plugin.getMessage("left_own_territory"));
                    return;
                }
                //From enemy territory to wilderness
                sendTitle(player, plugin.getMessage("wildlands_title"), plugin.getMessage("left_enemy_territory"));
                return;
            }
        }
        // Player moved FROM the wilderness TO a territory
        if (fromTerritory == null && toTerritory != null) {
            if (toTerritory.getOwner() != null) {
                //To own territory
                if (toTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, plugin.getMessage("ally_territory_title"), "§7" + toTerritory.getDisplayName());
                    return;
                }
                //To enemy territory
                sendTitle(player, plugin.getMessage("enemy_territory_title"), "§7" + toTerritory.getDisplayName());
                return;
            }
        }
    }

     private void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
}
