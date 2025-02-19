package com.alantaru.territorywars;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Location;
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
            event.getPlayer().sendMessage(plugin.getMessage("cannot_place_block_type"));
            return;
        }

        // Check player permission
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessage("cannot_build_in_territory"));
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

        if (!canBreak(event.getPlayer(), event.getBlock().getLocation(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessage("cannot_break_blocks_in_territory"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory != null && !canBuild(null, event.getBlock().getLocation(), territory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory != null && !canBuild(null, event.getBlock().getLocation(), territory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        event.blockList().removeIf(block -> {
            Territory territory = territoryManager.getTerritoryAt(block.getLocation());
            return territory != null && !canBuild(null, block.getLocation(), territory);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) return;
        event.blockList().removeIf(block -> {
            Territory territory = territoryManager.getTerritoryAt(block.getLocation());
             return territory != null && !canBuild(null, block.getLocation(), territory);
        });
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

        if (!canUseBuckets(event.getPlayer(), event.getBlock().getLocation(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessage("cannot_use_buckets_in_territory"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getBlock().getLocation());
        if (territory == null) return;

        if (!canUseBuckets(event.getPlayer(), event.getBlock().getLocation(), territory)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getMessage("cannot_use_buckets_in_territory"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.isCancelled()) return;
        Territory territory = territoryManager.getTerritoryAt(event.getEntity().getLocation());
        if (territory == null) return;

        if (event.getRemover() instanceof Player) {
            Player player = (Player) event.getRemover();
            if (!canBuild(player, event.getEntity().getLocation(), territory)) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessage("cannot_break_decorations_in_territory"));
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

        if (damager != null && !canAttack(damager, event.getEntity().getLocation(), territory)) {
            event.setCancelled(true);
            damager.sendMessage(plugin.getMessage("cannot_cause_damage_in_territory"));
        }
    }

    private boolean canBuild(Player player, Location location, Territory territory) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        if (player == null) {
            return set.testState(null, Flags.BUILD);
        }
        Clan playerClan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        Clan territoryOwner = territory.getOwner();
        return playerClan != null && playerClan.equals(territoryOwner) && set.testState(plugin.getWorldGuard().wrapPlayer(player), Flags.BUILD);
    }

    private boolean canBreak(Player player, Location location, Territory territory) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        if (player == null) {
            return set.testState(null, Flags.BLOCK_BREAK);
        }
        Clan playerClan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        Clan territoryOwner = territory.getOwner();
        return playerClan != null && playerClan.equals(territoryOwner) && set.testState(plugin.getWorldGuard().wrapPlayer(player), Flags.BLOCK_BREAK);
    }

    private boolean canUseBuckets(Player player, Location location, Territory territory) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        if (player == null) {
            return set.testState(null, Flags.USE);
        }
        Clan playerClan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        Clan territoryOwner = territory.getOwner();
        return playerClan != null && playerClan.equals(territoryOwner) && set.testState(plugin.getWorldGuard().wrapPlayer(player), Flags.USE);
    }

    private boolean canAttack(Player player, Location location, Territory territory) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        if (player == null) {
            return set.testState(null, Flags.PVP);
        }
        Clan playerClan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        Clan territoryOwner = territory.getOwner();
        return playerClan != null && playerClan.equals(territoryOwner) && set.testState(plugin.getWorldGuard().wrapPlayer(player), Flags.PVP);
    }
}
