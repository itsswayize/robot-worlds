package za.co.wethinkcode.robots.domain;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import za.co.wethinkcode.robots.commands.Command;
import za.co.wethinkcode.robots.handlers.CommandHandler;
import za.co.wethinkcode.robots.server.*;

public class World {
    // ————————————————————————————————————————————————————————————————
    // EXISTING CODE (truncated for clarity)
    // ————————————————————————————————————————————————————————————————

    // Make singleton lazily initialized so Server can decide whether to load from config or create from args
    private static World INSTANCE = null;

    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 50;
    private static final int DEFAULT_SHIELD = 10;
    private static final int DEFAULT_REPAIR_TIME = 5;
    private static final int DEFAULT_RELOAD_TIME = 3;
    private static final int DEFAULT_MINE_SET_TIME = 3; // seconds
    private static final double OBSTACLE_DENSITY = 0.30;

    private final CommandHandler commandHandler;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Robot> robots = new ArrayList<>();
    private final Random random = new Random();

    private int width, height, halfWidth, halfHeight;
    private int maxShieldStrength, shieldRepairTime, reloadTime, visibility;
    private int mineSetTimeSeconds = DEFAULT_MINE_SET_TIME;
    // No automatic obstacle generation on config load. Obstacles are only created
    // when explicitly requested (Server.generateObstacles call or via -o argument).

