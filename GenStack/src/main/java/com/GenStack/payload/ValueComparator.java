package com.GenStack.payload;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

// Custom functional interface for three arguments
@FunctionalInterface
interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
}

/**
 * Class for comparing values of different types such as Strings, Integers, 
 * Dates, and more using specified comparison modes.
 */
public class ValueComparator {

    /** 
     * Map of comparison methods for different value types.
     */
    private static final Map<String, TriFunction<String, String, String, Boolean>> comparisonMethods = new HashMap<>();

    /** 
     * Map of integer comparison methods.
     */
    private static final Map<String, BiFunction<Integer, Integer, Boolean>> integerComparisons = new HashMap<>();

    /** 
     * Map of date comparison methods.
     */
    private static final Map<String, BiFunction<Date, Date, Boolean>> dateComparisons = new HashMap<>();

    /** 
     * Map of string comparison methods.
     */
    private static final Map<String, BiFunction<String, String, Boolean>> stringComparisons = new HashMap<>();

    static {
        // Initialize comparison methods
        comparisonMethods.put("str", ValueComparator::compareStrings);
        comparisonMethods.put("int", ValueComparator::compareIntegers);
        comparisonMethods.put("positiveint", ValueComparator::comparePositiveIntegers);
        comparisonMethods.put("date", ValueComparator::compareDates);
        comparisonMethods.put("time", ValueComparator::compareTimes);
        comparisonMethods.put("duration", ValueComparator::compareDurations);

        // Initialize integer comparison modes
        integerComparisons.put("=", (a, b) -> a.equals(b));
        integerComparisons.put("<", (a, b) -> a < b);
        integerComparisons.put("<=", (a, b) -> a <= b);
        integerComparisons.put(">", (a, b) -> a > b);
        integerComparisons.put(">=", (a, b) -> a >= b);

        // Initialize date comparison modes
        dateComparisons.put("=", Date::equals);
        dateComparisons.put("<", Date::before);
        dateComparisons.put("<=", (a, b) -> !a.after(b));
        dateComparisons.put(">", Date::after);
        dateComparisons.put(">=", (a, b) -> !a.before(b));

        // Initialize string comparison modes
        stringComparisons.put("=", String::equalsIgnoreCase); // Case-insensitive equality
        stringComparisons.put("!=", (a, b) -> !a.equals(b)); // Case-sensitive inequality
        stringComparisons.put("==", String::equals); // Case-sensitive equality
        stringComparisons.put("startsWith", (a, b) -> a.startsWith(b)); // Checks if one string starts with another
        stringComparisons.put("endsWith", (a, b) -> a.endsWith(b)); // Checks if one string ends with another
        stringComparisons.put("contains", (a, b) -> a.contains(b)); // Checks if one string contains another
        stringComparisons.put("length", (a, b) -> a.length() == b.length()); // Checks if two strings have the same length
        stringComparisons.put("compareTo", (a, b) -> a.compareTo(b) == 0); // Compares two strings lexicographically
        stringComparisons.put("compareToIgnoreCase", (a, b) -> a.compareToIgnoreCase(b) == 0); // Lexicographical comparison ignoring case
    }

    /**
     * Validates a value against a comparison string based on the specified type and comparison mode.
     * 
     * @param valueStr the value to validate as a String
     * @param compareStr the value to compare against as a String
     * @param type the type of the value (e.g., "str", "int", "date")
     * @param compareMode the comparison mode (e.g., "=", "<", "startsWith")
     * @return true if the value is valid according to the comparison, otherwise false
     * @throws IllegalArgumentException if an invalid type or comparison mode is specified
     */
    public static boolean validateValue(String valueStr, String compareStr, String type, String compareMode) {
        if (compareStr == null || compareStr.isEmpty()) {
            return true; // Passed if no comparison string is provided
        }

        // Get the appropriate comparison method
        TriFunction<String, String, String, Boolean> comparisonFunction = comparisonMethods.get(type.toLowerCase());
        if (comparisonFunction == null) {
            throw new IllegalArgumentException("Invalid type specified: " + type);
        }

        // Call the comparison method using the retrieved function
        return comparisonFunction.apply(valueStr, compareStr, compareMode); // pass compareMode
    }

