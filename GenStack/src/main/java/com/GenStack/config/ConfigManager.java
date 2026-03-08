package com.GenStack.config;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import com.GenStack.helper.DebugUtil;

public class ConfigManager {
    private static ConfigManager instance;
    private JSONObject configData;
    private String appDataFolder;
    private String configFile;

    // Private constructor for singleton
    private ConfigManager(String appDataFolder, String configFile) {
        this.appDataFolder = appDataFolder;
        this.configFile = configFile;
        configData = new JSONObject();
        loadConfig();
    }

    // Public method to get the single instance of ConfigManager
    public static ConfigManager getInstance(String appDataFolder, String configFile) {
        if (instance == null) {
            instance = new ConfigManager(appDataFolder, configFile);
        }
        return instance;
    }

    // Load configurations from a JSON file
    private void loadConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(appDataFolder, configFile)));
            configData = new JSONObject(content);
        } catch (IOException | JSONException e) {
            System.out.println("No configuration file found to load.");
        }
    }

    // Save configurations to a JSON file
    public void saveConfig() {
        try {
            Path path = Paths.get(appDataFolder, configFile);
            Files.createDirectories(path.getParent()); // Create directories if they don't exist
            try (FileWriter file = new FileWriter(path.toFile())) {
                file.write(configData.toString(4)); // Pretty print with an indent of 4
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T castToGeneric(Object value) {
        return (T) value; // Unchecked cast
    }

    public <T> T getSetting(String section, String key, T defaultValue) {
        JSONObject sectionObj = getSection(section);

        if (sectionObj != null && sectionObj.has(key)) {
            Object value = sectionObj.get(key);
            return castToGeneric(value);
        }
        return defaultValue; // Return default if not found
    }

    public <T> void setSetting(String section, String key, T value) {
        JSONObject sectionObj = getSection(section);
        if (sectionObj == null) {
            sectionObj = new JSONObject();
            if (section != null) {
                configData.put(section, sectionObj);
            }
        }
        sectionObj.put(key, value); // Store value directly
        saveConfig(); // Save after setting
    }

    // Helper method to get a specific section
    private JSONObject getSection(String section) {
        if (section == null) {
            return configData; // Return the root config
        }

        String[] keys = section.split("\\.");
        JSONObject currentSection = configData;
        for (String key : keys) {
            if (currentSection.has(key)) {
                currentSection = currentSection.getJSONObject(key);
            } else {
                return null; // Section does not exist
            }
        }
        return currentSection;
    }

    /**
     * Returns the ConfigManager as a JSONObject representation
     * @return JSONObject containing all ConfigManager state
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("appDataFolder", appDataFolder);
        json.put("configFile", configFile);
        json.put("configData", configData);
        return json;
    }

    /**
     * Returns a formatted string representation of the ConfigManager
     * @return String representation using toJSON()
     */
    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int... indentation) {
        int indent = indentation.length > 0 ? indentation[0] : 0;
        return toJSON().toString(indent);
    }
}
