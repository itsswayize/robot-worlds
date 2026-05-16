package za.co.wethinkcode.robots.debug;

import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.World;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.domain.Position;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.Status;

import static org.junit.jupiter.api.Assertions.*;

public class WorldSpawnDebugTest {

    @Test
    void spawnAvoidsObstacles() {
        World w = new World(2, 2);
        w.setWorldProperties(5, 3, 10, 5);

        // Place known obstacle at (1,1)
        Obstacle obs = new Obstacle(ObstacleType.MOUNTAIN, 1, 1, 1, 1);
        assertTrue(w.addObstacle(obs), "Failed to add obstacle for test");

        // Try to find and add multiple robots; none should end up on the obstacle
        for (int i = 1; i <= 8; i++) {
            Robot r = new Robot("Robot" + i, "tank");
            // Use findFreePosition to pick a candidate spawn
            Position pos = w.findFreePosition();
            assertNotNull(pos, "findFreePosition returned null");
            assertFalse(pos.getX() == 1 && pos.getY() == 1, "findFreePosition returned the obstacle position");

            r.setPosition(pos.getX(), pos.getY());
            Status s = w.addRobot(r);
            // If addRobot returns OK, robot shouldn't be on the obstacle
            if (s == Status.OK) {
                assertFalse(r.getX() == 1 && r.getY() == 1, "addRobot placed robot on obstacle");
            }
        }
    }
}

