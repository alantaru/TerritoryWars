package com.alantaru.territorywars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("command_only_by_player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("available_subcommands"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!player.hasPermission("territorywars.reload")) {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                    return true;
                }
                plugin.reloadConfig();
                territoryManager.reload();
                if (plugin.getDynmapManager() != null) {
                    plugin.getDynmapManager().reload();
                }
                plugin.getTributeManager().reload();
                sender.sendMessage(plugin.getMessage("territorywars_reloaded"));
                break;

            case "setdisplayname":
                if (!player.hasPermission("territorywars.setdisplayname")) {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getMessage("setdisplayname_usage"));
                    return true;
                }
                Territory territoryToRename = territoryManager.getTerritoryAt(player.getLocation());
                if (territoryToRename == null){
                    sender.sendMessage(plugin.getMessage("territory_not_found"));
                    return true;
                }
                String newDisplayName = String.join(" ", List.of(args).subList(1, args.length));
                territoryToRename.setDisplayName(newDisplayName);
                territoryManager.save();
                sender.sendMessage(plugin.getMessage("setdisplayname_success").replace("{displayName}", newDisplayName));
                break;

            case "setdescription":
                if (!player.hasPermission("territorywars.setdescription")) {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getMessage("setdescription_usage"));
                    return true;
                }
                Territory territoryToDescribe = territoryManager.getTerritoryAt(player.getLocation());
                if (territoryToDescribe == null){
                    sender.sendMessage(plugin.getMessage("territory_not_found"));
                    return true;
                }
                String newDescription = String.join(" ", List.of(args).subList(1, args.length));
                territoryToDescribe.setDescription(newDescription);
                territoryManager.save();
                sender.sendMessage(plugin.getMessage("setdescription_success").replace("{description}", newDescription));
                break;

            case "create":
                if (!player.hasPermission("territorywars.create")) {
                    sender.sendMessage(plugin.getMessage("no_permission_create"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessage("create_usage"));
                    return true;
                }
                String territoryName = args[1];

                if (territoryManager.getTerritoryAt(player.getLocation()) != null) {
                    sender.sendMessage(plugin.getMessage("territory_exists"));
                    return true;
                }

                Territory createdTerritory = territoryManager.createTerritory(player, player.getLocation(), territoryName);
                if (createdTerritory != null) {
                    sender.sendMessage(plugin.getMessage("territory_created").replace("{territoryName}", territoryName));
                } else {
                    sender.sendMessage(plugin.getMessage("territory_creation_failed"));
                }
                break;

            case "delete":
                if (!player.hasPermission("territorywars.delete")) {
                    sender.sendMessage(plugin.getMessage("no_permission_delete"));
                    return true;
                }

                Territory territoryToDelete = territoryManager.getTerritoryAt(player.getLocation());
                if (territoryToDelete == null) {
                    sender.sendMessage(plugin.getMessage("territory_not_found"));
                    return true;
                }

                territoryManager.removeTerritory(territoryToDelete.getId(), true);
                sender.sendMessage(plugin.getMessage("territory_deleted"));
                break;

            case "list":
                if (!player.hasPermission("territorywars.list")) {
                    sender.sendMessage(plugin.getMessage("no_permission_list"));
                    return true;
                }
                sender.sendMessage(plugin.getMessage("list_territories"));
                territoryManager.getTerritories().values().forEach(territory ->
                        sender.sendMessage("§e" + territory.getDisplayName() + "§f - Owner: §e" + territory.getOwner().getName()));
                break;

            case "info":
                if (!player.hasPermission("territorywars.info")) {
                    sender.sendMessage(plugin.getMessage("no_permission"));
                    return true;
                }
                Territory territory = territoryManager.getTerritoryAt(player.getLocation());
                if (territory == null) {
                    sender.sendMessage(plugin.getMessage("territory_not_found"));
                    return true;
                }
                sender.sendMessage(plugin.getMessage("territory_info"));
                sender.sendMessage("§eID: §f" + territory.getId());
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
                break;

            case "capture":
                if (!player.hasPermission("territorywars.capture")) {
                    sender.sendMessage(plugin.getMessage("no_permission_capture"));
                    return true;
                }

                Territory territoryToCapture = territoryManager.getTerritoryAt(player.getLocation());

                if (territoryToCapture == null) {
                    sender.sendMessage(plugin.getMessage("territory_not_found"));
                    return true;
                }

                if (!territoryToCapture.isInside(player.getLocation())) {
                    sender.sendMessage("§cYou are not inside the territory.");
                    return true;
                }

                // TODO: Implement capture logic

                sender.sendMessage("§cCapture functionality not yet implemented.");
                return true;
            
            default:
                sender.sendMessage(plugin.getMessage("available_subcommands"));
                return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("create", "delete", "list", "info", "reload", "setdisplayname", "setdescription", "capture"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("setdisplayname") || args[0].equalsIgnoreCase("setdescription") || args[0].equalsIgnoreCase("capture"))) {
            completions.addAll(territoryManager.getTerritories().values().stream()
                    .map(Territory::getDisplayName)
                    .collect(Collectors.toList()));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
