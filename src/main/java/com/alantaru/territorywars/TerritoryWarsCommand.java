package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
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
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;
        Clan clan = plugin.getClans().getClanManager().getClanByPlayerUniqueId(player.getUniqueId());

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("territorywars.create")) {
                    player.sendMessage("§cVocê não tem permissão para criar territórios!");
                    return true;
                }
                if (clan == null) {
                    player.sendMessage("§cVocê precisa fazer parte de um clã para criar territórios!");
                    return true;
                }
                if (!clan.isLeader(player.getUniqueId()) && 
                    !plugin.getClans().getPermissionsManager().has(player, "simpleclans.leader.can-create")) {
                    player.sendMessage("§cApenas líderes e membros autorizados podem criar territórios!");
                    return true;
                }
                territoryManager.createTerritory(player, player.getLocation());
                displayParticleBorder(player, territoryManager.getTerritoryAt(player.getLocation()));
                break;

            case "abandon":
                if (!player.hasPermission("territorywars.abandon")) {
                    player.sendMessage("§cVocê não tem permissão para abandonar territórios!");
                    return true;
                }
                if (clan == null) {
                    player.sendMessage("§cVocê precisa fazer parte de um clã para abandonar territórios!");
                    return true;
                }
                Territory territoryToAbandon = territoryManager.getTerritoryAt(player.getLocation());
                if (territoryToAbandon == null) {
                    player.sendMessage("§cVocê não está em um território!");
                    return true;
                }
                if (!territoryToAbandon.getOwner().equals(clan)) {
                    player.sendMessage("§cEste território não pertence ao seu clã!");
                    return true;
                }
                territoryManager.removeTerritory(territoryToAbandon.getId(), true);
                player.sendMessage("§aTerritório abandonado com sucesso!");
                break;

            case "info":
                if (!player.hasPermission("territorywars.info")) {
                    player.sendMessage("§cVocê não tem permissão para ver informações de territórios!");
                    return true;
                }
                Territory territory = territoryManager.getTerritoryAt(player.getLocation());
                if (territory == null) {
                    player.sendMessage("§cVocê não está em um território!");
                    return true;
                }
                sendTerritoryInfo(player, territory);
                displayParticleBorder(player, territory);
                break;

            case "movecore":
                if (!player.hasPermission("territorywars.movecore")) {
                    player.sendMessage("§cVocê não tem permissão para mover o núcleo!");
                    return true;
                }
                handleMoveCore(player, clan);
                break;

            case "setname":
                if (!player.hasPermission("territorywars.modify")) {
                    player.sendMessage("§cVocê não tem permissão para modificar territórios!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso correto: /tw setname <nome>");
                    return true;
                }
                handleSetName(player, clan, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                break;

            case "setdesc":
                if (!player.hasPermission("territorywars.modify")) {
                    player.sendMessage("§cVocê não tem permissão para modificar territórios!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso correto: /tw setdesc <descrição>");
                    return true;
                }
                handleSetDescription(player, clan, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                break;

            case "setbanner":
                if (!player.hasPermission("territorywars.modify")) {
                    player.sendMessage("§cVocê não tem permissão para modificar territórios!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso correto: /tw setbanner <url>");
                    return true;
                }
                handleSetBanner(player, clan, args[1]);
                break;

            case "mode":
                if (!player.hasPermission("territorywars.mode")) {
                    player.sendMessage("§cVocê não tem permissão para alterar o modo de proteção!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso correto: /tw mode <modo>");
                    return true;
                }
                handleSetMode(player, clan, args[1].toUpperCase());
                break;

            case "reload":
                if (!player.hasPermission("territorywars.*")) {
                    player.sendMessage("§cVocê não tem permissão para recarregar o plugin!");
                    return true;
                }
                plugin.reloadPlugin();
                player.sendMessage("§aPlugin recarregado com sucesso!");
                break;

            case "rename":
                if (!player.hasPermission("territorywars.rename")) {
                    player.sendMessage("§cVocê não tem permissão para renomear territórios!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUso correto: /tw rename <nome>");
                    return true;
                }
                handleRename(player, clan, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                break;

            case "exibir":
                 if (!player.hasPermission("territorywars.exibir")) {
                    player.sendMessage("§cVocê não tem permissão para exibir a borda do território!");
                    return true;
                }
                Territory territoryExibir = territoryManager.getTerritoryAt(player.getLocation());
                if (territoryExibir == null) {
                    player.sendMessage("§cVocê não está em um território!");
                    return true;
                }
                displayParticleBorder(player, territoryExibir);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void displayParticleBorder(Player player, Territory territory) {
        if (territory == null) {
            return;
        }

        // Implement particle display logic here
        Bukkit.getLogger().info("Displaying particle border for territory: " + territory.getName());

        int minX = territory.getGridX() * plugin.getConfig().getInt("territory.gridSize");
        int minZ = territory.getGridZ() * plugin.getConfig().getInt("territory.gridSize");
        int maxX = minX + plugin.getConfig().getInt("territory.gridSize");
        int maxZ = minZ + plugin.getConfig().getInt("territory.gridSize");
        int y = player.getLocation().getBlockY();
        String particleTypeName = plugin.getConfig().getString("territory.particleType", "BARRIER");
        org.bukkit.Particle particleType = org.bukkit.Particle.valueOf(particleTypeName);

        player.getWorld().spawnParticle(particleType, minX, y, minZ, 1);
        player.getWorld().spawnParticle(particleType, maxX, y, minZ, 1);
        player.getWorld().spawnParticle(particleType, minX, y, maxZ, 1);
        player.getWorld().spawnParticle(particleType, maxX, y, maxZ, 1);
    }

    private void handleRename(Player player, Clan clan, String name) {
        Territory territory = territoryManager.getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§cVocê não está em um território!");
            return;
        }

        if (clan == null || !clan.equals(territory.getOwner())) {
            player.sendMessage("§cEste território não pertence ao seu clã!");
            return;
        }

        territory.setName(name);
        territoryManager.saveTerritories();
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
        }
        player.sendMessage("§aTerritório renomeado para: " + name);
    }

    private void handleMoveCore(Player player, Clan clan) {
        Territory territory = territoryManager.getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§cVocê não está em um território!");
            return;
        }

        if (clan == null || !clan.equals(territory.getOwner())) {
            player.sendMessage("§cEste território não pertence ao seu clã!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId()) && 
            !plugin.getClans().getPermissionsManager().has(player, "simpleclans.leader.can-movecore")) {
            player.sendMessage("§cApenas líderes e membros autorizados podem mover o núcleo!");
            return;
        }

        try {
            territory.setCoreLocation(player.getLocation());
            territory.spawnCoreStructure(plugin);
            player.sendMessage("§aNúcleo movido com sucesso!");
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cLocalização inválida! O núcleo deve estar dentro do território.");
        }
    }

    private void handleSetName(Player player, Clan clan, String name) {
        Territory territory = territoryManager.getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§cVocê não está em um território!");
            return;
        }

        if (clan == null || !clan.equals(territory.getOwner())) {
            player.sendMessage("§cEste território não pertence ao seu clã!");
            return;
        }

        territory.setDisplayName(name);
        territoryManager.saveTerritories();
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
        }
        player.sendMessage("§aNome do território alterado para: " + name);
    }

    private void handleSetDescription(Player player, Clan clan, String description) {
        Territory territory = territoryManager.getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§cVocê não está em um território!");
            return;
        }

        if (clan == null || !clan.equals(territory.getOwner())) {
            player.sendMessage("§cEste território não pertence ao seu clã!");
            return;
        }

        territory.setDescription(description);
        territoryManager.saveTerritories();
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
        }
        player.sendMessage("§aDescrição do território alterada!");
    }

    private void handleSetBanner(Player player, Clan clan, String url) {
        Territory territory = territoryManager.getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§cVocê não está em um território!");
            return;
        }

        if (clan == null || !clan.equals(territory.getOwner())) {
            player.sendMessage("§cEste território não pertence ao seu clã!");
            return;
        }

        territory.setBanner(url);
        territoryManager.saveTerritories();
        if (plugin.getConfig().getBoolean("dynmap.enabled", true)) {
            plugin.updateDynmapTerritory(territory);
            plugin.getDynmapManager().updateAdjacencyLines(territory);
            territory.getAdjacentTerritories().forEach(adjacentId -> {
                Territory adjacent = territoryManager.getTerritories().get(adjacentId);
                if (adjacent != null) {
                    plugin.getDynmapManager().updateAdjacencyLines(adjacent);
                }
            });
        }
        player.sendMessage("§aBandeira do território alterada!");
    }

    private void handleSetMode(Player player, Clan clan, String modeName) {
        Territory territory = territoryManager.getTerritoryAt(player.getLocation());
        if (territory == null) {
            player.sendMessage("§cVocê não está em um território!");
            return;
        }

        if (clan == null || !clan.equals(territory.getOwner())) {
            player.sendMessage("§cEste território não pertence ao seu clã!");
            return;
        }

        try {
            ProtectionMode mode = ProtectionMode.valueOf(modeName);
            territory.setProtectionMode(mode);
            territoryManager.saveTerritories();
            player.sendMessage("§aModo de proteção alterado para: " + mode.name());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cModo de proteção inválido! Use: INFINITE_WAR, RAID_HOURS ou MINIMUM_ONLINE_PLAYERS");
        }
    }

    private void sendTerritoryInfo(Player player, Territory territory) {
        player.sendMessage("§6=== Informações do Território ===");
        player.sendMessage("§eNome: §f" + territory.getDisplayName());
        player.sendMessage("§eDescrição: §f" + territory.getDescription());
        player.sendMessage("§eClã: §f" + territory.getOwner().getName());
        player.sendMessage("§eModo de Proteção: §f" + territory.getProtectionMode().name());
        player.sendMessage("§ePosição: §fX:" + territory.getGridX() + " Z:" + territory.getGridZ());
        player.sendMessage("§eNúcleo: §fX:" + territory.getCoreLocation().getBlockX() + 
                          " Y:" + territory.getCoreLocation().getBlockY() + 
                          " Z:" + territory.getCoreLocation().getBlockZ());
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== TerritoryWars Comandos ===");
        if (player.hasPermission("territorywars.create"))
            player.sendMessage("§e/tw create §f- Cria um território no local atual");
        if (player.hasPermission("territorywars.info"))
            player.sendMessage("§e/tw info §f- Mostra informações do território atual");
        if (player.hasPermission("territorywars.movecore"))
            player.sendMessage("§e/tw movecore §f- Move o núcleo para sua localização");
        if (player.hasPermission("territorywars.modify")) {
            player.sendMessage("§e/tw setname <nome> §f- Define o nome do território");
            player.sendMessage("§e/tw setdesc <desc> §f- Define a descrição");
            player.sendMessage("§e/tw setbanner <url> §f- Define a bandeira");
        }
        if (player.hasPermission("territorywars.mode"))
            player.sendMessage("§e/tw mode <modo> §f- Define o modo de proteção");
        if (player.hasPermission("territorywars.*"))
            player.sendMessage("§e/tw reload §f- Recarrega o plugin");
        if (player.hasPermission("territorywars.rename"))
            player.sendMessage("§e/tw rename <nome> §f- Renomeia o territorio");
        if (player.hasPermission("territorywars.exibir"))
            player.sendMessage("§e/tw exibir §f- Exibe a borda do territorio");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("territorywars.create")) commands.add("create");
            if (sender.hasPermission("territorywars.info")) commands.add("info");
            if (sender.hasPermission("territorywars.movecore")) commands.add("movecore");
            if (sender.hasPermission("territorywars.modify")) {
                commands.add("setname");
                commands.add("setdesc");
                commands.add("setbanner");
            }
            if (sender.hasPermission("territorywars.mode")) commands.add("mode");
            if (sender.hasPermission("territorywars.*")) commands.add("reload");
            if (sender.hasPermission("territorywars.rename")) commands.add("rename");
            if (sender.hasPermission("territorywars.exibir")) commands.add("exibir");

            return commands.stream()
                .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return Arrays.stream(ProtectionMode.values())
                .map(Enum::name)
                .filter(mode -> mode.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
