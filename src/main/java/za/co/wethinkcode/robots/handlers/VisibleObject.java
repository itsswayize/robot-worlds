package za.co.wethinkcode.robots.handlers;

import za.co.wethinkcode.robots.domain.Direction;

import java.util.Objects;

/**
 * Simple typed holder for visible objects reported by the visibility code.
 */
public class VisibleObject {
    private final Object object;
    private final Direction.CardinalDirection direction;
    private final int distance;
    private final VisibleType type;

    public VisibleObject(Object object, Direction.CardinalDirection direction, int distance, VisibleType type) {
        this.object = object;
        this.direction = direction;
        this.distance = distance;
        this.type = type == null ? VisibleType.UNKNOWN : type;
    }

    public Object getObject() {
        return object;
    }

    public Direction.CardinalDirection getDirection() {
        return direction;
    }

    public int getDistance() {
        return distance;
    }

    public VisibleType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisibleObject that)) return false;
        return Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }
}
