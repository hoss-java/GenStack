package com.GenStack.payload;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import com.GenStack.helper.DebugUtil;
import com.GenStack.payload.PayloadCommand;

/**
 * Represents a payload containing an identifier, optional data, 
 * and a list of commands.
 */
public class Payload {
    private String identifier;  /**< The identifier of the payload */
    private Map<String, Object> data;  /**< Optional data associated with the payload */
    private List<PayloadCommand> commands;  /**< List of commands associated with the payload */

    // Getters

    /**
     * Gets the identifier of the payload.
     * 
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the optional data map associated with the payload.
     * 
     * @return a map of data, or null if not set
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Gets the list of commands associated with the payload.
     * 
     * @return a list of PayloadCommand instances, or null if not set
     */
    public List<PayloadCommand> getCommands() {
        return commands;
    }

    // Setters

    /**
     * Sets the identifier of the payload.
     * 
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the optional data map for the payload.
     * 
     * @param data the data to associate with this payload
     */
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * Sets the list of commands associated with the payload.
     * 
     * @param commands the list of commands to associate with this payload
     */
    public void setCommands(List<PayloadCommand> commands) {
        this.commands = commands;
    }

    /**
     * Converts this Payload instance to a JSON object.
     * 
     * @return a JSONObject representing the Payload instance
     */
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();

        // Add identifier
        jsonObject.put("identifier", identifier);

        // Create JSON object for data map
        if (data != null) {
            JSONObject dataJson = new JSONObject();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                dataJson.put(entry.getKey(), entry.getValue());
            }
            jsonObject.put("data", dataJson);
        }

        // Create JSON array for commands list
        if (commands != null) {
            JSONArray commandsJson = new JSONArray();
            for (PayloadCommand command : commands) {
                commandsJson.put(command.toJSON()); // Assuming PayloadCommand has a toJSON method
            }
            jsonObject.put("commands", commandsJson);
        }

        return jsonObject;
    }

    /**
     * Returns a string representation of this Payload instance in JSON format.
     * 
     * @return a JSON string representation of this instance
     */
    @Override
    public String toString() {
        return toJSON().toString();
    }
}
