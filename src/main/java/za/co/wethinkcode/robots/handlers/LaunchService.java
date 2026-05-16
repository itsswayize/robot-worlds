package za.co.wethinkcode.robots.handlers;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.LaunchCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.Status;
import za.co.wethinkcode.robots.domain.World;

import java.util.HashMap;
import java.util.Map;

public class LaunchService {
    private final World world;

    public LaunchService(World world) {
        this.world = world;
    }

    public Response handleLaunch(LaunchCommand command, String clientId, Map<String, HashMap<String, String>> clientRobots) {
        String robotName = command.robot.getName();
        clientRobots.putIfAbsent(clientId, new HashMap<>());

        // No per-client limit: allow a client to launch as many robots as the world has space for.
        // We still track ownership in clientRobots for cleanup when the client disconnects.

        Status status = world.addRobot(command.robot);
        if (status == Status.OK) {
            clientRobots.get(clientId).put(robotName, command.robot.getMake());
        }

        Response response = buildLaunchResponse(status, command.robot);
        if (status == Status.OK) addLaunchData(response, command.robot);
        world.stateForRobot(command.robot, response);
        return response;
    }

    private Response buildLaunchResponse(Status status, Robot robot) {
        return switch (status) {
            case HitObstaclePIT -> new Response("ERROR", robot.getName() + " fell into a pit and died.");
            case OK -> new Response("OK", "Launched " + robot.getName() + " into the world");
            case ExistingName -> new Response("ERROR", "Robot with the same name already exists");
            case OutOfBounds -> new Response("ERROR", "Failed to launch " + robot.getName() + " because it crashed outside of the world");
            case HitObstacle -> new Response("ERROR", "Failed to launch " + robot.getName() + " because it hit an obstacle");
            case HitObstacleMINE -> new Response("ERROR", robot.getName() + " hit a mine and died.");
            case NoSpace -> new Response("ERROR", "No more space in this world");
        };
    }

    private void addLaunchData(Response response, Robot robot) {
        JSONObject data = new JSONObject();
        data.put("position", new JSONArray().put(robot.getX()).put(robot.getY()));
        data.put("visibility", this.world.getVisibility());
        data.put("reload", this.world.getReloadTime());
        data.put("repair", this.world.getShieldRepairTime());
        data.put("shields", this.world.getMaxShieldStrength());
        response.object.put("data", data);
    }
}
