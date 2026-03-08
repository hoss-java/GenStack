package com.GenStack.interfaces;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Properties;

import com.GenStack.interfaces.BaseInterface;
import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;
import com.GenStack.helper.RestServiceUtil;
import com.GenStack.ui.MenuUI;
import com.GenStack.ui.InputUI;

public class RemoteInterface extends BaseInterface {
    private final Scanner scanner;
    private String configFile = "config/remoteinterface.properties";
    private String serviceAddress = "172.32.0.11";
    private int servicePort = 32768;
    private String servicePath = "api";
    private String serviceUrl;
    private String serviceApiPath = "get";
    private String serviceCmdPath = "cmd";

    private void loadProperties(String appDataFolder) {
        Properties props = new Properties();
        try (InputStream input = getConfigInputStream(appDataFolder, configFile)) {
            if (input == null) {
                out.println("Sorry, unable to find " + configFile);
                return;
            }

            // Load properties file
            props.load(input);

            // Get the properties
            this.servicePort = Integer.parseInt(props.getProperty("remoterest.port"));
            this.servicePath = props.getProperty("remoterest.path");
            this.serviceAddress = props.getProperty("remoterest.address");
            this.serviceUrl = "http://" + this.serviceAddress + ":" + this.servicePort + "/" + this.servicePath;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RemoteInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in, String appDataFolder) {
        super(callback, out, in, appDataFolder);
        this.scanner = new Scanner(in);
        loadProperties(appDataFolder);
    }

    public RemoteInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in) {
        this(callback, out, in, null);
    }

    public RemoteInterface(ResponseCallbackInterface callback, String appDataFolder) {
        this(callback, System.out, System.in, appDataFolder);
    }

    public RemoteInterface(ResponseCallbackInterface callback) {
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

        String response = RestServiceUtil.callRestService(this.serviceUrl + "/" + this.serviceCmdPath, payload.toString());
        printJson(response);
        inputUI.waitForKeyPress();
    }

    @Override
    public JSONObject executeCommands(JSONObject commands) {
        if (!RestServiceUtil.isServiceAvailable(this.serviceUrl + "/" + this.serviceApiPath)) {
            out.println("Service is not available " + this.serviceUrl);
            return null;
        }
        
        // Step 1: Fetch commands from remote service
        String response = RestServiceUtil.callRestService(this.serviceUrl + "/" + this.serviceApiPath, null);
        JSONObject commandsJSONFromRemote = new JSONObject(response);
        
        JSONObject commandsToUse = commandsJSONFromRemote;
        
        // Step 2: Select app if apps exist
        if (commandsJSONFromRemote.has("apps")) {
            JSONObject selectedApp = selectApp(commandsJSONFromRemote.getJSONArray("apps"));
            if (selectedApp == null) {
                return new JSONObject().put("RemoteInterface", "exit");
            }
            commandsToUse = selectedApp;
        }
        
        // Step 3: Run command menu with selected app (or root commands if no apps)
        return runCommandMenu(commandsToUse);
    }

    private JSONObject selectApp(JSONArray appsArray) {
        if (appsArray.length() == 1) {
            return appsArray.getJSONObject(0);
        }
        
        MenuUI menuUI = new MenuUI("Select Application", this.out, this.in);
        String selectedAppId = menuUI.displayCommandsAndGetChoice(appsArray, "Exit");
        
        if (selectedAppId == null || selectedAppId.equals("Exit")) {
            return null;
        }
        
        return appsArray.getJSONObject(findAppIndex(appsArray, selectedAppId));
    }

    private JSONObject runCommandMenu(JSONObject commands) {
        MenuUI menuUI = new MenuUI("Available Commands (RemoteInterface)", this.out, this.in);
        
        while (true) {
            JSONObject selectedMenuObject = menuUI.displayMenu(commands);
            if (selectedMenuObject.isEmpty()) {
                return selectedMenuObject.put("RemoteInterface", "exit");
            }
            runSelectedCommand(selectedMenuObject);
        }
    }

    private int findAppIndex(JSONArray appsArray, String appId) {
        for (int i = 0; i < appsArray.length(); i++) {
            if (appsArray.getJSONObject(i).getString("id").equals(appId)) {
                return i;
            }
        }
        return 0;
    }
}
