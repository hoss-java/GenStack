package com.GenStack.interfaces;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.GenStack.interfaces.BaseInterface;
import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;
import com.GenStack.helper.DebugUtil;
import com.GenStack.ui.MenuUI;
import com.GenStack.ui.InputUI;

public class ConsoleInterface extends BaseInterface {
    private final Scanner scanner;

    public ConsoleInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in, String appDataFolder) {
        super(callback, out, in, appDataFolder);
        this.scanner = new Scanner(this.in);
    }

    public ConsoleInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in) {
        this(callback, out, in, null);
    }

    public ConsoleInterface(ResponseCallbackInterface callback, String appDataFolder) {
        this(callback, System.out, System.in, appDataFolder);
    }

    public ConsoleInterface(ResponseCallbackInterface callback) {
        this(callback, System.out, System.in, null);
    }

    private void runSelectedCommand(JSONObject selectedMenuObject) {
        InputUI inputUI = new InputUI(this.out, this.in);

        JSONObject payload = new JSONObject();

        payload.put("identifier", selectedMenuObject.getString("identifier"));

        JSONObject command = selectedMenuObject.getJSONObject("command");
        if (command.has("args")) {
            JSONObject args = new JSONObject();
            JSONObject arguments = command.getJSONObject("args");

            for (String argName : arguments.keySet()) {
                JSONObject argType = arguments.getJSONObject(argName);
                String argValue = inputUI.getUserInput(argName, argType);
                String argField = argType.optString("field", argName);
                args.put(argField, argValue);
            }
            JSONObject payloadCommand = new JSONObject();
            payloadCommand.put("id", command.getString("action"));
            payloadCommand.put("data", args);
            //payloadCommand.put("args", arguments);
            payloadCommand.put("args", new JSONObject());

            JSONArray payloadCommandsArray = new JSONArray();
            payloadCommandsArray.put(payloadCommand);
            // Put the commands list into the JSON object
            payload.put("commands", payloadCommandsArray);
        }
        String response = callback.ResponseHandler("ConsoleInterface", payload.toString());
        printJson(response);
        inputUI.waitForKeyPress();
    }

    @Override
    public JSONObject executeCommands(JSONObject commands) {
        return runCommandMenu(runAppsMenu(commands));
    }

    private JSONObject runCommandMenu(JSONObject appCommands) {
        MenuUI menuUI = new MenuUI("Available Commands (ConsoleInterface)", this.out, this.in);
        while (true) {
            JSONObject selectedMenuObject = menuUI.displayMenu(appCommands);
            String identifier = selectedMenuObject.optString("identifier", "");
            if (selectedMenuObject.isEmpty() || identifier.equals("root")) {
                return selectedMenuObject.put("ConsoleInterface", "exit");
            }
            runSelectedCommand(selectedMenuObject);
        }
    }

    private JSONObject findAppCommands(JSONObject commands, String appId) {
        JSONArray appsArray = commands.getJSONArray("apps");
        for (int i = 0; i < appsArray.length(); i++) {
            JSONObject app = appsArray.getJSONObject(i);
            if (app.getString("id").equals(appId)) {
                return app;
            }
        }
        return new JSONObject();
    }

    private JSONObject runAppsMenu(JSONObject commands) {
        if (!commands.has("apps") || commands.getJSONArray("apps").length() == 0) {
            return commands.has("commands") ? commands : new JSONObject();
        }
        
        MenuUI menuUI = new MenuUI("Available Application", this.out, this.in);
        JSONObject appsList = new JSONObject();
        JSONArray appsArray = new JSONArray();
        commands.getJSONArray("apps").forEach(app -> {
            JSONObject appObj = (JSONObject)app;
            appsArray.put(new JSONObject().put("id", appObj.getString("id")).put("description", appObj.getString("description")));
        });
        appsList.put("commands", appsArray);

        JSONObject selectedApp = menuUI.displayMenu(appsList);
        
        JSONObject commandObj = selectedApp.optJSONObject("command");
        if (commandObj == null) {
            return new JSONObject();
        }
        
        String selectedAppId = commandObj.optString("id", "");
        JSONObject selectedAppCommands = findAppCommands(commands, selectedAppId);
        
        return selectedAppCommands == null || selectedAppCommands.isEmpty() ? new JSONObject() : selectedAppCommands;
    }
}
