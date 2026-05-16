package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;

public class DumpCommand extends Command {
    public DumpCommand(Robot robot, String[] arguments) {
        super(robot, arguments);
    }

    @Override
    public String commandName() {
        return "dump";
    }

    @Override
    public String execute() {
        JSONObject data = new JSONObject();
        JSONObject state = new JSONObject();

        // You can add more robot info here if needed
        data.put("message", "Robot info dumped for " + robot.getName());

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