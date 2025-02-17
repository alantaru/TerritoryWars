package com.alantaru.territorywars;

import org.bukkit.plugin.java.JavaPlugin;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;
    
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void initializeDatabase() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String dbPath = new File(dataFolder, "database.db").getAbsolutePath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        
        try {
            connection = DriverManager.getConnection(jdbcUrl);
            runMigrations(jdbcUrl);
            plugin.getLogger().info("Banco de dados inicializado com sucesso!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao conectar ao banco de dados: " + e.getMessage());
        }
    }
    
    private void runMigrations(String jdbcUrl) {
        FluentConfiguration config = Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true);
        
        Flyway flyway = config.load();
        flyway.migrate();
    }
    
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initializeDatabase();
        }
        return connection;
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao fechar conexão com o banco de dados: " + e.getMessage());
        }
    }
}
