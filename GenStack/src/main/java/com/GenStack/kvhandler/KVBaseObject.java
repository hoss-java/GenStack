package com.GenStack.kvhandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.GenStack.helper.validators.*;

public abstract class KVBaseObject<T> {
    protected List<Field<T>> fields;

    public KVBaseObject() {
        fields = new ArrayList<>();
    }

    protected void addField(String name, T value) {
        if (hasField(name) != false) {
            throw new IllegalArgumentException("Field already exists: " + name);
        }
        fields.add(new Field<>(name, value));
    }

    public List<Field<T>> getFields() {
        return fields;
    }

    public T getFieldValue(String fieldName) {
        for (Field<T> field : getFields()) {
            if (field.getName().equals(fieldName)) {
                return field.getValue();
            }
        }
        throw new IllegalArgumentException("Field not found: " + fieldName);
    }

    public boolean hasField(String fieldName) {
        for (Field<T> field : getFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public void updateField(String fieldName, T newValue) {
        for (Field<T> field : getFields()) {
            if (field.getName().equals(fieldName)) {
                field.value = newValue; // Assuming Field has setValue method
                return;
            }
        }
        throw new IllegalArgumentException("Field not found: " + fieldName);
    }

    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        for (Field<T> field : fields) {
            fieldNames.add(field.getName());
        }
        return fieldNames;
    }

    public static class Field<T> {
        private String name;
        private T value;

        public Field(String name, T value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public T getValue() {
            return value;
        }
    }
}
