package com.GenStack.helper;

import com.GenStack.helper.DebugUtil;

public class TokenizedString {
    private String originalString;
    private String separator;
    private String[] parts;

    // Constructor to initialize the multi-part string and separator
    public TokenizedString(String input) {
        this(input, "."); // Set default separator to dot
    }
    
    // Constructor with specified separator
    public TokenizedString(String input, String separator) {
        this.originalString = input;
        this.separator = separator;
        // Escape dot character for splitting
        this.parts = (input != null && !input.isEmpty()) 
            ? input.split(separator.equals(".") ? "\\." : separator) 
            : new String[0];
    }

    // Method to get a specific part based on the index without default value
    public String getPart(int index) {
        return getPart(index, null); // Calls overloaded method with default value as null
    }

    // Method to get a specific part based on the index with a default return value
    public String getPart(int index, String defaultValue) {
        if (index == -1) {
            return parts.length > 0 ? parts[parts.length - 1] : defaultValue; // Return last part or default
        }
        if (index < 0 || index >= parts.length) {
            return defaultValue; // Return default if out of bounds
        }
        return parts[index]; // Return the part at the given index
    }

    // Method to get the count of parts
    public int getCount() {
        return parts.length;
    }

    // Method to remove a specified part and return the updated string
    public String removePart(int index) {
        if (index == -1) {
            // Remove the last element
            String[] newParts = new String[parts.length - 1];
            System.arraycopy(parts, 0, newParts, 0, parts.length - 1);
            parts = newParts;
        } else if (index >= 0 && index < parts.length) {
            // Remove the specified index
            String[] newParts = new String[parts.length - 1];
            for (int i = 0, j = 0; i < parts.length; i++) {
                if (i != index) {
                    newParts[j++] = parts[i];
                }
            }
            parts = newParts;
        }

        // Update originalString based on the modified parts array
        originalString = String.join(separator, parts);
        return originalString;
    }

    // Method to add a specified part at a given index
    public String addPart(String newPart, int index) {
        String[] newParts = new String[parts.length + 1];

        if (index == -1) {
            // Add to the end
            System.arraycopy(parts, 0, newParts, 0, parts.length);
            newParts[parts.length] = newPart;
        } else if (index >= 0 && index <= parts.length) {
            // Insert at the specified index
            System.arraycopy(parts, 0, newParts, 0, index);
            newParts[index] = newPart;
            System.arraycopy(parts, index, newParts, index + 1, parts.length - index);
        }

        // Update the parts and originalString
        parts = newParts;
        originalString = String.join(separator, parts);
        return originalString;
    }

    // Method to get the original string
    public String getOriginalString() {
        return originalString;
    }
}
