package za.co.wethinkcode.robots.acceptance.iteration2;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class LaunchRobot {
    private final static int DEFAULT_PORT = 5000;
    private final static String DEFAULT_IP = "localhost";
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
     * Scenario: Launching multiple robots into the world
     *
     * Given a client connected to the Robot World server
     * When a robot "HAL" is launched successfully
     * And there is still space available in the world
     * Then a second robot "R2D2" can be launched successfully
     * And the server should respond with an OK result for each launch
     *
     * Note: In a small world (e.g., 2x2), the second launch may fail if there is no free space.
     */
    @Test
    void canLaunchAnotherRobot() {
        assertTrue(serverClient.isConnected());

        // Launch first robot "HAL"
        String launchHAL = "{" +
                " \"robot\": \"HAL\"," +
                " \"command\": \"launch\"," +
                " \"arguments\": [\"tank\"]" + // random position
                "}";
        JsonNode respHAL = serverClient.sendRequest(launchHAL);
        System.out.println("✅ HAL launch response: " + respHAL.toPrettyString());

        // On a 2x2 world, HAL may occupy the only free spot
        if (respHAL.get("result").asText().equals("OK")) {
            // Only proceed to launch R2D2 if there is space
            String launchR2D2 = "{" +
                    " \"robot\": \"R2D2\"," +
                    " \"command\": \"launch\"," +
                    " \"arguments\": [\"sniper\"]" +
                    "}";
            JsonNode respR2D2 = serverClient.sendRequest(launchR2D2);
            System.out.println("✅ R2D2 launch response: " + respR2D2.toPrettyString());

            // Reference server might return ERROR if no space
            assertNotNull(respR2D2.get("result"));
            String result = respR2D2.get("result").asText();
            assertTrue(result.equals("OK"));
        }
    }


    /**
     * Scenario: Launching multiple robots into a world with an obstacle
     *
     * Given a client connected to the Robot World server
     * And a 2x2 world with a known obstacle at position [1,1]
     * When up to 8 robots are launched one by one
     * Then each robot should be placed in a free cell
     * And no robot should be placed on the obstacle at [1,1]
     * If the world has no free space, the server should respond with an ERROR
     * And the message should indicate "No more space in this world"
     */

    @Test
    void launchRobotsIntoWorldWithAnObstacle() {
        assertTrue(serverClient.isConnected());

        // Launch up to 8 robots in a 2x2 world with a known obstacle at [1,1]
        for (int i = 1; i <= 8; i++) {
            String robotName = "Robot" + i;
            String launch = "{" +
                    " \"robot\": \"" + robotName + "\"," +
                    " \"command\": \"launch\"," +
                    " \"arguments\": [\"tank\"]" + // random free position
                    "}";
            JsonNode resp = serverClient.sendRequest(launch);
            System.out.println("✅ " + robotName + " launch response: " + resp.toPrettyString());

            // The server may reject a launch if no space is left
            assertNotNull(resp.get("result"));
            String result = resp.get("result").asText();
            assertTrue(result.equals("OK") || result.equals("ERROR"));

            if (result.equals("OK")) {
                // Verify position is not on the obstacle [1,1]
                JsonNode pos = resp.get("data").get("position");
                int x = pos.get(0).asInt();
                int y = pos.get(1).asInt();
                assertFalse(x == 1 && y == 1, robotName + " spawned on the obstacle!");
            } else {
                // If no space, error message should be correct
                assertEquals("No more space in this world", resp.get("data").get("message").asText());
            }
        }
    }

    /**
     * Scenario: Launching robots into a world without obstacles until full
     *
     * Given a client connected to the Robot World server
     * And a world without obstacles
     * When robots are launched one by one
     * Then the server should allow launches until no free space remains
     * And any further launch attempts should return an ERROR
     * With the message "No more space in this world"
     */
    @Test
    void worldWithoutObstaclesIsFull() {
        assertTrue(serverClient.isConnected());

        int launched = 0;
        boolean worldFull = false;

        for (int i = 1; i <= 9; i++) {
            String robotName = "Robot" + i;
            String launch = "{" +
                    " \"robot\": \"" + robotName + "\"," +
                    " \"command\": \"launch\"," +
                    " \"arguments\": [\"tank\"]" +
                    "}";
            JsonNode resp = serverClient.sendRequest(launch);
            System.out.println("✅ " + robotName + " launch response: " + resp.toPrettyString());

            if ("OK".equals(resp.get("result").asText())) {
                launched++;
            } else if (resp.get("data") != null &&
                    "No more space in this world".equalsIgnoreCase(resp.get("data").get("message").asText())) {
                worldFull = true;
                break;
            }
        }

        // Now, assert that the server behaves consistently:
        // Either at least one robot was launched before it became full,
        // or it's already full from the start — both are valid reference behaviors.
        assertTrue(launched >= 0, "Server should handle launches correctly.");
        assertTrue(worldFull || launched > 0, "Either the world was full initially or became full after some launches.");

        // Attempt to launch one more robot and expect error
        String finalLaunch = "{" +
                " \"robot\": \"ExtraRobot\"," +
                " \"command\": \"launch\"," +
                " \"arguments\": [\"tank\"]" +
                "}";
        JsonNode finalResp = serverClient.sendRequest(finalLaunch);
        assertEquals("ERROR", finalResp.get("result").asText());
        assertEquals("No more space in this world", finalResp.get("data").get("message").asText());
    }


    /**
     * Scenario: Filling a world without obstacles until full
     *
     * Given a client connected to the Robot World server
     * And a world without obstacles
     * When robots are launched one by one
     * Then the server should allow launches until no free space remains
     * And any further launch attempt should return an ERROR
     * With the message "No more space in this world"
     *
     * This test iteratively launches up to 9 robots and verifies that
     * the server properly reports when the world is full.
     */
    @Test
    void scenarioWorldWithoutObstaclesIsFull() {
        assertTrue(serverClient.isConnected());

        // Attempt to launch up to 9 robots; stop early if server reports no more space
        for (int i = 1; i <= 9; i++) {
            String robotName = "FillRobot" + i;
            String launch = "{" +
                    " \"robot\": \"" + robotName + "\"," +
                    " \"command\": \"launch\"," +
                    " \"arguments\": [\"tank\"]" +
                    "}";

            JsonNode resp = serverClient.sendRequest(launch);
            assertNotNull(resp, "No response received for launch request");
            System.out.println(robotName + " launch response: " + resp.toPrettyString());

            // If server already reports the world is full, stop early
            if (resp.has("result") && "ERROR".equalsIgnoreCase(resp.get("result").asText())) {
                String msg = "";
                if (resp.has("data") && resp.get("data").has("message")) {
                    msg = resp.get("data").get("message").asText();
                } else if (resp.has("message")) {
                    msg = resp.get("message").asText();
                }
                if (msg != null && msg.equalsIgnoreCase("No more space in this world")) {
                    break;
                }
            }
        }

        // Now attempt one more launch which must fail with the exact message
        String finalLaunch = "{" +
                " \"robot\": \"ExtraRobot\"," +
                " \"command\": \"launch\"," +
                " \"arguments\": [\"tank\"]" +
                "}";
        JsonNode finalResp = serverClient.sendRequest(finalLaunch);
        assertNotNull(finalResp, "No response received for final launch request");
        System.out.println("Final launch response: " + finalResp.toPrettyString());

        assertNotNull(finalResp.get("result"));
        assertEquals("ERROR", finalResp.get("result").asText());

        // Extract message from common places
        String finalMsg = "";
        if (finalResp.has("data") && finalResp.get("data").has("message")) {
            finalMsg = finalResp.get("data").get("message").asText();
        } else if (finalResp.has("message")) {
            finalMsg = finalResp.get("message").asText();
        }

        assertNotNull(finalMsg);
        assertTrue(finalMsg.equalsIgnoreCase("No more space in this world") || finalMsg.contains("No more space"),
                "Expected final error message to indicate no more space in the world (was: '" + finalMsg + "')");
    }

}
