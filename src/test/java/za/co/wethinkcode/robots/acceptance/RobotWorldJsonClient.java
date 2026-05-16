package za.co.wethinkcode.robots.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

public class RobotWorldJsonClient implements RobotWorldClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to server: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public JsonNode sendRequest(String request) {
        try {
            out.println(request);
            String response = in.readLine();
            return mapper.readTree(response);
        } catch (IOException e) {
            throw new RuntimeException("Error communicating with server: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error closing connection: " + e.getMessage());
        }
    }
}
