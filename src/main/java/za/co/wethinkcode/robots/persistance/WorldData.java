package za.co.wethinkcode.robots.persistance;

/**
 * Data object for the worlds table.
 */
public class WorldData {
    private int id;
    private String name;
    private int width;
    private int height;

    public WorldData() {}

    public WorldData(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
}