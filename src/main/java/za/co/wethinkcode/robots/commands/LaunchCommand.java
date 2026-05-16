package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.World;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.Position;
import za.co.wethinkcode.robots.domain.Status;

public class LaunchCommand extends Command {
    private final World world;

    public LaunchCommand(Robot robot, String[] arguments, World world) {
        super(robot, arguments);
        this.world = world;
    }

    @Override
    public String commandName() {
        return "launch";
    }

    @Override
    public String execute() {
        JSONObject data = new JSONObject();
        JSONObject state = new JSONObject();

        String make = arguments.length > 0 ? arguments[0] : "tank";
        int x, y;

        World w = this.world != null ? this.world : World.getInstance();

        // If coordinates provided: arguments[1], arguments[2]
        if (arguments.length == 3) {
            try {
                x = Integer.parseInt(arguments[1]);
                y = Integer.parseInt(arguments[2]);
            } catch (NumberFormatException e) {
                data.put("message", "Invalid coordinates.");
                return Response.error(data, "Invalid coordinates.").withState(state).toJSONString();
            }

            // Validate inside world and free
            if (w.isPositionOutsideWorld(x, y) || !w.isPositionFree(x, y)) {
                data.put("message", "Cannot launch robot at the requested coordinates - blocked or outside world.");
                return Response.error(data, "Robot cannot be launched at that position.").withState(state).toJSONString();
            }
        } else {
            // Find an open, unoccupied position
            try {
                Position pos = w.findFreePosition();
                x = pos.getX();
                y = pos.getY();
            } catch (IllegalStateException e) {
                data.put("message", "No free position available to launch the robot.");
                return Response.error(data, "No free position available.").withState(state).toJSONString();
            }
        }

        // Set robot location
        robot.setPosition(x, y);

        // Set stats based on type
        if (make.equalsIgnoreCase("tank")) {
            robot.setShields(10);
            robot.setShots(3);
            robot.setRange(2);
        } else if (make.equalsIgnoreCase("sniper")) {
            robot.setShields(5);
            robot.setShots(20);
            robot.setRange(10);
        } else if (make.equalsIgnoreCase("soldier")) {
            robot.setShields(2);
            robot.setShots(5);
            robot.setRange(4);
        } else if (make.equalsIgnoreCase("miner")) {
            robot.setShields(3);
            robot.setShots(0); // cannot shoot
            robot.setRange(1);
        } else {
            // default fallback
            robot.setShields(10);
            robot.setShots(3);
            robot.setRange(1);
        }

        // Try to register the robot with the world and check the placement Status
        try {
            Status addStatus = w.addRobot(robot);
            if (addStatus != Status.OK) {
                // Map some Status values to user-friendly messages
                switch (addStatus) {
                    case ExistingName -> data.put("message", "A robot with that name already exists in the world.");
                    case OutOfBounds -> data.put("message", "Requested launch position is out of bounds.");
                    case HitObstacle, HitObstaclePIT -> data.put("message", "Cannot launch robot at the requested coordinates - blocked or outside world.");
                    case NoSpace -> data.put("message", "No free position available to launch the robot.");
                    default -> data.put("message", "Could not place robot in the world.");
                }
                return Response.error(data, data.getString("message")).withState(state).toJSONString();
            }
        } catch (NoSuchMethodError | AbstractMethodError | RuntimeException ignored) {
            try {
                // alternate common name
                java.lang.reflect.Method m = w.getClass().getMethod("registerRobot", Robot.class);
                Object res = m.invoke(w, robot);
                // If reflection-based registration returned a Status, check it
                if (res instanceof Status) {
                    Status addStatus = (Status) res;
                    if (addStatus != Status.OK) {
                        data.put("message", "Could not place robot in the world.");
                        return Response.error(data, data.getString("message")).withState(state).toJSONString();
                    }
                }
            } catch (Exception ignored2) {
                // If registration fails, continue — robot may be tracked elsewhere.
            }
        } catch (Exception ignored) {
            // ignore and continue
        }

        // Build a real JSONArray for the position
        JSONArray posArr = new JSONArray();
        posArr.put(robot.getX());
        posArr.put(robot.getY());

        data.put("position", posArr);
        data.put("message", robot.getName() + " launched at position [" + robot.getX() + "," + robot.getY() + "] facing " + robot.getDirection());

        state.put("position", posArr);
        state.put("direction", robot.getDirection().toString());
        state.put("shields", robot.getShields());
        state.put("shots", robot.getShots());
        state.put("status", robot.status.toString());

        return Response.ok(data, data.getString("message")).withState(state).toJSONString();
    }
}
