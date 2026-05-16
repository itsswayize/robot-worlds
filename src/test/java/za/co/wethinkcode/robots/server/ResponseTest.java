package za.co.wethinkcode.robots.server;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ResponseTest {

    @Test
    public void testOkResponse() {
        JSONObject data = new JSONObject();
        data.put("key", "value");

        Response response = Response.ok(data, "Request successful");


        assertTrue(response.object.has("result"), "Response should contain 'result' key.");
        assertEquals("OK", response.object.getString("result")); // Ensure this matches actual output

        assertTrue(response.object.has("message"), "Response should contain 'message' key.");
        assertEquals("Request successful", response.object.getString("message"));

        assertTrue(response.object.has("data"), "Response should contain 'data' key.");
        assertEquals(data.toString(), response.object.getJSONObject("data").toString());
    }

    @Test
    public void testErrorResponse() {
        Response response = new Response("ERROR", "An error occurred");

        assertEquals("ERROR", response.object.getString("result"));
        assertEquals("An error occurred", response.object.getString("message"));
        assertFalse(response.object.has("data"), "Error response should not have data.");
    }

    @Test
    public void testEmptyOkResponse() {
        Response response = Response.ok(new JSONObject(), "No data");

        assertEquals("OK", response.object.getString("result"));
        assertEquals("No data", response.object.getString("message"));
        assertTrue(response.object.has("data"));
        assertTrue(response.object.getJSONObject("data").isEmpty(), "Data should be empty.");
    }

    @Test
    public void testMessageHandling() {
        Response response = new Response("OK", "Test message");

        assertEquals("Test message", response.object.getString("message"));
        assertEquals("OK", response.object.getString("result"));
    }
}