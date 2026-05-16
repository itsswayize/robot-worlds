package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;

public class FireCommand extends Command {
    public FireCommand(Robot robot, String[] arguments) {
        super(robot, arguments);
    }

    @Override
    public String commandName() {
        return "fire";
    }

    @Override
    public String execute() {
        JSONObject data = new JSONObject();
        JSONObject state = new JSONObject();

        // Example fire logic (customize as needed)
        if (robot.getShots() > 0) {
            robot.setShots(robot.getShots() - 1);
            data.put("message", "You have fired!");
        } else {
            data.put("message", "No shots left!");
        }

        JSONArray pos = new JSONArray();
        pos.put(robot.getX());
        pos.put(robot.getY());
        state.put("position", pos);
        state.put("direction", robot.getDirection().toString());
        state.put("shields", robot.getShields());
        state.put("shots", robot.getShots());
        state.put("status", robot.status.toString());

        return za.co.wethinkcode.robots.server.Response.ok(data, data.getString("message")).withState(state).toJSONString();
    }
}