package com.GenStack;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.GenStack.config.AppConfig;
import com.GenStack.config.ConfigManager;
import com.GenStack.kvhandler.KVSubjectHandler;
import com.GenStack.kvhandler.KVObjectHandler;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.kvhandler.KVSubjectStorage;

import com.GenStack.interfaces.BaseInterface;
import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;
import com.GenStack.payload.PayloadHandler;
import com.GenStack.payload.CommandBuilder;
import com.GenStack.payload.CommandManager;
import com.GenStack.helper.JSONHelper;
import com.GenStack.helper.ResponseHelper;
import com.GenStack.loghandler.LogHandler;
import com.GenStack.storages.NamespaceStorage;
import com.GenStack.storages.NamespaceStorageConfig;
import com.GenStack.storages.MultiNamespaceStorageManager;
import com.GenStack.config.StorageConfig;
import com.GenStack.ui.MenuUI;
import com.GenStack.ui.InputUI;
import com.GenStack.helper.DebugUtil;
import com.GenStack.CommandLineParser;

/**
 * @file GenStack.java
 * @brief Interactive console event managment application.
 *
 * This class provides the program entry point for the event managment application.
 *
 * Features:
 * -
 */
public class GenStack {
    private static final String appDefaultPropertiesFile = "default/app.properties";
    private static final String storageDefaultPropertiesFile = "default/storage.properties";

    private static final String defaultCommandsFile = "commands.json";
    private static final String defaultModulesFile = "modules.xml";
    private static final String defaultSubjectsFile = "subjects.xml";

    private static final String configFile = "config/config.json";
    private static final String appPropertiesFile = "config/app.properties";
    private static final String storagePropertiesFile = "config/storage.properties";

    private static final StringBuilder logBuffer = new StringBuilder();
    private boolean running = true;
    private static List<BaseInterface> interfaceInstances = new ArrayList<>();
    private static LogHandler logHandler = new LogHandler();
    private static KVObjectHandler kvObjectHandler;
    private static KVSubjectHandler kvSubjectHandler;
    private static PayloadHandler payloadHandler;
    // List to keep track of background threads
    private static List<Thread> backgroundThreads = new ArrayList<>();

    public static ResponseCallbackInterface responseHandler = (callerID, menuItem) -> {
        //logHandler.addLog(callerID,"responseHandler",menuItem);

        String commandId = "selectedCommand.getString";
        JSONObject jsonResponse = null;

        jsonResponse = payloadHandler.parsePayload(menuItem);

        // Handle invalid command
        if (jsonResponse == null) {
            jsonResponse = ResponseHelper.createInvalidCommandResponse(commandId);
        }

        return jsonResponse.toString();
    };

    public static ActionCallbackInterface actionHandler = (String callerID, JSONObject payload) -> {
        logHandler.addLog(callerID,"actionHandler",payload.toString());

        String commandId = "selectedCommand.getString";
        JSONObject jsonResponse = null;

        // Handle invalid command
        if (jsonResponse == null) {
            jsonResponse = ResponseHelper.createInvalidCommandResponse(commandId);
        }
       return jsonResponse;
    };

