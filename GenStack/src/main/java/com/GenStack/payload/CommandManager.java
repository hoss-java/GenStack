package com.GenStack.payload;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;

public class CommandManager {
    private JSONObject commandsJson;

    private String appDataFolder;
    /**
     * Constructor accepts a JSONObject (not a string)
     */
    public CommandManager(String appDataFolder, JSONObject commandsJson) {
        this.appDataFolder = appDataFolder;
        this.commandsJson = commandsJson;
        refineCommandsJson();
    }

    private void refineCommandsJson() {
        List<String> appIds = getAllAppIds();
        
        for (String appId : appIds) {
            JSONObject app = getApp(appId);
            if (app != null) {
                List<String> commandIds = getAllCommandIds(app);
                for (String commandId : commandIds) {
                    JSONObject args = getCommandArgs(app, commandId);
                    if (args != null && args.length() > 0) {
                        refineArgsFields(appId, args);
                    }
                }
            }
        }
    }

    private void refineArgsFields(String appId, JSONObject args) {
        Iterator<String> keys = args.keys();
        
        while (keys.hasNext()) {
            String fieldName = keys.next();
            JSONObject field = args.optJSONObject(fieldName);
            
            if (field != null) {
                // Check if this field has a "referencedfield" object with a "link" property
                JSONObject referencedField = field.optJSONObject("referencedfield");
                if (referencedField != null && referencedField.has("link")) {
                    // Add "type": "str" to the referencedfield object
                    String namespace = appId;
                    TokenizedString linkFieldTokens = new TokenizedString(referencedField.getString("link"),":");
                    String fieldIdStr = linkFieldTokens.getPart(1);
                    if ( fieldIdStr != null ){
                        TokenizedString fieldIdTokens = new TokenizedString(fieldIdStr,".");
                        String linkId = fieldIdTokens.getPart(0);
                        String linkField = fieldIdTokens.getPart(1);
                        if ( linkId != null  && linkField != null ){
                            String linkFieldType = getFieldProperty(appId, linkId+"@"+appId,linkField,"type").toString();
                            if ( linkFieldType != null ){
                                referencedField.put("type", "str");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get all app IDs from external JSON
     */
    public List<String> getAllAppIds() {
        List<String> appIds = new ArrayList<>();
        JSONArray apps = commandsJson.optJSONArray("apps");
        
        if (apps == null) {
            return appIds;
        }
        
        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.getJSONObject(i);
            appIds.add(app.getString("id"));
        }
        
        return appIds;
    }

    /**
     * Get an app by its ID from external JSON
     */
    public JSONObject getApp(String appId) {
        JSONArray apps = commandsJson.optJSONArray("apps");
        
        if (apps == null) {
            return null;
        }
        
        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.getJSONObject(i);
            if (app.getString("id").equals(appId)) {
                return app;
            }
        }
        
        return null;
    }

    /**
     * Get a command by app ID and command ID
     * Recursively searches through nested commands
     * 
     * @param appId the application ID (e.g., "genstack")
     * @param commandId the command ID (e.g., "event.getsall")
     * @return JSONObject containing the command definition, or null if not found
     */
    public JSONObject getCommand(JSONObject app, String commandId) {
        if (app == null) {
            return null;
        }

        // Get the commands array from the app
        JSONArray commands = app.optJSONArray("commands");
        if (commands == null) {
            return null;
        }

        // Recursively search through commands
        return searchCommand(commands, commandId);
    }

    public JSONObject getCommand(String appId, String commandId) {
        return getCommand(getApp(appId), commandId);
    }

    public String getCommandId(JSONObject app, String commandId) {
        JSONObject command = getCommand(app, commandId);
        if (command == null) {
            return "";
        }
        return command.optString("id", "");
    }

    public String getCommandId(String appId, String commandId) {
        return getCommandId(getApp(appId), commandId);
    }

    public String getCommandDescription(JSONObject app, String commandId) {
        JSONObject command = getCommand(app, commandId);
        if (command == null) {
            return "";
        }
        return command.optString("description", "");
    }

    public String getCommandDescription(String appId, String commandId) {
        return getCommandDescription(getApp(appId), commandId);
    }

    public JSONObject getCommandArgs(JSONObject app, String commandId) {
        JSONObject command = getCommand(app, commandId);
        if (command == null) {
            return new JSONObject();
        }
        JSONObject args = command.optJSONObject("args");
        return args != null ? args : new JSONObject();
    }

    public JSONObject getCommandArgs(String appId, String commandId) {
        return getCommandArgs(getApp(appId), commandId);
    }

    /**
     * Get all command IDs from an app
     * Recursively collects all command IDs including nested ones
     * 
     * @param appId the application ID (e.g., "genstack")
     * @return List of all command IDs in the app
     */
    public List<String> getAllCommandIds(JSONObject app) {
        List<String> commandIds = new ArrayList<>();
        
        if (app == null) {
            return commandIds;
        }

        // Get the commands array from the app
        JSONArray commands = app.optJSONArray("commands");
        if (commands == null) {
            return commandIds;
        }

        // Recursively collect all command IDs
        collectAllCommandIds(commands, commandIds);
        
        return commandIds;
    }
    public List<String> getAllCommandIds(String appId){
        return getAllCommandIds(getApp(appId));
    }


    /**
     * Get all command IDs from an app
     * Recursively collects all command IDs including nested ones
     * 
     * @param appId the application ID (e.g., "genstack")
     * @return List of all command IDs in the app
     */
    public List<String> getAllSectionsIds(String appId){
        List<String> sectionIds = new ArrayList<>();

        JSONObject app = getApp(appId);
        if (app == null) {
            return sectionIds;
        }

        // Get the commands array from the app
        JSONArray commands = app.optJSONArray("commands");
        if (commands == null) {
            return sectionIds;
        }

        // Recursively collect all command IDs
        collectAllCommandIds(commands, sectionIds, appId);
        
        return sectionIds;
    }

    /**
     * Get only executable command IDs from an app (leaf commands only)
     * Recursively collects only commands that have an "action" field
     * 
     * @param appId the application ID (e.g., "genstack")
     * @return List of executable command IDs in the app
     */
    public List<String> getLeafCommandIds(JSONObject app) {
        List<String> commandIds = new ArrayList<>();
        
        if (app == null) {
            return commandIds;
        }

        // Get the commands array from the app
        JSONArray commands = app.optJSONArray("commands");
        if (commands == null) {
            return commandIds;
        }

        // Recursively collect only leaf command IDs
        collectLeafCommandIds(commands, commandIds);
        
        return commandIds;
    }
    public List<String> getLeafCommandIds(String appId) {
        return getLeafCommandIds(getApp(appId));
    }

    public Object getFieldProperty(String appId, String sectionId, String fieldName, String propertyName) {
        JSONObject field = getFieldFromSection(appId, sectionId, fieldName);
        if (field != null) {
            return field.opt(propertyName);
        }
        return null;
    }

    public JSONObject getFieldFromSection(String appId, String sectionId, String fieldName) {
        JSONObject section = findSectionRecursively(getApp(appId), sectionId);
        if (section != null) {
            // Search for the field in all commands under this section
            JSONArray commands = section.optJSONArray("commands");
            return findFieldInCommands(commands, fieldName);
        }
        return null;
    }

    private JSONObject findSectionRecursively(JSONObject app, String sectionId) {
        if (app == null) return null;
        
        JSONArray commands = app.optJSONArray("commands");
        return searchSectionInCommands(commands, sectionId);
    }

    private JSONObject searchSectionInCommands(JSONArray commands, String sectionId) {
        if (commands == null) return null;
        
        for (int i = 0; i < commands.length(); i++) {
            JSONObject item = commands.optJSONObject(i);
            if (item != null) {
                if (sectionId.equals(item.optString("id"))) {
                    return item;
                }
                JSONObject found = searchSectionInCommands(item.optJSONArray("commands"), sectionId);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JSONObject findFieldInCommands(JSONArray commands, String fieldName) {
        if (commands == null) return null;
        
        for (int i = 0; i < commands.length(); i++) {
            JSONObject command = commands.optJSONObject(i);
            if (command != null) {
                JSONObject args = command.optJSONObject("args");
                if (args != null && args.has(fieldName)) {
                    return args.optJSONObject(fieldName);
                }
            }
        }
        return null;
    }

    /**
     * Test and print the entire command structure
     */
    public void testCommandManager() {
        new CommandManagerTester(this).test();
    }

    /**
     * Recursively search through command tree to find a command by ID
     * 
     * @param commands JSONArray of commands to search
     * @param commandId the command ID to find
     * @return JSONObject containing the command definition, or null if not found
     */
    private JSONObject searchCommand(JSONArray commands, String commandId) {
        for (int i = 0; i < commands.length(); i++) {
            JSONObject command = commands.getJSONObject(i);
            
            // Check if this command matches the ID
            if (command.getString("id").equals(commandId)) {
                return command;
            }

            // If this command has nested commands, search recursively
            if (command.has("commands")) {
                JSONArray nestedCommands = command.getJSONArray("commands");
                JSONObject result = searchCommand(nestedCommands, commandId);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Recursively collect all command IDs from command tree
     * Collects ALL commands (both leaf commands and commands with nested commands)
     * 
     * @param commands JSONArray of commands to process
     * @param commandIds List to accumulate command IDs
     * @param appIds limit list to only sections id
     */
    private void collectAllCommandIds(JSONArray commands, List<String> commandIds, String appId) {
        for (int i = 0; i < commands.length(); i++) {
            JSONObject command = commands.getJSONObject(i);
            String commandId = command.getString("id");
            
            // Add this command's ID
            if ((appId != null && commandId.matches(".*@[^@]+$")) || appId == null) {
                commandIds.add(commandId);
            }
            
            // If this command has nested commands, recurse into them
            if (command.has("commands")) {
                JSONArray nestedCommands = command.getJSONArray("commands");
                collectAllCommandIds(nestedCommands, commandIds, appId);
            }
        }
    }
    private void collectAllCommandIds(JSONArray commands, List<String> commandIds) {
        collectAllCommandIds(commands, commandIds, null);
    }

    /**
     * Recursively collect only leaf command IDs (commands with "action" field)
     * 
     * @param commands JSONArray of commands to process
     * @param commandIds List to accumulate command IDs
     */
    private void collectLeafCommandIds(JSONArray commands, List<String> commandIds) {
        for (int i = 0; i < commands.length(); i++) {
            JSONObject command = commands.getJSONObject(i);
            
            // If this command has an "action" field, it's executable
            if (command.has("action")) {
                commandIds.add(command.getString("id"));
            }
            
            // If this command has nested commands, recurse into them
            if (command.has("commands")) {
                JSONArray nestedCommands = command.getJSONArray("commands");
                collectLeafCommandIds(nestedCommands, commandIds);
            }
        }
    }

    /**
     * Get the generated commands JSON
     */
    public JSONObject getCommandsJson() {
        return commandsJson;
    }

    /**
     * Save the generated JSON to a file with default path and overwrite if exists
     */
    public void saveToFile() {
        String defaultPath = Paths.get(appDataFolder, "commands.json").toString();
        saveToFile(commandsJson, defaultPath);
    }

    public void saveToFile(JSONObject commands) {
        String defaultPath = Paths.get(appDataFolder, "commands.json").toString();
        saveToFile(commands, defaultPath);
    }

    public void saveToFile(JSONObject commands, String outputFilePath) {
        try {
            if (commands != null) {
                String jsonString = commands.toString(2);
                Files.write(Paths.get(outputFilePath), jsonString.getBytes(), 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save commands to file: " + outputFilePath, e);
        }
    }

    /**
     * Save the generated JSON to a file
     */
    public void saveToFile(String outputFilePath) {
        saveToFile(commandsJson, outputFilePath);
    }

    /**
     * Private inner class for testing and debugging CommandManager structure
     */
    private class CommandManagerTester {
        private final CommandManager commandManager;
        
        CommandManagerTester(CommandManager commandManager) {
            this.commandManager = commandManager;
        }
        
        /**
         * Execute the test and print all command structure information
         */
        public void test() {
            System.out.println("=== COMMAND STRUCTURE TEST ===");
            
            List<String> allAppIds = commandManager.getAllAppIds();
            System.out.println("Total Apps: " + allAppIds.size());
            
            for (String appId : allAppIds) {
                printSection("APP: " + appId, "═");
                
                JSONObject app = commandManager.getApp(appId);
                if (app != null) {
                    printField("Description", getStringOrDefault(app, "description", "No description"));
                    
                    List<String> commandIds = commandManager.getAllCommandIds(appId);
                    System.out.println("Total Commands: " + commandIds.size());
                    
                    int commandIndex = 0;
                    for (String commandId : commandIds) {
                        commandIndex++;
                        printCommand(appId, commandId, commandIndex == commandIds.size());
                    }
                }
            }
            
            printSection("END OF TEST REPORT", "═");
        }
        
        /**
         * Print a single command with all its details
         */
        private void printCommand(String appId, String commandId, boolean isLast) {
            JSONObject commandObj = commandManager.getCommand(appId, commandId);
            if (commandObj == null) {
                return;
            }
            
            String prefix = isLast ? "└─ " : "├─ ";
            String subPrefix = isLast ? "   " : "│  ";
            
            String commandAction = getStringOrDefault(commandObj, "action", "N/A");
            String commandDesc = getStringOrDefault(commandObj, "description", "No description");
            
            System.out.println("\n  " + prefix + "Command ID: " + commandId);
            System.out.println("  " + subPrefix + "├─ Action: " + commandAction);
            System.out.println("  " + subPrefix + "├─ Description: " + commandDesc);
            
            JSONObject args = commandObj.optJSONObject("args");
            if (args == null || args.length() == 0) {
                System.out.println("  " + subPrefix + "└─ Arguments: (none)");
            } else {
                System.out.println("  " + subPrefix + "├─ Arguments: " + args.length());
                printArguments(args, subPrefix);
            }
        }
        
        /**
         * Print all arguments for a command
         */
        private void printArguments(JSONObject args, String parentPrefix) {
            Iterator<String> argKeys = args.keys();
            List<String> argNames = new ArrayList<>();
            
            while (argKeys.hasNext()) {
                argNames.add(argKeys.next());
            }
            
            for (int i = 0; i < argNames.size(); i++) {
                String argName = argNames.get(i);
                JSONObject argData = args.getJSONObject(argName);
                boolean isLastArg = i == argNames.size() - 1;
                
                printArgumentDetails(argName, argData, isLastArg, parentPrefix);
            }
        }
        
        /**
         * Print details for a single argument
         */
        private void printArgumentDetails(String argName, JSONObject argData, boolean isLastArg, String parentPrefix) {
            String prefix = isLastArg ? "└─ " : "├─ ";
            String subPrefix = isLastArg ? "   " : "│  ";
            
            String argType = getStringOrDefault(argData, "type", "unknown");
            boolean argMandatory = getBooleanOrDefault(argData, "mandatory", false);
            String argField = getStringOrDefault(argData, "field", argName);
            String argDescription = getStringOrDefault(argData, "description", "No description");
            
            System.out.println("  " + parentPrefix + "│  " + prefix + argName + " (field: " + argField + ")");
            System.out.println("  " + parentPrefix + "│  " + subPrefix + "├─ Type: " + argType);
            System.out.println("  " + parentPrefix + "│  " + subPrefix + "├─ Mandatory: " + argMandatory);
            System.out.println("  " + parentPrefix + "│  " + subPrefix + "├─ Description: " + argDescription);
            
            if (argData.has("compareMode")) {
                System.out.println("  " + parentPrefix + "│  " + subPrefix + "├─ Compare Mode: " + argData.getString("compareMode"));
            }
            
            if (argData.has("modifier")) {
                System.out.println("  " + parentPrefix + "│  " + subPrefix + "├─ Modifier: " + argData.getString("modifier"));
            }
            
            if (argData.has("defaultValue")) {
                System.out.println("  " + parentPrefix + "│  " + subPrefix + "└─ Default Value: " + argData.getString("defaultValue"));
            }
        }
        
        /**
         * Print a main section header
         */
        private void printSection(String title, String borderChar) {
            String border = "╔" + String.join("", java.util.Collections.nCopies(66, borderChar)) + "╗";
            System.out.println("\n" + border);
            System.out.println("║ " + title);
            System.out.println("╚" + String.join("", java.util.Collections.nCopies(66, borderChar)) + "╝");
        }
        
        /**
         * Print a field with label and value
         */
        private void printField(String label, String value) {
            System.out.println(label + ": " + value);
        }
        
        /**
         * Helper to safely get string from JSON object with default
         */
        private String getStringOrDefault(JSONObject obj, String key, String defaultValue) {
            return obj.has(key) ? obj.getString(key) : defaultValue;
        }
        
        /**
         * Helper to safely get boolean from JSON object with default
         */
        private boolean getBooleanOrDefault(JSONObject obj, String key, boolean defaultValue) {
            return obj.has(key) ? obj.getBoolean(key) : defaultValue;
        }
    }
}
