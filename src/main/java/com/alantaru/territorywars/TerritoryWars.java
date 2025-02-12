package com.alantaru.territorywars;

import org.bukkit.plugin.java.JavaPlugin;

public class TerritoryWars extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("territorywars").setExecutor(new TerritoryWarsCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
