package za.co.wethinkcode.robots.handlers;

import za.co.wethinkcode.robots.domain.Robot;
import za.co.wethinkcode.robots.server.Response;
import za.co.wethinkcode.robots.domain.World;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class ReloadService {
    private final World world;

    public ReloadService(World world) {
        this.world = world;
    }

    public void handleReload(Robot inputRobot, CommandHandler.CompletionHandler completionHandler) {
        if (inputRobot == null) {
            completionHandler.onComplete(makeError("Could not find robot: null"));
            return;
        }

        Robot robot = world.findRobot(inputRobot.getName());
        if (robot == null) {
            completionHandler.onComplete(makeError("Could not find robot: " + inputRobot.getName()));
            return;
        }

        if (robot.isReloading()) {
            completionHandler.onComplete(makeError(robot.getName() + " is already reloading."));
            return;
        }

        robot.setReloading(true);
        completionHandler.onComplete(new Response("OK", robot.getName() + " is now reloading."));

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                robot.setReloading(false);
                robot.setShots(robot.getMaxShots());
                Response response = new Response("OK", robot.getName() + " is done.");
                world.stateForRobot(robot, response);
                completionHandler.onComplete(response);
            }
        }, world.getReloadTime() * 1000L);
    }

    private Response makeError(String msg) {
        Response r = new Response("ERROR", msg);
        JSONObject data = new JSONObject();
        data.put("message", msg);
        r.object.put("data", data);
        return r;
    }
}

