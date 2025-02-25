package com.alantaru.territorywars;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.entity.Player;

public class TerritoryWarsUtils {

    /**
     * Gets the online player from a clan member.
     * @param clan The clan the member belongs to.
     * @param plugin The TerritoryWars plugin.
     * @param member The clan member.
     * @return The online player, or null if the player is offline or not found.
     */
    public static Player getOnlinePlayer(Clan clan, TerritoryWars plugin, net.sacredlabyrinth.phaed.simpleclans.ClanPlayer member) {
        try {
            Player player = plugin.getServer().getPlayer(member.getUniqueId());
            if (player != null && player.isOnline()) {
                return player;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting online player: " + e.getMessage());
            return null;
        }
        return null;
    }
}
