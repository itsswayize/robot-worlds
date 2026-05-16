package za.co.wethinkcode.robots.handlers;

import java.util.*;

import org.json.JSONObject;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.Command;
import za.co.wethinkcode.robots.commands.DisconnectCommand;
import za.co.wethinkcode.robots.commands.FireCommand;
import za.co.wethinkcode.robots.commands.HelpCommand;
import za.co.wethinkcode.robots.commands.LaunchCommand;
import za.co.wethinkcode.robots.commands.LookCommand;
import za.co.wethinkcode.robots.commands.MoveCommand;
import za.co.wethinkcode.robots.commands.OrientationCommand;
import za.co.wethinkcode.robots.commands.ReloadCommand;
import za.co.wethinkcode.robots.commands.RepairCommand;
import za.co.wethinkcode.robots.commands.ShutdownCommand;
import za.co.wethinkcode.robots.commands.StateCommand;
import za.co.wethinkcode.robots.commands.TurnCommand;
import za.co.wethinkcode.robots.commands.SetMineCommand;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;

public class CommandHandler {
    @FunctionalInterface
    public interface CompletionHandler {
        void onComplete(Response response);
    }

    private final World world;
    private final Map<String, HashMap<String, String>> clientRobots = new HashMap<>();
    private final VisibilityHandler visibilityHandler;
    private final MovementService movementService;
    private final FireService fireService;
    private final ReloadService reloadService;

    // New service fields for delegated handlers
    private final LaunchService launchService;
    private final LookService lookService;
    private final StateService stateService;
    private final TurnService turnService;
    private final ShutdownService shutdownService;
    private final MineService mineService;

    private final Map<Class<? extends Command>, CommandExecutor> executors = new HashMap<>();

    @FunctionalInterface
    private interface CommandExecutor {
        void execute(Command command, String clientId, CompletionHandler handler);
    }

    public CommandHandler(World world) {
        this.world = world;

        this.visibilityHandler = new VisibilityHandler(
                world.getRobots(),
                world.getObstacles(),
                world.getHalfWidth(),
                world.getHalfHeight(),
                world.getVisibility(),
                world
        );

        this.movementService = new MovementService(world);
        this.fireService = new FireService(world);
        this.reloadService = new ReloadService(world);

        // Instantiate new services
        this.launchService = new LaunchService(world);
        this.lookService = new LookService(world, visibilityHandler);
        this.stateService = new StateService(world);
        this.turnService = new TurnService(world);
        this.shutdownService = new ShutdownService(world, clientRobots);
        this.mineService = new MineService(world, movementService);

        // Register handlers in a map to reduce complexity in `handle`
        executors.put(HelpCommand.class, (c, clientId, h) -> handleHelp((HelpCommand) c, h));
        executors.put(LaunchCommand.class, (c, clientId, h) -> {
            Response resp = launchService.handleLaunch((LaunchCommand) c, clientId, clientRobots);
            h.onComplete(resp);
        });
        executors.put(StateCommand.class, (c, clientId, h) -> {
            StateCommand sc = (StateCommand) c;
            Response resp = stateService.handleState(sc, sc.robot.getName());
            h.onComplete(resp);
        });
        executors.put(OrientationCommand.class, (c, clientId, h) -> {
            OrientationCommand oc = (OrientationCommand) c; handleOrientation(oc, h);
        });
        executors.put(LookCommand.class, (c, clientId, h) -> {
            Response resp = lookService.handleLook((LookCommand) c);
            h.onComplete(resp);
        });
        executors.put(MoveCommand.class, (c, clientId, h) -> {
            System.out.println("[CommandHandler] Dispatching MoveCommand for client=" + clientId + " command=" + c.commandName());
            Response resp = movementService.handleMove((MoveCommand) c);
            h.onComplete(resp);
        });
        executors.put(TurnCommand.class, (c, clientId, h) -> {
            Response resp = turnService.handleTurn((TurnCommand) c);
            h.onComplete(resp);
        });
        executors.put(ShutdownCommand.class, (c, clientId, h) -> {
            Response resp = shutdownService.handleShutdown((ShutdownCommand) c);
            h.onComplete(resp);
        });
        executors.put(DisconnectCommand.class, (c, clientId, h) -> handleDisconnect(clientId, h));
        executors.put(FireCommand.class, (c, clientId, h) -> {
            FireCommand fc = (FireCommand) c; Response resp = fireService.handleFire(fc.robot); h.onComplete(resp);
        });
        executors.put(ReloadCommand.class, (c, clientId, h) -> {
            ReloadCommand rc = (ReloadCommand) c; reloadService.handleReload(rc.robot, h);
        });
        executors.put(RepairCommand.class, (c, clientId, h) -> handleRepair((RepairCommand) c, h));
        executors.put(SetMineCommand.class, (c, clientId, h) -> {
            mineService.handleSetMine((SetMineCommand) c, h);
        });
    }

