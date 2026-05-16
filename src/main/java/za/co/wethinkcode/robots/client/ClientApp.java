package za.co.wethinkcode.robots.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import za.co.wethinkcode.robots.commands.Command;
import za.co.wethinkcode.robots.server.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ClientApp {

    /*** Console-related constants ***/
    private static final int MAX_ROBOTS = 2;
    private static final List<RobotType> VALID_ROBOT_TYPES = List.of(RobotType.SNIPER, RobotType.TANK, RobotType.SOLDIER, RobotType.MINER);

    /*** Networking fields ***/
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ObjectMapper mapper = new ObjectMapper();

    /*** ====================== PUBLIC METHODS FOR TEST ====================== ***/

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Failed to connect to server: " + e.getMessage());
            socket = null;
            out = null;
            in = null;
        }
    }

    public void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Failed to disconnect: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public JsonNode sendRequest(String jsonRequest) {
        if (!isConnected()) {
            System.out.println("Not connected to server.");
            return null;
        }
        try {
            out.println(jsonRequest);
            String response = in.readLine();
            JsonNode node = mapper.readTree(response);

            return normalizeResponse(node);
        } catch (IOException e) {
            System.out.println("Failed to send request: " + e.getMessage());
            return null;
        }
    }

    /*** ====================== CONSOLE APPLICATION ====================== ***/

    public static void main(String[] args) {
        ClientApp client = new ClientApp();
        client.runConsole();
    }

    private void runConsole() {
        Scanner scanner = new Scanner(System.in);
        List<RobotSpec> robots = new ArrayList<>();

        if (!connectToServer(scanner)) {
            System.out.println("Connection failed. Exiting.");
            return;
        }

        System.out.println("Ready for launch!");

        while (robots.size() < MAX_ROBOTS) {
            if (attemptLaunch(scanner, robots)) break;
        }

        if (robots.size() >= MAX_ROBOTS) {
            System.out.println("ERROR: Cannot launch more than " + MAX_ROBOTS + " robots.");
        }

        disconnect();
    }

    // Prompt the user for host and port and attempt to connect. Returns true if connected.
    private boolean connectToServer(Scanner scanner) {
        String host = prompt(scanner, "Hello! Welcome to RobotWorld. Please enter the IP address of the server you'd like to connect to:");
        int portNumber = promptPort(scanner);
        connect(host, portNumber);
        return isConnected();
    }

    // Attempt to read launch details from the console, send a launch command and handle the server response.
    // Returns true when a robot was successfully launched (and commands were handled), false to continue.
    private boolean attemptLaunch(Scanner scanner, List<RobotSpec> robots) {
        String robotName = prompt(scanner, "Enter a name for your robot:");
        if (!isValidRobotName(robotName)) {
            System.out.println("Robot name must only contain letters and numbers. Please try again.");
            return false;
        }

        String rawType = prompt(scanner, "Enter a type for your robot (sniper/tank/soldier/miner):");
        RobotType robotType = RobotType.fromString(rawType);
        if (robotType == null || !VALID_ROBOT_TYPES.contains(robotType)) {
            System.out.println("Invalid robot type. Valid types are: sniper, tank, soldier, miner. Please try again.");
            return false;
        }

        try {
            Command cmd = Command.fromInput("launch " + robotType.id() + " " + robotName, robotName);
            JsonNode responseJson = sendRequest(cmd.toJSONString());

            Response response = parseServerResponse(responseJson != null ? responseJson.toString() : null);
            if (!response.isOKResponse()) {
                System.out.println("Server: " + response.getMessage());
                return false;
            }

            System.out.println("Launching your robot into the world 🚀");
            sleepDefault();

            JSONObject data = response.getData();
            JSONObject state = response.getState();
            if (data != null && state != null) {
                System.out.printf(
                        "Robot launched! Position: %s, Direction: %s, Shields: %d, Shots: %d, Status: %s\n",
                        data.optJSONArray("position"),
                        state.optString("direction"),
                        state.optInt("shields"),
                        state.optInt("shots"),
                        state.optString("status")
                );
            } else {
                System.out.println(response.getMessage());
            }

            robots.add(new RobotSpec(robotName, robotType));
            System.out.println("To check what you can do: use 'help'\n");

            handleCommands(scanner, robotName);
            return true;

        } catch (Exception e) {
            System.out.println("Error sending command: " + e.getMessage());
            return false;
        }
    }

    private void handleCommands(Scanner scanner, String robotName) {
        while (true) {
            String message = prompt(scanner, "Enter command:").trim();
            if (message.equalsIgnoreCase("disconnect")) break;

            if (isRestrictedCommand(message)) {
                System.out.println("This command can only be run by the server admin");
                continue;
            }

            try {
                processCommand(message, robotName);
            } catch (Exception e) {
                System.out.println("Invalid Command. Try again");
            }
        }
    }

    // Process a single console command: send to server and print responses. Handles multi-response commands.
    private void processCommand(String message, String robotName) {
        Command cmd = Command.fromInput(message, robotName);
        JsonNode responseJson = sendRequest(cmd.toJSONString());
        Response cmdResponse = parseServerResponse(responseJson != null ? responseJson.toString() : null);
        System.out.println("Server: " + cmdResponse.getMessage());

        // Some commands (reload/repair) produce two server messages; re-send to collect the follow-up.
        if (isMultiResponseCommand(message)) {
            JsonNode extraJson = sendRequest(cmd.toJSONString());
            Response extraResponse = parseServerResponse(extraJson != null ? extraJson.toString() : null);
            System.out.println("Server: " + extraResponse.getMessage());
        }
    }

    private boolean isMultiResponseCommand(String message) {
        String lower = message.toLowerCase();
        return lower.contains("reload") || lower.contains("repair");
    }

    /*** ====================== HELPER METHODS ====================== ***/

    private static Response parseServerResponse(String responseString) {
        if (responseString == null || responseString.trim().isEmpty()) {
            return new Response("ERROR", "No response from server.");
        }
        return Response.responseFromJSONString(responseString);
    }

    private static String prompt(Scanner scanner, String message) {
        System.out.println(message);
        return scanner.nextLine();
    }

    private static int promptPort(Scanner scanner) {
        System.out.println("Enter the port number:");
        while (!scanner.hasNextInt()) {
            System.out.println("Please enter a valid number:");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return value;
    }

    private static boolean isValidRobotName(String name) {
        return !name.trim().isEmpty() && name.matches("[a-zA-Z0-9]+");
    }

    private static boolean isRestrictedCommand(String message) {
        String lower = message.toLowerCase();
        return lower.equals("quit") || lower.equals("robots") || lower.equals("dump");
    }

    private static void sleepDefault() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ignored) {}
    }

    // Move 'objects'/'message'/'position' into a 'data' node if present to normalize server responses.
    private JsonNode normalizeResponse(JsonNode node) {
        if (node != null && node.isObject() && !node.has("data")) {
            com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
            com.fasterxml.jackson.databind.node.ObjectNode dataNode = mapper.createObjectNode();
            boolean moved = false;

            if (obj.has("objects")) { dataNode.set("objects", obj.get("objects")); moved = true; }
            if (obj.has("message")) { dataNode.set("message", obj.get("message")); moved = true; }
            if (obj.has("position")) { dataNode.set("position", obj.get("position")); moved = true; }

            if (moved) obj.set("data", dataNode);
        }
        return node;
    }
}
