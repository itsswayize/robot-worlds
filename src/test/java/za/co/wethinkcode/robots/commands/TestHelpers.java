package za.co.wethinkcode.robots.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

public final class TestHelpers {
    private TestHelpers() {}

    public static World createWorld(int width, int height) {
        World world = new World(width, height);
        world.setDefaultWorldProperties();
        return world;
    }

    public static Robot launchRobot(World world, Robot robot, String type, String clientId) {
        LaunchCommand command = new LaunchCommand(robot, new String[]{ type }, world);
        final Robot[] launched = new Robot[1];
        world.execute(command, clientId, response -> {
            assertTrue(response.isOKResponse(), "Launch failed: " + response);
            launched[0] = world.getRobots().getFirst();
        });
        return launched[0];
    }

    public static Robot addRobotAt(World world, Robot robot, int x, int y) {
        world.addRobot(robot);
        robot.setPosition(x, y);
        return robot;
    }

    public static void assertLookResponseHasObjects(Response lookResponse) {
        assertTrue(lookResponse.isOKResponse());
        org.json.JSONArray objects = null;
        org.json.JSONObject data = lookResponse.object.optJSONObject("data");
        if (data != null) objects = data.optJSONArray("objects");
        if (objects == null) objects = lookResponse.object.optJSONArray("objects");
        assertNotNull(objects, "Look response must contain 'objects' array (data.objects or top-level): " + lookResponse.object);
    }

    public static void assertFireHit(Response fireResponse, Robot shooter, int shooterInitialShots, Robot target, int expectedTargetShields) {
        assertTrue(fireResponse.isOKResponse());
        assertEquals(shooterInitialShots - 1, shooter.getShots());
        assertEquals(expectedTargetShields, fireResponse.object.getJSONObject("data").getJSONObject("state").getInt("shields"));
        assertEquals(target.getName(), fireResponse.object.getJSONObject("data").getString("robot"));
    }

    public static void assertFireKill(Response fireResponse, Robot shooter, int shooterInitialShots, Robot target) {
        assertTrue(fireResponse.isOKResponse());
        assertEquals(shooterInitialShots - 1, shooter.getShots());
        assertEquals(0, fireResponse.object.getJSONObject("data").getJSONObject("state").getInt("shields"));
        assertEquals("DEAD", fireResponse.object.getJSONObject("data").getJSONObject("state").getString("status"));
        assertEquals(target.getName(), fireResponse.object.getJSONObject("data").getString("robot"));
    }
}
