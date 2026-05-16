package za.co.wethinkcode.robots.acceptance.iteration3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.wethinkcode.robots.commands.RestoreCommand;
import za.co.wethinkcode.robots.commands.SaveCommand;
import za.co.wethinkcode.robots.persistance.WorldRepository;
import za.co.wethinkcode.robots.domain.World;

import static org.junit.jupiter.api.Assertions.*;

public class RestoreCommandAcceptanceTest {

    private static final String TEST_WORLD = "myworld";

    @Test
    public void restoreShouldWorkEndToEnd() {
        WorldRepository repo = new WorldRepository();
        repo.deleteWorld(TEST_WORLD);

        // Save a 100x100 world
        World worldToSave = new World(100, 100);
        new SaveCommand(worldToSave, TEST_WORLD).execute();

        // Simulate fresh server start with tiny world
        World.initializeInstance(new World(10, 10));

        // Restore
        String result = new RestoreCommand(TEST_WORLD).execute();
        assertEquals("World '" + TEST_WORLD + "' restored successfully.", result);

        // Verify it really changed
        World current = World.getInstance();
        assertEquals(100, current.getWidth());
        assertEquals(100, current.getHeight());
    }

    @AfterEach
    public void tearDown() {
        // THIS LINE FIXES EVERYTHING
        World.initializeInstance(new World(1, 1));
        new WorldRepository().deleteWorld(TEST_WORLD);
    }
}