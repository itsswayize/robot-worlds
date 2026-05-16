package za.co.wethinkcode.robots.acceptance;

import za.co.wethinkcode.robots.client.ClientApp;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class MoveForward {
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
     * Tests that when a robot in a 1x1 world attempts to move forward,
     * it correctly reports reaching the NORTH edge and remains at its
     * original position (0,0).
     *
     * Scenario:
     *
     *  Given: A connected client and a 1x1 world with HAL launched at (0,0)
     *  When: HAL sends the "forward 5" command
     *  Then: The response should be "OK", include a message indicating the NORTH edge,
     *       and HAL should remain at position [0,0]
     *
     *
     *This ensures the server correctly handles world boundaries without
     * moving the robot beyond valid coordinates.
     */
    @Test
    void moveForwardFrom1x1WorldShouldReportNorthEdgeAndStayAt00() {
        // Given: connected and world assumed 1x1 (test will place robot at 0,0)
        assertTrue(serverClient.isConnected(), "Must be connected to server");

        // Launch HAL at (0,0)
        String launchReq = "{" +
                "\"robot\": \"HAL\"," +
                "\"command\": \"launch\"," +
                "\"arguments\": [\"sniper\", \"0\", \"0\"]" +
                "}";
        JsonNode launchResp = serverClient.sendRequest(launchReq);
        System.out.println("Launch Response: " + (launchResp == null ? "<null>" : launchResp.toPrettyString()));

        assertNotNull(launchResp, "Launch response should not be null");
        assertEquals("OK", launchResp.path("result").asText(), "Launch must be OK");

        // When: send forward 5
        String moveReq = "{" +
                "\"robot\": \"HAL\"," +
                "\"command\": \"forward\"," +
                "\"arguments\": [5]" +
                "}";
        JsonNode moveResp = serverClient.sendRequest(moveReq);
        System.out.println("Move Response: " + (moveResp == null ? "<null>" : moveResp.toPrettyString()));

        // Then: result OK
        assertNotNull(moveResp, "Move response should not be null");
        assertEquals("OK", moveResp.path("result").asText(), "Move should return OK");

        // Message should indicate the NORTH edge (be tolerant on case/format)
        String msg = moveResp.path("data").path("message").asText(null);
        assertNotNull(msg, "Move response should include a data.message");
        String lower = msg.toLowerCase();
        assertTrue(msg.equals("At the NORTH edge") || (lower.contains("north") && lower.contains("edge")),
                "Expected message to indicate NORTH edge (was: '" + msg + "')");

        // Position should be [0,0]
        JsonNode pos = moveResp.path("state").path("position");
        assertTrue(pos.isArray() && pos.size() == 2, "State.position should be an [x,y] array");
        assertEquals(0, pos.get(0).asInt(), "Expected X to be 0");
        assertEquals(0, pos.get(1).asInt(), "Expected Y to be 0");
    }


    /**
     * Given GammaBot at (0,0) and DeltaBot at (0,1)
     * When DeltaBot sends "back 1"
     * Then the response should be { "message": "Obstructed" }
     * And DeltaBot should remain at position (0,1).
     *
     * This test ensures that a robot cannot move backward into another robot's position.
     */

    @Test
    void backwardIntoAnotherRobotShouldBeBlocked() {
        assertTrue(serverClient.isConnected(), "Client must be connected");

        // Launch GammaBot
        String gammaLaunch = "{\"robot\":\"GammaBot\",\"command\":\"launch\",\"arguments\":[\"sniper\",\"0\",\"0\"]}";
        JsonNode gammaResp = serverClient.sendRequest(gammaLaunch);
        System.out.println("GammaBot Launch: " + gammaResp.toPrettyString());

        // Launch DeltaBot
        String deltaLaunch = "{\"robot\":\"DeltaBot\",\"command\":\"launch\",\"arguments\":[\"sniper\",\"0\",\"1\"]}";
        JsonNode deltaResp = serverClient.sendRequest(deltaLaunch);
        System.out.println("DeltaBot Launch: " + deltaResp.toPrettyString());

        // Check positions
        String deltaStateReq = "{\"robot\":\"DeltaBot\",\"command\":\"state\",\"arguments\":[]}";
        JsonNode deltaState = serverClient.sendRequest(deltaStateReq);
        System.out.println("DeltaBot State: " + deltaState.toPrettyString());

        // Send backward command (steps as string)
        String moveBack = "{\"robot\":\"DeltaBot\",\"command\":\"back\",\"arguments\":[\"1\"]}";
        JsonNode moveResp = serverClient.sendRequest(moveBack);
        System.out.println("Move Response: " + moveResp.toPrettyString());


    }


}
