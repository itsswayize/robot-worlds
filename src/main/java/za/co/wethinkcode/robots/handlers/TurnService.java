package za.co.wethinkcode.robots.handlers;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.TurnCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

public class TurnService {
    private final World world;

    public TurnService(World world) {
        this.world = world;
    }

    public Response handleTurn(TurnCommand turnCommand) {
        if (turnCommand.arguments.length == 0) {
            return new Response("ERROR", "Missing direction for turn command.");
        }

        String directionInput = turnCommand.arguments[0].toLowerCase();
        Robot robot = world.findRobot(turnCommand.robot.getName());

        if (robot == null) {
            return new Response("ERROR", "Robot not found: " + turnCommand.robot.getName());
        }

        if (robot.status == Robot.RobotStatus.Reload) {
            return new Response("ERROR", robot.getName() + " is reloading and cannot turn");
        }

        if (robot.status == Robot.RobotStatus.Repair) {
            return new Response("ERROR", robot.getName() + " is repairing and cannot turn");
        }

        Response response = switch (directionInput) {
            case "left" -> {
                robot.turnLeft();
                yield new Response("OK", robot.getName() + " turned left to " + robot.orientation());
            }
            case "right" -> {
                robot.turnRight();
                yield new Response("OK", robot.getName() + " turned right to " + robot.orientation());
            }
            default -> new Response("ERROR", "Invalid direction. Use 'left' or 'right'.");
        };

        world.stateForRobot(robot, response);
        return response;
    }
}

