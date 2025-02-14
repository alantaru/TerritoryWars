package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import java.util.List;

public class TerritoryProtectionListener implements Listener {
    private final TerritoryWars plugin;
    private final TerritoryManager territoryManager;
    private final List<String> blockedBlocks;

    public TerritoryProtectionListener(TerritoryWars plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.blockedBlocks = plugin.getConfig().getStringList("territory.blocked-blocks");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        // Check blocked blocks
        if (blockedBlocks.contains(event.getBlock().getType().name())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cEste tipo de bloco não pode ser colocado em territórios!");
            return;
        }

        // Check player permission
        if (!hasPermission(event.getPlayer(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode construir neste território!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        // Allow core breaking to be handled by CoreBreakListener
        if (territory.isBlockPartOfCore(event.getBlock().getLocation())) {
            return;
        }

        if (!hasPermission(event.getPlayer(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode quebrar blocos neste território!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        event.blockList().removeIf(block -> 
            territoryManager.getTerritoryAt(block.getLocation()) != null
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        event.blockList().removeIf(block -> 
            territoryManager.getTerritoryAt(block.getLocation()) != null
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.isCancelled()) return;
        for (Block block : event.getBlocks()) {
            Territory fromTerritory = territoryManager.getTerritoryAt(block.getLocation());
            Territory toTerritory = territoryManager.getTerritoryAt(
                block.getRelative(event.getDirection()).getLocation()
            );
            
            if (fromTerritory != toTerritory) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.isCancelled()) return;
        for (Block block : event.getBlocks()) {
            Territory fromTerritory = territoryManager.getTerritoryAt(block.getLocation());
            Territory toTerritory = territoryManager.getTerritoryAt(
                block.getRelative(event.getDirection()).getLocation()
            );
            
            if (fromTerritory != toTerritory) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        if (!hasPermission(event.getPlayer(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode usar baldes neste território!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        if (!hasPermission(event.getPlayer(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode usar baldes neste território!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getEntity().getLocation());
        if (territory == null) return;

        if (event.getRemover() instanceof Player) {
            Player player = (Player) event.getRemover();
            if (!hasPermission(player, territory)) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não pode quebrar decorações neste território!");
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getEntity().getLocation());
        if (territory == null) return;

        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
            }
        }

        if (damager != null && !hasPermission(damager, territory)) {
            event.setCancelled(true);
            damager.sendMessage("§cVocê não pode causar dano neste território!");
        }
    }

    private boolean hasPermission(Player player, Territory territory) {
        Clan playerClan = plugin.getClans().getClanManager()
            .getClanByPlayerUniqueId(player.getUniqueId());
        return playerClan != null && playerClan.equals(territory.getOwner());
    }
}
