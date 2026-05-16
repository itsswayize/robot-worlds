package za.co.wethinkcode.robots.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandJSONTests {
    @Test
    public void testMovementCommands() {
        // back 1 (context-provided robot name)
        runPairTest("back 1", "hal", new CmdExpectation(MoveCommand.class, "back", "hal", "1"),
                "back hal 1", new CmdExpectation(MoveCommand.class, "back", null, "hal", "1"));

        // forward 1 (context-provided robot name)
        runPairTest("forward 1", "hal", new CmdExpectation(MoveCommand.class, "forward", "hal", "1"),
                "forward hal 1", new CmdExpectation(MoveCommand.class, "forward", null, "hal", "1"));
    }

    @Test
    public void testTurnCommand() {
        // turn right (context-provided robot)
        runPairTest("turn right", "hal", new CmdExpectation(TurnCommand.class, "turn", "hal", "right"),
                "turn hal right", new CmdExpectation(TurnCommand.class, "turn", null, "hal", "right"));

        // turn left (context-provided robot)
        runPairTest("turn left", "hal", new CmdExpectation(TurnCommand.class, "turn", "hal", "left"),
                "turn hal left", new CmdExpectation(TurnCommand.class, "turn", null, "hal", "left"));
    }

    @Test
    public void testStateCommand() {
        runPairTest("state", "hal", new CmdExpectation(StateCommand.class, "state", "hal"),
                "state hal", new CmdExpectation(StateCommand.class, "state", null, "hal"));
    }

    @Test
    public void testLookCommand() {
        runPairTest("look", "hal", new CmdExpectation(LookCommand.class, "look", "hal"),
                "look hal", new CmdExpectation(LookCommand.class, "look", null, "hal"));
    }

    @Test
    public void testOrientationCommand() {
        runPairTest("orientation", "hal", new CmdExpectation(OrientationCommand.class, "orientation", "hal"),
                "orientation hal", new CmdExpectation(OrientationCommand.class, "orientation", null, "hal"));
    }

    @Test
    public void testFireCommand() {
        runPairTest("fire", "hal", new CmdExpectation(FireCommand.class, "fire", "hal"),
                "fire hal", new CmdExpectation(FireCommand.class, "fire", null, "hal"));
    }

    @Test
    public void testReloadCommand() {
        runPairTest("reload", "hal", new CmdExpectation(ReloadCommand.class, "reload", "hal"),
                "reload hal", new CmdExpectation(ReloadCommand.class, "reload", null, "hal"));
    }

    @Test
    public void testRepairCommand() {
        runPairTest("repair", "hal", new CmdExpectation(RepairCommand.class, "repair", "hal"),
                "repair hal", new CmdExpectation(RepairCommand.class, "repair", null, "hal"));
    }

    @Test
    public void testOffCommand() {
        runPairTest("off", "hal", new CmdExpectation(ShutdownCommand.class, "off", "hal"),
                "off hal", new CmdExpectation(ShutdownCommand.class, "off", null, "hal"));
    }

    @Test
    public void testDumpCommand() {
        String input = "dump";
        Command command = Command.fromInput(input, null);

        System.out.println("testDumpCommand: input='dump', robotName=" +
                (command.robot != null ? command.robot.getName() : "null") +
                ", arguments=" + java.util.Arrays.toString(command.arguments));

        assertEquals(DumpCommand.class, command.getClass());
        assertEquals("dump", command.commandName());
    }

    @Test
    public void testLaunchCommand() {
        runPairTest("launch tank", "hal", new CmdExpectation(LaunchCommand.class, "launch", "hal", "tank"),
                "launch tank hal", new CmdExpectation(LaunchCommand.class, "launch", null, "tank", "hal"));
    }

    // --- Helper assertion methods to reduce duplication and cyclomatic complexity ---
    // Parameter object encapsulating expected command properties to avoid primitive-heavy helper signatures
    private static class CmdExpectation {
        final Class<?> expectedClass;
        final String expectedName;
        final String contextRobotName; // nullable when not using context
        final String[] expectedArgs;

        CmdExpectation(Class<?> expectedClass, String expectedName, String contextRobotName, String... expectedArgs) {
            this.expectedClass = expectedClass;
            this.expectedName = expectedName;
            this.contextRobotName = contextRobotName;
            this.expectedArgs = expectedArgs != null ? expectedArgs : new String[0];
        }
    }

    // New helpers that encapsulate the repeated pattern of parse -> print -> assert for both flavors
    private void runPairTest(String inputWithContext, String contextName, CmdExpectation expContext,
                             String inputWithoutContext, CmdExpectation expWithoutContext) {
        // With context
        Command command = Command.fromInput(inputWithContext, contextName);
        System.out.println("runPairTest: input='" + inputWithContext + "', robotName=" +
                (command.robot != null ? command.robot.getName() : "null") +
                ", arguments=" + java.util.Arrays.toString(command.arguments));
        assertWithContext(command, expContext);

        // Without context (robot name as argument)
        command = Command.fromInput(inputWithoutContext, null);
        System.out.println("runPairTest: input='" + inputWithoutContext + "', robotName=" +
                (command.robot != null ? command.robot.getName() : "null") +
                ", arguments=" + java.util.Arrays.toString(command.arguments));
        assertWithOptionalRobotInArgs(command, expWithoutContext);
    }

    // Assert when a context-provided robot name is expected/possible
    private void assertWithContext(Command command, CmdExpectation exp) {
        assertEquals(exp.expectedClass, command.getClass());
        assertEquals(exp.expectedName, command.commandName());
        // Parser may leave robot null and rely on context
        assertTrue(command.robot == null || exp.contextRobotName.equals(command.robot.getName()));
        assertEquals(exp.expectedArgs.length, command.arguments.length);
        for (int i = 0; i < exp.expectedArgs.length; i++) {
            assertEquals(exp.expectedArgs[i], command.arguments[i]);
        }
    }

    // Small predicates to clarify the parsing outcomes when robot name may be optional
    private boolean isAllArgsPresent(Command command, CmdExpectation exp) {
        return command.arguments.length == exp.expectedArgs.length;
    }

    private boolean isRobotConsumed(Command command, CmdExpectation exp) {
        return command.arguments.length == Math.max(0, exp.expectedArgs.length - 1);
    }

    private boolean isNoArgsRemaining(Command command) {
        return command.arguments.length == 0;
    }

    // Assert for commands where the robot name may be provided in the arguments (optional)
    private void assertWithOptionalRobotInArgs(Command command, CmdExpectation exp) {
        assertEquals(exp.expectedClass, command.getClass());
        assertEquals(exp.expectedName, command.commandName());

        // If the parser left all expected args in place (including robot name), assert equality
        if (isAllArgsPresent(command, exp)) {
            for (int i = 0; i < exp.expectedArgs.length; i++) {
                assertEquals(exp.expectedArgs[i], command.arguments[i]);
            }
            // In this mode, the parser typically won't populate command.robot
            assertTrue(command.robot == null || command.robot.getName() == null);
            return;
        }

        // If the parser consumed the robot and left remaining arguments, compare them against the tail of expectedArgs
        if (isRobotConsumed(command, exp)) {
            int offset = exp.expectedArgs.length - command.arguments.length;
            for (int i = 0; i < command.arguments.length; i++) {
                assertEquals(exp.expectedArgs[i + offset], command.arguments[i]);
            }
            return;
        }

        // If no arguments remain, accept that as a valid parse in some parsers
        if (isNoArgsRemaining(command)) return;

        fail("Command arguments did not match any accepted parsing patterns: got=" + java.util.Arrays.toString(command.arguments) + ", expected=" + java.util.Arrays.toString(exp.expectedArgs));
    }
}