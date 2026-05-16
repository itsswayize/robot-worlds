package za.co.wethinkcode.robots.handlers;

import org.json.JSONObject;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.LookCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import java.util.List;

public class LookService {
    private final World world;
    private final VisibilityHandler visibilityHandler;

    public LookService(World world, VisibilityHandler visibilityHandler) {
        this.world = world;
        this.visibilityHandler = visibilityHandler;
    }

    public Response handleLook(LookCommand command) {
        String robotName = command.robot != null ? command.robot.getName() : null;
        if (robotName == null || robotName.isBlank()) {
            List<Robot> robots = world.getRobots();
            if (robots.isEmpty()) {
                Response resp = new Response("ERROR", "No robots available in the world.");
                JSONObject data = new JSONObject();
                data.put("message", "No robots available in the world.");
                resp.object.put("data", data);
                return resp;
            }
            robotName = robots.get(0).getName();
        }

        Robot robot = world.findRobot(robotName);
        if (robot == null) {
            Response resp = new Response("ERROR", "Could not find robot: " + robotName);
            JSONObject data = new JSONObject();
            data.put("message", "Could not find robot: " + robotName);
            resp.object.put("data", data);
            return resp;
        }

        return visibilityHandler.lookAround(robot);
    }
}

