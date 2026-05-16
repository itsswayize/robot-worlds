package za.co.wethinkcode.robots.server;
import za.co.wethinkcode.flow.Recorder;
import za.co.wethinkcode.robots.domain.Obstacle;
import za.co.wethinkcode.robots.domain.ObstacleType;
import za.co.wethinkcode.robots.domain.World;
import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.commands.LaunchCommand;
import za.co.wethinkcode.robots.handlers.LaunchService;
import za.co.wethinkcode.robots.handlers.CommandHandler;
import za.co.wethinkcode.robots.commands.Command;
import za.co.wethinkcode.robots.server.Response;

import org.json.JSONObject;
import org.json.JSONArray;

// Add missing imports for DB initialization
import java.sql.Connection;
import java.sql.DriverManager;
import net.lemnik.eodsql.QueryTool;
import za.co.wethinkcode.robots.persistance.WorldDaoInterface;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import spark.Spark;
import static spark.Spark.*;

/**
 * Main server class that accepts client connections and provides an admin console for server control.
 * Supports real-time robot monitoring, world state inspection, and graceful shutdown.
 */
public class Server {
     private static final Logger logger = Logger.getLogger(Server.class.getName());
     private static volatile boolean isRunning = true;
     private static ServerSocket serverSocket;
     private static CommandHandler commandHandler = new CommandHandler(World.getInstance());

    public static void main(String[] args) {
        int portNumber = 5000; // default

        // Defaults when running with explicit CLI args (we do NOT load config.properties in that case)
        final int CLI_DEFAULT_VISIBILITY = 5;
        final int CLI_DEFAULT_REPAIR_TIME = 5;
        final int CLI_DEFAULT_RELOAD_TIME = 3;
        final int CLI_DEFAULT_MAX_SHIELD = 5;

        boolean argsProvided = args != null && args.length > 0;
        int size = 1; // default world side when args are provided but -s not given
        Integer obstacleX = null, obstacleY = null;
        boolean oProvided = false; // whether -o was provided
        boolean oNone = false; // whether -o none was explicitly specified

        if (argsProvided) {
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                try {
                    switch (a) {
                        case "-p":
                            if (i + 1 < args.length) {
                                portNumber = Integer.parseInt(args[++i]);
                            } else {
                                logger.warning("-p requires a port number; using default 5000");
                            }
                            break;
                        case "-s":
                            if (i + 1 < args.length) {
                                size = Integer.parseInt(args[++i]);
                                if (size < 1) size = 1;
                            } else {
                                logger.warning("-s requires a size; using default 1");
                            }
                            break;
                        case "-o":
                            if (i + 1 < args.length) {
                                oProvided = true;
                                String val = args[++i];
                                if ("none".equalsIgnoreCase(val)) {
                                    oNone = true;
                                } else {
                                    String[] parts = val.split(",");
                                    if (parts.length == 2) {
                                        try {
                                            obstacleX = Integer.parseInt(parts[0]);
                                            obstacleY = Integer.parseInt(parts[1]);
                                        } catch (NumberFormatException nfe) {
                                            logger.warning("Invalid obstacle coordinates for -o; ignoring obstacle");
                                        }
                                    } else {
                                        logger.warning("-o expects x,y or none; ignoring");
                                    }
                                }
                            } else {
                                logger.warning("-o requires a value like x,y or 'none'; ignoring");
                            }
                            break;
                        default:
                            // ignore unknown args but log
                            if (a.startsWith("-")) logger.fine("Unknown CLI arg: " + a);
                            break;
                    }
                } catch (NumberFormatException nfe) {
                    logger.warning("Number format error parsing arguments: " + nfe.getMessage());
                }
            }

            // Build a world from CLI args (do not use config.properties)
            World cliWorld = new World(size, size);
            // Apply the CLI defaults for properties as requested
            cliWorld.setWorldProperties(CLI_DEFAULT_REPAIR_TIME, CLI_DEFAULT_RELOAD_TIME, CLI_DEFAULT_MAX_SHIELD, CLI_DEFAULT_VISIBILITY);

            // Only auto-generate obstacles when the user did NOT supply -o.
            // If the user provided explicit coordinates (e.g. -o 1,1) we should honor those
            // coordinates and not create additional random obstacles that could conflict.
            if (!oProvided) {
                cliWorld.generateObstacles();
            }

            // If explicit coordinates were provided with -o, try to add that obstacle as well.
            if (obstacleX != null && obstacleY != null) {
                Obstacle obs = new Obstacle(ObstacleType.MOUNTAIN, obstacleX, obstacleY, 1, 1);
                if (!cliWorld.addObstacle(obs)) {
                    logger.warning("Could not place obstacle at provided coordinates");
                }
            }

            // Replace singleton instance so Server.start will pick up this world instead of loading config
            World.initializeInstance(cliWorld);
        } else {
            // No args provided - port remains default unless user passed single numeric arg in old usage
            if (args != null && args.length == 1) {
                // Backwards compat: legacy behaviour where a single numeric arg indicates port
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (NumberFormatException ignored) { }
            }
        }

