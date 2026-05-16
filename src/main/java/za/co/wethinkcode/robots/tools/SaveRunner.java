package za.co.wethinkcode.robots.tools;

import za.co.wethinkcode.robots.commands.SaveCommand;
import za.co.wethinkcode.robots.domain.World;

public class SaveRunner {
    public static void main(String[] args) {
        World w = new World(5, 5);
        w.generateObstacles();
        SaveCommand cmd = new SaveCommand(w, "test_world_runner");
        String result = cmd.execute();
        System.out.println("SaveCommand result: " + result);
    }
}

