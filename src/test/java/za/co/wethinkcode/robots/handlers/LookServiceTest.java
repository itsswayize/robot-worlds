package za.co.wethinkcode.robots.handlers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.LookCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import static org.junit.jupiter.api.Assertions.*;

public class LookServiceTest {
    private World world;
    private VisibilityHandler visibilityHandler;
    private LookService lookService;
    private Robot robot;

    @BeforeEach
    public void setUp() {
        world = new World(10, 10);
        robot = new Robot("R1", "tank", 0, 0);
        world.addRobot(robot);
        visibilityHandler = new VisibilityHandler(world.getRobots(), world.getObstacles(), world.getHalfWidth(), world.getHalfHeight(), world.getVisibility(), world);
        lookService = new LookService(world, visibilityHandler);
    }

    @Test
    public void testLookReturnsObjectsArray() {
        LookCommand cmd = new LookCommand(robot, new String[]{});
        Response resp = lookService.handleLook(cmd);
        JSONObject data = resp.object.optJSONObject("data");
        JSONArray objects = null;
        if (data != null) objects = data.optJSONArray("objects");
        if (objects == null) objects = resp.object.optJSONArray("objects");
        assertNotNull(objects, "Look response must contain an objects array");
    }
}

