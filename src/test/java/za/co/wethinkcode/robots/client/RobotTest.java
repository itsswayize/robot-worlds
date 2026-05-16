package  za.co.wethinkcode.robots.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import za.co.wethinkcode.robots.domain.Robot;

public class RobotTest {
    private Robot robot;

    @BeforeEach
    public void setUp() {
        robot = new Robot("TestBot", "tank", 0, 0);
    }

    @Test
    public void testInitialState() {
        assertEquals("TestBot", robot.getName());
        assertEquals("tank", robot.getMake());
        assertEquals(10, robot.getShields());
        assertEquals(3, robot.getShots());
        assertEquals(Robot.RobotStatus.Normal, robot.status);
        assertEquals(0, robot.getX());
        assertEquals(0, robot.getY());
    }

    @Test
    public void testMoveForward() {
        robot.moveForward(2);
        assertEquals(0, robot.getX());
        assertEquals(2, robot.getY());
    }

    @Test
    public void testMoveBackward() {
        robot.moveBackward(1);
        assertEquals(0, robot.getX());
        assertEquals(-1, robot.getY());
    }

    @Test
    public void testTurnLeft() {
        robot.turnLeft();
        assertEquals("WEST", robot.orientation());
    }

    @Test
    public void testTurnRight() {
        robot.turnRight();
        assertEquals("EAST", robot.orientation());
    }

}