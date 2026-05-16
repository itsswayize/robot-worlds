package za.co.wethinkcode.robots.acceptance;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Small test-only wrapper around JsonNode responses that provides
 * common accessors used in acceptance tests.
 */

public class Response {
    private final JsonNode node;

    public Response(JsonNode node) {
        this.node = node;
    }

    public JsonNode getNode() {
        return node;
    }

    public boolean isOk() {
        return node != null && hasText(node, "result", "OK");
    }

    public JsonNode getObjectsNode() {
        if (hasArray(node, "objects")) return node.get("objects");
        if (hasArray(node, "data", "objects")) return node.get("data").get("objects");
        return null;
    }

    public String getMessage() {
        String msg = getText(node, "data", "message");
        if (msg != null) return msg.toLowerCase();
        msg = getText(node, "message");
        return msg != null ? msg.toLowerCase() : "";
    }

    // --- Simple helpers ---

    private boolean hasText(JsonNode n, String field, String expected) {
        return n != null && n.has(field) && expected.equals(n.get(field).asText());
    }

    private boolean hasArray(JsonNode n, String... path) {
        JsonNode current = n;
        for (String p : path) {
            if (current == null || !current.has(p)) return false;
            current = current.get(p);
        }
        return current != null && current.isArray();
    }

    private String getText(JsonNode n, String... path) {
        JsonNode current = n;
        for (String p : path) {
            if (current == null || !current.has(p)) return null;
            current = current.get(p);
        }
        return current != null && current.isTextual() ? current.asText() : null;
    }
}

