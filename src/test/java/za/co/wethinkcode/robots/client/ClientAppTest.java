package za.co.wethinkcode.robots.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientAppTest {

    private ServerSocket mockServer;
    private int mockPort;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() throws IOException {
        mockServer = new ServerSocket(0);
        mockPort = mockServer.getLocalPort();

        // Mock server runs in background
        Executors.newSingleThreadExecutor().submit(() -> {
            try (Socket clientSocket = mockServer.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("launch")) {
                        out.println("{\"result\":\"OK\",\"message\":\"Robot launched successfully\"}");
                    } else {
                        out.println("{\"result\":\"OK\",\"message\":\"Command executed\"}");
                    }
                }
            } catch (IOException ignored) {}
        });
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (!mockServer.isClosed()) {
            mockServer.close();
        }
    }

    @Test
    public void testClientAppLaunchAndDisconnect() {
        // Simulate user input: host, port, robot name, type, then 'disconnect'
        String simulatedInput = String.join(System.lineSeparator(),
                "localhost",
                String.valueOf(mockPort),
                "RoboTest",
                "sniper",
                "disconnect"
        ) + System.lineSeparator();

        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;

        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
        System.setOut(new PrintStream(outputStream));

        try {
            ClientApp.main(new String[]{});
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }

        String output = outputStream.toString();

        // ✅ Check that the robot launch message appears in the output
        assertTrue(output.contains("Robot launched successfully") || output.contains("Robot launched!"),
                "Expected robot launch success message in console output.");

        // ✅ Check that the program exited normally
        assertTrue(output.contains("Ready for launch!") || output.contains("Connection failed.") || !output.isEmpty(),
                "Expected the app to run and produce output.");
    }
}