    public static World getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new World();
        }
        return INSTANCE;
    }

    /**
     * Explicitly set/replace the singleton instance. Used by Server when creating a World based on
     * command-line arguments (so we avoid loading the config file in that case).
     */
    public static void initializeInstance(World instance) {
        INSTANCE = instance;
    }

    public World() {
        new ConfigLoader().applyConfigToWorld(this, "config.properties");
        this.commandHandler = new CommandHandler(this);
        // Do not auto-generate obstacles here. Server.start() will display the world
        // and will generate obstacles only if requested via CLI args.
        // Do not call displayWorld() here — Server.start() will print the initial world once.
    }

    public World(int width, int height) {
        setDimensions(width, height);
        this.visibility = this.halfWidth;
        this.commandHandler = new CommandHandler(this);
        // For worlds created directly (for example via CLI args) do not auto-generate obstacles
        // or display. The Server controls whether obstacles are placed so that passing
        // `-o none` results in a world with no obstacles as expected.
    }

    /* ---------- World Setup ---------- */
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        this.halfWidth = Math.max(0, width / 2);
        this.halfHeight = Math.max(0, height / 2);
    }

    public void setDefaultDimensions() {
        setDimensions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void setWorldProperties(int shieldRepairTime, int reloadTime, int maxShieldStrength, int visibility) {
        this.shieldRepairTime = shieldRepairTime;
        this.reloadTime = reloadTime;
        this.maxShieldStrength = maxShieldStrength;
        this.visibility = visibility;
    }

    public void setDefaultWorldProperties() {
        int visibility = (int) (this.getWidth() * OBSTACLE_DENSITY);
        setWorldProperties(DEFAULT_REPAIR_TIME, DEFAULT_RELOAD_TIME, DEFAULT_SHIELD, visibility);
    }

    /* ---------- Command Execution ---------- */
    public void execute(Command command, String clientId, CommandHandler.CompletionHandler completionHandler) {
        commandHandler.handle(command, clientId, completionHandler);
    }

    /* ---------- Display Methods ---------- */
    public void displayWorld() {
        // viewWidth and viewHeight should correspond to the number of cells in each axis which is
        // (2 * halfWidth + 1) and (2 * halfHeight + 1) respectively. Previously the code passed
        // `width` and `height` directly which produced an incorrect grid size (e.g. width=2 -> 2 columns).
        int viewWidth = Math.max(1, 2 * halfWidth + 1);
        int viewHeight = Math.max(1, 2 * halfHeight + 1);
        System.out.println(displayViewport(-halfWidth, halfHeight, viewWidth, viewHeight));
    }

    public String displayViewport(int originX, int originY, int viewWidth, int viewHeight) {
        String[][] grid = new String[viewHeight][viewWidth];
        Viewport vp = new Viewport(originX, originY, viewWidth, viewHeight);
        fillGrid(grid, vp);
        placeObstacles(grid, vp);
        placeRobots(grid, vp);
        return gridToString(grid);
    }

    private void fillGrid(String[][] grid, Viewport vp) {
        for (int i = 0; i < vp.viewHeight; i++) {
            for (int j = 0; j < vp.viewWidth; j++) {
                int worldX = vp.originX + j;
                int worldY = vp.originY - i;
                grid[i][j] = isWithinBounds(worldX, worldY) ? "◾️" : " ";
            }
        }
    }

    private void placeObstacles(String[][] grid, Viewport vp) {
        for (Obstacle obstacle : obstacles) {
            markObstacleArea(obstacle, grid, vp);
        }
    }

    private void placeRobots(String[][] grid, Viewport vp) {
        for (Robot robot : robots) {
            int x = robot.getX(), y = robot.getY();
            vp.placeIfVisible(grid, x, y, "🤖");
        }
    }

    public String displayDirectionalCross(Robot robot, int maxDistance) {
        int robotX = robot.getX(), robotY = robot.getY();
        int minX = Math.max(robotX - maxDistance, -halfWidth);
        int maxX = Math.min(robotX + maxDistance, halfWidth);
        int minY = Math.max(robotY - maxDistance, -halfHeight);
        int maxY = Math.min(robotY + maxDistance, halfHeight);
        int width = maxX - minX + 1, height = maxY - minY + 1;
        String[][] grid = new String[height][width];
        for (String[] row : grid) Arrays.fill(row, " ");

        Bounds b = new Bounds(minX, maxX, minY, maxY);
        CrossContext ctx = new CrossContext(b, robotX, robotY);
        drawCrossLines(grid, ctx);
        markObstaclesOnCross(grid, ctx);
        markOtherRobotsOnCross(grid, robot, ctx);
        markRobot(grid, robotX, robotY, ctx);
        return gridToString(grid);
    }

    private void drawCrossLines(String[][] grid, CrossContext ctx) {
        int verticalCol = ctx.robotX - ctx.minX;
        int horizontalRow = ctx.maxY - ctx.robotY;
        setVerticalLine(grid, verticalCol);
        setHorizontalLine(grid, horizontalRow);
    }

    private void markObstaclesOnCross(String[][] grid, CrossContext ctx) {
        for (Obstacle obstacle : obstacles) {
            for (int y = obstacle.getY(); y < obstacle.getMaxY(); y++) {
                for (int x = obstacle.getX(); x < obstacle.getMaxX(); x++) {
                    ctx.placeOnCrossIfInBounds(grid, x, y, obstacle.type().getSymbol());
                }
            }
        }
    }

    private void markOtherRobotsOnCross(String[][] grid, Robot robot, CrossContext ctx) {
        for (Robot other : robots) {
            if (other.equals(robot)) continue;
            int x = other.getX(), y = other.getY();
            ctx.placeOnCrossIfInBounds(grid, x, y, "🤖");
        }
    }

    private void markObstacleArea(Obstacle obstacle, String[][] grid, Viewport vp) {
        for (int y = obstacle.getY(); y < obstacle.getMaxY(); y++) {
            for (int x = obstacle.getX(); x < obstacle.getMaxX(); x++) {
                vp.placeIfVisible(grid, x, y, obstacle.type().getSymbol());
            }
        }
    }

    private void setVerticalLine(String[][] grid, int col) {
        for (int row = 0; row < grid.length; row++) grid[row][col] = "◾️";
    }

    private void setHorizontalLine(String[][] grid, int row) {
        for (int col = 0; col < grid[0].length; col++) grid[row][col] = "◾️";
    }

    private void markRobot(String[][] grid, int robotX, int robotY, CrossContext ctx) {
        int row = ctx.maxY - robotY;
        int col = robotX - ctx.minX;
        if (isWithinGrid(grid, row, col)) {
            grid[row][col] = "🤖";
        }
    }

    private String gridToString(String[][] grid) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : grid) {
            for (String cell : row) {
                sb.append(cell == null ? " " : cell).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /* ---------- Game Logic ---------- */
    public Status isPositionValid(Position position) {
        if (!isWithinBounds(position.getX(), position.getY())) return Status.OutOfBounds;
        for (Obstacle obstacle : obstacles) {
            if (obstacle.contains(position)) return (obstacle.type() == ObstacleType.PIT) ? Status.HitObstaclePIT : (obstacle.type() == ObstacleType.MINE ? Status.HitObstacleMINE : Status.HitObstacle);
        }
        return Status.OK;
    }

    public boolean addObstacle(Obstacle obstacle) {
        if (!canPlaceObstacle(obstacle)) return false;
        obstacles.add(obstacle);
        return true;
    }

    private boolean canPlaceObstacle(Obstacle obstacle) {
        boolean overlaps = obstacles.stream().anyMatch(o -> o.overlaps(obstacle));
        int maxXIncluded = obstacle.getMaxX() - 1;
        int maxYIncluded = obstacle.getMaxY() - 1;
        return !overlaps && isWithinBounds(obstacle.getX(), obstacle.getY()) && isWithinBounds(maxXIncluded, maxYIncluded);
    }

    public synchronized Status addRobot(Robot robot) {
        if (robots.stream().anyMatch(r -> r.getName().equals(robot.getName()))) {
            return Status.ExistingName;
        }
        int x, y;
        try {
            if (robot.getPosition() == null) {
                Position pos = chooseSpawnPosition();
                x = pos.getX(); y = pos.getY();
            } else {
                x = robot.getX(); y = robot.getY();
                if (isPositionOutsideWorld(new Position(x, y))) return Status.OutOfBounds;
            }
        } catch (IllegalStateException e) {
            return Status.NoSpace;
        }
        Position chosen = new Position(x, y);
        Status placementStatus = checkPlacement(chosen);
        if (placementStatus != Status.OK) return placementStatus;
        robot.setPosition(x, y);
        robots.add(robot);
        return Status.OK;
    }

    private Status checkPlacement(Position p) {
        Status s = isPositionValid(p);
        if (s != Status.OK) return s;
        if (!isPositionFree(p)) return Status.NoSpace;
        return Status.OK;
    }

    private Position chooseSpawnPosition() {
        if (robots.isEmpty()) {
            int defX = 0, defY = 0;
            if (!isPositionOutsideWorld(defX, defY) && isPositionFree(defX, defY)) {
                return new Position(defX, defY);
            }
        }
        return findRandomFreePosition();
    }

    public Position findRandomFreePosition() {
        List<Position> freePositions = new ArrayList<>();
        int halfW = getHalfWidth();
        int halfH = getHalfHeight();
        for (int xx = -halfW; xx <= halfW; xx++) {
            for (int yy = -halfH; yy <= halfH; yy++) {
                if (isPositionOutsideWorld(xx, yy)) continue;
                if (isPositionFree(xx, yy)) freePositions.add(new Position(xx, yy));
            }
        }
        if (freePositions.isEmpty()) throw new IllegalStateException("No free position available in the world.");
        return freePositions.get(random.nextInt(freePositions.size()));
    }

    public Position findFreePosition() {
        int halfW = getHalfWidth();
        int halfH = getHalfHeight();
        for (int x = -halfW; x <= halfW; x++) {
            for (int y = -halfH; y <= halfH; y++) {
                if (isPositionOutsideWorld(x, y)) continue;
                if (isPositionFree(x, y)) {
                    return new Position(x, y);
                }
            }
        }
        throw new IllegalStateException("No free position available in the world.");
    }

    public Robot findRobot(String name) {
        return robots.stream().filter(r -> r.getName().equals(name)).findFirst().orElse(null);
    }

    public Response removeRobot(String robotName) {
        Robot robot = findRobot(robotName);
        if (robot == null) return new Response("ERROR", "Robot not found.");
        robots.remove(robot);
        return new Response("OK", "Removed robot " + robotName + " from the world.");
    }

    /* ---------- State & Info ---------- */
    public void stateForRobot(Robot robot, Response response) {
        JSONObject json = new JSONObject();
        // Provide position as a proper JSON array [x, y] so tests and clients can parse it
        JSONArray pos = new JSONArray();
        pos.put(robot.getX());
        pos.put(robot.getY());
        json.put("position", pos);
        json.put("direction", robot.orientation().toUpperCase());
        json.put("shields", robot.getShields());
        json.put("shots", robot.getShots());
        json.put("status", robot.status.toString().toUpperCase());
        response.object.put("state", json);
    }

    public String getAllRobotsInfo() {
        if (robots.isEmpty()) return "No robots in the world.";
        StringBuilder sb = new StringBuilder("Robots in the world:");
        for (Robot r : robots) {
            Response resp = new Response("", "State for " + r.getName());
            stateForRobot(r, resp);
            sb.append("\n- ").append(r.getName()).append(" ").append(resp.toJSONString());
        }
        return sb.toString();
    }

    public String getFullWorldState() {
        StringBuilder sb = new StringBuilder("World State:\n");
        sb.append("Dimensions: ").append(width).append(" x ").append(height).append("\n")
                .append("Obstacles (").append(obstacles.size()).append("):\n");
        obstacles.forEach(o -> sb.append(" - ").append(o).append("\n"));
        sb.append("Robots (").append(robots.size()).append("):\n");
        robots.forEach(r -> sb.append("- ").append(r.getName())
                .append(" at (").append(r.getX()).append(", ").append(r.getY()).append(")\n"));
        return sb.toString();
    }

    /* ---------- Getters ---------- */
    public List<Robot> getRobots() {
        return Collections.unmodifiableList(robots);
    }

    public List<Obstacle> getObstacles() {
        return Collections.unmodifiableList(obstacles);
    }

    public int getHalfWidth() { return halfWidth; }
    public int getHalfHeight() { return halfHeight; }
    public int getMaxShieldStrength() { return maxShieldStrength; }
    public int getReloadTime() { return reloadTime; }
    public int getShieldRepairTime() { return shieldRepairTime; }
    public int getVisibility() { return visibility; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getMineSetTimeSeconds() { return mineSetTimeSeconds; }
    public void setMineSetTimeSeconds(int seconds) { this.mineSetTimeSeconds = Math.max(1, seconds); }

    /* ---------- Private Helpers ---------- */
    /**
     * Public wrapper so external callers (e.g. Server when started with CLI args)
     * can request the default obstacle generation behavior.
     */
    public void generateObstacles() {
        generateDefaultObstacles();
    }

    private void generateDefaultObstacles() {
        // If the world is trivially small (1x1) nothing to place
        if (this.width == 1 && this.height == 1) return;

        // Regular generation for non-config small worlds or larger worlds.
        int obstacleCount = Math.max(0, (int) ((height + width) * OBSTACLE_DENSITY));
        Random random = new Random();
        final int MAX_ATTEMPTS_PER_OBSTACLE = 200;

        for (int i = 0; i < obstacleCount; i++) {
            boolean added = false;
            int attempts = 0;
            while (!added && attempts < MAX_ATTEMPTS_PER_OBSTACLE) {
                attempts++;
                ObstacleType type = randomObstacleType(random);
                int w = Math.max(1, Math.min(this.width, random.nextInt(1, 4)));
                int h = Math.max(1, Math.min(this.height, random.nextInt(1, 4)));
                int x = random.nextInt(-halfWidth, halfWidth + 1);
                int y = random.nextInt(-halfHeight, halfHeight + 1);
                added = addObstacle(new Obstacle(type, x, y, w, h));
            }
            if (!added) {
                System.out.println("[World] Could not place obstacle #" + i + " after " + MAX_ATTEMPTS_PER_OBSTACLE + " attempts; skipping remaining placements.");
                break;
            }
        }
    }

    private ObstacleType randomObstacleType(Random random) {
        ObstacleType[] values = ObstacleType.values();
        return values[random.nextInt(values.length)];
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= -halfWidth && x <= halfWidth && y >= -halfHeight && y <= halfHeight;
    }

    public boolean isPositionOutsideWorld(int x, int y) {
        return !isWithinBounds(x, y);
    }

    public boolean isPositionOutsideWorld(Position p) {
        return !isWithinBounds(p.getX(), p.getY());
    }

    public boolean isPositionFree(int x, int y) {
        return isPositionFree(new Position(x, y));
    }

    private boolean isOccupiedByObstacleAt(Position p) {
        for (Obstacle o : getObstacles()) {
            if (o.contains(p)) return true;
        }
        return false;
    }

    private boolean obstacleCoversAt(Obstacle o, Position p) {
        int x = p.getX(); int y = p.getY();
        int ox = o.getX(); int oy = o.getY();
        int ow = probeObstacleDimension(o, true);
        int oh = probeObstacleDimension(o, false);
        if (ow <= 0) ow = 1;
        if (oh <= 0) oh = 1;
        return x >= ox && x < ox + ow && y >= oy && y < oy + oh;
    }

    private boolean isOccupiedByRobotAt(Position p) {
        int x = p.getX(); int y = p.getY();
        for (Robot r : getRobots()) {
            if (r.getX() == x && r.getY() == y) return true;
        }
        return false;
    }

    public boolean isPositionFree(Position p) {
        if (isPositionOutsideWorld(p)) return false;
        if (isOccupiedByObstacleAt(p)) return false;
        return !isOccupiedByRobotAt(p);
    }

    // ————————————————————————————————————————————————————————————————
    // NEW METHOD: Check if a position is occupied by another robot
    // ————————————————————————————————————————————————————————————————
    /**
     * Checks if the given (x,y) is occupied by any robot except the excluded one.
     *
     * @param x            X coordinate
     * @param y            Y coordinate
     * @param excludeRobot The robot trying to move (we ignore it)
     * @return true if another robot is at (x,y)
     */
    public boolean isPositionOccupied(int x, int y, Robot excludeRobot) {
        for (Robot r : robots) {
            if (r != excludeRobot && r.getX() == x && r.getY() == y) {
                return true;
            }
        }
        return false;
    }

    // Optional: Overload for Position (cleaner in future code)
    public boolean isPositionOccupied(Position pos, Robot excludeRobot) {
        return isPositionOccupied(pos.getX(), pos.getY(), excludeRobot);
    }
    // ————————————————————————————————————————————————————————————————

    private int probeObstacleDimension(Obstacle o, boolean isWidth) {
        Integer fromGetters = getDimensionFromGetters(o, isWidth);
        if (fromGetters != null) return fromGetters;
        Integer fromFields = getDimensionFromFields(o, isWidth);
        if (fromFields != null) return fromFields;
        return 1;
    }

    private Integer getDimensionFromGetters(Obstacle o, boolean isWidth) {
        String[] candidates = isWidth
                ? new String[] {"getWidth", "width", "getW", "getSize", "getSizeX", "sizeX"}
                : new String[] {"getHeight", "height", "getH", "getSize", "getSizeY", "sizeY"};
        for (String name : candidates) {
            Integer v = tryInvokeGetter(o, name);
            if (v != null) return v;
        }
        return null;
    }

    private Integer getDimensionFromFields(Obstacle o, boolean isWidth) {
        String[] candidates = isWidth
                ? new String[] {"width", "w", "size", "sizeX"}
                : new String[] {"height", "h", "size", "sizeY"};
        for (String name : candidates) {
            Integer v = tryReadField(o, name);
            if (v != null) return v;
        }
        return null;
    }

    private Integer tryInvokeGetter(Obstacle o, String methodName) {
        try {
            Method m = o.getClass().getMethod(methodName);
            Object val = m.invoke(o);
            return convertToInteger(val);
        } catch (NoSuchMethodException ignored) { return null; }
        catch (Exception ignored) { return null; }
    }

    private Integer tryReadField(Obstacle o, String fieldName) {
        try {
            Field f = o.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object val = f.get(o);
            return convertToInteger(val);
        } catch (NoSuchFieldException ignored) { return null; }
        catch (Exception ignored) { return null; }
    }

    private Integer convertToInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); }
            catch (NumberFormatException ignored) { }
        }
        return null;
    }

    /* ---------- Helper Classes ---------- */
    private static class Viewport {
        final int originX, originY, viewWidth, viewHeight;
        Viewport(int originX, int originY, int viewWidth, int viewHeight) {
            this.originX = originX; this.originY = originY;
            this.viewWidth = viewWidth; this.viewHeight = viewHeight;
        }
        boolean isVisible(int x, int y) {
            return x >= originX && x < originX + viewWidth &&
                    y <= originY && y > originY - viewHeight;
        }
        void placeIfVisible(String[][] grid, int x, int y, String symbol) {
            if (!isVisible(x, y)) return;
            int row = originY - y;
            int col = x - originX;
            if (isIndexInGrid(grid, row, col)) grid[row][col] = symbol;
        }
        private boolean isIndexInGrid(String[][] grid, int row, int col) {
            return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
        }
    }

    private static class Bounds {
        final int minX, maxX, minY, maxY;
        Bounds(int minX, int maxX, int minY, int maxY) {
            this.minX = minX; this.maxX = maxX; this.minY = minY; this.maxY = maxY;
        }
    }

    private static class CrossContext {
        final int minX, maxY, robotX, robotY;
        final Bounds bounds;
        CrossContext(Bounds bounds, int robotX, int robotY) {
            this.bounds = bounds; this.minX = bounds.minX; this.maxY = bounds.maxY;
            this.robotX = robotX; this.robotY = robotY;
        }
        boolean isOutsideBounds(int x, int y) {
            return x < bounds.minX || x > bounds.maxX || y < bounds.minY || y > bounds.maxY;
        }
        boolean isOnCrossLine(int x, int y) {
            return x == robotX || y == robotY;
        }
        void placeOnCrossIfInBounds(String[][] grid, int x, int y, String symbol) {
            if (isOutsideBounds(x, y) || !isOnCrossLine(x, y)) return;
            int row = maxY - y;
            int col = x - minX;
            if (isIndexInGrid(grid, row, col)) grid[row][col] = symbol;
        }
        private boolean isIndexInGrid(String[][] grid, int row, int col) {
            return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
        }
    }

    private boolean isWithinGrid(String[][] grid, int row, int col) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }

    // Remove any obstacle that covers the given coordinate. Returns true if removed.
    public boolean removeObstacleAt(int x, int y) {
        Iterator<Obstacle> it = obstacles.iterator();
        Position p = new Position(x, y);
        while (it.hasNext()) {
            Obstacle o = it.next();
            if (obstacleCoversAt(o, p)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    // Check whether an obstacle of the specified type exists at the coordinate
    public boolean isObstacleAt(int x, int y, ObstacleType type) {
        Position p = new Position(x, y);
        for (Obstacle o : obstacles) {
            if (o.type() == type && obstacleCoversAt(o, p)) return true;
        }
        return false;
    }

    // Public helper so external code (MovementService, MineService, FireService) can notify the world
    // that a robot has died. The CommandHandler will handle removing ownership and disconnecting the client.
    public void notifyRobotDeath(String robotName) {
        try {
            if (this.commandHandler != null) {
                this.commandHandler.handleRobotDeath(robotName);
            }
        } catch (Exception e) {
            System.out.println("[World] Error notifying robot death for " + robotName + ": " + e.getMessage());
        }
    }
}
