package za.co.wethinkcode.robots.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.ShutdownCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class ShutdownServiceTest {
    private World world;
    private ShutdownService shutdownService;

    @BeforeEach
    public void setUp() {
        world = new World(10, 10);
        shutdownService = new ShutdownService(world, new HashMap<>());
    }

    @Test
    public void testShutdownRemovesRobot() {
        Robot r = new Robot("R1", "tank", 0, 0);
        world.addRobot(r);
        ShutdownCommand cmd = new ShutdownCommand(r, new String[]{});
        Response resp = shutdownService.handleShutdown(cmd);
        assertTrue(resp.isOKResponse());
        assertNull(world.findRobot("R1"));
    }
}

