package com.GenStack.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.json.JSONObject;

import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;

public abstract class BaseInterface {
    protected final ResponseCallbackInterface callback;
    private boolean runInBackground;
    private boolean running = true;
    protected final PrintStream out;
    protected final InputStream in;
    protected final boolean isNetworkStream;
    private String appDataFolder;

    public BaseInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in, String appDataFolder) {
        this.callback = callback;
        this.out = out;
        this.in = in;
        this.isNetworkStream = !in.getClass().getName().equals("java.io.BufferedInputStream");
        this.appDataFolder = appDataFolder;
    }

    public BaseInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in) {
        this(callback, out, in, null);
    }

    public BaseInterface(ResponseCallbackInterface callback, String appDataFolder) {
        this(callback, System.out, System.in, appDataFolder);
    }

    public BaseInterface(ResponseCallbackInterface callback) {
        this(callback, System.out, System.in, null);
    }

    // Getter and Setter for runInBackground
    public boolean isRunInBackground() {
        return runInBackground;
    }

    public void setRunInBackground(boolean runInBackground) {
        this.runInBackground = runInBackground;
    }

    public void setRunningFlag(boolean running) {
        this.running = running;
    }

    public boolean getRunningFlag() {
        return this.running;
    }


    /**
     * Helper method to get the correct InputStream based on appDataFolder availability.
     * If appDataFolder is set, reads from the file system; otherwise reads from resources.
     * 
     * @return InputStream for the config file, or null if file not found
     * @throws IOException if an I/O error occurs
     */
    protected InputStream getConfigInputStream(String appDataFolder, String configFile) throws IOException {
        if (appDataFolder != null && !appDataFolder.isEmpty()) {
            // Read from folder
            Path configPath = Paths.get(appDataFolder, configFile);
            if (Files.exists(configPath)) {
                return Files.newInputStream(configPath);
            } else {
                out.println("Config file not found at: " + configPath);
                return null;
            }
        } else {
            // Read from resources
            return getClass().getClassLoader().getResourceAsStream(configFile);
        }
    }

    public void printJson(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        printJson(jsonObject);
    }

    public void print(String message) {
        this.out.print("\r" + message.replace("\n", "\r\n"));
        this.out.flush();
    }

    public void println(String message) {
        if (this.isNetworkStream) {
            this.out.print( "\r" + message.replace("\n", "\r\n") + "\r\n");
        } else {
            this.out.print(message + "\n");
        }
        this.out.flush();
    }

    public void println() {
        if (this.isNetworkStream) {
            this.out.print("\r\n");
        } else {
            this.out.print("\n");
        }
        this.out.flush();
    }

    public void printJson(JSONObject jsonObject) {
        // Print the JSON with indentation for readability
        String formattedJson = jsonObject.toString(2); // Indent with 2 spaces
        println("JSON Response:\n" + formattedJson);
    }

    // Abstract method to be implemented by subclasses
    public abstract JSONObject executeCommands(JSONObject commands);
}
