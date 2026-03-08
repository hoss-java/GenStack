package com.GenStack.interfaces;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.InputStream;
import java.util.Properties;
import java.nio.charset.StandardCharsets;

import com.GenStack.interfaces.BaseInterface;
import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;
import com.GenStack.helper.IPAddressHelper;

public class RESTInterface extends BaseInterface {
    private HttpServer server;
    private String configFile = "config/restinterface.properties";
    private int servicePort = 4567;
    private String servicePath = "api";
    private String serviceApiPath = "get";
    private String serviceCmdPath = "cmd";
    private JSONObject commands;

    private void loadProperties(String appDataFolder) {
        Properties props = new Properties();
        try (InputStream input = getConfigInputStream(appDataFolder, configFile)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + configFile);
                return;
            }

            // Load properties file
            props.load(input);

            // Get the properties
            this.servicePort = Integer.parseInt(props.getProperty("rest.port"));
            this.servicePath = props.getProperty("rest.path");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RESTInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in, String appDataFolder) {
        super(callback, out, in, appDataFolder);
        loadProperties(appDataFolder);
        printServiceDetails();
    }
    public RESTInterface(ResponseCallbackInterface callback, PrintStream out, InputStream in) {
        this(callback, out, in, null);
    }

    public RESTInterface(ResponseCallbackInterface callback, String appDataFolder) {
        this(callback, System.out, System.in, appDataFolder);
    }

    public RESTInterface(ResponseCallbackInterface callback) {
        this(callback, System.out, System.in, null);
    }

    private void printServiceDetails() {
        // Print the connection details
        out.println("RESTService Details:");
        out.println(" RESTService Address: " + IPAddressHelper.getLocalIPAddress());
        out.println(" RESTService Path: " + servicePath);
        out.println(" RESTService Port: " + servicePort);
        out.println(" RESTService API Path: " + "/" + this.servicePath + "/" + serviceApiPath);
        out.println(" RESTService CMD Path: " + "/" + this.servicePath + "/" + serviceCmdPath);
    }

    @Override
    public JSONObject executeCommands(JSONObject commands) {
        this.commands = commands;
        start(); // Start the server upon instantiation

        try {
            // Keep the service running while the running flag is true
            while (getRunningFlag()) {
                Thread.sleep(100); // Short sleep to yield control
            }
        } catch (InterruptedException e) {
            // Log interruption and reset the interrupt status
            Thread.currentThread().interrupt();
            out.println("Service thread was interrupted.");
        } finally {
            shutDown(); // Call shutdown method before exiting
        }
        return new JSONObject().put("RESTInterface", "stopped");
    }

    private void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(this.servicePort), 0);

            // Define your endpoints
            server.createContext("/" + this.servicePath + "/" + serviceApiPath, this::handleGetAPI);
            server.createContext("/" + this.servicePath + "/" + serviceCmdPath, this::handleCommand);

            server.start(); // Start the server
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetAPI(HttpExchange exchange) throws IOException {
        // Read the request body sent by the client
        InputStream inputStream = exchange.getRequestBody();
        String payload = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // Set response headers and send response
        String response = commands.toString();
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleCommand(HttpExchange exchange) throws IOException {
        // Read the request body sent by the client
        InputStream inputStream = exchange.getRequestBody();
        String payload = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // Process the payload using your callback
        String response = callback.ResponseHandler("RESTInterface", payload);

        // Set response headers and send response
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void shutDown() {
        if (server != null) {
            server.stop(0); // Stop the HTTP server immediately
        }
    }
}
