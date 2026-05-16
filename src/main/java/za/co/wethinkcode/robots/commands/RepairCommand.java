package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;

public class RepairCommand extends Command {
    public RepairCommand(Robot robot, String[] arguments) {
        super(robot, arguments);
    }

    @Override
    public String commandName() {
        return "repair";    
    }

    @Override
    public String execute() {
        JSONObject data = new JSONObject();
        JSONObject state = new JSONObject();

        // Example repair logic: restore shields to 5 for sniper, 10 for tank
        if (robot.getMake().equalsIgnoreCase("tank")) {
            robot.setShields(10);
            data.put("message", "Tank repaired to full shields (10).");
        } else if (robot.getMake().equalsIgnoreCase("sniper")) {
            robot.setShields(5);
            data.put("message", "Sniper repaired to full shields (5).");
        } else {
            data.put("message", "Robot repaired.");
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
