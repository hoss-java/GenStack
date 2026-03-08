package com.GenStack.storages;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.GenStack.storages.StorageSettings;

public class NamespaceStorageConfig {
    private final Map<String, StorageTypeConfig> subjectStorages;
    private final Map<String, StorageTypeConfig> objectStorages;
    
    /**
     * StorageTypeConfig holds type and configuration for a single storage
     */
    public static class StorageTypeConfig {
        private final String type;
        private final Object config;
        
        public StorageTypeConfig(String type, Object config) {
            this.type = type;
            this.config = config;
        }
        
        public String getType() {
            return type;
        }
        
        public Object getConfig() {
            return config;
        }
    }
    
    /**
     * Constructor using maps
     */
    public NamespaceStorageConfig(
            Map<String, StorageTypeConfig> subjectStorages,
            Map<String, StorageTypeConfig> objectStorages) {
        this.subjectStorages = new ConcurrentHashMap<>(subjectStorages);
        this.objectStorages = new ConcurrentHashMap<>(objectStorages);
        
        if (this.subjectStorages.isEmpty()) {
            throw new IllegalArgumentException("At least one subject storage must be configured");
        }
        if (this.objectStorages.isEmpty()) {
            throw new IllegalArgumentException("At least one object storage must be configured");
        }
    }

    /**
     * Empty constructor - storages can be added later
     */
    public NamespaceStorageConfig() {
        this.subjectStorages = new ConcurrentHashMap<>();
        this.objectStorages = new ConcurrentHashMap<>();
    }    

    /**
     * Get all subject storages
     */
    public Map<String, StorageTypeConfig> getSubjectStorages() {
        return new HashMap<>(subjectStorages);
    }
    
    /**
     * Get all object storages
     */
    public Map<String, StorageTypeConfig> getObjectStorages() {
        return new HashMap<>(objectStorages);
    }
    
    /**
     * Get a specific subject storage config by name
     */
    public StorageTypeConfig getSubjectStorage(String storageName) {
        StorageTypeConfig config = subjectStorages.get(storageName);
        if (config == null) {
            throw new IllegalArgumentException("Subject storage '" + storageName + "' not found");
        }
        return config;
    }
    
    /**
     * Get a specific object storage config by name
     */
    public StorageTypeConfig getObjectStorage(String storageName) {
        StorageTypeConfig config = objectStorages.get(storageName);
        if (config == null) {
            throw new IllegalArgumentException("Object storage '" + storageName + "' not found");
        }
        return config;
    }
    
    /**
     * Check if subject storage exists
     */
    public boolean hasSubjectStorage(String storageName) {
        return subjectStorages.containsKey(storageName);
    }
    
    /**
     * Check if object storage exists
     */
    public boolean hasObjectStorage(String storageName) {
        return objectStorages.containsKey(storageName);
    }
    
    /**
     * Get all subject storage names
     */
    public Set<String> getSubjectStorageNames() {
        return new HashSet<>(subjectStorages.keySet());
    }
    
    /**
     * Get all object storage names
     */
    public Set<String> getObjectStorageNames() {
        return new HashSet<>(objectStorages.keySet());
    }


    /**
     * Add or update a subject storage configuration
     */
    public void putSubjectStorage(String storageName, StorageSettings storageSettings) {
        String type = storageName;
        if (storageName == null || storageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage type cannot be null or empty");
        }
        if (storageSettings == null) {
            throw new IllegalArgumentException("Storage config cannot be null");
        }
        subjectStorages.put(storageName, new StorageTypeConfig(type, storageSettings));
    }

    /**
     * Add or update a subject storage configuration using StorageTypeConfig
     */
    public void putSubjectStorage(String storageName, StorageTypeConfig storageTypeConfig) {
        if (storageName == null || storageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage name cannot be null or empty");
        }
        if (storageTypeConfig == null) {
            throw new IllegalArgumentException("StorageTypeConfig cannot be null");
        }
        subjectStorages.put(storageName, storageTypeConfig);
    }

    /**
     * Add or update an object storage configuration
     */
    public void putObjectStorage(String storageName, StorageSettings storageSettings) {
        String type = storageName;
        if (storageName == null || storageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage type cannot be null or empty");
        }
        if (storageSettings == null) {
            throw new IllegalArgumentException("Storage storageSettings cannot be null");
        }
        objectStorages.put(storageName, new StorageTypeConfig(type, storageSettings));
    }

    /**
     * Add or update an object storage configuration using StorageTypeConfig
     */
    public void putObjectStorage(String storageName, StorageTypeConfig storageTypeConfig) {
        if (storageName == null || storageName.trim().isEmpty()) {
            throw new IllegalArgumentException("Storage name cannot be null or empty");
        }
        if (storageTypeConfig == null) {
            throw new IllegalArgumentException("StorageTypeConfig cannot be null");
        }
        objectStorages.put(storageName, storageTypeConfig);
    }

    /**
     * Remove a subject storage configuration by name
     */
    public StorageTypeConfig removeSubjectStorage(String storageName) {
        return subjectStorages.remove(storageName);
    }

    /**
     * Remove an object storage configuration by name
     */
    public StorageTypeConfig removeObjectStorage(String storageName) {
        return objectStorages.remove(storageName);
    }

    /**
     * Clear all subject storage configurations
     */
    public void clearSubjectStorages() {
        subjectStorages.clear();
    }

    /**
     * Clear all object storage configurations
     */
    public void clearObjectStorages() {
        objectStorages.clear();
    }

    /**
     * Clear all subject and object storage configurations
     */
    public void clearAll() {
        subjectStorages.clear();
        objectStorages.clear();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("subjectStorages", mapToJSON(subjectStorages));
        json.put("objectStorages", mapToJSON(objectStorages));
        return json;
    }

    private JSONObject mapToJSON(Map<String, StorageTypeConfig> storages) {
        JSONObject json = new JSONObject();
        storages.forEach((key, config) -> 
            json.put(key, configToJSON(config))
        );
        return json;
    }

    private JSONObject configToJSON(StorageTypeConfig config) {
        JSONObject json = new JSONObject();
        json.put("type", config.getType());
        json.put("config", config.getConfig());
        return json;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int... indentation) {
        int indent = indentation.length > 0 ? indentation[0] : 0;
        return toJSON().toString(indent);
    }

}
