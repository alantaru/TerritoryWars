package com.alantaru.territorywars;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the database connection and migrations.
 */
public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;
    
    /**
     * Constructor for the DatabaseManager.
     * @param plugin The plugin instance.
     */
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initializes the database connection and runs migrations.
     */
    public void initializeDatabase() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String dbName = plugin.getConfig().getString("database.name", "database.db");
        String dbPath = new File(dataFolder, dbName).getAbsolutePath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;

        try {
            connection = DriverManager.getConnection(jdbcUrl);
            runMigrations(jdbcUrl);
            plugin.getLogger().info("Database initialized successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error connecting to the database: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().severe("Error initializing the database: " + e.getMessage());
        }
    }

    /**
     * Runs the database migrations using Flyway.
     * @param jdbcUrl The JDBC URL to connect to the database.
     */
    private void runMigrations(String jdbcUrl) {
        FluentConfiguration config = Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true);
        
        Flyway flyway = config.load();
        flyway.migrate();
    }
    
	/**
	 * Gets the database connection.
	 * @return The database connection.
	 * @throws SQLException If an error occurs while getting the connection.
	 */
	public Connection getConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			initializeDatabase(); // Could throw an exception, which will propagate
		}
		return connection;
	}

	/**
	 * Closes the database connection.
	 */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }
}
