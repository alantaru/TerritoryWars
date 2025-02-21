package com.alantaru.territorywars;

import net.milkbowl.vault.economy.Economy;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TributeManager implements Listener {
    private final TerritoryWars plugin;
    private final TerritoryManager territoryManager;
    private final Map<UUID, Long> lastWarningTime;
    private BukkitTask tributeTask;

    public TributeManager(TerritoryWars plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
        this.lastWarningTime = new HashMap<>();
        startTributeCollection();
    }

    private void startTributeCollection() {
        // Convert minutes to ticks (20 ticks = 1 second)
        long interval = plugin.getTaxInterval() * 60L * 20L;
        
        // Cancel existing task if any
        if (tributeTask != null) {
            tributeTask.cancel();
        }
        
        // Start new tribute collection task
        tributeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Territory territory : territoryManager.getTerritories().values()) {
                collectTribute(territory);
            }
        }, interval, interval);
    }

    public void collectTribute(Territory territory) {
        Clan clan = territory.getOwner();
        if (clan == null) return;

        // Calculate tribute amount
        double totalTribute = territory.calculateTribute(plugin);
        int memberCount = clan.getMembers().size();
        if (memberCount == 0) return;

        // Calculate per-member tribute
        double tributePerMember = totalTribute / memberCount;

        // Try to collect from each member
        boolean allPaid = true;
        for (var clanMember : clan.getMembers()) {
            UUID memberId = clanMember.getUniqueId();
            Player player = Bukkit.getPlayer(memberId);
            if (player == null) continue;

            Economy economy = plugin.getEconomy();
            try {
                if (!economy.has(player, tributePerMember)) {
                    allPaid = false;
                    sendWarning(player, territory, tributePerMember);
                    continue;
                }

                economy.withdrawPlayer(player, tributePerMember);
                player.sendMessage(String.format(
                    "§aTributo de §f%.2f §apago para o território em X:%d Z:%d",
                    tributePerMember,
                territory.getGridX() * 3 * 16,
                territory.getGridZ() * 3 * 16
            ));
            } catch(Exception e){
                plugin.getLogger().severe("Error collecting tribute from player " + player.getName() + ": " + e.getMessage());
            }
        }

        if (allPaid) {
            territory.updateTributePayment();
        } else {
            handleUnpaidTribute(territory);
        }
    }

    private void sendWarning(Player player, Territory territory, double amount) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastWarning = lastWarningTime.get(playerId);

        // Only send warning once per minute to avoid spam
        if (lastWarning == null || now - lastWarning > 60000) {
            player.sendMessage(String.format(
                "§c⚠ AVISO: Você precisa de §f%.2f §cpara pagar o tributo do território em X:%d Z:%d",
                amount,
                territory.getGridX() * 3 * 16,
                territory.getGridZ() * 3 * 16
            ));
            lastWarningTime.put(playerId, now);
        }
    }

    private void handleUnpaidTribute(Territory territory) {
        // Get time since last payment
        long now = System.currentTimeMillis();
        long lastPaid = territory.getLastTributePaid();
        long gracePeriod = plugin.getConfig().getLong("economy.tribute.grace-period", 72) * 3600000; // hours to ms

        if (now - lastPaid > gracePeriod) {
            // Territory is lost due to unpaid tribute
            Clan owner = territory.getOwner();
            String message = String.format(
                "§c⚠ O território em X:%d Z:%d foi perdido por falta de pagamento do tributo!",
                territory.getGridX() * 3 * 16,
                territory.getGridZ() * 3 * 16
            );

            // Notify clan members
            owner.getMembers().stream()
                .map(member -> plugin.getServer().getPlayer(member.getUniqueId()))
                .filter(player -> player != null && player.isOnline())
                .forEach(player -> player.sendMessage(message));

            // Remove territory
            territoryManager.removeTerritory(territory.getId(), true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());
        if (clan == null) return;

        // Check if player has unpaid tributes
        for (Territory territory : territoryManager.getTerritories().values()) {
            if (!territory.getOwner().equals(clan)) continue;

            double totalTribute = territory.calculateTribute(plugin);
            double tributePerMember = totalTribute / clan.getMembers().size();

            if (!plugin.getEconomy().has(player, tributePerMember)) {
                sendWarning(player, territory, tributePerMember);
            }
        }
    }

    public void reload() {
        startTributeCollection();
    }

    public void disable() {
        if (tributeTask != null) {
            tributeTask.cancel();
            tributeTask = null;
        }
    }
}
