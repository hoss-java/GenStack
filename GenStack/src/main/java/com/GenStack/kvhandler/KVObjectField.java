package com.GenStack.kvhandler;

import org.json.JSONObject;
import java.lang.reflect.Field;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.TokenizedString;

public class KVObjectField {
    private String field;
    private String type;
    private boolean mandatory;
    private String modifier;
    private String defaultValue;

    // No-argument constructor required for deserialization
    public KVObjectField() {
    }

    public KVObjectField(String field, String type, boolean mandatory, String modifier, String defaultValue) {
        this.field = field;
        this.type = type;
        this.mandatory = mandatory;
        this.modifier = modifier;
        this.defaultValue = defaultValue;
    }

    public String getFullField() {
        return field;
    }

    public String getField() {
        return (new TokenizedString(field,"@")).getPart(0);
    }

    public String getEnterField() {
        return (new TokenizedString(field,"@")).getPart(-1);
    }

    public String getFullType() {
        return type;
    }

    public String getType() {
        return (new TokenizedString(type,"@")).getPart(0);
    }

    public String getEnterType() {
        return (new TokenizedString(type,"@")).getPart(-1);
    }

    public String getSqlType() {
        switch (getType().toLowerCase()) {
            case "str":
                return "TEXT";
            case "int":
                return "INTEGER";
            case "unsigned":
                return "INTEGER";
            case "date":
                return "DATE";
            case "time":
                return "TIME";
            case "duration":
                return "VARCHAR(255)";
            default:
                System.out.println("Hint: Unknown argument type: " + type);
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getModifier() {
        return modifier;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                jsonObject.put(field.getName(), field.get(this));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
