package za.co.wethinkcode.robots.persistance;

import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.domain.World;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manual JDBC-based DAO for backward compatibility / legacy usage.
 * This is intentionally NOT the EoDSQL-generated implementation.
 */
public class EodWorldDao {
    private static final String DB_URL = "jdbc:sqlite:robot_worlds.db";

    public void saveWorld(String name, World world) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try {
                // Delete existing world if it exists (overwrite)
                deleteWorld(name);

                // Insert world
                WorldData worldData = new WorldData(name, world.getWidth(), world.getHeight());
                int worldId = insertWorld(conn, worldData);

                // Insert obstacles
                for (Obstacle obstacle : world.getObstacles()) {
                    ObstacleData obstacleData = new ObstacleData(
                        worldId,
                        obstacle.type().name(),
                        obstacle.getX(),
                        obstacle.getY(),
                        obstacle.width(),
                        obstacle.height()
                    );
                    insertObstacle(conn, obstacleData);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save world: " + name, e);
        }
    }

    private int insertWorld(Connection conn, WorldData worldData) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO worlds (name, width, height) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, worldData.getName());
            stmt.setInt(2, worldData.getWidth());
            stmt.setInt(3, worldData.getHeight());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                worldData.setId(rs.getInt(1));
                return rs.getInt(1);
            }
            throw new SQLException("Failed to get generated world ID");
        }
    }

    private void insertObstacle(Connection conn, ObstacleData obstacleData) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO obstacles (world_id, type, x, y, width, height) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, obstacleData.getWorldId());
            stmt.setString(2, obstacleData.getType());
            stmt.setInt(3, obstacleData.getX());
            stmt.setInt(4, obstacleData.getY());
            stmt.setInt(5, obstacleData.getWidth());
            stmt.setInt(6, obstacleData.getHeight());
            stmt.executeUpdate();
        }
    }

    public World loadWorld(String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            WorldData worldData = selectWorldByName(conn, name);
            if (worldData == null) {
                throw new RuntimeException("World not found: " + name);
            }

            World world = new World(worldData.getWidth(), worldData.getHeight());

            // Load obstacles
            List<ObstacleData> obstacles = selectObstaclesByWorldId(conn, worldData.getId());
            for (ObstacleData obstacleData : obstacles) {
                ObstacleType type = ObstacleType.valueOf(obstacleData.getType());
                Obstacle obstacle = new Obstacle(type,
                    obstacleData.getX(),
                    obstacleData.getY(),
                    obstacleData.getWidth(),
                    obstacleData.getHeight());
                world.addObstacle(obstacle);
            }

            return world;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load world: " + name, e);
        }
    }

    private WorldData selectWorldByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, width, height FROM worlds WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                WorldData data = new WorldData();
                data.setId(rs.getInt("id"));
                data.setName(rs.getString("name"));
                data.setWidth(rs.getInt("width"));
                data.setHeight(rs.getInt("height"));
                return data;
            }
            return null;
        }
    }

    private List<ObstacleData> selectObstaclesByWorldId(Connection conn, int worldId) throws SQLException {
        List<ObstacleData> obstacles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, world_id, type, x, y, width, height FROM obstacles WHERE world_id = ?")) {
            stmt.setInt(1, worldId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ObstacleData data = new ObstacleData();
                data.setId(rs.getInt("id"));
                data.setWorldId(rs.getInt("world_id"));
                data.setType(rs.getString("type"));
                data.setX(rs.getInt("x"));
                data.setY(rs.getInt("y"));
                data.setWidth(rs.getInt("width"));
                data.setHeight(rs.getInt("height"));
                obstacles.add(data);
            }
        }
        return obstacles;
    }

    public void deleteWorld(String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM worlds WHERE name = ?")) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete world: " + name, e);
        }
    }

    public List<String> listWorlds() {
        List<String> worlds = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM worlds ORDER BY name")) {
            while (rs.next()) {
                worlds.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list worlds", e);
        }
        return worlds;
    }
}