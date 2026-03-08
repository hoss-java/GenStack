package com.GenStack.helper;

import org.json.JSONObject;

public class ResponseHelper {
    private static final String DEFAULT_VERSION = "1.0"; // Default version

    public ResponseHelper() {
    }

    public static JSONObject createInvalidCommandResponse(String commandId, String version) {
        JSONObject response = new JSONObject();
        response.put("version", version);
        response.put("message", "Invalid command: " + commandId);
        return response;
    }

    public static JSONObject createInvalidCommandResponse(String commandId) {
        return createInvalidCommandResponse(commandId, DEFAULT_VERSION);
    }

    // Overloaded method with version
    public static JSONObject createResponse(String message, JSONObject data, String version) {
        JSONObject response = new JSONObject();
        response.put("version", version);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }

    // Overloaded method without version
    public static JSONObject createResponse(String message, JSONObject data) {
        return createResponse(message, data, DEFAULT_VERSION); // Calls the overload with default version
    }
}
