package za.co.wethinkcode.robots.domain;

public enum ObstacleType {
    MOUNTAIN,
    LAKE,
    PIT,
    MINE;

    public String getSymbol() {
        return switch (this) {
            case MOUNTAIN -> "🌋";
            case LAKE -> "🌊";
            case PIT -> "⚫️";
            case MINE -> "💣";
        };
    }
}