    /**
     * Handles commands by directing each command to its specific handling logic.
     * This method dispatches using a map of executors to reduce cyclomatic complexity.
     */
    public void handle(Command command, String clientId, CompletionHandler handler) {
        // Return a proper ERROR response for null/unknown commands so clients/tests get a JSON "result" + "data.message"
        if (command == null) {
            handler.onComplete(createUnsupportedCommandResponse());
            return;
        }

        System.out.println("Executing command: " + command.commandName());

        CommandExecutor exec = executors.get(command.getClass());
        if (exec != null) {
            exec.execute(command, clientId, handler);
        } else {
            handler.onComplete(createUnsupportedCommandResponse());
        }
    }

    // Helper: create a standardized Unsupported-Command response (keeps messages consistent)
    private Response createUnsupportedCommandResponse() {
        Response resp = new Response("ERROR", "Unsupported command");
        JSONObject data = new JSONObject();
        data.put("message", "Unsupported command");
        resp.object.put("data", data);
        return resp;
    }

 // java
     private void handleDisconnect(String clientId, CompletionHandler handler) {
        // Take ownership map and remove it from tracking
        HashMap<String, String> owned = clientRobots.remove(clientId);

        if (owned == null || owned.isEmpty()) {
            handler.onComplete(new Response("OK", "Client disconnected. No robots to remove."));
            return;
        }

        String message = removeOwnedRobotsMessage(owned);
        handler.onComplete(new Response("OK", message));
    }

    // Helper: remove robots and build a human readable message
    private String removeOwnedRobotsMessage(HashMap<String, String> owned) {
        List<String> results = new ArrayList<>();
        for (String robotName : List.copyOf(owned.keySet())) {
            results.add(removeRobotAndReport(robotName));
        }

        if (results.isEmpty()) {
            return "Client disconnected. No robots removed.";
        }

        return "Client disconnected. Removed robots: " + String.join(", ", results);
    }

    // Return a single report string for a robot removal attempt (either the name or a not-removed note)
    private String removeRobotAndReport(String robotName) {
        Response resp = world.removeRobot(robotName);
        boolean ok = resp != null && resp.isOKResponse();
        return ok ? robotName : robotName + " (not removed)";
    }


    private void handleHelp(HelpCommand ignored, CompletionHandler handler) {
        String helpText = String.join("\n",
                """
                         🌸🤖✨ I CAN UNDERSTAND THESE COMMANDS 🌸🤖✨
                        ┌────────────────────┬──────────────────────────────────────────────┐
                         COMMAND             | DESCRIPTION
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |1.❓ help           | Show this help message 🆘
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |2.🧭 orientation    | What direction you are facing
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |3.forward <name> <n>| Move forward by n steps (max 5) ⏩
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |4.back <name> <n>   | Move backward by n steps (max 5) ⏪
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |5. left             | Turn left 🔄  e.g. turn <name> left
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |6. right            | Turn right 🔁 e.g. turn <name> right
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |7. look             | List visible objects 👀
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |8. state            | Show current robot status 📊
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |9. fire             | Fire a shot (tank or sniper rules) 🔫
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |7. reload           | Refill your ammo to maximum 🔄💥
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |8. repair           | Restore your shields (takes time) 🛠️🛡️
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |9. disconnect       | Disconnect the client completely 🫤
                        ├────────────────────┼──────────────────────────────────────────────┤
                        |10. launch          | Launch another robot 🚀 e.g. <type> <name>
                        ├────────────────────┼──────────────────────────────────────────────┤

                        """
        );

        handler.onComplete(new Response("OK", helpText));
    }

    // Movement-related helpers were extracted to MovementService; removed duplicates from CommandHandler

