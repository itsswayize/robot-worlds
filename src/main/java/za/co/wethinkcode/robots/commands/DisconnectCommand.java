// java
package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;

public class DisconnectCommand extends Command {
    public DisconnectCommand() {
        super(null, new String[0]);
    }

    @Override
    public String commandName() {
        return "disconnect";
    }

    @Override
    public String execute() {
        JSONObject data = new JSONObject();
        JSONObject state = new JSONObject();

        String name = (robot != null) ? robot.getName() : "Client";
        data.put("message", name + " disconnected.");

        // If robot is null, return empty/default state
        if (robot != null) {
            JSONArray pos = new JSONArray();
            pos.put(robot.getX());
            pos.put(robot.getY());
            state.put("position", pos);
            state.put("direction", robot.getDirection().toString());
            state.put("shields", robot.getShields());
            state.put("shots", robot.getShots());
            state.put("status", robot.status.toString());
        } else {
            state.put("position", new JSONArray());
            state.put("direction", "");
            state.put("shields", 0);
            state.put("shots", 0);
            state.put("status", "");
        }

        return za.co.wethinkcode.robots.server.Response.ok(data, data.getString("message")).withState(state).toJSONString();
    }
}
