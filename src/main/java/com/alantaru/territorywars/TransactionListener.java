package com.alantaru.territorywars;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransactionListener implements Listener {

    private final TerritoryWars plugin;
    private final Map<UUID, Double> pendingWithdrawals = new HashMap<>();
    private final long VAULT_TRANSACTION_TIMEOUT = 5; // 5 seconds

    public TransactionListener(TerritoryWars plugin) {
        this.plugin = plugin;
        hookIntoVault();
    }

    private void hookIntoVault() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault Economy provider not found!");
            return;
        }
        Economy realEconomy = rsp.getProvider();

        Economy proxy = (Economy) Proxy.newProxyInstance(Economy.class.getClassLoader(), new Class<?>[]{Economy.class}, new EconomyInvocationHandler(realEconomy));
        plugin.getServer().getServicesManager().unregister(Economy.class, realEconomy);
        plugin.getServer().getServicesManager().register(Economy.class, proxy, plugin, ServicePriority.Highest);

        plugin.getLogger().info("Successfully hooked into Vault Economy.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Clear pending withdrawals for the player
        pendingWithdrawals.remove(event.getPlayer().getUniqueId());
    }

    class EconomyInvocationHandler implements InvocationHandler {

        private final Economy realEconomy;

        public EconomyInvocationHandler(Economy realEconomy) {
            this.realEconomy = realEconomy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();

            if (methodName.equals("withdrawPlayer")) {
                if (args != null && args.length > 1 && args[0] instanceof Player && args[1] instanceof Double) {
                    Player player = (Player) args[0];
                    Double amount = (Double) args[1];
                    recordWithdrawal(player, amount);
                }
            } else if (methodName.equals("depositPlayer")) {
                if (args != null && args.length > 1 && args[0] instanceof Player && args[1] instanceof Double) {
                    Player player = (Player) args[0];
                    Double amount = (Double) args[1];
                    processDeposit(player, amount);
                }
            }

            return method.invoke(realEconomy, args);
        }
    }

    public void recordWithdrawal(Player player, double amount) {
        UUID playerId = player.getUniqueId();
        pendingWithdrawals.put(playerId, amount);

        // Schedule a task to remove the withdrawal after a timeout
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingWithdrawals.remove(playerId);
            }
        }.runTaskLater(plugin, VAULT_TRANSACTION_TIMEOUT * 20L); // Convert seconds to ticks
    }

    public void processDeposit(Player player, double amount) {
        UUID playerId = player.getUniqueId();
        if (pendingWithdrawals.containsKey(playerId)) {
            double withdrawnAmount = pendingWithdrawals.get(playerId);
            if (Math.abs(withdrawnAmount - amount) < 0.01) { // Use a small tolerance for double comparison
                // This is likely a player-to-player transaction
                plugin.getLogger().info("Detected player-to-player transaction: " + playerId + " deposited " + amount);

                // Get dominant clan
                Clan dominantClan = getDominantClan();

                if (dominantClan != null) {
                    double taxPercentage = plugin.getConfig().getDouble("economy.dominant-clan-tax.percentage", 0.05);
                    double taxAmount = amount * taxPercentage;

                    Economy economy = plugin.getEconomy();
                    if (economy.has(player, taxAmount)) {
                        economy.withdrawPlayer(player, taxAmount);
                        // Assuming clan deposit method exists
                        if (dominantClan.getBalance() >= 0) {
                            dominantClan.deposit(taxAmount, null);
                            String taxMessage = String.format("§aTax of §f%.2f§a paid to dominant clan: %s", taxAmount, dominantClan.getName());
                            player.sendMessage(taxMessage);
                        } else {
                            plugin.getLogger().warning("Could not deposit tax to dominant clan.");
                            player.sendMessage("§cCould not pay tax to dominant clan.");
                        }
                    } else {
                        player.sendMessage("§cCould not pay tax to dominant clan due to insufficient funds.");
                    }
                }
            }
            pendingWithdrawals.remove(playerId); // Remove the withdrawal regardless
        }
    }

    private Clan getDominantClan() {
        //TODO: Implement dominant clan logic
        return plugin.getTerritoryManager().getDominantClan();
    }
}
