package com.GenStack.storages;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.helper.DebugUtil;

public class NamespaceStorage {
    private String namespaceName;
    private String defaultStorage;
    private final Map<String, KVSubjectStorage> subjectStorages;
    private final Map<String, KVObjectStorage> objectStorages;
        
    public NamespaceStorage(
            String namespaceName,
            String defaultStorage,
            Map<String, KVSubjectStorage> subjectStorages,
            Map<String, KVObjectStorage> objectStorages) {
        this.namespaceName = namespaceName;
        this.defaultStorage = defaultStorage;
        this.subjectStorages = new ConcurrentHashMap<>(subjectStorages);
        this.objectStorages = new ConcurrentHashMap<>(objectStorages);
    }
    
    public String getNamespaceName() {
        return namespaceName;
    }

    public String getdefaultStorage() {
        return defaultStorage;
    }

    public void setdefaultStorage(String defaultStorage) {
        this.defaultStorage = defaultStorage;
    }

    public String getStorageNameToUse(String storageName) {
        return (storageName == null || storageName.trim().isEmpty()) ? defaultStorage : storageName;
    }
    
    /**
     * Get a specific subject storage by name
     */
    public KVSubjectStorage getSubjectStorage(String storageName) {
        KVSubjectStorage storage = subjectStorages.get(getStorageNameToUse(storageName));
        if (storage == null) {
            String errorMsg = "Subject storage '" + getStorageNameToUse(storageName)
                + "' not found in namespace '" + namespaceName + "'";
            System.err.println(errorMsg);  // or use a logger
            return null;
        }
        return storage;
    }

    /**
     * Verify a specific subject storage by name
     */
    public boolean hasSubjectStorage(String storageName) {
        KVSubjectStorage storage = subjectStorages.get(getStorageNameToUse(storageName));
        if (storage == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Get a specific object storage by name
     */
    public KVObjectStorage getObjectStorage(String storageName) {
        KVObjectStorage storage = objectStorages.get(getStorageNameToUse(storageName));
        if (storage == null) {
            String errorMsg = "Object storage '" + getStorageNameToUse(storageName) + 
                "' not found in namespace '" + namespaceName + "'";
            System.err.println(errorMsg);  // or use a logger
            return null;
        }
        return storage;
    }

    /**
     * Verify a specific object storage by name
     */
    public boolean hasObjectStorage(String storageName) {
        KVObjectStorage storage = objectStorages.get(getStorageNameToUse(storageName));
        if (storage == null) {
            return false;
        }
        return true;
    }

    /**
     * Add a subject storage dynamically
     */
    public KVSubjectStorage addSubjectStorage(String storageName, KVSubjectStorage storage) {
        if (subjectStorages.containsKey(getStorageNameToUse(storageName))) {
            String errorMsg = "Subject storage '" + getStorageNameToUse(storageName) 
                + "' already exists in namespace '" + namespaceName + "'";
            System.err.println(errorMsg);  // or use a logger
            return null;
        }
        subjectStorages.put(getStorageNameToUse(storageName), storage);
        return storage;  // return the added storage
    }
    
    /**
     * Add an object storage dynamically
     */
    public void addObjectStorage(String storageName, KVObjectStorage storage) {
        if (objectStorages.containsKey(getStorageNameToUse(storageName))) {
            String errorMsg = "Object storage '" + getStorageNameToUse(storageName)
                + "' already exists in namespace '" + namespaceName + "'";
            System.err.println(errorMsg);  // or use a logger
        }
        objectStorages.put(getStorageNameToUse(storageName), storage);
    }
    
    /**
     * Remove a subject storage
     */
    public void removeSubjectStorage(String storageName) {
        KVSubjectStorage storage = subjectStorages.remove(getStorageNameToUse(storageName));
        if (storage != null) {
            try {
                storage.close();
            } catch (Exception e) {
                System.err.println("Error closing subject storage: " + e.getMessage());
            }
        }
    }
    
    /**
     * Remove an object storage
     */
    public void removeObjectStorage(String storageName) {
        KVObjectStorage storage = objectStorages.remove(getStorageNameToUse(storageName));
        if (storage != null) {
            try {
                storage.close();
            } catch (Exception e) {
                System.err.println("Error closing object storage: " + e.getMessage());
            }
        }
    }

    /**
     * Get all subject storages
     */
    public Map<String, KVSubjectStorage> getAllSubjectStorages() {
        return new HashMap<>(subjectStorages);
    }
    
    /**
     * Get all object storages
     */
    public Map<String, KVObjectStorage> getAllObjectStorages() {
        return new HashMap<>(objectStorages);
    }
    
    /**
     * Get subject storage names
     */
    public Set<String> getSubjectStorageNames() {
        return new HashSet<>(subjectStorages.keySet());
    }
    
    /**
     * Get object storage names
     */
    public Set<String> getObjectStorageNames() {
        return new HashSet<>(objectStorages.keySet());
    }
    
    /**
     * Close all storages
     */
    public void close() {
        // Close all subject storages
        subjectStorages.values().forEach(storage -> {
            try {
                storage.close();
            } catch (Exception e) {
                System.err.println("Error closing subject storage: " + e.getMessage());
            }
        });
        
        // Close all object storages
        objectStorages.values().forEach(storage -> {
            try {
                storage.close();
            } catch (Exception e) {
                System.err.println("Error closing object storage: " + e.getMessage());
            }
        });
        
        subjectStorages.clear();
        objectStorages.clear();
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("namespaceName", namespaceName);
        json.put("defaultStorage", defaultStorage);
        json.put("subjectStorages", mapStoragesToJSON(subjectStorages));
        json.put("objectStorages", mapStoragesToJSON(objectStorages));
        return json;
    }

    private JSONObject mapStoragesToJSON(Map<String, ?> storages) {
        JSONObject json = new JSONObject();
        storages.forEach((key, value) -> {
            try {
                // Try to call toJSON() method if it exists
                java.lang.reflect.Method toJsonMethod = value.getClass().getMethod("toJSON");
                Object jsonResult = toJsonMethod.invoke(value);
                json.put(key, jsonResult);
            } catch (NoSuchMethodException e) {
                // If toJSON() doesn't exist, fall back to toString()
                json.put(key, value.toString());
            } catch (Exception e) {
                // If there's any other error, use toString()
                json.put(key, value.toString());
            }
        });
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