package za.co.wethinkcode.robots.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.Direction;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import static org.junit.jupiter.api.Assertions.*;

public class VisibilityHandlerTest {

    private World world;
    private Robot robot;

    @BeforeEach
    public void setUp() {
        world = new World(10, 10);
        robot = new Robot("Robot", "tank", 0, 0);
        world.addRobot(robot);
        robot.setPosition(0, 0);
    }

    @Test
    public void testVisibility() {
        VisibilityHandler visibilityHandler = new VisibilityHandler(
                world.getRobots(),
                world.getObstacles(),
                world.getHalfWidth(),
                world.getHalfHeight(),
                world.getVisibility(),
                world
        );
        Response response = visibilityHandler.lookAround(robot);
        org.json.JSONArray objects = null;
        org.json.JSONObject data = response.object.optJSONObject("data");
        if (data != null) objects = data.optJSONArray("objects");
        if (objects == null) objects = response.object.optJSONArray("objects");
        assertNotNull(objects, "Response must contain an 'objects' array (data.objects or top-level): " + response.object);
        assertTrue(objects.length() >= 0);
    }

    @Test
    public void testVisibilityWithObstacle() {
        Obstacle obstacle = new Obstacle(ObstacleType.MOUNTAIN, 1, 0, 1, 1);
        world.addObstacle(obstacle); // Add obstacle to the world
        VisibilityHandler visibilityHandler = new VisibilityHandler(
                world.getRobots(),
                world.getObstacles(),
                world.getHalfWidth(),
                world.getHalfHeight(),
                world.getVisibility(),
                world
        );
        robot.setPosition(3, 0);
        world.displayWorld();
        Response response = visibilityHandler.lookAround(robot);


        org.json.JSONArray objects = null;
        org.json.JSONObject data = response.object.optJSONObject("data");
        if (data != null) objects = data.optJSONArray("objects");
        if (objects == null) objects = response.object.optJSONArray("objects");
        assertNotNull(objects, "Response must contain an 'objects' array (data.objects or top-level): " + response.object);
        System.out.println("Objects array: " + objects);


        boolean obstacleFound = false;
        for (int i = 0; i < objects.length(); i++) {
            org.json.JSONObject jsonObject = objects.getJSONObject(i);
            if ("OBSTACLE".equals(jsonObject.getString("type"))
                    && Direction.CardinalDirection.WEST.name().equals(jsonObject.getString("direction"))
                    && jsonObject.getInt("distance") == 2) { // Changed distance to 2
                obstacleFound = true;
                break;
            }
        }
        assertTrue(obstacleFound, "Obstacle with type 'OBSTACLE', direction 'WEST', and distance 2 not found in objects array: " + objects);
        assertFalse(objects.isEmpty(), "Objects array should not be empty");
    }

    @Test
    public void testVisibilityWithRobot() {
        Robot otherRobot = new Robot("OtherRobot", "tank", 1, 0);
        world.addRobot(otherRobot);
        otherRobot.setPosition(1, 0);
        VisibilityHandler visibilityHandler = new VisibilityHandler(
                world.getRobots(),
                world.getObstacles(),
                world.getHalfWidth(),
                world.getHalfHeight(),
                world.getVisibility(),
                world
        );
        Response response = visibilityHandler.lookAround(robot);


        org.json.JSONArray objects = null;
        org.json.JSONObject data = response.object.optJSONObject("data");
        if (data != null) objects = data.optJSONArray("objects");
        if (objects == null) objects = response.object.optJSONArray("objects");
        assertNotNull(objects, "Response must contain an 'objects' array (data.objects or top-level): " + response.object);
        System.out.println("Objects array: " + objects);


        boolean robotFound = false;
        for (int i = 0; i < objects.length(); i++) {
            org.json.JSONObject jsonObject = objects.getJSONObject(i);
            if ("ROBOT".equals(jsonObject.getString("type"))
                    && Direction.CardinalDirection.EAST.name().equals(jsonObject.getString("direction"))
                    && jsonObject.getInt("distance") == 1) {
                robotFound = true;
                break;
            }
        }
        assertTrue(robotFound, "Robot with type 'ROBOT', direction 'EAST', and distance 1 not found in objects array: " + objects);
        assertFalse(objects.isEmpty(), "Objects array should not be empty");
    }
}