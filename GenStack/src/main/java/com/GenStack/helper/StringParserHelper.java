package com.GenStack.helper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringParserHelper {
    private static Map<String, String> replacements = new HashMap<>();

    // Static block to initialize default replacements
    static {
        replacements.put("%DATE%", LocalDate.now().toString());
        replacements.put("%TIME%", LocalTime.now().toString());
    }

    // Constructor to initialize default replacements
    public StringParserHelper() {
    }

    // Static method to add a new replacement
    public static void addReplacement(String placeholder, String value) {
        replacements.put(placeholder, value);
    }

    // Static method to parse the input string with optional replacements
    public static String parseString(String input, Map<String, String> customReplacements) {
        Map<String, String> effectiveReplacements = new HashMap<>(replacements); // Start with the default replacements

        if (customReplacements != null) {
            effectiveReplacements.putAll(customReplacements); // Add custom replacements if provided
        }

        for (Map.Entry<String, String> entry : effectiveReplacements.entrySet()) {
            String placeholder = entry.getKey();
            String replacement = entry.getValue();
            input = input.replace(placeholder, replacement);
        }
        return input;
    }

    // Overloaded method to use default replacements only
    public static String parseString(String input) {
        return parseString(input, null);
    }

    public static Duration parseDuration(String durationString) {
        String[] parts = durationString.split(":");
        
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Invalid duration format");
        }

        try {
            long hours = 0;
            long minutes;
            long seconds;

            if (parts.length == 2) {
                minutes = Long.parseLong(parts[0].trim());
                seconds = Long.parseLong(parts[1].trim());
            } else {
                hours = Long.parseLong(parts[0].trim());
                minutes = Long.parseLong(parts[1].trim());
                seconds = Long.parseLong(parts[2].trim());
            }

            return Duration.ofHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration format", e);
        }
    }
}
