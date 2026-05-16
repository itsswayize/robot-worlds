package za.co.wethinkcode.robots.domain;

public record Obstacle(ObstacleType type, int x, int y, int width, int height) {
    public int getMaxY() {
        return y + height;
    }

    public int getMaxX() {
        return x + width;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean contains(Position position) {
        int px = position.getX();
        int py = position.getY();
        return px >= this.x && px < this.getMaxX() &&
                py >= this.y && py < this.getMaxY();
    }

    public boolean overlaps(Obstacle other) {
        return this.x <= other.getMaxX() && this.getMaxX() >= other.x &&
                this.y <= other.getMaxY() && this.getMaxY() >= other.y;
    }

    @Override
    public String toString() {
        return type + "[" + x + ", " + y + "] (" + width + "x" + height + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == Obstacle.class) {
            Obstacle otherObstacle = (Obstacle) obj;

            return otherObstacle.type == this.type
                    && otherObstacle.x == this.x
                    && otherObstacle.y == this.y
                    && otherObstacle.getMaxY() == this.getMaxY()
                    && otherObstacle.getMaxX() == this.getMaxX();
        } else {
            return false;
        }
    }
}
