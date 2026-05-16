package za.co.wethinkcode.robots.client;

public enum RobotType {
    SNIPER("sniper"),
    TANK("tank"),
    SOLDIER("soldier"),
    MINER("miner");

    private final String id;

    RobotType(String id) { this.id = id; }

    public String id() { return id; }

    public static RobotType fromString(String s) {
        if (s == null) return null;
        String n = s.toLowerCase().trim();
        for (RobotType t : values()) if (t.id.equals(n)) return t;
        return null;
    }
}
