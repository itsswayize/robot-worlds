package za.co.wethinkcode.robots.acceptance;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class GetRobotStateTests {
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
     * Scenario: Checking if a launched robot exists in the world
     *
     * Given a client connected to the server
     * And a robot named "HALState" has been successfully launched at coordinates (5,5) with type "sniper"
     * When the client requests the state of "HALState"
     * Then the server should respond with an OK result
     * And the response should include the robot's state
     * And the state should contain the robot's position as an array of two integers
     */
    @Test
    void robotExistsInWorld() {
        // --- Given ---
        assertTrue(serverClient.isConnected(), "Client should be connected to server");

        // Launch a valid robot into the world
        String launchRequest = "{" +
                "\"robot\": \"HALState\"," +
                "\"command\": \"launch\"," +
                "\"arguments\": [\"sniper\", \"5\", \"5\"]" +
                "}";
        JsonNode launchResponse = serverClient.sendRequest(launchRequest);

        // --- Then ---
        assertEquals("OK", launchResponse.get("result").asText(), "Launch should succeed");

        // --- When ---
        // Request the state of the robot
        String stateRequest = "{" +
                "\"robot\": \"HALState\"," +
                "\"command\": \"state\"," +
                "\"arguments\": []" +
                "}";
        JsonNode stateResponse = serverClient.sendRequest(stateRequest);
        System.out.println(stateResponse.toPrettyString());

        // --- Then ---
        assertNotNull(stateResponse.get("result"));
        assertEquals("OK", stateResponse.get("result").asText(), "Expected OK result");

        assertNotNull(stateResponse.get("state"));
        assertNotNull(stateResponse.get("state").get("position"));
        assertTrue(stateResponse.get("state").get("position").isArray(), "Position should be an array");

        if (stateResponse.has("data") && stateResponse.get("data").has("message")) {
            System.out.println("Server message: " + stateResponse.get("data").get("message").asText());
        }
    }


    /**
     * Scenario: Checking the state of a robot that does not exist in the world
     *
     * Given a client connected to the server
     * When the client requests the state of a robot named "GhostBot" that has never been launched
     * Then the server should respond with an ERROR result
     * And the data.message should indicate that the robot was not found or does not exist
     */
    @Test
    void robotNotInWorld() {
        // --- Given ---
        assertTrue(serverClient.isConnected(), "Client should be connected to server");

        // --- When ---
        // Request the state of a robot that was never launched
        String stateRequest = "{" +
                "\"robot\": \"GhostBot\"," +
                "\"command\": \"state\"," +
                "\"arguments\": []" +
                "}";
        JsonNode stateResponse = serverClient.sendRequest(stateRequest);
        System.out.println(stateResponse.toPrettyString());

        // --- Then ---
        assertNotNull(stateResponse.get("result"));
        assertEquals("ERROR", stateResponse.get("result").asText(), "Expected ERROR result");

        assertNotNull(stateResponse.get("data"));
        String msg = stateResponse.get("data").get("message").asText().toLowerCase();
        // Accept several common 'not found' message variants used by different servers
        assertTrue(msg.contains("not found") || msg.contains("does not exist") || msg.contains("not exist") || msg.contains("could not find"),
                "Should return a 'not found' style message (was: '" + msg + "')");
    }
}
