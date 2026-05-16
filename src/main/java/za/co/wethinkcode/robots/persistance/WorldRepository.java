package za.co.wethinkcode.robots.persistance;

import net.lemnik.eodsql.QueryTool;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.domain.World;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Repository class that prefers EoDSQL-style DAO for world persistence but
 * falls back to the legacy JDBC `EodWorldDao` when EoDSQL is unavailable.
 */
public class WorldRepository implements AutoCloseable {
    private final WorldDaoInterface dao; // may be null if EoDSQL not available
    private final EodWorldDao legacyDao; // fallback
    private final Connection connection;
    private final boolean usingLegacy;

    public WorldRepository() {
        WorldDaoInterface tmpDao = null;
        EodWorldDao tmpLegacy = null;
        Connection tmpConnection = null;
        boolean legacy = false;
        try {
            tmpConnection = DriverManager.getConnection("jdbc:sqlite:robot_worlds.db");
            tmpDao = QueryTool.getQuery(tmpConnection, WorldDaoInterface.class);
            if (tmpDao != null) {
                tmpDao.createWorldTable();
                tmpDao.createObstacleTable();
            } else {
                tmpLegacy = new EodWorldDao();
                legacy = true;
            }
        } catch (SQLException e) {
            tmpLegacy = new EodWorldDao();
            legacy = true;
        }
        this.dao = tmpDao;
        this.legacyDao = tmpLegacy;
        this.connection = tmpConnection;
        this.usingLegacy = legacy || (this.dao == null);
    }

    public void saveWorld(String name, World world) {
        if (usingLegacy) {
            legacyDao.saveWorld(name, world);
            return;
        }
        // EoDSQL path
        dao.deleteWorld(name);
        dao.saveWorldMeta(name, world.getWidth(), world.getHeight());
        Integer id = dao.getWorldId(name);
        if (id == null) throw new RuntimeException("Failed to get world id after insert");
        for (Obstacle obstacle : world.getObstacles()) {
            dao.saveObstacle(id, obstacle.type().name(), obstacle.getX(), obstacle.getY(), obstacle.width(), obstacle.height());
        }
    }

    public World loadWorld(String name) {
        if (usingLegacy) {
            return legacyDao.loadWorld(name);
        }
        WorldDO wd = dao.getWorldByName(name);
        if (wd == null) return null;

        World world = new World(wd.width, wd.height);

        List<ObstacleDo> obstacles = dao.getObstacles(name);
        if (obstacles != null) {
            for (ObstacleDo od : obstacles) {
                ObstacleType type = ObstacleType.valueOf(od.type);
                int w = od.width != null ? od.width : 1;
                int h = od.height != null ? od.height : 1;
                za.co.wethinkcode.robots.domain.Obstacle obstacle = new za.co.wethinkcode.robots.domain.Obstacle(type, od.x, od.y, w, h);
                world.addObstacle(obstacle);
            }
        }

        return world;
    }

    public void deleteWorld(String name) {
        if (usingLegacy) {
            legacyDao.deleteWorld(name);
            return;
        }
        dao.deleteWorld(name);
    }

    public List<String> listWorlds() {
        if (usingLegacy) return legacyDao.listWorlds();
        return dao.listWorlds();
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {
        }
    }
}