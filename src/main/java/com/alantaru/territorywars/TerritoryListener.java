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

    private final TerritoryManager territoryManager;
    private final SimpleClans clans;

    public TerritoryListener(TerritoryManager territoryManager, SimpleClans clans) {
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
            if (fromTerritory != toTerritory) {
                // Both territories belong to clans
                if(fromTerritory.getOwner() != null && toTerritory.getOwner() != null){
                    //From own territory to enemy territory
                    if (fromTerritory.getOwner().equals(playerClan) && !toTerritory.getOwner().equals(playerClan)) {
                        sendTitle(player, "§cTerritório Inimigo", "§7" + toTerritory.getDisplayName());
                    //From enemy territory to own territory
                    } else if (!fromTerritory.getOwner().equals(playerClan) && toTerritory.getOwner().equals(playerClan)) {
                        sendTitle(player, "§aTerritório Aliado", "§7" + toTerritory.getDisplayName());
                    }
                    //From enemy to another enemy
                    else if (!fromTerritory.getOwner().equals(playerClan) && !toTerritory.getOwner().equals(playerClan)){
                        sendTitle(player, "§cTerritório Inimigo", "§7" + toTerritory.getDisplayName());
                    }
                    //From own to own
                    else if (fromTerritory.getOwner().equals(playerClan) && toTerritory.getOwner().equals(playerClan)){
                        sendTitle(player, "§aTerritório Aliado", "§7" + toTerritory.getDisplayName());
                    }
                }
            }
        }
        // Player moved FROM a territory TO the wilderness
        else if (fromTerritory != null && toTerritory == null) {
            //From own territory to wilderness
            if(fromTerritory.getOwner() != null){
                if (fromTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, "§7Terras Selvagens", "§7Você deixou seu território");
                }
                //From enemy territory to wilderness
                else {
                    sendTitle(player, "§7Terras Selvagens", "§7Você deixou um território inimigo");
                }
            }
        }
        // Player moved FROM the wilderness TO a territory
        else if (fromTerritory == null && toTerritory != null) {
            if(toTerritory.getOwner() != null){
                //To own territory
                if (toTerritory.getOwner().equals(playerClan)) {
                    sendTitle(player, "§aTerritório Aliado", "§7" + toTerritory.getDisplayName());
                }
                //To enemy territory
                else {
                    sendTitle(player, "§cTerritório Inimigo", "§7" + toTerritory.getDisplayName());
                }
            }
        }
    }

    private void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
}
