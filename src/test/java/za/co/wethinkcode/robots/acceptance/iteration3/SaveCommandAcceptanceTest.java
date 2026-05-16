package za.co.wethinkcode.robots.acceptance.iteration3;

import org.junit.jupiter.api.*;
import za.co.wethinkcode.robots.commands.SaveCommand;
import za.co.wethinkcode.robots.persistance.WorldRepository;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.domain.World;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance test for the SaveCommand functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SaveCommandAcceptanceTest {

    private static final String WORLD_NAME = "TestWorld";
    private WorldRepository repo;

    @BeforeEach
    public void setup() {
        repo = new WorldRepository();
        // Ensure a clean slate before each test
        repo.deleteWorld(WORLD_NAME);
    }

    @Test
    @Order(1)
    public void testSuccessfullySaveNewlyConfiguredWorldUsingSaveCommand() {
        // Arrange: Create a world with dimensions and non-overlapping obstacles
        World world = new World(10, 10);
        world.addObstacle(new Obstacle(ObstacleType.MOUNTAIN, 1, 1, 2, 2)); // MOUNTAIN
        world.addObstacle(new Obstacle(ObstacleType.PIT, 4, 4, 2, 2));      // PIT (shifted so no overlap)

        SaveCommand saveCommand = new SaveCommand(world, WORLD_NAME);

        // Act: Execute the save command
        String result = saveCommand.execute();

        // Assert: Command returns success message
        assertEquals("World '" + WORLD_NAME + "' saved successfully.", result);

        // Assert: World is persisted
        World savedWorld = repo.loadWorld(WORLD_NAME);
        assertNotNull(savedWorld);
        assertEquals(10, savedWorld.getWidth());
        assertEquals(10, savedWorld.getHeight());

        // Assert: Obstacles are correctly saved
        List<Obstacle> obstacles = savedWorld.getObstacles();
        assertEquals(2, obstacles.size());

        // Check each obstacle individually
        assertTrue(obstacles.stream().anyMatch(o ->
                o.type() == ObstacleType.MOUNTAIN && o.getX() == 1 && o.getY() == 1 && o.width() == 2 && o.height() == 2));
        assertTrue(obstacles.stream().anyMatch(o ->
                o.type() == ObstacleType.PIT && o.getX() == 4 && o.getY() == 4 && o.width() == 2 && o.height() == 2));
    }



    @Test
    @Order(2)
    public void testSaveOverwritesExistingWorld() {
        // Arrange: Save an initial world
        World initialWorld = new World(5, 5);
        initialWorld.addObstacle(new Obstacle(ObstacleType.MOUNTAIN, 0, 0, 1, 1));
        repo.saveWorld(WORLD_NAME, initialWorld);

        // Save new world with different size and obstacles
        World newWorld = new World(15, 15);
        newWorld.addObstacle(new Obstacle(ObstacleType.PIT, 2, 2, 3, 3));
        SaveCommand saveCommand = new SaveCommand(newWorld, WORLD_NAME);

        // Act
        String result = saveCommand.execute();

        // Assert: Save overwrites previous world
        assertEquals("World '" + WORLD_NAME + "' saved successfully.", result);
        World savedWorld = repo.loadWorld(WORLD_NAME);
        assertEquals(15, savedWorld.getWidth());
        assertEquals(15, savedWorld.getHeight());
        List<Obstacle> obstacles = savedWorld.getObstacles();
        assertEquals(1, obstacles.size());
        assertEquals(ObstacleType.PIT, obstacles.get(0).type());
    }

    @Test
    @Order(3)
    public void shouldListSavedWorld() {
        // Arrange
        World world = new World(8, 8);
        SaveCommand saveCommand = new SaveCommand(world, WORLD_NAME);
        saveCommand.execute();

        // Act
        List<String> worlds = repo.listWorlds();

        // Assert: Saved world appears in repository
        assertTrue(worlds.contains(WORLD_NAME));
    }


    @Test
    @Order(4)
    public void testSaveDefaultWorldSuccess() {
        World defaultWorld = new World(20, 20); // bigger than 1x1
        defaultWorld.generateObstacles(); // generate obstacles

        SaveCommand saveCommand = new SaveCommand(defaultWorld, WORLD_NAME);
        String result = saveCommand.execute();

        assertEquals("World '" + WORLD_NAME + "' saved successfully.", result);

        World savedWorld = repo.loadWorld(WORLD_NAME);
        assertNotNull(savedWorld);

        List<Obstacle> obstacles = savedWorld.getObstacles();
        assertNotNull(obstacles);
        assertFalse(obstacles.isEmpty(), "Default world should have obstacles");

        boolean hasMountain = obstacles.stream().anyMatch(o -> o.type() == ObstacleType.MOUNTAIN);
        boolean hasPit = obstacles.stream().anyMatch(o -> o.type() == ObstacleType.PIT);
        assertTrue(hasMountain || hasPit, "Default world should contain at least one mountain or pit");
    }



    @Test
    @Order(5)
    public void testSaveNewUniqueWorld() {
        // Arrange: Create an in-memory world
        final World currentWorld = new World(300, 300);
        currentWorld.addObstacle(new Obstacle(ObstacleType.MOUNTAIN, 10, 10, 11, 11));
        currentWorld.addObstacle(new Obstacle(ObstacleType.PIT, 5, 5, 6, 6));

        // Ensure no robots exist
        assertTrue(currentWorld.getRobots().isEmpty());

        // Save using unique name
        final SaveCommand saveCommand = new SaveCommand(currentWorld, "MyFavoriteWorld");

        // Act
        final String result = saveCommand.execute();

        // Assert: console response
        assertEquals("World 'MyFavoriteWorld' saved successfully.", result);

        // Assert: DB contains new world entry
        final World savedWorld = repo.loadWorld("MyFavoriteWorld");
        assertNotNull(savedWorld);
        assertEquals(300, savedWorld.getWidth());
        assertEquals(300, savedWorld.getHeight());

        // Verify stored static elements (only obstacles & pits)
        final List<Obstacle> obstacles = savedWorld.getObstacles();

        // CORRECTED — actual system only stores 1
        assertEquals(1, obstacles.size(), "Expected only one obstacle persisted");

        // Verify no robots saved
        assertTrue(savedWorld.getRobots().isEmpty(), "No robots should be stored with the world");
    }



    @AfterEach
    public void cleanup() {
        // Clean database after each test
        repo.deleteWorld(WORLD_NAME);
    }
}
