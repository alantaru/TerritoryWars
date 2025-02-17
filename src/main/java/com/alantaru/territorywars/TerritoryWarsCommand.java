package com.alantaru.territorywars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TerritoryWarsCommand implements CommandExecutor, TabCompleter {

    private final TerritoryWars plugin;
    private final TerritoryManager territoryManager;

    public TerritoryWarsCommand(TerritoryWars plugin, TerritoryManager territoryManager) {
        this.plugin = plugin;
        this.territoryManager = territoryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Available subcommands: create, delete, list, info, reload, setdisplayname, setdescription");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getTerritoryManager().reload();
            if (plugin.getDynmapManager() != null) {
                plugin.getDynmapManager().reload();
            }
            plugin.getTributeManager().reload();

            sender.sendMessage("TerritoryWars configuration reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setdisplayname")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tw setdisplayname <territoryName> <displayName>");
                return true;
            }

            String territoryName = args[1];
            String displayName = args[2];

            for (int i = 3; i < args.length; i++) {
                displayName += " " + args[i];
            }


            Territory territory = territoryManager.getTerritoryByName(territoryName);

            if (territory == null) {
                sender.sendMessage("§cTerritory not found.");
                return true;
            }

            territory.setDisplayName(displayName);
            territoryManager.save();

            sender.sendMessage("§aDisplay name for territory '" + territoryName + "' set to '" + displayName + "'.");
            return true;

        }

        if (args[0].equalsIgnoreCase("setdescription")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tw setdescription <territoryName> <description>");
                return true;
            }

            String territoryName = args[1];
            String description = args[2];

            for (int i = 3; i < args.length; i++) {
                description += " " + args[i];
            }

            Territory territory = territoryManager.getTerritoryByName(territoryName);
            if (territory == null) {
                sender.sendMessage("§cTerritory not found.");
                return true;
            }

            territory.setDescription(description);
            territoryManager.save();
            sender.sendMessage("§aDescription for territory '" + territoryName + "' set to '" + description + "'.");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            Player player = (Player) sender;
            Territory territory = null;

            if (args.length > 1) {
                try {
                    String name = args[1];
                    territory = territoryManager.getTerritoryByName(name);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cTerritory not found.");
                    return true;
                }
            } else {
                territory = territoryManager.getTerritoryAt(player.getLocation());
            }

            if (territory == null) {
                sender.sendMessage("§cTerritory not found.");
                return true;
            }

            sender.sendMessage("§6--- Territory Info ---");
            sender.sendMessage("§eID: §f" + territory.getName());
            sender.sendMessage("§eClan: §f" + territory.getOwner().getName());
            sender.sendMessage("§eGrid: §f" + territory.getGridX() + ", " + territory.getGridZ());
            sender.sendMessage("§eCore Health: §f" + territory.getCoreHealth());
            sender.sendMessage("§eCreation Cost: §f" + territory.getCreationCost());
            sender.sendMessage("§eResistance Multiplier: §f" + territory.getResistanceMultiplier());
            sender.sendMessage("§eProtection Mode: §f" + territory.getProtectionMode());
            sender.sendMessage("§eLast Damage Time: §f" + territory.getLastDamageTime());
            sender.sendMessage("§eLast Tribute Paid: §f" + territory.getLastTributePaid());
            sender.sendMessage("§eDisplay Name: §f" + territory.getDisplayName());
            sender.sendMessage("§eDescription: §f" + territory.getDescription());
            sender.sendMessage("§eBanner: §f" + territory.getBanner());


            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("territorywars.create")) {
                sender.sendMessage("§cYou do not have permission to create territories.");
                return true;
            }

            if (territoryManager.getTerritoryAt(player.getLocation()) != null) {
                sender.sendMessage("§cA territory already exists at your location.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /tw create <clan> <territoryName>");
                return true;
            }

            String clanName = args[1];
            String territoryName = args[2];

            // Placeholder for clan existence check

            try {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by a player.");
                    return true;
                }
                Player playerSender = (Player) sender;
                Territory territory = territoryManager.createTerritory(playerSender, player.getLocation(), territoryName);
                if(territory != null) {
                    sender.sendMessage("§aTerritory '" + territoryName + "' created successfully for clan '" + clanName + "'.");
                } else {
                    sender.sendMessage("§cTerritory creation failed.");
                }

            } catch (IllegalArgumentException e) {
                    sender.sendMessage("§c" + e.getMessage());
                }

            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("territorywars.delete")) {
                sender.sendMessage("§cYou do not have permission to delete territories.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§cUsage: /tw delete <territoryName>");
                return true;
            }

            String territoryName = args[1];
            Territory territory = territoryManager.getTerritoryByName(territoryName);

            if (territory == null) {
                sender.sendMessage("§cTerritory not found.");
                return true;
            }

            territoryManager.removeTerritory(territory.getId(), true);
            sender.sendMessage("§aTerritory '" + territoryName + "' deleted successfully.");

            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("territorywars.list")) {
                sender.sendMessage("§cYou do not have permission to list territories.");
                return true;
            }

            sender.sendMessage("§6--- List of Territories ---");
            territoryManager.getTerritories().values().forEach(territory -> {
                sender.sendMessage("§e" + territory.getName() + "§f - Owner: §e" + territory.getOwner().getName());
            });

            return true;
        }

        if (args[0].equalsIgnoreCase("capture")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                sender.sendMessage("§cUsage: /tw capture <territoryName>");
                return true;
            }

            String territoryName = args[1];

            Territory territory = territoryManager.getTerritoryByName(territoryName);

            if (territory == null) {
                sender.sendMessage("§cTerritory not found.");
                return true;
            }

            if (!territory.isInside(player.getLocation())) {
                sender.sendMessage("§cYou are not inside the territory.");
                return true;
            }

            // Placeholder for clan check

            // Placeholder for capture process

            sender.sendMessage("§aYou have captured territory: " + territoryName);

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "delete", "list", "info", "reload", "setdisplayname", "setdescription", "capture");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return territoryManager.getTerritories().values().stream()
                .map(Territory::getName)
                .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return territoryManager.getTerritories().values().stream()
                    .map(Territory::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setdisplayname")) {
            return territoryManager.getTerritories().values().stream()
                    .map(Territory::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setdescription")) {
            return territoryManager.getTerritories().values().stream()
                    .map(Territory::getName)
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return new ArrayList<>(); // Placeholder for clan name suggestions
        } else if (args.length == 2 && args[0].equalsIgnoreCase("capture")) {
            return territoryManager.getTerritories().values().stream()
                    .map(Territory::getName)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
