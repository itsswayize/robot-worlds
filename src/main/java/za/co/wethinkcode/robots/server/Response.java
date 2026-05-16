package za.co.wethinkcode.robots.server;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a standardized response object used for communication between the client and server.
 * Encapsulates a JSON structure with result, message, and optional data for consistency.
 */
public class Response {
    public final JSONObject object;

    public static Response responseFromJSONString(String string) {
    try {
        // Try to parse as JSON first
        JSONObject jsonObject = new JSONObject(string);
        return new Response(jsonObject);
    } catch (JSONException e) {
        // Try to parse as Java object string (e.g., WorldResponse{result='OK', ...})
        if (string != null && string.startsWith("WorldResponse{")) {
            String result = extractField(string, "result");
            String message = extractField(string, "message");
            if (message.isEmpty()) message = extractField(string, "data"); // fallback
            JSONObject obj = new JSONObject();
            obj.put("result", result != null ? result : "ERROR");
            obj.put("message", message != null ? message : string);
            return new Response(obj);
        }
        // fallback: wrap error
        JSONObject error = new JSONObject();
        error.put("result", "ERROR");
        error.put("message", "Invalid server response: " + string);
        return new Response(error);
    }
}

    public Response(String result, String message) {
        this.object = new JSONObject();
        this.object.put("result", result);
        this.object.put("message", message);
    }
    
    public Response(JSONObject object) {
        this.object = object;
    }

    private static String extractField(String input, String field) {
    String pattern = field + "='([^']*)'";
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(input);
    if (matcher.find()) {
        return matcher.group(1);
    }
    return "";
}

    public static Response ok(JSONObject data, String message) {
        Response response = new Response("OK", message != null ? message : "");
        response.object.put("data", data);
        return response;
    }

    public String getMessage() {
        // Prefer "message" in data, then top-level "message"
        if (object.has("data") && object.get("data") instanceof JSONObject) {
            JSONObject data = object.getJSONObject("data");
            if (data.has("message")) return data.getString("message");
        }
        if (object.has("message")) return object.getString("message");
        // Fallback: show the result or a generic message
        if (object.has("result")) return object.getString("result");
        return "(no message from server)";
    }

    public String toJSONString() {
        return this.object.toString();
    }

     public JSONObject getData() {
        return object.optJSONObject("data");
    }

    // Add after your existing methods, e.g. after getState()
    public Response withState(JSONObject state) {
        this.object.put("state", state);
        return this;
    }

    public static Response error(JSONObject data, String message) {
        Response response = new Response("ERROR", message != null ? message : "");
        response.object.put("data", data);
        return response;
    }

    public JSONObject getState() {
        return object.optJSONObject("state");
    }

    public boolean isOKResponse() {
        return this.object.getString("result").equalsIgnoreCase("OK");
    }
}
