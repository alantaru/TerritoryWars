package com.alantaru.territorywars;

/**
 * Represents the protection mode of a territory.
 */
public enum ProtectionMode {
    /**
     * Territory can only be attacked during raid hours.
     */
    RAID_HOURS,
    
    /**
     * Territory can only be attacked when a minimum percentage of defending clan members are online.
     */
    MINIMUM_ONLINE_PLAYERS,
    
    /**
     * Territory can always be attacked.
     */
    INFINITE_WAR
}
