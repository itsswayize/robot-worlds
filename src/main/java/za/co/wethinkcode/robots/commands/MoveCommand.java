package za.co.wethinkcode.robots.commands;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

public class MoveCommand extends Command {
    private final String direction;
    private final World world;

    /* --------------------------------------------------------------
       Constructor – receive the World instance (injected by the server)
       -------------------------------------------------------------- */
    public MoveCommand(Robot robot, World world, String direction, String[] arguments) {
        super(robot, arguments);
        this.world = world;
        this.direction = direction.toLowerCase();
    }

    @Override
    public String commandName() {
        return direction;
    }

    @Override
    public String execute() {
        JSONObject data  = new JSONObject();
        JSONObject state = new JSONObject();

        /* ---------- Argument validation ---------- */
        if (arguments.length != 1) {
            data.put("message", "Invalid move command format. Use '<steps>'.");
            return Response.error(data, "Invalid move command format.")
                    .withState(state).toJSONString();
        }

        int steps;
        try {
            steps = Integer.parseInt(arguments[0]);
        } catch (NumberFormatException e) {
            data.put("message", "Steps must be a number.");
            return Response.error(data, "Steps must be a number.")
                    .withState(state).toJSONString();
        }

        /* ---------- Helper: adjust X/Y for a single step ---------- */
        // Returns a new int[]{x, y} that is one step in the requested direction.
        // This eliminates the three duplicated switch blocks.
        java.util.function.BiFunction<Integer, Boolean, int[]> stepAdjuster = (i, forward) -> {
            int dx = 0, dy = 0;
            switch (robot.getDirection().getDirection()) {
                case NORTH -> dy = forward ? i : -i;
                case EAST  -> dx = forward ? i : -i;
                case SOUTH -> dy = forward ? -i : i;
                case WEST  -> dx = forward ? -i : i;
                default -> throw new IllegalStateException(
                        "Unknown direction: " + robot.getDirection());
            }
            return new int[]{robot.getX() + dx, robot.getY() + dy};
        };

        /* ---------- Path-obstruction check (step-by-step) ---------- */
        boolean forward = commandName().equals("forward");
        for (int i = 1; i <= steps; i++) {
            int[] pos = stepAdjuster.apply(i, forward);
            int checkX = pos[0];
            int checkY = pos[1];

            if (world.isPositionOccupied(checkX, checkY, robot)) {
                data.put("message", "Obstructed");
                return Response.error(data, "Obstructed")
                        .withState(getCurrentState(state)).toJSONString();
            }
        }

        /* ---------- All clear – actually move ---------- */
        if (forward) {
            robot.moveForward(steps);
        } else {
            robot.moveBackward(steps);
        }

        /* ---------- Success response ---------- */
        JSONArray posArray = new JSONArray();
        posArray.put(robot.getX());
        posArray.put(robot.getY());

        data.put("position", posArray);
        data.put("message", "Moved " + commandName() + " " + steps + " steps.");

        state = getCurrentState(state);
        state.put("position", posArray);

        return Response.ok(data, data.getString("message"))
                .withState(state).toJSONString();
    }

    /* ---------- Helper: build the current robot state JSON ---------- */
    private JSONObject getCurrentState(JSONObject state) {
        JSONArray pos = new JSONArray();
        pos.put(robot.getX());
        pos.put(robot.getY());

        state.put("position", pos);
        state.put("direction", robot.getDirection().toString());
        state.put("shields",   robot.getShields());
        state.put("shots",     robot.getShots());
        state.put("status",    robot.status.toString());
        return state;
    }
}