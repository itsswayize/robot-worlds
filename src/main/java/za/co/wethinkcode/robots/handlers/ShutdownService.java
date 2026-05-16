package za.co.wethinkcode.robots.handlers;

import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ShutdownService {
    private final World world;
    private final Map<String, HashMap<String, String>> clientRobots;

    public ShutdownService(World world, Map<String, HashMap<String, String>> clientRobots) {
        this.world = world;
        this.clientRobots = clientRobots;
    }

    public Response handleShutdown(za.co.wethinkcode.robots.commands.ShutdownCommand command) {
        String robotName = command.robot.getName();
        // Remove robot from any client's ownership map to keep clientRobots consistent
        for (Iterator<Map.Entry<String, HashMap<String, String>>> it = clientRobots.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, HashMap<String, String>> entry = it.next();
            HashMap<String, String> owned = entry.getValue();
            if (owned != null) {
                owned.remove(robotName);
                if (owned.isEmpty()) {
                    it.remove();
                }
            }
        }
        return world.removeRobot(robotName);
    }
}

