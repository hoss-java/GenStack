package com.GenStack.helper;

import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.io.OutputStream;

public class JSONHelper {

    public JSONHelper() {
    }

    // This method must be defined within the Main class
    public static JSONObject loadJsonFromFile(String fileName) {
        try (InputStream inputStream = JSONHelper.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("File not found: " + fileName);
            }
            // Read the input stream into a string
            String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Create JSONObject using LinkedHashMap to preserve order
            return new JSONObject(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void traverseAndPrint(JSONObject jsonObject, String parentKey) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            if (value instanceof JSONObject) {
                // Recursive call for nested JSONObject
                traverseAndPrint((JSONObject) value, fullKey);
            } else {
                // Print the key and value
                System.out.println(fullKey + ": " + value);
            }
        }
    }

    public static Map<String, String> createEventFields(JSONObject json) {
        Map<String, String> eventFields = new HashMap<>();

        // Iterate through the keys of the JSONObject
        for (String key : json.keySet()) {
            // Get value as string and put it in the map
            eventFields.put(key, json.get(key).toString());
        }

        // Validate required fields if necessary
        validateRequiredFields(eventFields);

        return eventFields;
    }

    private static void validateRequiredFields(Map<String, String> eventFields) {
        if (!eventFields.containsKey("uid")) {
            throw new IllegalArgumentException("Field 'uid' is required.");
        }
        if (!eventFields.containsKey("title")) {
            throw new IllegalArgumentException("Field 'title' is required.");
        }
        // Add validation for additional required fields if necessary
    }

    public static JSONObject getJsonValue(JSONObject jsonObject, String key) {
        // Check if the key exists and if its value is a JSONObject
        if (jsonObject.has(key)) {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                return (JSONObject) value; // Return the JSONObject
            }
        }
        return null; // Return null if the key is absent or not a JSONObject
    }

    public static JSONObject updateJSONObject(JSONObject args, String[][] keyValuePairs) {
        // Iterate through each key-value pair in the array
        for (String[] pair : keyValuePairs) {
            if (pair.length != 2) {
                throw new IllegalArgumentException("Each pair must contain exactly two elements: [key, value].");
            }

            String key = pair[0];
            int value; // Assuming the value is always an integer
            
            try {
                value = Integer.parseInt(pair[1]); // Convert string to integer
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value for key '" + key + "' must be an integer.");
            }

            // Update or add the key-value pair in the JSONObject
            args.put(key, value);
        }

        return args; // Return the updated JSONObject
    }
}