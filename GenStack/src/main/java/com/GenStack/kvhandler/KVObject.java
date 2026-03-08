package com.GenStack.kvhandler;

import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.kvhandler.KVBaseObject;
import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.StringParserHelper;
import com.GenStack.helper.validators.*;

public class KVObject extends KVBaseObject<Object> {
    private String namespace;
    private String identifier;
    private Map<String, KVObjectField> fieldTypeMap;

    public KVObject(String namespace, String identifier, Map<String, KVObjectField> fieldTypeMap, Map<String, String> jsonFields) {
        super();
        //DebugUtil.debug(identifier, fieldTypeMap.toString(), jsonFields.toString());

        this.namespace = namespace;
        this.identifier = identifier;
        this.fieldTypeMap = new HashMap<>();

        // Initialize the field type map
        if (fieldTypeMap != null) {
            this.fieldTypeMap.putAll(fieldTypeMap);
        }

        for (Map.Entry<String, String> entry : jsonFields.entrySet()) {
            String fieldName = entry.getKey();
            String valueStr = StringParserHelper.parseString(entry.getValue());
            KVObjectField definition = this.fieldTypeMap.get(fieldName);
            String expectedType = (definition != null) ? definition.getEnterType() : null;


            Object value;
            if (expectedType == null) {
                // If field type does not exist, check for mandatory status
                continue; // or throw an exception based on your requirements
            } else {
                value = validator(valueStr, expectedType, fieldName);
            }
            addField(fieldName, value);
        }

        // Handle default values for non-mandatory fields
        for (Map.Entry<String, KVObjectField> entry : this.fieldTypeMap.entrySet()) {
            String fieldName = entry.getKey();
            KVObjectField definition = entry.getValue();
            if (!jsonFields.containsKey(fieldName) && !definition.isMandatory()) {
                addField(fieldName, definition.getDefaultValue());
            }
        }
    }

    public String getNamespace() {
        //DebugUtil.debug();
        return namespace;
    }

    public String getIdentifier() {
        //DebugUtil.debug();
        return identifier;
    }

    public Map<String, KVObjectField> getFieldTypeMap() {
        //DebugUtil.debug();
        return fieldTypeMap;  // Return a reference to the field type map
    }

    public String getFieldType(String fieldName) {
        //DebugUtil.debug(fieldName);
        KVObjectField fieldDetail = fieldTypeMap.get(fieldName);
        return (fieldDetail != null) ? fieldDetail.getEnterType() : null; // Return the type or null if not found
    }

    public Object validator(String valueStr, String expectedType, String fieldName) {
        //DebugUtil.debug(valueStr, expectedType,fieldName);
        String fieldNameStr = "";
        if ( fieldName != null){
            fieldNameStr = fieldName;
        }

        switch (expectedType) {
            case "str":
                return valueStr;
            case "int":
                return Integer.parseInt(valueStr);
            case "unsigned":
                int unsignedValue = Integer.parseInt(valueStr);
                if (unsignedValue <= 0) {
                    throw new IllegalArgumentException(fieldNameStr + " must be a positive integer.");
                }
                return unsignedValue;
            case "date":
                return java.time.LocalDate.parse(valueStr);
            case "time":
                return java.time.LocalTime.parse(valueStr);
            case "duration":
                return java.time.Duration.parse(valueStr);
            default:
                throw new IllegalArgumentException("Unknown type for field: " + fieldNameStr + "(" + valueStr + "," + expectedType + ")" );
        }
    }


    // Method to convert fieldTypeMap to JSONObject
    public JSONObject getfieldTypeMapJSON() {
        JSONObject jsonObject = new JSONObject();

        for (Map.Entry<String, KVObjectField> entry : fieldTypeMap.entrySet()) {
            String key = entry.getKey();
            KVObjectField value = entry.getValue();

            // Assuming KVObjectField has a toJSON method that returns JSONObject
            if (value != null) {
                jsonObject.put(key, value.toJSON());
            }
        }

        return jsonObject;
    }

    public JSONObject getKVObjectData() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("identifier", this.identifier); // Add identifier to JSON

        for (Field<Object> field : getFields()) {
            jsonObject.put(field.getName(), field.getValue()); // Add fields to JSON
        }
        
        return jsonObject;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("identifier", this.identifier); // Add identifier to JSON
        jsonObject.put("fieldTypeMap", getfieldTypeMapJSON()); // Add identifier to JSON

        for (Field<Object> field : getFields()) {
            jsonObject.put(field.getName(), field.getValue()); // Add fields to JSON
        }
        
        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}