    /**
     * Compares two strings based on the specified comparison mode.
     *
     * @param valueStr the first string
     * @param compareStr the second string
     * @param compareMode the comparison mode
     * @return true if the comparison is valid, otherwise false
     * @throws IllegalArgumentException if an invalid mode is specified for string comparison
     */
    private static Boolean compareStrings(String valueStr, String compareStr, String compareMode) {
        BiFunction<String, String, Boolean> comparisonFunction = stringComparisons.get(compareMode);
        if (comparisonFunction != null) {
            return comparisonFunction.apply(valueStr, compareStr);
        }
        throw new IllegalArgumentException("Invalid comparison mode for string: " + compareMode);
    }

    /**
     * Compares two integers represented as strings based on the specified comparison mode.
     *
     * @param valueStr the first integer as a string
     * @param compareStr the second integer as a string
     * @param compareMode the comparison mode
     * @return true if the comparison is valid, otherwise false
     */
    private static Boolean compareIntegers(String valueStr, String compareStr, String compareMode) {
        try {
            int value = Integer.parseInt(valueStr);
            int compareValue = Integer.parseInt(compareStr);

            BiFunction<Integer, Integer, Boolean> comparisonFunction = integerComparisons.get(compareMode);
            if (comparisonFunction == null) {
                throw new IllegalArgumentException("Invalid comparison mode for integer: " + compareMode);
            }
            return comparisonFunction.apply(value, compareValue);
        } catch (NumberFormatException e) {
            return false; // Invalid integer format
        }
    }

    /**
     * Compares positive integers represented as strings based on the specified comparison mode.
     *
     * @param valueStr the first positive integer as a string
     * @param compareStr the second integer as a string
     * @param compareMode the comparison mode
     * @return true if the comparison is valid, otherwise false
     */
    private static Boolean comparePositiveIntegers(String valueStr, String compareStr, String compareMode) {
        try {
            int value = Integer.parseInt(valueStr);
            if (value <= 0) return false; // Must be positive
            
            return compareIntegers(valueStr, compareStr, compareMode); // Pass compareMode directly
        } catch (NumberFormatException e) {
            return false; // Invalid integer format
        }
    }

    /**
     * Compares two dates based on the specified comparison mode.
     *
     * @param valueStr the first date as a string
     * @param compareStr the second date as a string
     * @param compareMode the comparison mode
     * @return true if the comparison is valid, otherwise false
     */
    private static Boolean compareDates(String valueStr, String compareStr, String compareMode) {
        return compareTimestamps(valueStr, compareStr, compareMode, "yyyy-MM-dd");
    }

    /**
     * Compares two times based on the specified comparison mode.
     *
     * @param valueStr the first time as a string
     * @param compareStr the second time as a string
     * @param compareMode the comparison mode
     * @return true if the comparison is valid, otherwise false
     */
    private static Boolean compareTimes(String valueStr, String compareStr, String compareMode) {
        return compareTimestamps(valueStr, compareStr, compareMode, "HH:mm:ss");
    }

    /**
     * Compares two timestamps based on the specified comparison mode and format.
     *
     * @param valueStr the first timestamp as a string
     * @param compareStr the second timestamp as a string
     * @param compareMode the comparison mode
     * @param format the date/time format
     * @return true if the comparison is valid, otherwise false
     */
    private static Boolean compareTimestamps(String valueStr, String compareStr, String compareMode, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setLenient(false);
        try {
            Date valueDate = sdf.parse(valueStr);
            Date compareDate = sdf.parse(compareStr);

            BiFunction<Date, Date, Boolean> comparisonFunction = dateComparisons.get(compareMode);
            if (comparisonFunction == null) {
                throw new IllegalArgumentException("Invalid comparison mode for date/time: " + compareMode);
            }
            return comparisonFunction.apply(valueDate, compareDate);
        } catch (ParseException e) {
            return false; // Invalid date/time format
        }
    }

    /**
     * Compares two durations represented as strings based on the specified comparison mode.
     *
     * @param valueStr the first duration as a string
     * @param compareStr the second duration as a string
     * @param compareMode the comparison mode
     * @return true if the comparison is valid, otherwise false
     * @throws IllegalArgumentException if an invalid mode is specified for duration comparison
     */
    private static Boolean compareDurations(String valueStr, String compareStr, String compareMode) {
        if ("=".equals(compareMode)) {
            return valueStr.equals(compareStr);
        }
        throw new IllegalArgumentException("Invalid comparison mode for duration: " + compareMode);
    }
}
