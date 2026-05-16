package za.co.wethinkcode.robots.acceptance;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class MoveRobotTests {
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


    /**
     * Scenario: Moving a robot forward successfully
     *
     * Given a client connected to the server
     * And a robot named "HALAs" is launched into the world
     * When the robot executes the "forward" command with 3 steps
     * Then the server should respond with an OK result
     * And the robot's position should be updated accordingly
     *   - If the move is blocked by the world's edge, the position should remain the same
     *   - Otherwise, the robot should move 3 steps forward in its current direction (assumed NORTH)
     */
    @Test
    void validMoveForwardShouldSucceed() {
        assertTrue(serverClient.isConnected(), "Must be connected");

        // === LAUNCH ===
        String launchRequest = "{\"robot\":\"HALAs\",\"command\":\"launch\",\"arguments\":[\"sniper\"]}";
        JsonNode launchResp = serverClient.sendRequest(launchRequest);
        System.out.println("Launch Response: " + launchResp.toPrettyString());

        assertEquals("OK", launchResp.path("result").asText(), "Launch must be OK");

        // === GET INITIAL STATE ===
        String stateRequest = "{\"robot\":\"HALAs\",\"command\":\"state\",\"arguments\":[]}";
        JsonNode stateResp = serverClient.sendRequest(stateRequest);
        JsonNode startPos = stateResp.path("state").path("position");
        assertTrue(startPos.isArray() && startPos.size() == 2, "Must have [x,y]");
        int startX = startPos.get(0).asInt();
        int startY = startPos.get(1).asInt();
        System.out.println("Started at: [" + startX + ", " + startY + "]");

        // === MOVE FORWARD 3 ===
        String moveRequest = "{\"robot\":\"HALAs\",\"command\":\"forward\",\"arguments\":[3]}";
        JsonNode moveResp = serverClient.sendRequest(moveRequest);
        System.out.println("Move Response: " + moveResp.toPrettyString());

        String result = moveResp.path("result").asText();
        assertEquals("OK", result, "Move should return OK");

        JsonNode newPos = moveResp.path("state").path("position");
        int newX = newPos.get(0).asInt();
        int newY = newPos.get(1).asInt();

        String message = moveResp.path("data").path("message").asText().toLowerCase();

        if (message.contains("edge")) {
            // Blocked by edge → position should NOT change
            assertEquals(startX, newX, "X should not change when blocked");
            assertEquals(startY, newY, "Y should not change when blocked");
            System.out.println("Move blocked by edge — expected.");
        } else {
            // Normal movement → should move 3 steps NORTH
            assertEquals(startY + 3, newY, "Should move 3 steps NORTH");
            assertEquals(startX, newX, "X should stay the same");
        }
    }

    /**
     * Scenario: Robot attempts an invalid move command
     *
     * Given a client connected to the Robot World server
     * And a robot named "HAL" is successfully launched
     * When the robot executes an invalid command (e.g., "forword" instead of "forward")
     * Then the server should respond with an ERROR result
     * And the data.message should indicate that the command is unsupported
     */

    @Test
    void invalidMoveShouldFail() {
        // --- Given ---
        // That I am connected to a running Robot World server
        assertTrue(serverClient.isConnected());

        // Launch a robot first
        String launchRequest = "{" +
                "\"robot\": \"HAL\"," +
                "\"command\": \"launch\"," +
                "\"arguments\": [\"sniper\", \"5\", \"5\"]" +
                "}";
        JsonNode launchResponse = serverClient.sendRequest(launchRequest);
        assertEquals("OK", launchResponse.get("result").asText(), "Launch should succeed");

        // --- When ---
        // I send an invalid move command (misspelled)
        String invalidMoveRequest = "{" +
                "\"robot\": \"HAL\"," +
                "\"command\": \"forword\"," +
                "\"arguments\": [\"3\"]" +
                "}";
        JsonNode invalidMoveResponse = serverClient.sendRequest(invalidMoveRequest);

        // --- Then ---
        assertNotNull(invalidMoveResponse.get("result"));
        assertEquals("ERROR", invalidMoveResponse.get("result").asText(), "Expected ERROR result");

        // And I should get an error message
        assertNotNull(invalidMoveResponse.get("data"));
        assertTrue(invalidMoveResponse.get("data").get("message").asText().contains("Unsupported command"));
    }
}
