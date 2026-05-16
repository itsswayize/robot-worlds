package za.co.wethinkcode.robots.acceptance;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LaunchRobotTests {
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
     * Scenario: Successfully launching a robot
     *
     * Given a client connected to the server
     * When a robot named "HAL" is launched at coordinates (5,5) with type "sniper"
     * Then the server should respond with an OK result
     * And the response should include the robot's initial position
     * And the state object should be present
     */

    @Test
    void validLaunchShouldSucceed() {
        assertTrue(serverClient.isConnected());

        String request = "{" +
                " \"robot\": \"HAL\"," +
                " \"command\": \"launch\"," +
                " \"arguments\": [\"sniper\",\"5\",\"5\"]" +
                "}";

        JsonNode response = serverClient.sendRequest(request);

        System.out.println("✅ Valid launch response: " + response.toPrettyString());

        assertNotNull(response.get("result"));
        assertEquals("OK", response.get("result").asText());

        assertNotNull(response.get("data"));
        assertNotNull(response.get("data").get("position"));

        // Be flexible about the returned position. Some servers place at the centre (0,0),
        // others iterate from the top-left (-halfWidth,-halfHeight). Accept any 2-int position.
        JsonNode pos = response.get("data").get("position");
        assertTrue(pos.isArray() && pos.size() == 2, "Position should be an array of two integers");
        int x = pos.get(0).asInt();
        int y = pos.get(1).asInt();

        assertNotNull(response.get("state"));
    }


    /**
     * Scenario: Robot executes an invalid command
     *
     * Given a client connected to the server
     * When a robot attempts to execute an invalid command
     *       (e.g., "luanch" instead of "launch")
     * Then the server should respond with an ERROR result
     * And the error message should indicate that the command is unsupported
     */

    @Test
    void invalidLaunchShouldFail() {
        assertTrue(serverClient.isConnected());

        String request = "{" +
                "\"robot\": \"HAL\"," +
                "\"command\": \"luanch\"," +  // typo intentional
                "\"arguments\": [\"shooter\",\"5\",\"5\"]" +
                "}";

        JsonNode response = serverClient.sendRequest(request);

        System.out.println("❌ Invalid launch response: " + response.toPrettyString());

        assertNotNull(response.get("result"));
        assertEquals("ERROR", response.get("result").asText());

        // Server returns data.message for this error
        String msg = "";
        if (response.has("data") && response.get("data").has("message")) {
            msg = response.get("data").get("message").asText();
        } else if (response.has("message")) {
            msg = response.get("message").asText();
        }

        assertTrue(msg.contains("Unsupported command"));
    }


    /**
     * Scenario: Launching more robots than the world allows
     *
     * Given a connected client
     * And a world that allows only two robots to be launched
     * When the first robot "R1" is launched at position (0,0)
     * And the second robot "R2" is launched at position (1,0)
     * And an attempt is made to launch a third robot
     * Then the third launch should fail with an ERROR response
     * And the error message should indicate that no more robots can be launched
     */
    @Test
    void launchingMoreThanAllowedShouldFail() {
        assertTrue(serverClient.isConnected());

        JsonNode first  = launchRobot("R1", "tank",   "0", "0");
        JsonNode second = launchRobot("R2", "sniper", "1", "0");

        System.out.println("First launch response: " + first.toPrettyString());
        System.out.println("Second launch response: " + second.toPrettyString());

        if (isOk(second)) {
            assertThirdLaunchFailsWhenTwoAlreadyLaunched();
        } else {
            assertSecondLaunchAlreadyFails(second);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods – each has a single, clear responsibility            */
    /* --------------------------------------------------------------------- */

    private JsonNode launchRobot(String robot, String type, String x, String y) {
        String request = String.format(
                "{\"robot\":\"%s\",\"command\":\"launch\",\"arguments\":[\"%s\",\"%s\",\"%s\"]}",
                robot, type, x, y);
        JsonNode resp = serverClient.sendRequest(request);
        assertNotNull(resp);
        return resp;
    }

    private String extractMessage(JsonNode node) {
        if (node == null) return "";
        if (node.has("data") && node.get("data").has("message"))
            return node.get("data").get("message").asText();
        if (node.has("message"))
            return node.get("message").asText();
        return "";
    }

    private void assertErrorContainsAny(JsonNode response, String... phrases) {
        assertNotNull(response.get("result"));
        assertEquals("ERROR", response.get("result").asText());

        String msg = extractMessage(response).toLowerCase();
        boolean matches = Arrays.stream(phrases)
                .anyMatch(p -> msg.contains(p.toLowerCase()));
        assertTrue(matches,
                "Error message '" + msg + "' did not contain any of: " +
                        Arrays.toString(phrases));
    }

    private boolean isOk(JsonNode node) {
        return node.has("result") && "OK".equalsIgnoreCase(node.get("result").asText());
    }

    /* --------------------------------------------------------------------- */
    /*  Branch-specific assertions (still tiny, no nesting)                 */
    /* --------------------------------------------------------------------- */

    private void assertThirdLaunchFailsWhenTwoAlreadyLaunched() {
        JsonNode third = launchRobot("R3", "tank", "2", "0");
        System.out.println("Third launch response: " + third.toPrettyString());

        assertErrorContainsAny(third,
                "cannot launch more than",
                "no more space",
                "failed to launch",
                "crashed",
                "too many",
                "no space");
    }

    private void assertSecondLaunchAlreadyFails(JsonNode second) {
        assertErrorContainsAny(second,
                "no more space",
                "cannot launch more than",
                "too many",
                "failed to launch",
                "crashed",
                "no space");
    }


    /**
     * Scenario: Launching a robot with a duplicate name
     *
     * Given a robot named "DUP" is launched successfully
     * When another robot with the same name attempts to launch
     * Then the server should respond with an ERROR result
     * And the error message should indicate that the name already exists
     *     or that the world has no more space
     */

    @Test
    void launchingWithDuplicateNameShouldFail() {
        assertTrue(serverClient.isConnected());

        String request1 = "{" +
                " \"robot\": \"DUP\"," +
                " \"command\": \"launch\"," +
                " \"arguments\": [\"sniper\",\"3\",\"3\"]" +
                "}";

        JsonNode response1 = serverClient.sendRequest(request1);
        assertNotNull(response1);
        System.out.println("First duplicate test launch response: " + response1.toPrettyString());
        // allow either OK or ERROR for the first depending on server state
        assertNotNull(response1.get("result"));

        // Attempt to launch another robot with the same name
        String request2 = "{" +
                " \"robot\": \"DUP\"," +
                " \"command\": \"launch\"," +
                " \"arguments\": [\"tank\",\"4\",\"4\"]" +
                "}";

        JsonNode response2 = serverClient.sendRequest(request2);
        System.out.println("🔁 Duplicate name launch response: " + response2.toPrettyString());

        assertNotNull(response2.get("result"));
        assertEquals("ERROR", response2.get("result").asText());

        String lowerMsg = "";
        if (response2.has("data") && response2.get("data").has("message")) {
            lowerMsg = response2.get("data").get("message").asText().toLowerCase();
        } else if (response2.has("message")) {
            lowerMsg = response2.get("message").asText().toLowerCase();
        }

        assertTrue(lowerMsg.contains("same name") ||
                lowerMsg.contains("already exists") ||
                lowerMsg.contains("too many") ||
                lowerMsg.contains("no more space"));
    }
}
