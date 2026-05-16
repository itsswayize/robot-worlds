package za.co.wethinkcode.robots.commands;

import org.json.JSONObject;
import za.co.wethinkcode.robots.domain.Robot;

public class SetMineCommand extends Command {
    public SetMineCommand(Robot robot, String[] arguments) {
        super(robot, arguments);
    }

    @Override
    public String commandName() {
        return "setmine";
    }

    @Override
    public String execute() {
        // Actual handling is delegated to MineService via CommandHandler.
        JSONObject data = new JSONObject();
        data.put("message", "SetMine command delegated to server handler.");
        return za.co.wethinkcode.robots.server.Response.ok(data, "Delegated").toJSONString();
    }
}