    private void handleOrientation(OrientationCommand command, CompletionHandler completionHandler) {
        Robot robot = world.findRobot(command.robot.getName());
        if (robot != null) {
            String direction = robot.orientation(); // Get the current direction
            completionHandler.onComplete(new Response("OK", robot.getName() + " is facing " + direction + "."));
        } else {
            completionHandler.onComplete(makeNotFoundResponse(command.robot.getName()));
        }
    }

    // Helper to build a standardized "Could not find robot" error Response with a data.message field
    private Response makeNotFoundResponse(String robotName) {
        Response resp = new Response("ERROR", "Could not find robot: " + robotName);
        JSONObject data = new JSONObject();
        data.put("message", "Could not find robot: " + robotName);
        resp.object.put("data", data);
        return resp;
    }


    public synchronized void removeAllRobotsForClient(String clientId) {
        HashMap<String, String> owned = clientRobots.get(clientId);
        if (owned == null || owned.isEmpty()) return;

        // copy keys to avoid concurrent modification
        List<String> names = new ArrayList<>(owned.keySet());
        for (String robotName : names) {
            world.removeRobot(robotName);
             // Print short server message for each removed robot
             System.out.println("server: Client disconnected removing " + robotName);
             // also remove from ownership maps
             owned.remove(robotName);
             world.displayWorld();
         }

        if (owned.isEmpty()) {
            clientRobots.remove(clientId);
        } else {
            clientRobots.put(clientId, owned);
        }
    }

    private void handleRepair(RepairCommand command, CompletionHandler completionHandler) {
        Robot robot = world.findRobot(command.robot.getName());
        if (robot == null) {
            completionHandler.onComplete(new Response("ERROR", "Robot not found: " + command.robot.getName()));
            return;
        }

        // Check if the robot is already repairing
        if (robot.isRepairing()) {
            completionHandler.onComplete(new Response("ERROR", robot.getName() + " is already repairing."));
            return;
        }

        robot.setRepairing(true);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                robot.setShields(world.getMaxShieldStrength()); // Repair to max shields
                robot.setRepairing(false);
                Response response = new Response("OK", robot.getName() + " has finished repairing");
                world.stateForRobot(robot, response);

                completionHandler.onComplete(response);
            }
        }, world.getShieldRepairTime() * 1000L); // Repair time in milliseconds

        completionHandler.onComplete(new Response("OK", robot.getName() + " is now repairing."));
    }

    // Called when a robot has died (e.g., fell into a pit or hit a mine).
    // This will remove the robot from the world and disconnect the owning client if any.
    public synchronized void handleRobotDeath(String robotName) {
        if (robotName == null || robotName.isEmpty()) return;

        // Remove robot from world (if present)
        try {
            world.removeRobot(robotName);
        } catch (Exception ignored) {}

        // Find owning client (if any) and remove ownership record
        String owningClient = null;
        for (Map.Entry<String, HashMap<String, String>> entry : clientRobots.entrySet()) {
            HashMap<String, String> owned = entry.getValue();
            if (owned != null && owned.containsKey(robotName)) {
                owningClient = entry.getKey();
                owned.remove(robotName);
                if (owned.isEmpty()) clientRobots.remove(owningClient);
                break;
            }
        }

        // If we found the owning client, instruct the Server to disconnect that client
        if (owningClient != null) {
            try {
                // Make a final copy for use inside the lambda (must be effectively final in Java lambdas)
                final String clientToDisconnect = owningClient;
                // First, reuse the existing Disconnect command handling to remove any owned robots
                // and produce the usual disconnect response. Then send that response to the client
                // and ensure the socket is closed via Server.sendDisconnectToClient.
                handleDisconnect(clientToDisconnect, response -> {
                    try {
                        za.co.wethinkcode.robots.server.Server.sendDisconnectToClient(clientToDisconnect, response);
                    } catch (Exception e) {
                        System.out.println("[CommandHandler] Failed to send disconnect response for client " + clientToDisconnect + ": " + e.getMessage());
                        // As a fallback, instruct Server to disconnect the client if sending failed
                        try { za.co.wethinkcode.robots.server.Server.disconnectClientById(clientToDisconnect); } catch (Exception ignored) {}
                    }
                });
             } catch (Exception e) {
                 System.out.println("[CommandHandler] Failed to disconnect owning client " + owningClient + ": " + e.getMessage());
             }
         }
     }
 }
