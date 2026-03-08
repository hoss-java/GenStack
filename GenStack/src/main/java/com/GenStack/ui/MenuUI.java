package com.GenStack.ui;

import java.io.InputStream;
import java.io.PrintStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.GenStack.helper.DebugUtil;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.json.JSONArray;

public class MenuUI {
    private final PrintStream out;
    private final InputStream in;
    private final BufferedReader reader;
    private final boolean isNetworkStream;
    private String menuTitle = "Available Commands";

    public MenuUI(String menuTitle, PrintStream out, InputStream in) {
        this.menuTitle = menuTitle;
        this.out = out;
        this.in = in;
        this.reader = new BufferedReader(
            new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)
        );
        // Detect if this is a network stream (not System.in)
        this.isNetworkStream = !in.getClass().getName().equals("java.io.BufferedInputStream");
    }

    public MenuUI(String menuTitle) {
        this(menuTitle, System.out, System.in);
    }

    private void print(String message) {
        this.out.print("\r" + message);
        this.out.flush();
    }

    private void println(String message) {
        if (this.isNetworkStream) {
            this.out.print("\r" + message + "\r\n");
        } else {
            this.out.print(message + "\n");
        }
        this.out.flush();
    }

    private void println() {
        if (this.isNetworkStream) {
            this.out.print("\r\n");
        } else {
            this.out.print("\n");
        }
        this.out.flush();
    }

    private void showMessage(String message) {
        println(message);
    }

    private void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                this.out.print("\033[H\033[2J");
                this.out.flush();
            }
        } catch (Exception e) {
            // Handle exceptions accordingly
        }
    }

    private JSONObject findCommandByName(JSONArray commandsArray, String commandName) {
        for (int i = 0; i < commandsArray.length(); i++) {
            JSONObject command = commandsArray.getJSONObject(i);
            if (command.getString("id").equals(commandName)) {
                return command;
            }
        }
        return null;
    }

    private String readLineFromStream() {
        try {
            StringBuilder inputBuffer = new StringBuilder();
            int character;
            while ((character = in.read()) != -1) {
                if (character == '\n' || character == '\r') {
                    // Line ending received
                    if (this.isNetworkStream) {
                        this.out.print("\r\n");
                    }
                    break;
                } else if (character == 8 || character == 127) {
                    // Backspace handling
                    if (inputBuffer.length() > 0) {
                        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                        this.out.print("\b \b");  // Backspace, space, backspace
                    }
                } else if (character >= 32 && character < 127) {
                    // Regular character - echo it immediately
                    inputBuffer.append((char) character);
                    if (this.isNetworkStream) {
                        this.out.print((char) character);
                    }
                }
            }

            if (character == -1) {
                return null;  // EOF reached
            }
            return inputBuffer.toString();
        } catch (IOException e) {
            println("Error reading input: " + e.getMessage());
            return null;
        }
    }

    public String displayCommandsAndGetChoice(JSONArray commandsArray, String backCommand) {
        clearConsole();
        
        println(this.menuTitle + ":");
        int index = 1;
        int commandCount = commandsArray.length() + (backCommand != null ? 1 : 0);
        String[] commandKeys = new String[commandCount];

        for (int i = 0; i < commandsArray.length(); i++) {
            JSONObject cmd = commandsArray.getJSONObject(i);
            println("  " + index + ". " + cmd.getString("description") + " (" + cmd.getString("id") + ")");
            commandKeys[index - 1] = cmd.getString("id");
            index++;
        }

        if (backCommand != null) {
            println("  " + index + ". " + backCommand);
            commandKeys[index - 1] = backCommand;
        }

        String selectedCommand = null;

        while (selectedCommand == null) {
            println();
            print("Please type the number of your choice: ");
            
            String input = readLineFromStream();
            
            if (input == null) {
                return null;
            }

            input = input.trim();

            if (input.isEmpty()) {
                showMessage("Input is empty. Please enter a number.");
                continue;
            }

            int choice;

            try {
                choice = Integer.parseInt(input);
                
                if (choice >= 1 && choice <= commandCount) {
                    selectedCommand = commandKeys[choice - 1];
                } else {
                    showMessage("Invalid choice. Please choose a number between 1 and " + commandCount + ".");
                }
            } catch (NumberFormatException e) {
                showMessage("Invalid input. Please enter a number.");
            }
        }

        return selectedCommand;
    }

    private void navigateCommands(String rootID, JSONArray commandsArray, JSONObject payload, String backCommand) {
        while (true) {
            String commandName = displayCommandsAndGetChoice(commandsArray, backCommand);

            if (commandName == null) {
                return;
            }
            
            if (commandName.equals(backCommand)) {
                return;
            }

            JSONObject command = findCommandByName(commandsArray, commandName);
            if (command == null) {
                showMessage("Invalid choice, please try again.");
                continue;
            }

            payload.put("identifier", rootID);
            payload.put("command", command);

            if (command.has("commands")) {
                navigateCommands(command.getString("id"), command.getJSONArray("commands"), payload, "Back");
                if (!rootID.equals(payload.getString("identifier"))) {
                    return;
                }
            } else {
                break;
            }
        }
    }

    public JSONObject displayMenu(JSONObject commands) {
        JSONObject payload = new JSONObject();
    
        if (commands != null && commands.has("commands")) {
            navigateCommands("root", commands.getJSONArray("commands"), payload, "Exit");
        }
        return payload;
    }
}
