package za.co.wethinkcode.robots.domain;

/**
 * Represents a cardinal direction (NORTH, SOUTH, EAST, WEST) with the ability to
 * turn left or right, and provides a symbol representation for each direction.
 *  Direction class encapsulates the current direction of an object and
 *  * provides methods to rotate the direction either left (counterclockwise) or right (clockwise).

 */
public class Direction {
    public enum CardinalDirection {
        NORTH, SOUTH, EAST, WEST;

        public String symbolForDirection() {
            return switch (this) {
                case NORTH -> "⬆️";
                case SOUTH -> "⬇️";
                case EAST -> "➡️️";
                case WEST -> "⬅️";
            };
        }
    }

    private CardinalDirection direction;

    public Direction(CardinalDirection direction) {
        this.direction = direction;
    }

    public CardinalDirection getDirection() {
        return direction;
    }

    public void turnLeft() {
        switch (direction) {
            case NORTH: direction = CardinalDirection.WEST; break;
            case WEST: direction = CardinalDirection.SOUTH; break;
            case SOUTH: direction = CardinalDirection.EAST; break;
            case EAST: direction = CardinalDirection.NORTH; break;
        }
    }

    public void turnRight() {
        switch (direction) {
            case NORTH: direction = CardinalDirection.EAST; break;
            case EAST: direction = CardinalDirection.SOUTH; break;
            case SOUTH: direction = CardinalDirection.WEST; break;
            case WEST: direction = CardinalDirection.NORTH; break;
        }
    }

    @Override
    public String toString() {
        return direction.name();
    }
}