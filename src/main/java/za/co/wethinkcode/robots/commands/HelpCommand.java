package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;

public class HelpCommand extends Command {
    public HelpCommand(Robot robot, String[] arguments) {
        super(robot, arguments);
    }

    @Override
    public String commandName() {
        return "help";
    }

    @Override
    public String execute() {
        JSONObject data = new JSONObject();
        JSONObject state = new JSONObject();

        data.put("message", "Available commands: forward, back, turn, state, fire, help, ...");

        JSONArray pos = new JSONArray();
        pos.put(robot.getX());
        pos.put(robot.getY());
        state.put("position", pos);
        state.put("direction", robot.getDirection().toString());
        state.put("shields", robot.getShields());
        state.put("shots", robot.getShots());
        state.put("status", robot.status.toString());

        return za.co.wethinkcode.robots.server.Response.ok(data, "Help info").withState(state).toJSONString();
    }
}
