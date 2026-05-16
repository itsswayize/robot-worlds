package za.co.wethinkcode.robots.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerTest {
    private static final int TEST_PORT = 1234;

    @BeforeAll
     public static void setUpServer() throws Exception {
         // Disable HTTP server for tests
         System.setProperty("isTest", "true");
         new Thread(() -> {
             try {
                 // Start server on the test port directly (no stdin needed)
                 za.co.wethinkcode.robots.server.Server.start(TEST_PORT);
             } catch (Exception e) {
                 System.err.println("Failed to start test server: " + e);
                 e.printStackTrace(System.err);
             }
         }).start();

        // Increased delay to ensure server is ready
        Thread.sleep(3000);
    }

    @Test
    @Order(1)
    public void testLaunchCommand() throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send a launch command
            String request = "{\"robot\":\"TestBot\",\"command\":\"launch\",\"arguments\":[\"shooter\"]}";
            out.write(request);
            out.newLine();
            out.flush();

            // Read response
            String response = in.readLine();
            assertNotNull(response, "Server should respond");
            assertTrue(response.contains("result"), "Response should contain a result field");
            assertTrue(response.contains("TestBot"), "Response should mention the robot name");
        }
    }

    @Test
    @Order(2)
    public void testInvalidCommand() throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send an invalid command
            String request = "{\"robot\":\"InvalidBot\",\"command\":\"fly\",\"arguments\":[]}";
            out.write(request);
            out.newLine();
            out.flush();

            // Read response
            String response = in.readLine();
            System.out.println("Server response: " + response); // Debug output
            assertNotNull(response, "Server response should not be null");

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonResponse = mapper.readTree(response);
                boolean hasError = (jsonResponse.has("result") && jsonResponse.get("result").asText().equalsIgnoreCase("ERROR"))
                        || (jsonResponse.has("message") && jsonResponse.get("message").asText().toLowerCase().contains("invalid"))
                        || (jsonResponse.has("message") && jsonResponse.get("message").asText().toLowerCase().contains("unknown"))
                        || (jsonResponse.has("message") && jsonResponse.get("message").asText().toLowerCase().contains("unsupported"));
                assertTrue(hasError, "Response should indicate an error for invalid command. Actual: " + response);
            } catch (Exception e) {
                fail("Server response is not valid JSON: " + response, e);
            }
        }
    }

    @Test
    @Order(3)
    public void testServerShutdown() throws Exception {
        Server.shutdown();

        Thread.sleep(500);

        assertThrows(IOException.class, () -> {
            Socket s = null;
            try {
                s = new Socket("localhost", TEST_PORT);
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException ignored) {
                        // ignore
                    }
                }
            }
        });
    }
}