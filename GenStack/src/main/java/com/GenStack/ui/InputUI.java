package com.GenStack.ui;

import java.io.InputStream;
import java.io.PrintStream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import org.json.JSONObject;
import java.time.format.DateTimeParseException;

import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;

public class InputUI {
    private final PrintStream out;
    private final InputStream in;
    private final BufferedReader reader;
    private final boolean isNetworkStream;

    public InputUI(PrintStream out, InputStream in) {
        this.out = out;
        this.in = in;
        this.reader = new BufferedReader(
            new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)
        );
        this.isNetworkStream = !in.getClass().getName().equals("java.io.BufferedInputStream");

    }

    public InputUI() {
        this(System.out, System.in);
    }

    private void print(String message) {
        this.out.print("\r" + message);
        this.out.flush();
    }

    private void println(String message) {
        if (this.isNetworkStream) {
            this.out.print( "\r" + message + "\r\n");
        } else {
            this.out.print(message + "\n");
        }
    }

    private void println() {
        if (this.isNetworkStream) {
            this.out.print("\r\n");
        } else {
            this.out.print("\n");
        }
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

    public String getUserInput(String argName, JSONObject argTypeAttr) {
        String argField = (new TokenizedString(argTypeAttr.optString("field", argName), "@")).getPart(-1);
        String argDescription = argTypeAttr.optString("description", argField);
        String argType = argTypeAttr.optJSONObject("referencedfield") != null ? 
            argTypeAttr.optJSONObject("referencedfield").optString("type", argTypeAttr.optString("type", "str")) : 
            argTypeAttr.optString("type", "str");        
        argType = (new TokenizedString(argType, "@")).getPart(-1);
        String argModifier = argTypeAttr.optString("modifier", "user");
        boolean argMandatory = argTypeAttr.optBoolean("mandatory", true);
        String argDefault = argTypeAttr.optString("default", "");

        while (true) {
            if (argModifier.equals("user")) {
                String prompt = "Enter " + argDescription + (argMandatory ? "*" : "") + " (" + argType + "): ";
                
                print(prompt);
                
                String input = readLineFromStream();
                
                if (input == null) {
                    return argDefault;
                }

                input = input.trim();

                if (input.isEmpty() && !argMandatory) {
                    return argDefault;
                }

                if (isValid(input, argType)) {
                    return input;
                } else {
                    showMessage("Invalid " + argType + ". Please try again.");
                }
            } else {
                return argDefault;
            }
        }
    }

    public String getDefaultValue(String baseType, String extraWord) {
        switch (baseType) {
            case "date":
                if ("default".equals(extraWord)) {
                    return getCurrentDate();
                }
                break;
            case "time":
                if ("default".equals(extraWord)) {
                    return getCurrentTime();
                }
                break;
        }
        return null;
    }

    private String getCurrentDate() {
        java.util.Date date = new java.util.Date();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private String getCurrentTime() {
        java.util.Date date = new java.util.Date();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("HH:mm:ss");
        return formatter.format(date);
    }

    private boolean isValid(String input, String argType) {
        switch (argType) {
            case "str":
                if (input.isEmpty()) {
                    println("Hint: Input should be a non-empty string.");
                    return false;
                }
                return true;

            case "int":
                if (!input.matches("-?\\d+")) {
                    println("Hint: Input should be a valid integer, which can be positive, negative, or zero. Example: 123, -45, or 0.");
                    return false;
                }
                return true;

            case "unsigned":
                if (!input.matches("\\d+")) {
                    println("Hint: Input should be a positive integer greater than zero. Example: 1, 100, or 456.");
                    return false;
                }
                return true;

            case "date":
                if (!isValidDate(input)) {
                    println("Hint: Input should be a valid date in the format YYYY-MM-DD. Example: 2026-02-15.");
                    return false;
                }
                return true;

            case "time":
                if (!isValidTime(input)) {
                    println("Hint: Input should be a valid time in the format HH:mm (24-hour format). Example: 14:30.");
                    return false;
                }
                return true;

            case "duration":
                if (!isValidDuration(input)) {
                    println("Hint: Input should be a valid duration format. Example: 1h 30m (for 1 hour and 30 minutes).");
                    return false;
                }
                return true;

            default:
                println("Hint: Unknown argument type: " + argType);
                return false;
        }
    }

    private boolean isValidDate(String date) {
        try {
            java.time.LocalDate.parse(date);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidTime(String time) {
        return time.matches("([01]\\d|2[0-3]):[0-5]\\d");
    }

    private boolean isValidDuration(String duration) {
        return java.util.regex.Pattern.matches("\\d+h \\d+m", duration);
    }

    public void showMessage(String message) {
        println(message);
    }

    public void waitForKeyPress() {
        showMessage("Press Enter to continue...");
        readLineFromStream();
    }
}
