package za.co.wethinkcode.robots.commands;

import za.co.wethinkcode.robots.persistance.WorldRepository;
import za.co.wethinkcode.robots.domain.World;
import za.co.wethinkcode.robots.server.Server;

public class RestoreCommand extends Command {
    private final String worldName;

    public RestoreCommand(String worldName) {
        super(null, new String[]{worldName});
        this.worldName = worldName;
    }

    @Override
    public String commandName() {
        return "restore";
    }

    @Override
    public String execute() {
        try (WorldRepository repo = new WorldRepository()) {
            World loadedWorld = repo.loadWorld(worldName);

            if (loadedWorld == null) {
                return "No saved world named '" + worldName + "'.";
            }

            World.initializeInstance(loadedWorld);
            Server.disconnectAllClients();  // Kick all robots, new world loaded
            return "World '" + worldName + "' restored successfully.";
        } catch (Exception e) {
            return "Error restoring world: " + e.getMessage();
        }
    }
}