package za.co.wethinkcode.robots.handlers;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.Direction;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import java.util.*;

/**
 * Handles visibility logic for robots in the world.
 * Determines which objects (robots, obstacles, or world edges) are visible from a given robot's position
 * in each cardinal direction within a defined viewing range. Filters and formats data for look command responses.
 */
public class VisibilityHandler {
    private final List<Robot> robots;
    private final List<Obstacle> obstacles;
    private final int halfWidth;
    private final int halfHeight;
    private final int maxDistance;
    private final World world;

    public VisibilityHandler(List<Robot> robots, List<Obstacle> obstacles, int halfWidth, int halfHeight, int maxDistance, World world) {
        this.robots = robots;
        this.obstacles = obstacles;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.maxDistance = maxDistance;
        this.world = world;
    }

    public Response lookAround(Robot robot) {
        List<VisibleObject> visibleObjects = new ArrayList<>();
        int maxDistance = this.maxDistance;

        for (Direction.CardinalDirection direction : Direction.CardinalDirection.values()) {
            List<VisibleObject> seen = checkVisibleObjects(robot, direction, maxDistance);
            visibleObjects.addAll(seen);
        }

        String message = buildLookMessage(robot, visibleObjects, maxDistance);

        org.json.JSONArray jsonObjects = buildObjectsJson(visibleObjects);

        org.json.JSONObject data = new org.json.JSONObject();
        data.put("objects", jsonObjects);
        data.put("message", message);

        return Response.ok(data, message);
    }

    private List<VisibleObject> checkVisibleObjects(Robot robot, Direction.CardinalDirection direction, int maxDistance) {
        List<VisibleObject> objects = new ArrayList<>();
        Set<Object> seen = new HashSet<>();

        int dx = 0, dy = 0;
        switch (direction) {
            case EAST -> dx = 1;
            case WEST -> dx = -1;
            case NORTH -> dy = 1;
            case SOUTH -> dy = -1;
        }

        int startX = robot.getX();
        int startY = robot.getY();

        int mineVisibility = Math.max(0, this.maxDistance / 4); // robots see mines within quarter visibility rounded down

        for (int step = 1; step <= maxDistance; step++) {
            int x = startX + dx * step;
            int y = startY + dy * step;

            // Check obstacles
            for (Obstacle obs : obstacles) {
                if (seen.contains(obs)) continue;
                // If obstacle is a mine and outside mine visibility, skip reporting it
                if (obs.type() == ObstacleType.MINE && step > mineVisibility) continue;
                if (obstacleCoversAt(obs, x, y)) {
                    objects.add(new VisibleObject(obs, direction, step, VisibleType.OBSTACLE));
                    seen.add(obs);
                }
            }

            // Check other robots
            for (Robot nextRobot : robots) {
                if (nextRobot.equals(robot)) continue;
                if (seen.contains(nextRobot)) continue;
                if (nextRobot.getX() == x && nextRobot.getY() == y) {
                    objects.add(new VisibleObject(nextRobot, direction, step, VisibleType.ROBOT));
                    seen.add(nextRobot);
                }
            }

            // Check world edge
            if (isAtEdge(x, y, direction)) {
                if (!seen.contains(direction)) {
                    objects.add(new VisibleObject(direction, direction, step, VisibleType.EDGE));
                    seen.add(direction);
                }
            }
        }

        // Sort by the closest objects
        objects.sort(Comparator.comparingInt(VisibleObject::getDistance));

        List<VisibleObject> result = new ArrayList<>();
        for (VisibleObject vo : objects) {
            Object o = vo.getObject();
            if (o instanceof Obstacle obstacle) {
                if (obstacle.type() == ObstacleType.MOUNTAIN) {
                    result.add(vo);
                    break;
                }
            }
            result.add(vo);
        }

        return result;
    }

    // Helper: returns true when the obstacle covers the given (x,y) coordinate.
    private boolean obstacleCoversAt(Obstacle obs, int x, int y) {
        if (obs == null) return false;
        // Use obstacle bounds to test coverage; rely on getX/getMaxX/getY/getMaxY provided by Obstacle
        for (int obX = obs.getX(); obX < obs.getMaxX(); obX++) {
            for (int obY = obs.getY(); obY < obs.getMaxY(); obY++) {
                if (obX == x && obY == y) return true;
            }
        }
        return false;
    }

    private String buildLookMessage(Robot robot, List<VisibleObject> visibleObjects, int maxDistance) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("\nLooking around for ").append(robot.getName()).append(" 🤖:");
        messageBuilder.append("\n  Objects").append(":");

        for (VisibleObject vo : visibleObjects) {
            VisibleType type = vo.getType();
            Direction.CardinalDirection dir = vo.getDirection();
            int distance = vo.getDistance();
            String directionSymbol = dir == null ? "" : dir.symbolForDirection();

            if (type == VisibleType.OBSTACLE) {
                messageBuilder.append("\n   🚧 Found an obstacle nearby!");
            } else if (type == VisibleType.ROBOT) {
                messageBuilder.append("\n   🤖 Found another robot nearby!");
            } else {
                messageBuilder.append("\n   🧭 Found the edge of the world");
            }

            messageBuilder.append("\n       🧭 Direction ").append(directionSymbol);
            messageBuilder.append("\n       🦶 Steps ").append(distance);
        }

        if (visibleObjects.isEmpty()) {
            messageBuilder.append("\n   🥲 Could not find anything (nothing visible), try moving around to find more objects");
        }

        boolean onlyEdges = !visibleObjects.isEmpty() && visibleObjects.stream().allMatch(v -> v.getType() == VisibleType.EDGE);
        if (onlyEdges) {
            messageBuilder.append("\n   🥲 Could not find anything (nothing visible), try moving around to find more objects");
        }

        String snapshot = world.displayDirectionalCross(robot, maxDistance);
        messageBuilder.append("\nHere is a snapshot of you can see:").append("\n").append(snapshot);

        return messageBuilder.toString();
    }

    private org.json.JSONArray buildObjectsJson(List<VisibleObject> visibleObjects) {
        org.json.JSONArray jsonObjects = new org.json.JSONArray();
        for (VisibleObject vo : visibleObjects) {
            org.json.JSONObject jo = new org.json.JSONObject();
            jo.put("type", vo.getType() == null ? "UNKNOWN" : vo.getType().name());
            Direction.CardinalDirection dir = vo.getDirection();
            jo.put("direction", dir == null ? "" : dir.name());
            jo.put("distance", vo.getDistance());
            jsonObjects.put(jo);
        }
        return jsonObjects;
    }

    private Map<String, Object> createObjectMap(String type, Direction.CardinalDirection direction, int distance) {
        // kept for backward compatibility with any callers that expect a Map — unused now
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("direction", direction);
        map.put("distance", distance);
        return map;
    }

    private boolean isAtEdge(int x, int y, Direction.CardinalDirection dir) {
        return switch (dir) {
            case NORTH -> y >= halfHeight;
            case SOUTH -> y <= -halfHeight;
            case EAST -> x >= halfWidth;
            case WEST -> x <= -halfWidth;
        };
    }
}
