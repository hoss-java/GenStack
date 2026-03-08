package com.GenStack.kvhandler;

import org.json.JSONObject;
import java.lang.reflect.Field;
import com.GenStack.helper.DebugUtil;

public class KVSubjectAttribute {
    private String namespace;
    private String identifier;
    private String description;
    private String storage;
    private String actions;

    // No-argument constructor required for deserialization
    public KVSubjectAttribute() {
    }

    public KVSubjectAttribute(
            String namespace,
            String identifier, 
            String description,
            String storage,
            String actions) {
        this.namespace = namespace;
        this.identifier = identifier;
        this.description = description;
        this.storage = storage;
        this.actions = actions;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDescription() {
        return description;
    }

    public String getStorage() {
        return storage;
    }

    public String getActions() {
        return actions;
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
