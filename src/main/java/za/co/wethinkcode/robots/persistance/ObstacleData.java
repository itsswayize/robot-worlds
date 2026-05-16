package za.co.wethinkcode.robots.persistance;

/**
 * Data object for the obstacles table.
 */
public class ObstacleData {
    private int id;
    private int worldId;
    private String type;
    private int x;
    private int y;
    private Integer width;
    private Integer height;

    public ObstacleData() {}

    public ObstacleData(int worldId, String type, int x, int y, Integer width, Integer height) {
        this.worldId = worldId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWorldId() { return worldId; }
    public void setWorldId(int worldId) { this.worldId = worldId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}