        // Initialize database tables using EoDSQL if available (best-effort)
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:robot_worlds.db")) {
            WorldDaoInterface dao = QueryTool.getQuery(connection, WorldDaoInterface.class);
            if (dao != null) {
                dao.createWorldTable();
                dao.createObstacleTable();
                logger.info("Database tables initialized via EoDSQL.");
            } else {
                logger.info("EoDSQL QueryTool returned null; skipping explicit table creation (legacy DAO will handle it on first save). ");
            }
        } catch (Exception e) {
            logger.warning("Database initialization failed: " + e.getMessage());
        }

        try {
            // Delegate to start so tests can call start(port) directly if desired
            start(portNumber);
        } catch (IOException e) {
            if (!isRunning) {
                logger.info("Server shutdown.");
            } else {
                logger.severe("Got an error: " + e.getMessage());
            }
        }
    }

    /**
     * Start the server on the provided port. This method blocks until shutdown (same behavior as main).
     * Tests can call this inside a Thread to run the server in-process on a custom port.
     */
    public static void start(int portNumber) throws IOException {
        // Always use the current singleton instance when starting and when accepting clients
        World.getInstance().displayWorld();

        serverSocket = new ServerSocket(portNumber);
        logger.info("Server started on port " + portNumber + ". Waiting for clients...");

        // launch admin console thread
        startAdminConsole();

        // Start HTTP server only if not in test mode
        if (!"true".equals(System.getProperty("isTest"))) {
            port(8080);
            post("/robot/hal", (req, res) -> {
                res.type("application/json");
                try {
                    JSONObject json = new JSONObject(req.body());
                    String commandStr = json.getString("command");
                    JSONArray argsArray = json.getJSONArray("arguments");
                    if (argsArray.length() == 0) {
                        return new Response("ERROR", "No arguments provided").toJSONString();
                    }
                    String robotName = argsArray.getString(0);
                    JSONObject commandJson = new JSONObject();
                    commandJson.put("command", commandStr);
                    commandJson.put("robot", robotName);
                    JSONArray newArgs = new JSONArray();
                    for (int i = 1; i < argsArray.length(); i++) {
                        newArgs.put(argsArray.get(i));
                    }
                    commandJson.put("arguments", newArgs);
                    Command command = Command.fromJSON(commandJson);
                    final String[] responseStr = new String[1];
                    commandHandler.handle(command, "http-client", response -> {
                        responseStr[0] = response.toJSONString();
                    });
                    return responseStr[0];
                } catch (Exception e) {
                    logger.warning("HTTP request error: " + e.getMessage());
                    return new Response("ERROR", "Invalid request").toJSONString();
                }
            });

            // GET /world/:name - restore a saved world via the web API
            get("/world/:name", (req, res) -> {
                res.type("application/json");
                String worldName = req.params(":name");
                try {
                    za.co.wethinkcode.robots.commands.RestoreCommand restoreCmd = new za.co.wethinkcode.robots.commands.RestoreCommand(worldName);
                    String result = restoreCmd.execute();
                    if (result == null) result = "";
                    if (result.startsWith("Error") || result.contains("No saved world")) {
                        res.status(404);
                        return new JSONObject().put("result", "ERROR").put("message", result).toString();
                    }
                    res.status(200);
                    return new JSONObject().put("result", "OK").put("message", result).toString();
                } catch (Exception e) {
                    res.status(500);
                    return new JSONObject().put("result", "ERROR").put("message", e.getMessage()).toString();
                }
            });

            init();
        }

        while (isRunning) {
            Socket clientSocket = serverSocket.accept();
            logger.info("New client connected: " + clientSocket.getRemoteSocketAddress());
            ClientHandler handler = new ClientHandler(clientSocket, World.getInstance());
            registerClient(handler);
            new Thread(handler).start();
        }
    }

    private static void startAdminConsole() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (isRunning) {
                logger.info("Valid Commands: 'quit', 'robots', 'dump', 'display', 'save <name>', 'restore <name>'");
                System.out.print("[Admin]: ");
                String input = scanner.nextLine().trim();
                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String argument = parts.length > 1 ? parts[1] : "";
                switch (command) {
                    case "quit":
                        logger.info("Shutting down server...");
                        shutdown();
                        break;
                    case "robots":
                        logger.info(World.getInstance().getAllRobotsInfo());
                        break;
                    case "dump":
                        logger.info(World.getInstance().getFullWorldState());
                        break;
                    case "display":
                        World.getInstance().displayWorld();
                        break;
                    case "save":
                        if (argument.isEmpty()) {
                            logger.warning("Save command requires a world name: save <name>");
                        } else {
                            try {
                                za.co.wethinkcode.robots.commands.SaveCommand saveCmd = new za.co.wethinkcode.robots.commands.SaveCommand(World.getInstance(), argument);
                                logger.info(saveCmd.execute());
                            } catch (Exception e) {
                                logger.warning("Error saving world: " + e.getMessage());
                            }
                        }
                        break;
                    case "restore":
                        if (argument.isEmpty()) {
                            logger.warning("Restore command requires a world name: restore <name>");
                        } else {
                            try {
                                za.co.wethinkcode.robots.commands.RestoreCommand restoreCmd = new za.co.wethinkcode.robots.commands.RestoreCommand(argument);
                                logger.info(restoreCmd.execute());
                                // no local world var — client handlers and admin console always use World.getInstance()
                            } catch (Exception e) {
                                logger.warning("Error restoring world: " + e.getMessage());
                            }
                        }
                        break;
                    case "purge":
                        if (argument.isEmpty()) {
                            logger.warning("Purge command requires a robot name: purge <robotName>");
                        } else {
                            try {
                                String robotName = argument.trim();
                                World world = World.getInstance();
                                if (world.findRobot(robotName) == null) {
                                    logger.warning("No robot named '" + robotName + "' found in the world.");
                                } else {
                                    // Use world's notifyRobotDeath so CommandHandler handles removal and client disconnect
                                    world.notifyRobotDeath(robotName);
                                    logger.info("Purged robot '" + robotName + "' from the world.");
                                }
                            } catch (Exception e) {
                                logger.warning("Error purging robot: " + e.getMessage());
                            }
                        }
                        break;
                    default:
                        logger.warning("Unknown admin command.");
                }
            }
        }, "AdminConsole").start();
    }


    private static final List<ClientHandler> clients = new ArrayList<>();

    public static synchronized void registerClient(ClientHandler handler) {
        clients.add(handler);
    }

    public static synchronized void unregisterClient(ClientHandler handler) {
        clients.remove(handler);
    }

    public static synchronized void disconnectAllClients() {
        for (ClientHandler handler : new ArrayList<>(clients)) {
            handler.disconnect();
        }
        clients.clear();
    }

    /**
     * Disconnect a specific client by its clientId (remote socket address string).
     * This finds the matching ClientHandler, calls its disconnect method and removes it
     * from the server's internal client list.
     */
    public static synchronized void disconnectClientById(String clientId) {
        if (clientId == null) return;
        for (ClientHandler handler : new ArrayList<>(clients)) {
            try {
                if (handler != null && clientId.equals(handler.getClientId())) {
                    handler.disconnect();
                    clients.remove(handler);
                    return;
                }
            } catch (Exception e) {
                logger.warning("Failed to disconnect client " + clientId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Send a server-generated response to the client identified by clientId and then disconnect it.
     * If the client is not found this method is a no-op.
     */
    public static synchronized void sendDisconnectToClient(String clientId, za.co.wethinkcode.robots.server.Response response) {
        if (clientId == null) return;
        for (ClientHandler handler : new ArrayList<>(clients)) {
            try {
                if (handler != null && clientId.equals(handler.getClientId())) {
                    // Try to send the provided response to the client before disconnecting
                    try {
                        if (response != null) handler.sendServerResponse(response);
                    } catch (Exception e) {
                        logger.warning("Failed to send disconnect response to client " + clientId + ": " + e.getMessage());
                    }
                    // Now disconnect the handler and remove it from the list
                    try {
                        handler.disconnect();
                    } catch (Exception e) {
                        logger.warning("Error while disconnecting client " + clientId + ": " + e.getMessage());
                    }
                    clients.remove(handler);
                    return;
                }
            } catch (Exception e) {
                logger.warning("Failed to send disconnect for client " + clientId + ": " + e.getMessage());
            }
        }
    }


    public static void shutdown() {
        isRunning = false;
        logger.info("Disconnecting all clients...");
        disconnectAllClients();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            Spark.stop();
            logger.info("Server shutdown complete.");
        } catch (Exception e) {
            logger.severe("Got an error when shutting down: " + e.getMessage());
        }
    }
    static {
        new Recorder().logRun();
    }

}
