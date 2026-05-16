package za.co.wethinkcode.robots.acceptance;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LookCommandTests {
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
     * Tests the "look" command behavior in an empty world.
     *
     * Given: A robot named "LookBot" launched at coordinates (5,5) in an empty world.
     * When: The robot sends the "look" command.
     * Then: The response should be OK, include a "data" object with an "objects" array,
     * which should either be empty or contain only edge objects (NORTH, SOUTH, EAST, WEST)
     * at distance 1. The optional message should indicate that the world is empty.
     *
     */
    @Test
    void lookInEmptyWorld() {
        // --- Given ---
        assertTrue(serverClient.isConnected(), "Client should be connected to server");

        // Launch a robot into an empty world
        String launchRequest = "{" +
                "\"robot\": \"LookBot\"," +
                "\"command\": \"launch\"," +
                "\"arguments\": [\"sniper\", \"5\", \"5\"]" +
                "}";
        JsonNode launchResponse = serverClient.sendRequest(launchRequest);
        assertEquals("OK", launchResponse.get("result").asText(), "Launch should succeed");

        // --- When ---
        // The robot looks around in an empty world
        String lookRequest = "{" +
                "\"robot\": \"LookBot\"," +
                "\"command\": \"look\"," +
                "\"arguments\": []" +
                "}";
        JsonNode lookResponse = serverClient.sendRequest(lookRequest);
        System.out.println(lookResponse.toPrettyString());

        // --- Then ---
        assertNotNull(lookResponse.get("result"), "Response should contain a result field");
        assertEquals("OK", lookResponse.get("result").asText(), "Expected OK result");

        // The data section should exist and contain an 'objects' array (even if empty)
        assertNotNull(lookResponse.get("data"), "Response should contain data");
        assertTrue(lookResponse.get("data").has("objects"), "Data should contain 'objects' key");
        assertTrue(lookResponse.get("data").get("objects").isArray(), "'objects' should be an array");
        // Accept either an empty list (no visible objects) or the reference-server behaviour
        // where the edges of a small world are reported as objects (NORTH/SOUTH/EAST/WEST with distance 1).
        JsonNode objects = lookResponse.get("data").get("objects");
        int size = objects.size();
        if (size != 0) {
            // Validate that each reported object is an EDGE at distance 1 in a cardinal direction
            for (JsonNode obj : objects) {
                assertTrue(obj.has("type"), "Each object should have a 'type'");
                assertEquals("EDGE", obj.get("type").asText(), "Non-empty objects should be EDGE in this test environment");
                assertTrue(obj.has("direction"), "Each object should have a 'direction'");
                String dir = obj.get("direction").asText();
                assertTrue(Arrays.asList("NORTH", "SOUTH", "EAST", "WEST").contains(dir), "Direction should be a cardinal one");
                assertTrue(obj.has("distance"), "Each object should have a 'distance'");
                assertEquals(1, obj.get("distance").asInt(), "Edge objects should be at distance 1 in this test");
            }
        }

        // Optionally check the message
        if (lookResponse.get("data").has("message")) {
            String msg = lookResponse.get("data").get("message").asText();
            System.out.println("Server message: " + msg);
            assertTrue(msg.toLowerCase().contains("nothing") || msg.toLowerCase().contains("empty"),
                    "Expected message to indicate an empty world");
        }
    }
}
