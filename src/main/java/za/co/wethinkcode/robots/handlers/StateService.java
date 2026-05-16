package za.co.wethinkcode.robots.handlers;

import org.json.JSONObject;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.StateCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

public class StateService {
    private final World world;

    public StateService(World world) {
        this.world = world;
    }

    public Response handleState(StateCommand command, String robotName) {
        Robot robot = world.findRobot(command.robot.getName());
        if (robot != null) {
            String message = "\n" +
                    "State for " + robotName + " 🤖:" +
                    "\n" +
                    " 🌎 Position: [" + robot.getX() + "," + robot.getY() + "]" +
                    "\n" +
                    " 🧭 Direction: " + robot.getDirection().getDirection().symbolForDirection() +
                    "\n" +
                    " 🛡️ Shields: " + robot.getShields() +
                    "\n" +
                    " 🔫 Shots: " + robot.getShots() +
                    "\n" +
                    " 📋 Status: " + robot.status.toString().toUpperCase() +
                    "\n";

            Response response = new Response("OK", message);
            world.stateForRobot(robot, response);
            return response;
        } else {
            Response resp = new Response("ERROR", "Could not find robot: " + robotName);
            JSONObject data = new JSONObject();
            data.put("message", "Could not find robot: " + robotName);
            resp.object.put("data", data);
            return resp;
        }
    }
}

