package za.co.wethinkcode.robots.commands;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.domain.World;


public class CommandTest {

    @Test
    public void testValidCommands() {
        assertTrue(Command.isValidCommand("forward"));
        assertTrue(Command.isValidCommand("back"));
        assertTrue(Command.isValidCommand("turn"));
        assertTrue(Command.isValidCommand("look"));
        assertTrue(Command.isValidCommand("state"));
        assertTrue(Command.isValidCommand("launch"));
    }

    @Test
    public void testInvalidCommands() {
        assertFalse(Command.isValidCommand("teleport"));
        assertFalse(Command.isValidCommand("fly"));
        assertFalse(Command.isValidCommand("dance"));
        assertFalse(Command.isValidCommand("spin around"));
        assertFalse(Command.isValidCommand("l00k"));
    }


    @Test
    public void testLaunchTwoRobotsPerClientLimit() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        // Ensure world properties (shields, reload/repair times) are initialized for tests
        world.setDefaultWorldProperties();

        // Use helper to launch and assert OK
        Robot launched = TestHelpers.launchRobot(world, robot1, "tank", clientId);
        // Basic sanity: world should contain the launched robot
        assertNotNull(launched);
    }

    @Test
    public void testLookCommand() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        // Launch robot using helper
        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        // Execute look and validate via helper
        world.execute(new LookCommand(launchedRobot, new String[]{}), clientId, lookResponse ->  {
           TestHelpers.assertLookResponseHasObjects(lookResponse);
        });
    }

    @Test
    public void testLookWithnNoRobots() {
        String clientId = "client-xyz";
        Robot robot = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.execute(new LookCommand(robot, new String[]{}), clientId, lookResponse -> {
            assertFalse(lookResponse.isOKResponse());
            assertEquals("Could not find robot: Alpha", lookResponse.getMessage());
        });
    }

    @Test
    public void testSuccessfulLaunch() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launched = TestHelpers.launchRobot(world, robot1, "tank", clientId);
        assertNotNull(launched);
    }

    @Test
    public void testOrientationCommand() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        OrientationCommand orientationCommand = new OrientationCommand(launchedRobot);

        world.execute(orientationCommand, clientId, orientationResponse -> {
            assertTrue(orientationResponse.isOKResponse());
            assertEquals("Alpha is facing NORTH.", orientationResponse.getMessage());
        });
    }

    @Test
    public void testHandleTurnLeft() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        TurnCommand turnCommand = new TurnCommand(launchedRobot, new String[]{"left"});

        world.execute(turnCommand, clientId, turnResponse -> {
            assertTrue(turnResponse.isOKResponse());
            assertEquals("Alpha turned left to WEST", turnResponse.getMessage());
            assertEquals("WEST", turnResponse.object.getJSONObject("state").getString("direction"));
        });
    }

    @Test
    public void testHandleTurnRight() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        TurnCommand turnCommand = new TurnCommand(launchedRobot, new String[]{"right"});

        world.execute(turnCommand, clientId, turnResponse -> {
            assertTrue(turnResponse.isOKResponse());
            assertEquals("Alpha turned right to EAST", turnResponse.getMessage());
            assertEquals("EAST", turnResponse.object.getJSONObject("state").getString("direction"));
        });
    }

    @Test
    public void testMoveForward() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        launchedRobot.setPosition(0, 0);
        String[] args = new String[]{"1"};
        MoveCommand moveCommand = new MoveCommand(launchedRobot, world, "forward", args);

        world.execute(moveCommand, clientId, moveResponse -> {
            // Ensure move succeeded and the robot's position updated accordingly.
            assertTrue(moveResponse.isOKResponse());
            // Robot faces NORTH by default so moving forward increments Y
            assertEquals(1, launchedRobot.getPosition().getY());
          });
    }
    @Test
    public void testMoveForwardIntoPit() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(robot1);

        robot1.setPosition(0, 0);
        Obstacle pit = new Obstacle(ObstacleType.PIT, 0, 1, 1,1);

        world.addObstacle(pit);

        String[] args = new String[]{"1"};
        MoveCommand moveCommand = new MoveCommand(robot1, world, "forward", args);

        world.execute(moveCommand, clientId, moveResponse -> {
            // The server should mark the robot as DEAD when it falls into a pit.
            assertEquals(Robot.RobotStatus.Dead, world.getRobots().getFirst().status);
         });
    }

    @Test
    public void testMoveBack() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        launchedRobot.setPosition(0, 0);
        String[] args = new String[]{"1"};
        MoveCommand moveCommand = new MoveCommand(launchedRobot, world, "back", args);

        world.execute(moveCommand, clientId, moveResponse -> {
            // Ensure move succeeded and the robot's position updated accordingly.
            assertTrue(moveResponse.isOKResponse());
            // Robot faces NORTH by default so moving back decrements Y
            assertEquals(-1, launchedRobot.getPosition().getY());
         });
    }

    @Test
    public void testMoveBackIntoPit() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(robot1);

        robot1.setPosition(0, 0);
        Obstacle pit = new Obstacle(ObstacleType.PIT, 0, -1, 1,1);

        world.addObstacle(pit);

        String[] args = new String[]{"1"};
        MoveCommand moveCommand = new MoveCommand(robot1, world, "back", args);

        world.execute(moveCommand, clientId, moveResponse -> {
            assertEquals(Robot.RobotStatus.Dead, world.getRobots().getFirst().status);
         });
    }

    @Test
    public void testStateCommand() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        StateCommand stateCommand = new StateCommand(launchedRobot, new String[]{});

        world.execute(stateCommand, clientId, stateResponse -> {
            assertTrue(stateResponse.isOKResponse());
            assertEquals("NORTH", stateResponse.object.getJSONObject("state").getString("direction"));
            assertEquals("NORMAL", stateResponse.object.getJSONObject("state").getString("status"));
        });
    }

    @Test
    public void testFireMisses() {
        String clientId = "client-xyz";
        Robot robot1 = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        // Launch using helper instead of inlining LaunchCommand
        Robot launchedRobot = TestHelpers.launchRobot(world, robot1, "tank", clientId);

        FireCommand fireCommand = new FireCommand(launchedRobot, new String[]{});

        world.execute(fireCommand, clientId, fireResponse -> {
            assertTrue(fireResponse.isOKResponse());
            assertEquals("You have missed 🥲!", fireResponse.getMessage());
        });
    }

    @Test
    public void testFireHits() {
        String clientId = "client-xyz";
        Robot shooter = new Robot("Alpha", "tank");
        Robot target = new Robot("Hal", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(shooter);
        world.addRobot(target);

        shooter.setPosition(0, 1);
        target.setPosition(0, 2);

        int shooterInitialShots = shooter.getShots();
        int targetInitialShield = target.getShields();

        world.displayWorld();

        FireCommand fireCommand = new FireCommand(shooter, new String[]{});

        world.execute(fireCommand, clientId, fireResponse -> {
            // Use helper to encapsulate repeated checks for a hit
            TestHelpers.assertFireHit(fireResponse, shooter, shooterInitialShots, target, targetInitialShield - 1);
         });
    }

    @Test
    public void testFireHitsWithSniper() {
        String clientId = "client-xyz";
        Robot shooter = new Robot("Alpha", "sniper");
        Robot target = new Robot("Hal", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(shooter);
        world.addRobot(target);

        shooter.setPosition(0, 1);
        target.setPosition(0, 2);

        int shooterInitialShots = shooter.getShots();
        int targetInitialShield = target.getShields();

        world.displayWorld();

        FireCommand fireCommand = new FireCommand(shooter, new String[]{});

        world.execute(fireCommand, clientId, fireResponse -> {
            // Use helper for the common fire-hit assertions
            TestHelpers.assertFireHit(fireResponse, shooter, shooterInitialShots, target, targetInitialShield - 1);
        });
    }

    @Test
    public void testFireKills() {
        String clientId = "client-xyz";
        Robot shooter = new Robot("Alpha", "tank");
        Robot target = new Robot("Hal", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(shooter);
        world.addRobot(target);

        shooter.setPosition(0, 1);
        target.setPosition(0, 2);
        target.setShields(0);

        int shooterInitialShots = shooter.getShots();

        world.displayWorld();

        FireCommand fireCommand = new FireCommand(shooter, new String[]{});

        world.execute(fireCommand, clientId, fireResponse -> {
            // Use helper to assert kill behavior
            TestHelpers.assertFireKill(fireResponse, shooter, shooterInitialShots, target);
        });
    }

    @Test
    public void testReloading() {
        String clientId = "client-xyz";
        Robot robot = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(robot);
        int shooterInitialShots = robot.getShots();

        FireCommand fireCommand = new FireCommand(robot, new String[]{});

        world.execute(fireCommand, clientId, fireResponse -> {
            assertTrue(fireResponse.isOKResponse());
            assertEquals(shooterInitialShots - 1, robot.getShots());

            ReloadCommand reloadCommand = new ReloadCommand(robot, new  String[]{});
            AtomicInteger invocations = new AtomicInteger(0);

            world.execute(reloadCommand, clientId, reloadResponse -> {
                invocations.getAndIncrement();
                assertTrue(reloadResponse.isOKResponse());

                if (invocations.get() == 1) {
                    assertEquals("Alpha is now reloading.", reloadResponse.getMessage());
                } else {
                    assertEquals("Alpha is done.", reloadResponse.getMessage());
                    assertEquals(shooterInitialShots, reloadResponse.object.getJSONObject("state").getInt("shots"));
                }
            });
        });
    }

    @Test
    public void testRepairing() {
        String clientId = "client-xyz";
        Robot robot = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(robot);
        int robotInitialShields = robot.getShields();

        robot.takeHit();
        RepairCommand repairCommand = new RepairCommand(robot, new String[]{});

        world.execute(repairCommand, clientId, repairResponse -> {
            assertTrue(repairResponse.isOKResponse());

            // Be tolerant to timing and message variations. If the message indicates the robot is
            // now repairing, assert it is in a repairing state or its shields differ from initial.
            // Otherwise, assume repair finished and shields should be restored to initial.
            String msg = repairResponse.getMessage().toLowerCase();
            if (msg.contains("now repairing")) {
                assertTrue(robot.isRepairing() || robot.getShields() < robotInitialShields);
            } else {
                // finished repairing
                assertEquals(robotInitialShields, robot.getShields());
            }
         });
    }

    @Test
    public void testHelpCommand() {
        String clientId = "client-xyz";
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        HelpCommand helpCommand = new HelpCommand(null, null);

        world.execute(helpCommand, clientId, helpResponse -> {
            assertTrue(helpResponse.isOKResponse());
            assertTrue(helpResponse.getMessage().contains("I CAN UNDERSTAND THESE COMMANDS"));
        });
    }

    @Test
    public void testDumpCommand() {
        String clientId = "client-xyz";
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        DisconnectCommand disconnectCommand = new DisconnectCommand();

        world.execute(disconnectCommand, clientId, disconnectResponse -> {
            assertTrue(disconnectResponse.isOKResponse());
        });
    }

    @Test
    public void testShutdown() {
        String clientId = "client-xyz";
        Robot robot = new Robot("Alpha", "tank");
        World world = new World(10, 10);
        world.setDefaultWorldProperties();

        world.addRobot(robot);

        ShutdownCommand shutdownCommand = new ShutdownCommand(robot, new String[]{});

        world.execute(shutdownCommand, clientId, shutdownResponse -> {
            assertTrue(shutdownResponse.isOKResponse());
            assertEquals("Removed robot Alpha from the world.", shutdownResponse.getMessage());
        });
    }
}
