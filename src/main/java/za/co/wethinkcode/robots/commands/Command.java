package za.co.wethinkcode.robots.commands;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.World;

/**
 * Abstract representation of a command sent to robots.
 * Defines interface and common behavior for all commands.
 */
public abstract class Command {
    public Robot robot;
    public String[] arguments;

    public Command(Robot robot, String[] arguments) {
        this.robot = robot;
        this.arguments = arguments;
    }

    public static boolean isValidCommand(String command) {
        return switch (command.toLowerCase()) {
            case "forward", "back", "turn", "look", "state", "launch", "dump", "orientation", "shutdown",
                 "disconnect", "fire", "repair", "reload", "help", "setmine" -> true;
            default -> false;
        };
    }

    public String toJSONString() {
        JSONObject json = new JSONObject();
        json.put("command", commandName().toLowerCase());
        json.put("arguments", arguments);

        if (robot != null) {
            json.put("robot", robot.getName());
        }

        return json.toString();
    }

    public static Command fromJSON(JSONObject json) {
        String command = json.getString("command").toLowerCase();
        if (command.equals("disconnect")) {
            return new DisconnectCommand(); // handle disconnect command separately
        }

        String robotName = json.getString("robot");
        JSONArray jsonArgs = json.getJSONArray("arguments");
        String[] args = new String[jsonArgs.length()];

        for (int i = 0; i < jsonArgs.length(); i++) {
            // Accept any JSON element type (number, string, boolean) by converting to String.
            Object elem = jsonArgs.get(i);
            args[i] = elem == null ? null : elem.toString();
        }

        return switch (command) {
            case "repair" -> new RepairCommand(new Robot(robotName), args);
            case "reload" -> new ReloadCommand(new Robot(robotName), args);
            case "help" -> new HelpCommand(new Robot(robotName), new String[]{});
            case "dump" -> new DumpCommand(new Robot(robotName), new String[]{});
            case "look" -> new LookCommand(new Robot(robotName), new String[]{});
            case "state" -> new StateCommand(new Robot(robotName), new String[]{});
            case "launch" -> new LaunchCommand(new Robot(robotName, args[0]), args, null);
            case "forward" -> new MoveCommand(new Robot(robotName), World.getInstance(), "forward", args);
            case "back" -> new MoveCommand(new Robot(robotName), World.getInstance(), "back", args);
            case "turn" -> new TurnCommand(new Robot(robotName), args);
            case "orientation" -> new OrientationCommand(new Robot(robotName));
            case "off" -> new ShutdownCommand(new Robot(robotName), new String[]{});
            case "fire" -> new FireCommand(new Robot(robotName), args);
            case "setmine" -> new za.co.wethinkcode.robots.commands.SetMineCommand(new Robot(robotName), args);
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        };
    }

    public abstract String execute();

    public static Command fromInput(String input, String robotName) {
        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        return switch (command) {
            case "repair" -> new RepairCommand(new Robot(robotName), args);
            case "reload" -> new ReloadCommand(new Robot(robotName), args);
            case "help" -> new HelpCommand(new Robot(robotName), new String[]{});
            case "dump" -> new DumpCommand(new Robot(robotName), new String[]{});
            case "look" -> new LookCommand(new Robot(robotName), new String[]{});
            case "state" -> new StateCommand(new Robot(robotName), new String[]{});
            case "launch" -> new LaunchCommand(new Robot(robotName, args[0]), args, null);
            case "forward" -> new MoveCommand(new Robot(robotName), World.getInstance(), "forward", args);
            case "back" -> new MoveCommand(new Robot(robotName), World.getInstance(), "back", args);
            case "turn" -> new TurnCommand(new Robot(robotName), args);
            case "orientation" -> new OrientationCommand(new Robot(robotName));
            case "off" -> new ShutdownCommand(new Robot(robotName), new String[]{});
            case "fire" -> new FireCommand(new Robot(robotName), args);
            case "setmine" -> new za.co.wethinkcode.robots.commands.SetMineCommand(new Robot(robotName), args);
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        };
    }

    public abstract String commandName();
}
