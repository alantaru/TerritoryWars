package com.alantaru.territorywars;

public enum ProtectionMode {
    /**
     * Allows attacks at any time, but only on territories adjacent to attacker's territory
     */
    INFINITE_WAR,

    /**
     * Only allows attacks during configured raid hours
     */
    RAID_HOURS,

    /**
     * Requires a percentage of defender clan members to be online for attacks
     */
    MINIMUM_ONLINE_PLAYERS
}
