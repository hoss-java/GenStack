package com.GenStack.kvhandler;

import java.util.HashMap;
import java.util.Map;

import com.GenStack.config.ConfigManager;
import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVSubjectAttribute;
import com.GenStack.helper.DebugUtil;

public abstract class KVBaseSubject {
    protected KVSubjectAttribute subjectAttribute;
    protected Map<String, KVObjectField> fieldTypeMap;

    // Constructor
    public KVBaseSubject(KVSubjectAttribute subjectAttribute) {
        this.subjectAttribute = subjectAttribute;
        // it needs to be initialized before using here
        this.fieldTypeMap = new HashMap<>(); // Initialize the map
    }

    // Getter for fieldTypeMap
    public Map<String, KVObjectField> getFieldTypeMap() {
        return fieldTypeMap;
    }

    // Setter for fieldTypeMap
    public void setFieldTypeMap(Map<String, KVObjectField> fieldTypeMap) {
        this.fieldTypeMap = fieldTypeMap; // Set the new fieldTypeMap
    }

    // Method to get the next ID
    public int getNextId() {
        ConfigManager configManager = ConfigManager.getInstance(null,null);
        int nextId = configManager.getSetting(subjectAttribute.getIdentifier(),"nextid",0);
        nextId++;
        configManager.setSetting(subjectAttribute.getIdentifier(),"nextid",nextId);
        return nextId;
    }

    public KVSubjectAttribute getAttribute() {
        return subjectAttribute;
    }

    // Method to get the namespace
    public String getNamespace() {
        return subjectAttribute.getNamespace();
    }

    // Method to get the identifier
    public String getIdentifier() {
        return subjectAttribute.getIdentifier();
    }

    // Method to get the storage
    public String getStorage() {
        return subjectAttribute.getStorage();
    }

    // Method to get the description
    public String getDescription() {
        return subjectAttribute.getDescription();
    }

    // Method to get the actions
    public String getActions() {
        return subjectAttribute.getActions();
    }

}