    private static void loadModulesFromXML(String fileName, String appDataFolder) {
        try (InputStream inputStream = GenStack.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new RuntimeException("File not found: " + fileName);
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();

            Element rootElement = doc.getDocumentElement();
            
            // Load interfaces from root element
            NodeList interfaceList = rootElement.getElementsByTagName("interface");
            for (int i = 0; i < interfaceList.getLength(); i++) {
                Element element = (Element) interfaceList.item(i);
                String interfaceName = element.getAttribute("name");
                boolean runInBackground = Boolean.parseBoolean(element.getAttribute("runInBackground"));

                try {
                    Class<?> clazz = Class.forName(interfaceName);
                    BaseInterface interfaceInstance = (BaseInterface) clazz.getDeclaredConstructor(ResponseCallbackInterface.class, String.class).newInstance(responseHandler, appDataFolder);
                    // Set the runInBackground attribute
                    interfaceInstance.setRunInBackground(runInBackground);

                    // Add the instance to the list
                    interfaceInstances.add(interfaceInstance);
                } catch (Exception e) {
                    System.err.println("Failed to instantiate interface class: " + interfaceName);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startbackgroundInterface(JSONObject commands) {
        // Check each interface and handle as background or foreground
        for (BaseInterface interfaceInstance : interfaceInstances) {
            if (interfaceInstance.isRunInBackground()) {
                // Start a new thread for each background interface
                Thread interfaceThread = new Thread(() -> {
                    // Execute commands for the background interface
                    JSONObject result = interfaceInstance.executeCommands(commands);
                // Print the class name along with the result
                System.out.println("Background Execution result from " + interfaceInstance.getClass().getSimpleName() + ": " + result);
                });

                backgroundThreads.add(interfaceThread);
                interfaceThread.start();
            }
        }
    }

    private static void stopbackgroundInterface() {
        // Stop all background threads gracefully
        for (BaseInterface backgroundInterface : interfaceInstances) {
            if (backgroundInterface.isRunInBackground()) {
                backgroundInterface.setRunningFlag(false); // Assuming you have a running flag in the interface
            }
        }

        // Wait for all background threads to complete
        for (Thread thread : backgroundThreads) {
            try {
                thread.join(15000); // Wait for the thread to finish (with a timeout if needed)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve the interrupted status
                // Optionally, you may want to log this interruption or handle it
            }

            if (thread.isAlive()) {
                // If the thread is still alive after waiting, you can choose to log it or take other actions
                System.out.println("Warning: Thread " + thread.getName() + " did not finish in time.");
            }
        }
    }

    private static void runForegroundInterface(JSONObject commands) {
        // Check each interface and handle as background or foreground
        // Create the root JSON object
        JSONObject rootJson = new JSONObject();
        JSONArray commandsArray = new JSONArray();

        for (BaseInterface interfaceInstance : interfaceInstances) {
            if (!interfaceInstance.isRunInBackground()) {
                // Get the class name to use for id and description
                String className = interfaceInstance.getClass().getSimpleName();
                
                // Create a JSON object for this command
                JSONObject commandJson = new JSONObject();
                commandJson.put("id", className);
                commandJson.put("description", "Run " + className);

                // Add the command to the array
                commandsArray.put(commandJson);                
            }
        }

        // Add the commands array to the root JSON object
        rootJson.put("commands", commandsArray);
        MenuUI menuUI = new MenuUI("Available Interfaces");
        JSONObject selectedMenuObject = menuUI.displayMenu(rootJson);

        String id = null;

        if (selectedMenuObject.has("command")) {
            JSONObject commandObject = selectedMenuObject.getJSONObject("command");
            if (commandObject.has("id")) {
                id = commandObject.getString("id");
                for (BaseInterface interfaceInstance : interfaceInstances) {
                    if (!interfaceInstance.isRunInBackground() && id.equals(interfaceInstance.getClass().getSimpleName())) {
                        // Execute foreground interfaces one by one
                        JSONObject result = interfaceInstance.executeCommands(commands);
                        //System.out.println("Foreground Execution result from " + interfaceInstance.getClass().getSimpleName() + ": " + result);
                    }
                }

            }
        }
    }


    public static void logActiveThreads() {
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
        for (Thread thread : allThreads.keySet()) {
            System.out.println("Thread Name: " + thread.getName() + " | State: " + thread.getState());
        }
    }

    private static void initAppDataFolders(String appDataFolder) throws IOException {
        // Create the appdata folder if it doesn't exist
        File directory = new File(appDataFolder);
        if (!directory.exists()) {
            directory.mkdirs();
            System.out.println("Created directory: " + directory.getAbsolutePath());
        }

        // Get the resource appdata folder (template)
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resourceUrl = classLoader.getResource("appdata");

        if (resourceUrl != null) {
            File resourceAppDataFolder = new File(resourceUrl.getPath());
            
            // Get all subfolders from template
            File[] resourceFolders = resourceAppDataFolder.listFiles(File::isDirectory);
            
            if (resourceFolders != null) {
                for (File resourceSubfolder : resourceFolders) {
                    String folderName = resourceSubfolder.getName();
                    File targetSubfolder = new File(directory, folderName);
                    
                    // Create subfolder if it doesn't exist
                    if (!targetSubfolder.exists()) {
                        targetSubfolder.mkdirs();
                        System.out.println("Created directory: " + targetSubfolder.getAbsolutePath());
                    }
                    
                    // Copy files from template folder
                    File[] files = resourceSubfolder.listFiles(File::isFile);
                    if (files != null) {
                        for (File file : files) {
                            File targetFile = new File(targetSubfolder, file.getName());
                            if (!targetFile.exists()) {
                                copyFile(file, targetFile);
                                System.out.println("Copied file: " + targetFile.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }

    private static void copyFile(File source, File destination) throws IOException {
        Files.copy(source.toPath(), destination.toPath());
    }

    /**
     * @brief Program entry point.
     *
     * @param args Command-line arguments (ignored by this application).
     */
    public static void main(String[] args) {
        InputUI inputUI = new InputUI();

        CommandLineParser parserArgs = new CommandLineParser(args);
        String appDataFolder = parserArgs.getParameter("datapath", ".appdata");
        String appDataStorageFolder = appDataFolder + "/data/";

        try {
            initAppDataFolders(appDataFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load config and initialize data folders
        AppConfig appsConfigFile = new AppConfig(appDataFolder);

        ConfigManager configManager = ConfigManager.getInstance(appDataFolder, configFile);

        CommandBuilder commandBuilder = new CommandBuilder(appDataFolder);
        commandBuilder.generateCommands(appsConfigFile.getFiles("subjects","xml"));
        commandBuilder.updateCommands(appsConfigFile.getFiles("commands","json"));

        CommandManager commandManager = new CommandManager(appDataFolder,commandBuilder.getCommandsJson());
        commandManager.saveToFile();
        //commandManager.testCommandManager();

        //DebugUtil.debugAndWait();

        loadModulesFromXML(defaultModulesFile, appDataFolder);

        StorageConfig storageConfigFile = new StorageConfig(appDataFolder, storagePropertiesFile);
        DebugUtil.debugAndWait(storageConfigFile.toString(2));

        MultiNamespaceStorageManager namespaceManager = MultiNamespaceStorageManager.getInstance(storageConfigFile);

        kvSubjectHandler = new KVSubjectHandler(appsConfigFile.getFiles("subjects","xml") ,namespaceManager);
        kvObjectHandler = new KVObjectHandler(null,namespaceManager);

        payloadHandler = new PayloadHandler(kvObjectHandler, kvSubjectHandler, commandManager);

        startbackgroundInterface(commandBuilder.getCommandsJson());
        inputUI.waitForKeyPress();

        runForegroundInterface(commandBuilder.getCommandsJson());
        stopbackgroundInterface();

//        logHandler.displayLogs();
//        DebugUtil.debugAndWait();
        namespaceManager.closeAll();
        configManager.saveConfig();
//        logActiveThreads();

        System.exit(0);
    }
}

