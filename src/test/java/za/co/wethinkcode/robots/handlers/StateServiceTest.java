package za.co.wethinkcode.robots.handlers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.StateCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import static org.junit.jupiter.api.Assertions.*;

public class StateServiceTest {
    private World world;
    private StateService stateService;
    private Robot robot;

    @BeforeEach
    public void setUp() {
        world = new World(10, 10);
        robot = new Robot("R1", "tank", 0, 0);
        world.addRobot(robot);
        stateService = new StateService(world);
    }

    @Test
    public void testHandleStateReturnsOKAndState() {
        StateCommand cmd = new StateCommand(robot, new String[]{});
        Response resp = stateService.handleState(cmd, robot.getName());
        assertTrue(resp.isOKResponse());
        JSONObject state = resp.getState();
        assertNotNull(state);
        assertTrue(state.has("position"));
        assertTrue(state.has("direction"));
    }
}

