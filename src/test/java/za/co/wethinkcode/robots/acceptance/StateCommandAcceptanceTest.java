package za.co.wethinkcode.robots.acceptance;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class StateCommandAcceptanceTest {
    private static final int DEFAULT_PORT = 5000;
    private static final String DEFAULT_IP = "localhost";
    private final ClientApp serverClient = new ClientApp();

    @BeforeEach
    void connectToServer() {
        serverClient.connect(DEFAULT_IP, DEFAULT_PORT);
    }

    @AfterEach
    void disconnectFromServer() {
        serverClient.disconnect();
    }

    // Helper: build a JSON request for state command
    private String buildStateRequest(String robotName) {
        return "{" +
                "\"robot\": \"" + robotName + "\"," +
                "\"command\": \"state\"," +
                "\"arguments\": []" +
                "}";
    }

    // Helper: send a state request
    private JsonNode sendState(String robotName) {
        return serverClient.sendRequest(buildStateRequest(robotName));
    }

    // Helper: launch a robot (simple wrapper)
    private String launchRobot(String baseName) {
        String request = "{" +
                "\"robot\": \"" + baseName + "\"," +
                "\"command\": \"launch\"," +
                "\"arguments\": [\"sniper\"]" +
                "}";
        JsonNode resp = serverClient.sendRequest(request);
        assertNotNull(resp, "Launch response was null");
        assertTrue(resp.has("result"), "Launch response missing result");
        assertEquals("OK", resp.get("result").asText(), "Robot launch should succeed");
        return baseName;
    }

    // Helper: retrieve the 'state' node from the server response in a tolerant way
    private JsonNode getStateNode(JsonNode response) {
        if (response == null) return null;
        if (response.has("state")) return response.get("state");
        if (response.has("data") && response.get("data").has("state")) return response.get("data").get("state");
        return null;
    }

    // Helper: get error message tolerant of where servers put it
    private String getErrorMessage(JsonNode response) {
        if (response == null) return null;
        if (response.has("message") && !response.get("message").isNull()) return response.get("message").asText();
        if (response.has("data") && response.get("data").has("message")) return response.get("data").get("message").asText();
        return null;
    }


    /**
     * Scenario: Retrieve state of a valid robot
     *
     * Given a client connected to the Robot World server
     * And a robot named "StateBot" is successfully launched
     * When the robot's state is requested
     * Then the server should respond with an OK result
     * And the response should include a state object
     * And the state should contain at least position or direction
     * And the state should include either:
     *     - health and ammo, or
     *     - shields and shots, or
     *     - a status field
     */
    @Test
    void validRobotStateShouldReturnOk() {
        // --- Given ---
        assertTrue(serverClient.isConnected(), "Client should be connected");

        String robotName = launchRobot("StateBot");

        // --- When ---
        JsonNode stateResponse = sendState(robotName);
        assertNotNull(stateResponse, "State response was null");
        System.out.println("State response:\n" + stateResponse.toPrettyString());

        // --- Then ---
        assertNotNull(stateResponse.get("result"), "Response should have a result field");
        assertEquals("OK", stateResponse.get("result").asText(), "Expected OK result");

        // Tolerantly find the state node
        JsonNode stateNode = getStateNode(stateResponse);
        assertNotNull(stateNode, "Response should include a state object (either top-level 'state' or 'data.state')");

        // Required: position and direction (or at least one of them must exist)
        assertTrue(stateNode.has("position") || stateNode.has("direction"),
                "State should include at least 'position' or 'direction' (was: " + stateNode.toPrettyString() + ")");

        // Accept either (health + ammo) or (shields + shots) as plausible server variants.
        boolean hasHealthAmmo = stateNode.has("health") && stateNode.has("ammo");
        boolean hasShieldsShots = stateNode.has("shields") && stateNode.has("shots");
        assertTrue(hasHealthAmmo || hasShieldsShots || stateNode.has("status"),
                "State should contain either (health & ammo) or (shields & shots) or at least a 'status' field. Actual state: " + stateNode.toPrettyString());
    }


    /**
     * Scenario: Request state of a non-existent robot
     *
     * Given a client connected to the Robot World server
     * When a state request is sent for a robot that was never launched (e.g., "NonExistentBot")
     * Then the server should respond with an ERROR result
     * And the response should include an error message
     * And the message should indicate that the robot was not found or does not exist
     */

    @Test
    void invalidRobotStateShouldReturnError() {
        // --- Given ---
        assertTrue(serverClient.isConnected(), "Client should be connected");

        // --- When ---
        JsonNode stateResponse = sendState("NonExistentBot");
        assertNotNull(stateResponse, "State response was null");
        System.out.println("Invalid-state response:\n" + stateResponse.toPrettyString());

        // --- Then ---
        assertNotNull(stateResponse.get("result"), "Response should include a result field");
        assertEquals("ERROR", stateResponse.get("result").asText(), "Expected ERROR result");

        // Tolerantly accept message either at top-level or inside data.message
        String msg = getErrorMessage(stateResponse);
        assertNotNull(msg, "Error response should include a message (either top-level 'message' or 'data.message')");
        assertFalse(msg.isBlank(), "Error message should not be empty");

        assertTrue(msg.toLowerCase().contains("not found")
                        || msg.toLowerCase().contains("does not exist")
                        || msg.toLowerCase().contains("could not find")
                        || msg.toLowerCase().contains("robot does not exist"),
                "Error message should indicate robot not found (was: '" + msg + "')");
    }
}
