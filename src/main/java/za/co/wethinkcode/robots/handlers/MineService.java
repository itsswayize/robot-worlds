package za.co.wethinkcode.robots.handlers;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.MoveCommand;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

import java.util.Timer;
import java.util.TimerTask;

public class MineService {
    private final World world;
    private final MovementService movementService;

    public MineService(World world, MovementService movementService) {
        this.world = world;
        this.movementService = movementService;
    }

    public void handleSetMine(za.co.wethinkcode.robots.commands.SetMineCommand cmd, za.co.wethinkcode.robots.handlers.CommandHandler.CompletionHandler completionHandler) {
        String robotName = cmd.robot.getName();
        Robot robot = world.findRobot(robotName);
        if (robot == null) {
            completionHandler.onComplete(new Response("ERROR", "Could not find robot: " + robotName));
            return;
        }

        // Only miners can set mines
        if (!"miner".equalsIgnoreCase(robot.getMake())) {
            completionHandler.onComplete(new Response("ERROR", "Only miners can set mines."));
            return;
        }

        if (robot.isPlacingMine()) {
            completionHandler.onComplete(new Response("ERROR", robot.getName() + " is already placing a mine."));
            return;
        }

        // Start placing: disable shields and mark
        robot.startPlacingMine();
        // Immediate response to initiating placement
        Response startResp = new Response("OK", robot.getName() + " started placing a mine.");
        completionHandler.onComplete(startResp);

        // Schedule the completion after configured time
        int delaySeconds = Math.max(1, world.getMineSetTimeSeconds());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // Place mine at current robot position
                int x = robot.getX();
                int y = robot.getY();
                Obstacle mine = new Obstacle(ObstacleType.MINE, x, y, 1, 1);
                world.addObstacle(mine);

                // Finish placing: restore shields
                robot.finishPlacingMine();

                // Attempt to move robot forward by 1 step automatically
                MoveCommand autoMove = new MoveCommand(robot, world, "forward", new String[]{"1"});
                Response moveResp = movementService.handleMove(autoMove);

                // If movement failed and mine still present, robot steps on its own mine -> immediate death
                boolean mineStillPresent = world.isObstacleAt(x, y, ObstacleType.MINE);
                if (!moveResp.isOKResponse() && mineStillPresent) {
                    // Robot steps on own mine -> die immediately
                    robot.status = Robot.RobotStatus.Dead;
                    // remove mine after triggering
                    world.removeObstacleAt(x, y);

                    // Build a completion response to notify the client about the final state
                    Response done = new Response("ERROR", robot.getName() + " died from a mine.");
                    world.stateForRobot(robot, done);
                    // Notify world/command handler so ownership is cleared and client disconnected
                    try {
                        world.notifyRobotDeath(robot.getName());
                    } catch (Exception ignored) {}

                    // Notify via completion handler
                    completionHandler.onComplete(done);
                    return;
                }

                // Build a completion response to notify the client about the final state
                Response done = new Response("OK", robot.getName() + " finished placing a mine.");
                world.stateForRobot(robot, done);
                // Notify via completion handler
                completionHandler.onComplete(done);
            }
        }, delaySeconds * 1000L);
    }
}
