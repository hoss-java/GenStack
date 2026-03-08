package com.GenStack.payload;

import org.json.JSONObject;

import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;

/**
 * Represents the details of a payload including various attributes 
 * such as field, type, mandatory status, default value, compare mode, 
 * and modifier.
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.IOException;

public class PayloadDetail {
    private String field;  /**< Optional, can be null */
    private String description; /**< Optional, can be null */
    private String type;  /**< Required */
    private Boolean mandatory;  /**< Optional, can be null */
    @JsonProperty("referencedfield") 
    private Map<String, Object> referencedField;  /**< Optional, can be null */
    @JsonProperty("defaultvalue") 
    private Map<String, Object> defaultValue; 
    private List<String> supports; /**< Optional, list of supported operations */
    private String compareMode;  /**< Optional, can be null */
    private String modifier; /**< Optional, can be null */

    /**
     * Default constructor.
     */
    public PayloadDetail() {
        this.supports = new ArrayList<>();
    }

    /**
     * Constructor for required fields.
     * 
     * @param type the required type of the payload detail
     */
    public PayloadDetail(String type) {
        this.type = type;
        this.supports = new ArrayList<>();
    }

    /**
     * Constructor for all fields except description and relation.
     * 
     * @param field the name of the field
     * @param type the required type of the payload detail
     * @param mandatory indicates if this field is mandatory
     * @param defaultValue the default value of the field as JSONObject
     * @param compareMode the comparison mode applied to the field
     */
    public PayloadDetail(String field, String type, Boolean mandatory, JSONObject defaultValue, String compareMode) {
        this(field, null, type, mandatory, null, jsonObjectToMap(defaultValue), new ArrayList<>(), "", compareMode);
    }

    /**
     * Constructor for all fields.
     * 
     * @param field the name of the field
     * @param description the description of the field
     * @param type the required type of the payload detail
     * @param mandatory indicates if this field is mandatory
     * @param defaultValue the default value of the field as JSONObject
     * @param supports list of supported operations
     * @param modifier the modifier applied to the field
     * @param compareMode the comparison mode applied to the field
     */
    public PayloadDetail(String field, String description, String type, Boolean mandatory, 
                        JSONObject defaultValue, List<String> supports, String modifier, String compareMode) {
        this(field, description, type, mandatory, null, jsonObjectToMap(defaultValue), supports, modifier, compareMode);
    }

    /**
     * Constructor that accepts defaultValue as Map (for Jackson deserialization).
     * 
     * @param field the name of the field
     * @param description the description of the field
     * @param type the required type of the payload detail
     * @param mandatory indicates if this field is mandatory
     * @param referencedField the referencedField value as Map<String, Object>
     * @param defaultValue the default value as Map<String, Object>
     * @param supports list of supported operations
     * @param modifier the modifier applied to the field
     * @param compareMode the comparison mode applied to the field
     */
    public PayloadDetail(String field, String description, String type, Boolean mandatory, Map<String, Object> referencedField,
                        Map<String, Object> defaultValue, List<String> supports, String modifier, String compareMode) {
        this.field = field;
        this.description = description;
        this.type = type;
        this.mandatory = mandatory; 
        this.referencedField = referencedField; 
        this.defaultValue = defaultValue;
        this.supports = supports != null ? supports : new ArrayList<>();
        this.compareMode = compareMode;
        this.modifier = modifier;
    }


    /**
     * Converts a JSONObject to a Map<String, Object>.
     */
    private static Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, jsonObject.get(key));
        }
        return map;
    }

    /**
     * Converts a Map<String, Object> to a JSONObject.
     */
    private static JSONObject mapToJsonObject(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return new JSONObject(map);
    }

    // Getters

    /**
     * Gets the field name.
     * 
     * @return the field name, or null if not set
     */
    public String getField() {
        return field;
    }

    /**
     * Gets the description.
     * 
     * @return the description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the referencedField value of the field as a JSONObject.
     * 
     * @return the referencedField value as JSONObject, or null if not set
     */
    public JSONObject getReferencedField() {
        return mapToJsonObject(referencedField);
    }

    /**
     * Gets a specific value from the default value object.
     * For example, if referencedField is {"link": "id:participant.name","type" : "str"}, calling getReferencedFieldByKey("type") returns "str"
     * 
     * @param key the key to retrieve from the referencedField value object
     * @return the value associated with the key, or null if not found
     */
    public String getReferencedFieldByKey(String key) {
        if (referencedField != null && referencedField.containsKey(key)) {
            Object value = referencedField.get(key);
            return value != null ? value.toString() : null;
        }
        return null;
    }

    /**
     * Gets the last part of the type string, tokenized by the "@" character.
     * 
     * @return the last part of the type
     */
    public String getType() {
        return type;
    }

    /**
     * Determines if the field is mandatory.
     * 
     * @return true if mandatory, false if not, or null if not applicable
     */
    public Boolean isMandatory() {
        return mandatory;
    }

    /**
     * Gets the default value of the field as a JSONObject.
     * 
     * @return the default value as JSONObject, or null if not set
     */
    public JSONObject getDefaultvalue() {
        return mapToJsonObject(defaultValue);
    }

    /**
     * Gets a specific value from the default value object.
     * For example, if defaultValue is {"add": "%DATE%"}, calling getDefaultValueByKey("add") returns "%DATE%"
     * 
     * @param key the key to retrieve from the default value object
     * @return the value associated with the key, or null if not found
     */
    public String getDefaultValueByKey(String key) {
        if (defaultValue != null && defaultValue.containsKey(key)) {
            Object value = defaultValue.get(key);
            return value != null ? value.toString() : null;
        }
        return null;
    }

    /**
     * Gets the list of supported operations.
     * 
     * @return the list of supported operations, or empty list if not set
     */
    public List<String> getSupports() {
        return supports;
    }

    /**
     * Checks if a specific operation is supported.
     * 
     * @param operation the operation to check
     * @return true if the operation is supported, false otherwise
     */
    public boolean isOperationSupported(String operation) {
        return supports != null && supports.contains(operation);
    }

    /**
     * Gets the comparison mode of the field.
     * 
     * @return the comparison mode, or null if not set
     */
    public String getCompareMode() {
        return compareMode;
    }

    /**
     * Gets the modifier of the field.
     * 
     * @return the modifier, or null if not set
     */
    public String getModifier() {
        return modifier;
    }

    // Setters

    /**
     * Sets the field name.
     * 
     * @param field the field name to set
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Sets the description.
     * 
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the mandatory status of the field.
     * 
     * @param mandatory true if the field is mandatory, false otherwise
     */
    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Sets the referencedField value of the field as a JSONObject.
     * 
     * @param referencedField the default value to set as JSONObject
     */
    public void setReferencedField(Map<String, Object> referencedField) {
        this.referencedField = referencedField;
    }

    /**
     * Sets the default value of the field as a JSONObject.
     * 
     * @param defaultValue the default value to set as JSONObject
     */
    public void setDefaultValue(Map<String, Object> defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Sets the list of supported operations.
     * 
     * @param supports the list of supported operations
     */
    public void setSupports(List<String> supports) {
        this.supports = supports != null ? supports : new ArrayList<>();
    }

    /**
     * Sets the comparison mode of the field.
     * 
     * @param compareMode the comparison mode to set
     */
    public void setCompareMode(String compareMode) {
        this.compareMode = compareMode;
    }

    /**
     * Sets the modifier of the field.
     * 
     * @param modifier the modifier to set
     */
    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    /**
     * Converts this PayloadDetail instance to a JSON object.
     * 
     * @return a JSONObject representing the PayloadDetail instance
     */
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        if (field != null) jsonObject.put("field", field);
        if (modifier != null) jsonObject.put("modifier", modifier);
        if (description != null) jsonObject.put("description", description);
        if (referencedField != null) jsonObject.put("referencedField", referencedField);
        if (defaultValue != null) jsonObject.put("defaultValue", defaultValue);
        if (supports != null && !supports.isEmpty()) jsonObject.put("supports", new JSONArray(supports));
        if (type != null) jsonObject.put("type", type);
        if (mandatory != null) jsonObject.put("mandatory", mandatory);
        if (compareMode != null) jsonObject.put("compareMode", compareMode);
        return jsonObject;
    }

    /**
     * Returns a string representation of this PayloadDetail instance in JSON format.
     * 
     * @return a JSON string representation of this instance
     */
    @Override
    public String toString() {
        return toJSON().toString();
    }
}
