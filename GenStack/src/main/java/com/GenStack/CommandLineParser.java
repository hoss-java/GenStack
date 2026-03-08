package com.GenStack;

import java.util.HashMap;
import java.util.Map;

public class CommandLineParser {
    private Map<String, String> parameters;

    public CommandLineParser(String[] args) {
        this.parameters = new HashMap<>();
        parseArguments(args);
    }

    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                String value = "";
                
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[i + 1];
                    i++;
                }
                
                parameters.put(key, value);
            }
        }
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getParameter(String key, String defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    public Map<String, String> getAllParameters() {
        return new HashMap<>(parameters);
    }
}
