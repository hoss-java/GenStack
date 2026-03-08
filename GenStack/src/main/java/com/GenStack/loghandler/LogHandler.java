package com.GenStack.loghandler;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.GenStack.helper.DebugUtil; // Optional if DebugUtil is used

public class LogHandler {
    private List<LogEntry> logBuffer;

    // Constructor
    public LogHandler() {
        this.logBuffer = new ArrayList<>();
    }

    // Inner class to represent a log entry
    private static class LogEntry {
        private final String logID;
        private final String logGroup;
        private final String message;

        public LogEntry(String logID, String logGroup, String message) {
            this.logID = logID;
            this.logGroup = logGroup;
            this.message = message;
        }

        public JSONObject toJSON() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("logID", this.logID);
            jsonObject.put("logGroup", this.logGroup);
            jsonObject.put("message", this.message);
            return jsonObject;
        }
    
        @Override
        public String toString() {
            return toJSON().toString();
        }
    }

    // Method to add a log entry
    public void addLog(String logID, String logGroup, String message) {
        LogEntry entry = new LogEntry(logID, logGroup, message);
        logBuffer.add(entry);
    }

    // Method to display logs and reset the buffer
    public void displayLogs() {
        System.out.println("--------");
        if (logBuffer.isEmpty()) {
            System.out.println("No logs to display.");
            return;
        }

        for (LogEntry entry : logBuffer) {
            System.out.println(entry);
        }

        logBuffer.clear(); // Reset the log buffer after displaying
    }

    // Method to save logs to a file
    public void saveLogsToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            for (LogEntry entry : logBuffer) {
                writer.write(entry.toString());
                writer.newLine();
            }
            logBuffer.clear(); // Optionally reset the buffer after saving
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
