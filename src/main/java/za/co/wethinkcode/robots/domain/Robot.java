package za.co.wethinkcode.robots.domain;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Models a robot with a name, position, direction, and state.
 * Handles robot-specific properties and actions.
 */
public class Robot {
    private String make;
    private String name;
    private int shields;
    private int shots;
    private int maxShots;
    private Direction direction;
    private Position position;
    public RobotStatus status;
    private boolean repairing;
    private boolean reloading;
    private int range = 1;

    // New: placing mine state and saved shields while placing
    private boolean placingMine = false;
    private Integer savedShields = null;

    public Object getWorld() {
        return World.getInstance();
    }

    public enum RobotStatus {
        Normal,
        Dead,
        Reload,
        Repair
    }

    // Full constructor - used when a concrete position is known
    public Robot(String name, String make, int x, int y) {
        this.name = name;
        this.make = make;
        this.position =  new Position(x, y);
        this.direction= new Direction(Direction.CardinalDirection.NORTH); // default direction

        setMakeDefaults(make);

        this.maxShots = shots;
        this.status = RobotStatus.Normal;
    }

    // Placeholder constructors - do NOT assign a position so the World can choose one
    public Robot(String name) {
        this(name, "tank");
    }

    public Robot(String name, String make) {
        this.name = name;
        this.make = make;
        this.position = null; // important: leave unset so World.addRobot will find a free position
        this.direction = new Direction(Direction.CardinalDirection.NORTH);

        setMakeDefaults(make);

        this.maxShots = shots;
        this.status = RobotStatus.Normal;
    }

    private void setMakeDefaults(String make) {
        if (make == null) make = "tank";
        if (make.equalsIgnoreCase("tank")) {
            this.shields = 10;
            this.shots = 3;
            this.range = 2;
        } else if (make.equalsIgnoreCase("sniper")) {
            this.shields = 5;
            this.shots = 20;
            this.range = 10;
        } else if (make.equalsIgnoreCase("soldier")) {
            this.shields = 2;
            this.shots = 5;
            this.range = 4;
        } else if (make.equalsIgnoreCase("miner")) {
            // Miner: cannot shoot, designed to place mines
            this.shields = 3; // small shield
            this.shots = 0;   // cannot shoot
            this.range = 1;
        } else {
            this.shields = 10;
            this.shots = 3;
            this.range = 1;
        }
    }

    public void moveForward(int steps) {
        for (int i = 0; i < steps; i++) {
            // Logic to update the robot's position based on its current direction
            switch (direction.getDirection()) {
                case NORTH: this.setPosition(getX(), getY() + 1); break;
                case SOUTH: this.setPosition(getX(), getY() - 1); break;
                case EAST: this.setPosition(getX() + 1, getY()); break;
                case WEST: this.setPosition(getX() - 1, getY()); break;
            }
        }
    }
    public void moveBackward(int steps) {
        for (int i = 0; i < steps; i++) {
            // Logic to update the robot's position based on its current direction
            switch (direction.getDirection()) {
                case NORTH: this.setPosition(getX(), getY() - 1); break;
                case SOUTH: this.setPosition(getX(), getY() + 1); break;
                case EAST: this.setPosition(getX() - 1, getY()); break;
                case WEST: this.setPosition(getX() + 1, getY()); break;
            }
        }
    }

    public JSONArray getPositionAsJSONArray() {
        JSONArray arr = new JSONArray();
        arr.put(getX());
        arr.put(getY());
        return arr;
    }

    public JSONObject getStateAsJSON() {
        JSONObject state = new JSONObject();
        state.put("position", getPositionAsJSONArray());
        state.put("direction", getDirection().toString());
        state.put("shields", getShields());
        state.put("shots", getShots());
        state.put("status", status.toString());
        return state;
    }

    public int getMaxShots() {
        return maxShots;
    }

    public boolean isRepairing() {
        return this.repairing;
    }

    public void setRepairing(boolean repairing) {
        this.repairing = repairing;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        this.reloading = reloading;
    }

    public void turnLeft() {
        direction.turnLeft();
    }

    public void turnRight() {
        direction.turnRight();
    }

    public String orientation() {
        return direction.toString();
    }

    public Position getPosition() {
        return position;
    }

    public int getX() {
        return position != null ? position.getX() : 0;
    }

    public int getY() {
        return position != null ? position.getY() : 0;
    }

    public String getName() {
        return name;
    }

    public String getMake() {
        return make;
    }

    public int getShields() {
        return shields;
    }

    public int getShots() {
        return shots;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }

    public void setPosition(int x, int y) {
        this.position = new Position(x, y);
    }

    public void setShields(int shields){
        this.shields = shields;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }


    public void takeHit() {
        if (shields > 0) {
            shields--;
        } else {
            this.status = RobotStatus.Dead;
        }
    }
    public boolean isDead() {
        return this.status == RobotStatus.Dead;
    }

    // New: placing-mine helpers
    public boolean isPlacingMine() { return placingMine; }

    public void startPlacingMine() {
        if (!placingMine) {
            placingMine = true;
            savedShields = this.shields;
            this.shields = 0; // shields disabled while placing
        }
    }

    public void finishPlacingMine() {
        if (placingMine) {
            placingMine = false;
            if (savedShields != null) {
                this.shields = savedShields;
                savedShields = null;
            }
        }
    }

}