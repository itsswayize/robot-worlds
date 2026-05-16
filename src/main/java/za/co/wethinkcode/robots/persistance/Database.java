package za.co.wethinkcode.robots.persistance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection manager.
 * Provides centralized database connection management.
 */
public class Database {
    private static final String DB_URL = "jdbc:sqlite:robot_worlds.db";
    private static Database instance;
    private Connection connection;

    private Database() {
        try {
            this.connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    private void initializeDatabase() {
        try (var stmt = connection.createStatement()) {
            // Create worlds table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS worlds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL
                )
            """);
            // Create obstacles table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS obstacles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    world_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    width INTEGER,
                    height INTEGER,
                    FOREIGN KEY (world_id) REFERENCES worlds(id) ON DELETE CASCADE
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // Log error but don't throw
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}