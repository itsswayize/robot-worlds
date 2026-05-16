package za.co.wethinkcode.robots.handlers;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.Position;
import za.co.wethinkcode.robots.commands.MoveCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.Status;
import za.co.wethinkcode.robots.domain.World;

/**
 * MovementService extracted from CommandHandler. Responsible for interpreting MoveCommand
 * and performing world movements. Keeps behavior identical to previous implementation,
 * except OutOfBounds is now treated as a blocking edge (does not kill the robot) and
 * returns a friendly OK message describing the edge (matching reference server behavior).
 */
public class MovementService {
    private final World world;

    public MovementService(World world) {
        this.world = world;
    }

    // Public entry used by CommandHandler
    public Response handleMove(MoveCommand command) {
        if (command == null || command.robot == null) {
            Response resp = new Response("ERROR", "Missing robot for move");
            JSONObject data = new JSONObject();
            data.put("message", "Missing robot for move");
            resp.object.put("data", data);
            return resp;
        }

        String robotName = command.robot.getName();
        Robot robot = world.findRobot(robotName);
        if (robot == null) {
            Response resp = new Response("ERROR", "Could not find robot: " + robotName);
            JSONObject data = new JSONObject();
            data.put("message", "Could not find robot: " + robotName);
            resp.object.put("data", data);
            return resp;
        }

        // Prevent movement while placing a mine
        if (robot.isPlacingMine()) {
            JSONObject data = new JSONObject();
            data.put("message", robot.getName() + " is currently placing a mine and cannot move.");
            Response resp = new Response("ERROR", robot.getName() + " is currently placing a mine and cannot move.");
            resp.object.put("data", data);
            return resp;
        }

        // Validate arguments: MoveCommand requires exactly one numeric argument
        if (command.arguments == null || command.arguments.length != 1) {
            JSONObject data = new JSONObject();
            data.put("message", "Invalid move command format. Use '<steps>'.");
            Response resp = new Response("ERROR", "Invalid move command format.");
            resp.object.put("data", data);
            // do NOT attach state for a malformed command (keeps old behaviour)
            return resp;
        }

        int steps;
        try {
            steps = Integer.parseInt(command.arguments[0]);
        } catch (NumberFormatException e) {
            JSONObject data = new JSONObject();
            data.put("message", "Steps must be a number.");
            Response resp = new Response("ERROR", "Steps must be a number.");
            resp.object.put("data", data);
            // do NOT attach state for parse errors
            return resp;
        }

        // Compute delta per step based on robot orientation using a small Delta value object
        Delta d = deltaFor(robot);
        int dx = d.dx();
        int dy = d.dy();

        // If command is 'back', invert delta
        if ("back".equalsIgnoreCase(command.commandName())) {
            dx = -dx;
            dy = -dy;
        }

        // Iterate steps and check world validity at each step
        int curX = robot.getX();
        int curY = robot.getY();

        System.out.println("[MovementService] Moving robot='" + robot.getName() + "' from=(" + curX + "," + curY + ") dx=" + dx + " dy=" + dy + " steps=" + steps);

        for (int step = 1; step <= steps; step++) {
            int nextX = curX + dx;
            int nextY = curY + dy;
            Position nextPos = new Position(nextX, nextY);

            Status s = world.isPositionValid(nextPos);
            System.out.println("[MovementService] step=" + step + " checkPos=(" + nextX + "," + nextY + ") status=" + s);
            if (s == Status.HitObstaclePIT) {
                // Robot falls into pit and dies
                System.out.println("[MovementService] Robot '" + robot.getName() + "' fell into pit at (" + nextX + "," + nextY + ")");
                robot.setPosition(nextX, nextY);
                robot.status = Robot.RobotStatus.Dead;
                JSONObject data = new JSONObject();
                JSONArray pos = new JSONArray();
                pos.put(nextX); pos.put(nextY);
                data.put("position", pos);
                data.put("message", robot.getName() + " fell into a pit and died.");
                Response resp = new Response("ERROR", robot.getName() + " fell into a pit and died.");
                resp.object.put("data", data);
                world.stateForRobot(robot, resp);
                // Do NOT notify world/command handler here; tests expect the dead robot to remain in the world
                return resp;
            } else if (s == Status.HitObstacleMINE) {
                System.out.println("[MovementService] Robot '" + robot.getName() + "' stepped on a mine at (" + nextX + "," + nextY + ")");
                // Move onto the mine cell, robot dies immediately (mine is lethal regardless of shields)
                robot.setPosition(nextX, nextY);
                robot.status = Robot.RobotStatus.Dead;
                // remove mine after triggering
                world.removeObstacleAt(nextX, nextY);

                JSONObject data = new JSONObject();
                JSONArray pos = new JSONArray();
                pos.put(nextX); pos.put(nextY);
                data.put("position", pos);
                String message = robot.getName() + " died from a mine.";
                Response resp = new Response("ERROR", message);
                data.put("message", message);
                resp.object.put("data", data);
                world.stateForRobot(robot, resp);
                // Notify world/command handler so ownership is cleared and client disconnected
                try {
                    world.notifyRobotDeath(robot.getName());
                } catch (Exception ignored) {}
                return resp;
            } else if (s == Status.HitObstacle) {
                System.out.println("[MovementService] Robot '" + robot.getName() + "' hit obstacle at (" + nextX + "," + nextY + ")");
                JSONObject data = new JSONObject();
                JSONArray pos = new JSONArray();
                pos.put(curX); pos.put(curY);
                data.put("position", pos);
                data.put("message", "Failed to move because it hit an obstacle");
                Response resp = new Response("ERROR", "Failed to move because it hit an obstacle");
                resp.object.put("data", data);
                // do NOT attach state for obstacle collisions
                return resp;
            } else if (s == Status.OutOfBounds) {
                System.out.println("[MovementService] Robot '" + robot.getName() + "' attempted to move outside at (" + nextX + "," + nextY + ") - reporting edge");
                // Treat world edge as a blocking edge and report an OK message similar to the reference server
                JSONObject data = new JSONObject();
                JSONArray pos = new JSONArray();
                // report current position (move not applied)
                pos.put(curX); pos.put(curY);
                data.put("position", pos);
                // Build a friendly edge message using the robot's facing direction
                String dirName = robot.getDirection() != null && robot.getDirection().getDirection() != null
                        ? robot.getDirection().getDirection().name() : "NORTH";
                String message = "At the " + dirName + " edge";
                data.put("message", message);
                data.put("visibility", world.getVisibility());
                data.put("objects", new org.json.JSONArray());

                Response resp = new Response("OK", message);
                resp.object.put("data", data);
                // Attach the robot state so clients can see current position/status
                world.stateForRobot(robot, resp);
                return resp;
            }

            // Position is valid - apply move
            robot.setPosition(nextX, nextY);
            curX = nextX; curY = nextY;
        }

        // Successful move
        JSONObject data = new JSONObject();
        JSONArray pos = new JSONArray();
        pos.put(robot.getX()); pos.put(robot.getY());
        data.put("position", pos);
        data.put("message", "Moved " + command.commandName() + " " + steps + " steps.");
        Response resp = new Response("OK", "Moved " + command.commandName() + " " + steps + " steps.");
        resp.object.put("data", data);
        world.stateForRobot(robot, resp);
        return resp;
    }

    // Helper value object for movement deltas
    private record Delta(int dx, int dy) { }

    private Delta deltaFor(Robot robot) {
        String dir = robot.getDirection() != null && robot.getDirection().getDirection() != null
                ? robot.getDirection().getDirection().toString() : "NORTH";
        return switch (dir) {
            case "NORTH" -> new Delta(0, 1);
            case "SOUTH" -> new Delta(0, -1);
            case "EAST" -> new Delta(1, 0);
            case "WEST" -> new Delta(-1, 0);
            default -> new Delta(0, 0);
        };
    }
}
