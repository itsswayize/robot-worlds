package za.co.wethinkcode.robots.handlers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.TurnCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import static org.junit.jupiter.api.Assertions.*;

public class TurnServiceTest {
    private World world;
    private TurnService turnService;
    private Robot robot;

    @BeforeEach
    public void setUp() {
        world = new World(10, 10);
        robot = new Robot("R1", "tank", 0, 0);
        world.addRobot(robot);
        turnService = new TurnService(world);
    }

    @Test
    public void testTurnLeftChangesOrientation() {
        TurnCommand cmd = new TurnCommand(new Robot("R1"), new String[]{"left"});
        Response resp = turnService.handleTurn(cmd);
        assertTrue(resp.isOKResponse());
        JSONObject state = resp.getState();
        assertNotNull(state);
        assertEquals("WEST", state.getString("direction"));
    }

    @Test
    public void testMissingDirectionArgument() {
        TurnCommand cmd = new TurnCommand(new Robot("R1"), new String[]{});
        Response resp = turnService.handleTurn(cmd);
        assertFalse(resp.isOKResponse());
        assertTrue(resp.getMessage().toLowerCase().contains("missing direction"));
    }
}

