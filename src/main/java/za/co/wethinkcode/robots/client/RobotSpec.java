package za.co.wethinkcode.robots.client;

public class RobotSpec {
    private final String name;
    private final RobotType type;

    public RobotSpec(String name, RobotType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public RobotType getType() { return type; }

    @Override
    public String toString() {
        return "RobotSpec{" + "name='" + name + '\'' + ", type='" + (type == null ? "null" : type.id()) + '\'' + '}';
    }
}
