package za.co.wethinkcode.robots.server;

import za.co.wethinkcode.robots.domain.World;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
/**
 * Loads configuration properties from a resource file and applies them to the World instance.
 * Handles missing or invalid files by falling back to default world settings.
 */
public class ConfigLoader {

    public Properties loadConfig(String resourcePath) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new FileNotFoundException("Property file not found in the classpath");
            }
            properties.load(input);
        }
        return properties;
    }

    public void applyConfigToWorld(World world, String resourcePath) {
        System.out.println("Loading configuration...");
        try {
            Properties properties = loadConfig(resourcePath);
            world.setDimensions(
                    Integer.parseInt(properties.getProperty("world.width")),
                    Integer.parseInt(properties.getProperty("world.height"))
            );
            world.setWorldProperties(
                    Integer.parseInt(properties.getProperty("shield_repair_time")),
                    Integer.parseInt(properties.getProperty("weapon_reload_time")),
                    Integer.parseInt(properties.getProperty("max_shield_strength")),
                    Integer.parseInt(properties.getProperty("visibility"))
            );
             // Optional mine set time
            String mineTime = properties.getProperty("mine_set_time_seconds");
            if (mineTime != null) {
                try {
                    int t = Integer.parseInt(mineTime);
                    world.setMineSetTimeSeconds(t);
                } catch (NumberFormatException ignored) { }
            }
            System.out.println("World successfully loaded with dimensions: " + world.getWidth() + " x " + world.getHeight());
        } catch (IOException e) {
            world.setDefaultDimensions();
            world.setDefaultWorldProperties();
            System.err.println("Could not load configuration: " + e.getMessage());
        }
    }
}