package za.co.wethinkcode.robots.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.LaunchCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LaunchServiceTest {
    private World world;
    private LaunchService launchService;
    private Map<String, HashMap<String, String>> clientMap;

    @BeforeEach
    public void setUp() {
        world = new World(10, 10);
        launchService = new LaunchService(world);
        clientMap = new HashMap<>();
    }

    @Test
    public void testSuccessfulLaunchRegistersClient() {
        Robot r1 = new Robot("R1", "tank");
        LaunchCommand cmd = new LaunchCommand(r1, new String[]{"tank"}, world);

        Response resp = launchService.handleLaunch(cmd, "client1", clientMap);
        assertTrue(resp.isOKResponse(), "Expected OK response for successful launch: " + resp.object);
        assertTrue(clientMap.containsKey("client1"));
        assertTrue(clientMap.get("client1").containsKey("R1"));
    }

    @Test
    public void testClientLaunchLimit() {
        // Launch two robots for the same client
        Robot r1 = new Robot("R1", "tank");
        Robot r2 = new Robot("R2", "tank");
        Robot r3 = new Robot("R3", "tank");

        Response r1Resp = launchService.handleLaunch(new LaunchCommand(r1, new String[]{"tank"}, world), "c", clientMap);
        Response r2Resp = launchService.handleLaunch(new LaunchCommand(r2, new String[]{"tank"}, world), "c", clientMap);
        Response r3Resp = launchService.handleLaunch(new LaunchCommand(r3, new String[]{"tank"}, world), "c", clientMap);

        assertTrue(r1Resp.isOKResponse());
        assertTrue(r2Resp.isOKResponse());
    }
}

