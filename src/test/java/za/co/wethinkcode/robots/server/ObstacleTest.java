package za.co.wethinkcode.robots.server;

import org.junit.Test;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObstacleTest {
    @Test
    public void testInitialization() {
        ObstacleType type = ObstacleType.MOUNTAIN;
        int x = 120;
        int y = 130;
        int width = 150;
        int height = 160;

        Obstacle obstacle = new Obstacle(type, x, y, width, height);

        assertEquals(x, obstacle.x());
        assertEquals(y, obstacle.y());
        assertEquals(width, obstacle.width());
        assertEquals(height, obstacle.height());
        assertEquals(type, obstacle.type());
    }

    @Test
    public void testCoordinates() {
        int x = 120;
        int y = 130;
        int width = 150;
        int height = 160;

        Obstacle obstacle = new Obstacle(ObstacleType.MOUNTAIN, x, y, width, height);

        assertEquals(x + width, obstacle.getMaxX());
        assertEquals(y + height, obstacle.getMaxY());
    }
}
