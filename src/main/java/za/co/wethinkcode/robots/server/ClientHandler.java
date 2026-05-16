// java
package za.co.wethinkcode.robots.server;

import org.json.JSONArray;
import org.json.JSONObject;
import za.co.wethinkcode.robots.commands.Command;
import za.co.wethinkcode.robots.domain.World;
import za.co.wethinkcode.robots.handlers.CommandHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CommandHandler commandHandler;
    private final String clientId;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, CommandHandler commandHandler) {
        this.clientSocket = socket;
        this.commandHandler = commandHandler;
        this.clientId = socket.getRemoteSocketAddress().toString();
    }

    public ClientHandler(Socket socket, World world) {
        this.clientSocket = socket;
        this.commandHandler = new CommandHandler(world);
        this.clientId = socket.getRemoteSocketAddress().toString();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {
            String line;
            while (running && (line = in.readLine()) != null && !clientSocket.isClosed()) {
                System.out.println("Client [" + clientId + "]: " + line);
                Command command = parseCommandFromJson(new JSONObject(line));
                if (command == null) {
                    sendUnsupportedCommandError(out);
                    continue;
                }
                commandHandler.handle(command, clientId, response -> {
                    if (response != null && response.object != null) {
                        try {
                            normalizePositionField(response.object, "state");
                            normalizePositionField(response.object, "data");
                            System.out.println("Sending to client " + clientId + ": " + response.object);
                            out.println(response.object.toString());
                            out.flush();

                            // Re-render the world on the server console after sending the response
                            World.getInstance().displayWorld();

                        } catch (Exception e) {
                            System.out.println("Client [" + clientId + "]: Error sending response: " + e.getMessage());
                            sendErrorResponse(out, "Error processing response");
                        }
                    } else {
                        sendErrorResponse(out, "Invalid server response");
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("Client [" + clientId + "]: IOException in client handler: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Client [" + clientId + "]: Unexpected error in client handler: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private Command parseCommandFromJson(JSONObject incoming) {
        try {
            if (!incoming.has("command")) {
                System.out.println("Client [" + clientId + "]: Missing 'command' field in JSON: " + incoming);
                return null;
            }
            return Command.fromJSON(incoming);
        } catch (Exception e) {
            System.out.println("Client [" + clientId + "]: Failed to parse JSON command: " + incoming + ", Error: " + e.getMessage());
            return null;
        }
    }

    private void sendErrorResponse(PrintWriter out, String message) {
        JSONObject response = new JSONObject();
        response.put("result", "ERROR");
        JSONObject data = new JSONObject();
        data.put("message", message);
        response.put("data", data);
        out.println(response.toString());
        out.flush();

        // Re-render world so server console shows latest grid after errors too
        World.getInstance().displayWorld();
    }

    private void sendUnsupportedCommandError(PrintWriter out) {
        sendErrorResponse(out, "Unsupported command");
    }

    // java
// Replace or update the disconnect() method in ClientHandler
    public void disconnect() {
        running = false;
        try {
            // Ensure we remove any robots owned by this client from the world
            try {
                commandHandler.removeAllRobotsForClient(clientId);
            } catch (Exception e) {
                System.out.println("Error removing client robots for " + clientId + ": " + e.getMessage());
            }

            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error disconnecting client " + clientId + ": " + e.getMessage());
        }
    }


    private void normalizePositionField(JSONObject root, String parentKey) {
        if (!root.has(parentKey)) return;
        JSONObject parent = root.optJSONObject(parentKey);
        if (parent == null) return;
        Object pos = parent.opt("position");
        if (pos instanceof JSONArray) return;
        if (!(pos instanceof String)) {
            parent.put("position", new JSONArray());
            return;
        }
        String posStr = ((String) pos).trim();
        if (!posStr.matches("\\[\\s*\\d+\\s*,\\s*\\d+\\s*\\]")) {
            parent.put("position", new JSONArray());
            return;
        }
        try {
            String[] parts = posStr.substring(1, posStr.length() - 1).split("\\s*,\\s*");
            JSONArray arr = new JSONArray();
            arr.put(Integer.parseInt(parts[0]));
            arr.put(Integer.parseInt(parts[1]));
            parent.put("position", arr);
        } catch (Exception e) {
            System.out.println("Failed to normalize position for " + clientId + ": " + e.getMessage());
            parent.put("position", new JSONArray());
        }
    }

    // Public accessor so Server can find this handler by id
    public String getClientId() {
        return this.clientId;
    }

    // Send a server-generated response directly to the client socket. Returns true if sent.
    public boolean sendServerResponse(za.co.wethinkcode.robots.server.Response response) {
        if (response == null) return false;
        try {
            if (clientSocket == null || clientSocket.isClosed()) return false;
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            out.println(response.object.toString());
            out.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Client [" + clientId + "]: Error sending server response: " + e.getMessage());
            return false;
        }
    }
}
