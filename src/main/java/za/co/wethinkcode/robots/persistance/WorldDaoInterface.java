package za.co.wethinkcode.robots.persistance;

import net.lemnik.eodsql.BaseQuery;
import net.lemnik.eodsql.Select;
import net.lemnik.eodsql.Update;
import za.co.wethinkcode.robots.domain.World;

import java.util.List;

/**
 * EoDSQL-style interface for World data access operations.
 * Implementations are generated at runtime via QueryTool.getQuery(connection, WorldDaoInterface.class)
 */
public interface WorldDaoInterface extends BaseQuery {

    // 1. Tables creation (if not exists)
    @Update("CREATE TABLE IF NOT EXISTS worlds (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, width INTEGER NOT NULL, height INTEGER NOT NULL)")
    void createWorldTable();

    @Update("CREATE TABLE IF NOT EXISTS obstacles (id INTEGER PRIMARY KEY AUTOINCREMENT, world_id INTEGER, type TEXT, x INTEGER, y INTEGER, width INTEGER, height INTEGER, FOREIGN KEY(world_id) REFERENCES worlds(id))")
    void createObstacleTable();

    // 2. Saving a World (basic world meta)
    @Update("INSERT INTO worlds (name, width, height) VALUES (?{1}, ?{2}, ?{3})")
    void saveWorldMeta(String name, int width, int height);

    @Select("SELECT id FROM worlds WHERE name = ?{1}")
    Integer getWorldId(String name);

    // 3. Saving Obstacles
    @Update("INSERT INTO obstacles (world_id, type, x, y, width, height) VALUES (?{1}, ?{2}, ?{3}, ?{4}, ?{5}, ?{6})")
    void saveObstacle(int worldId, String type, int x, int y, Integer width, Integer height);

    // 4. Loading a World
    @Select("SELECT id, name, width, height FROM worlds WHERE name = ?{1}")
    WorldDO getWorldByName(String name);

    @Select("SELECT type, x, y, width, height FROM obstacles WHERE world_id = (SELECT id FROM worlds WHERE name = ?{1})")
    List<ObstacleDo> getObstacles(String worldName);

    // 5. Utilities
    @Update("DELETE FROM worlds WHERE name = ?{1}")
    void deleteWorld(String name);

    @Select("SELECT name FROM worlds ORDER BY name")
    List<String> listWorlds();
}