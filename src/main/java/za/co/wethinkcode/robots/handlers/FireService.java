package za.co.wethinkcode.robots.handlers;

import org.json.JSONObject;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.Position;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

public class FireService {
    private final World world;

    public FireService(World world) {
        this.world = world;
    }

    // Result object to return (mirrors previous behaviour: a single Response)
    public Response handleFire(Robot shooter) {
        if (shooter == null) {
            return makeError("Could not find robot: null");
        }

        Robot found = world.findRobot(shooter.getName());
        if (found == null) {
            return makeError("Could not find robot: " + shooter.getName());
        }

        // Miners cannot fire
        if (found.getMake() != null && found.getMake().equalsIgnoreCase("miner")) {
            return makeError("Miners cannot fire.");
        }

        // Compute direction and range
        Position robotP = found.getPosition();
        Delta delta = directionDelta(found);
        int range = computeRange(found);

        // Decrease shots
        found.setShots(found.getShots() - 1);

        // find hit
        HitResult hit = findHitRobot(robotP, delta, range, found.getName());

        if (hit == null) {
            return new Response("OK", "You have missed 🥲!");
        }

        Robot hitRobot = hit.robot();
        int distance = hit.distance();

        // apply effects
        applyHitEffects(hitRobot, found, distance);

        // build response
        Response hitRobotResponse = new Response("OK", "I got hit");
        world.stateForRobot(hitRobot, hitRobotResponse); // ensure state included

        Response resp;
        if (hitRobot.isDead()) {
            resp = new Response("OK", "You have hit 💥 " + hitRobot.getName() + "! It is now destroyed.");
        } else {
            resp = new Response("OK", "You have hit 💥 " + hitRobot.getName() + "! Remaining shield: " + hitRobot.getShields());
        }

        JSONObject data = new JSONObject();
        data.put("message", "Hit");
        data.put("distance", distance);
        data.put("robot", hitRobot.getName());
        data.put("state", hitRobotResponse.object.optJSONObject("state"));
        resp.object.put("data", data);

        return resp;
    }

    private void applyHitEffects(Robot target, Robot shooter, int distance) {
        target.takeHit();
        try {
            // include distance in target state
            Response tmp = new Response("OK", "I got hit");
            tmp.object.put("distance", distance);
            world.stateForRobot(target, tmp);
        } catch (Exception ignored) {}
        world.stateForRobot(shooter, new Response("OK", "Shot fired"));

        // If the target died from the hit, notify the world so ownership is cleared and client is disconnected
        if (target.isDead()) {
            try {
                world.notifyRobotDeath(target.getName());
            } catch (Exception e) {
                System.out.println("[FireService] Failed to notify robot death for " + target.getName() + ": " + e.getMessage());
            }
        }
    }

    // small helpers copied from CommandHandler
    private record HitResult(Robot robot, int distance) { }
    private record Delta(int dx, int dy) { }

    private Delta directionDelta(Robot robot) {
        if (robot == null || robot.getDirection() == null || robot.getDirection().getDirection() == null) {
            return new Delta(0,0);
        }
        String dir = robot.getDirection().getDirection().toString();
        return switch (dir) {
            case "NORTH" -> new Delta(0,1);
            case "SOUTH" -> new Delta(0,-1);
            case "EAST" -> new Delta(1,0);
            case "WEST" -> new Delta(-1,0);
            default -> new Delta(0,0);
        };
    }

    private HitResult findHitRobot(Position start, Delta delta, int range, String shooterName) {
        for (int step = 1; step <= range; step++) {
            Position checkPos = new Position(start.getX() + step * delta.dx(), start.getY() + step * delta.dy());
            for (Robot other : world.getRobots()) {
                if (!other.getName().equals(shooterName) && other.getPosition().equals(checkPos)) {
                    return new HitResult(other, step);
                }
            }
        }
        return null;
    }

    // computeRange logic copied
    private int computeRange(Robot robot) {
        if (robot == null) return 0;
        String make = robot.getMake();
        int shots = robot.getShots();
        if (make != null && make.equalsIgnoreCase("tank")) return computeRangeForTank(shots);
        return computeRangeForSniper(shots);
    }

    private int computeRangeForTank(int shots) {
        if (shots == 3) return 3;
        if (shots == 2) return 4;
        return 5;
    }

    private int computeRangeForSniper(int shots) {
        int range = 11 - shots;
        if (range < 1) range = 1;
        if (range > 10) range = 10;
        return range;
    }

    private Response makeError(String msg) {
        Response r = new Response("ERROR", msg);
        JSONObject data = new JSONObject();
        data.put("message", msg);
        r.object.put("data", data);
        return r;
    }
}
