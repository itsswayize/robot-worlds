package za.co.wethinkcode.robots.tools;

import za.co.wethinkcode.robots.persistance.WorldRepository;

import java.util.List;

public class ListRunner {
    public static void main(String[] args) {
        try (WorldRepository repo = new WorldRepository()) {
            List<String> worlds = repo.listWorlds();
            System.out.println("Saved worlds (count=" + (worlds == null ? 0 : worlds.size()) + "):\n");
            if (worlds != null) {
                for (String w : worlds) System.out.println(w);
            }
        } catch (Exception e) {
            System.err.println("Error listing worlds: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

