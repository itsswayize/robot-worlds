package za.co.wethinkcode.robots.acceptance;

import com.fasterxml.jackson.databind.JsonNode;

public interface RobotWorldClient {
    void connect(String host, int port);
    boolean isConnected();
    JsonNode sendRequest(String request);
    void disconnect();
}
