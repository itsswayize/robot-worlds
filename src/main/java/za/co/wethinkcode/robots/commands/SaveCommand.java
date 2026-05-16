package za.co.wethinkcode.robots.commands;

import za.co.wethinkcode.robots.persistance.WorldRepository;
import za.co.wethinkcode.robots.domain.World;

public class SaveCommand extends Command {
    private final World world;
    private final String worldName;

    public SaveCommand(World world, String worldName) {
        super(null, new String[]{worldName});
        this.world = world;
        this.worldName = worldName;
    }

    @Override
    public String commandName() {
        return "save";
    }

    @Override
    public String execute() {
        try (WorldRepository repo = new WorldRepository()) {
            repo.saveWorld(worldName, world);
            return "World '" + worldName + "' saved successfully.";
        } catch (Exception e) {
            return "Error saving world: " + e.getMessage();
        }
    }
}