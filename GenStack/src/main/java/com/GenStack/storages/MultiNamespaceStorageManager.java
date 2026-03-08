package com.GenStack.storages;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.json.JSONObject;

import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.storages.SubjectStorageFactory;
import com.GenStack.storages.ObjectStorageFactory;
import com.GenStack.storages.NamespaceStorage;
import com.GenStack.storages.NamespaceStorageConfig;
import com.GenStack.storages.StorageSettings;
import com.GenStack.config.StorageConfig;
import com.GenStack.helper.DebugUtil;

public class MultiNamespaceStorageManager {
    private final Map<String, NamespaceStorage> namespaces = new ConcurrentHashMap<>();
    private final Map<String, NamespaceStorageConfig> namespaceConfigs = new ConcurrentHashMap<>();
    
    private static MultiNamespaceStorageManager INSTANCE;
    private StorageConfig storageConfig;

    private MultiNamespaceStorageManager(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public static MultiNamespaceStorageManager getInstance(StorageConfig storageConfig) {
        if (INSTANCE == null) {
            synchronized (MultiNamespaceStorageManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MultiNamespaceStorageManager(storageConfig);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Register a namespace with configuration (Optional - can be added later)
     */
    public void registerNamespace(String namespaceName, String defaultStorageName) {
        registerNamespace(namespaceName, defaultStorageName, null);
    }
    
    /**
     * Register a namespace with configuration
     */
    public void registerNamespace(String namespaceName, String defaultStorageName, NamespaceStorageConfig config) {
        if (namespaces.containsKey(namespaceName)) {
            throw new IllegalArgumentException("Namespace '" + namespaceName + "' already exists");
        }
        
        try {
            Map<String, KVSubjectStorage> subjectStorages = new HashMap<>();
            Map<String, KVObjectStorage> objectStorages = new HashMap<>();
            
            // If config is provided, create storages
            if (config != null) {
                // Create all subject storages
                for (String storageName : config.getSubjectStorageNames()) {
                    NamespaceStorageConfig.StorageTypeConfig storageConfig = config.getSubjectStorage(storageName);
                    KVSubjectStorage storage = SubjectStorageFactory.createKVSubjectStorage(
                        storageConfig.getType(),
                        (StorageSettings) storageConfig.getConfig()
                    );
                    subjectStorages.put(storageName, storage);
                }
                
                // Create all object storages
                for (String storageName : config.getObjectStorageNames()) {
                    NamespaceStorageConfig.StorageTypeConfig storageConfig = config.getObjectStorage(storageName);
                    KVObjectStorage storage = ObjectStorageFactory.createKVObjectStorage(
                        storageConfig.getType(),
                        (StorageSettings) storageConfig.getConfig()
                    );
                    objectStorages.put(storageName, storage);
                }
                
                namespaceConfigs.put(namespaceName, config);
            }
            
            NamespaceStorage namespaceStorage = new NamespaceStorage(
                namespaceName,
                defaultStorageName,
                subjectStorages,
                objectStorages
            );
            
            namespaces.put(namespaceName, namespaceStorage);
            
            String storageInfo = (config != null) ?
                "with " + subjectStorages.size() + " subject storages and " + objectStorages.size() + " object storages" :
                "with no storages (can be added later)";
            
            System.out.println("Namespace '" + namespaceName + "' registered " + storageInfo);
            
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to register namespace '" + namespaceName + "': " + e.getMessage(), e
            );
        }
    }
    
    /**
     * Add a subject storage to an existing namespace
     */
    public void addSubjectStorage(String namespaceName, String storageName, StorageSettings storageSettings) {
        NamespaceStorage namespaceStorage = getNamespace(namespaceName);
        String type = storageName;

        if ( hasSubjectStorage(namespaceName, storageName) == false ){
            try {
                KVSubjectStorage storage = SubjectStorageFactory.createKVSubjectStorage(type, storageSettings);
                namespaceStorage.addSubjectStorage(storageName, storage);
                
                // Update config if it exists
                NamespaceStorageConfig existingConfig = namespaceConfigs.get(namespaceName);
                if (existingConfig != null) {
                    existingConfig.putSubjectStorage(storageName, storageSettings);
                }
                
                System.out.println("Subject storage '" + namespaceStorage.getStorageNameToUse(storageName)  + "' added to namespace '" + namespaceName + "'");
                
            } catch (Exception e) {
                System.out.println("Failed to add subject storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' to namespace '" + namespaceName);
                //throw new RuntimeException(
                //    "Failed to add subject storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' to namespace '" + namespaceName + "': " + e.getMessage(), e
                //);
            }
        }
    }
    public void addSubjectStorage(String namespaceName, String storageName) {
        StorageSettings storageSettings = storageConfig.getStorageConfig(namespaceName, storageName);
        addSubjectStorage(namespaceName, storageName, storageSettings);
    }
    
    /**
     * Add an object storage to an existing namespace
     */
    public void addObjectStorage(String namespaceName, String storageName, StorageSettings storageSettings) {
        NamespaceStorage namespaceStorage = getNamespace(namespaceName);
        String type = namespaceStorage.getStorageNameToUse(storageName);
        StorageSettings storageSettingsToUse =  storageSettings == null ? storageConfig.getStorageConfig(namespaceName, type) : storageSettings;

        if ( hasObjectStorage(namespaceName, storageName) == false ){
            try {
                KVObjectStorage storage = ObjectStorageFactory.createKVObjectStorage(type, storageSettingsToUse);
                namespaceStorage.addObjectStorage(storageName, storage);
                
                // Update config if it exists
                NamespaceStorageConfig existingConfig = namespaceConfigs.get(namespaceName);
                if (existingConfig != null) {
                    existingConfig.putSubjectStorage(storageName, storageSettingsToUse);
                }
                
                System.out.println("Object storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' added to namespace '" + namespaceName + "'");
                
            } catch (Exception e) {
                System.out.println("Failed to add object storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' to namespace '" + namespaceName );
                //throw new RuntimeException(
                //    "Failed to add object storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' to namespace '" + namespaceName + "': " + e.getMessage(), e
                //);
            }
        }
    }
    public void addObjectStorage(String namespaceName, String storageName) {
        StorageSettings storageSettings = storageConfig.getStorageConfig(namespaceName, storageName);
        addObjectStorage(namespaceName, storageName, storageSettings);
    }
    
    /**
     * Remove a subject storage from a namespace
     */
    public void removeSubjectStorage(String namespaceName, String storageName) {
        NamespaceStorage namespaceStorage = getNamespace(namespaceName);
        namespaceStorage.removeSubjectStorage(storageName);
        
        // Update config if it exists
        NamespaceStorageConfig existingConfig = namespaceConfigs.get(namespaceName);
        if (existingConfig != null) {
            existingConfig.removeSubjectStorage(storageName);
        }
        
        System.out.println("Subject storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' removed from namespace '" + namespaceName + "'");
    }
    
    /**
     * Remove an object storage from a namespace
     */
    public void removeObjectStorage(String namespaceName, String storageName) {
        NamespaceStorage namespaceStorage = getNamespace(namespaceName);
        namespaceStorage.removeObjectStorage(storageName);
        
        // Update config if it exists
        NamespaceStorageConfig existingConfig = namespaceConfigs.get(namespaceName);
        if (existingConfig != null) {
            existingConfig.removeObjectStorage(storageName);
        }
        
        System.out.println("Object storage '" + namespaceStorage.getStorageNameToUse(storageName) + "' removed from namespace '" + namespaceName + "'");
    }
    
    /**
     * Get namespace storage by name
     */
    public NamespaceStorage getNamespace(String namespaceName) {
        NamespaceStorage storage = namespaces.get(namespaceName);
        if (storage == null) {
            throw new IllegalArgumentException("Namespace '" + namespaceName + "' not found");
        }
        return storage;
    }
    
    /**
     * Get a specific subject storage
     */
    public KVSubjectStorage getSubjectStorage(String namespaceName, String storageName) {
        KVSubjectStorage kvSubjectStorage = getNamespace(namespaceName).getSubjectStorage(storageName);
        if ( kvSubjectStorage == null ){
            addSubjectStorage(namespaceName, storageName);
            kvSubjectStorage = getNamespace(namespaceName).getSubjectStorage(storageName);
        }
        return kvSubjectStorage;
    }

    /**
     * Verify a specific subject storage
     */
    public boolean hasSubjectStorage(String namespaceName, String storageName) {
        return getNamespace(namespaceName).hasSubjectStorage(storageName);
    }

    /**
     * Get a specific object storage
     */
    public KVObjectStorage getObjectStorage(String namespaceName, String storageName) {
        KVObjectStorage kvObjectStorage = getNamespace(namespaceName).getObjectStorage(storageName);
        if ( kvObjectStorage == null ){
            addObjectStorage(namespaceName, storageName);
            kvObjectStorage = getNamespace(namespaceName).getObjectStorage(storageName);
        }
        return kvObjectStorage;
    }

    /**
     * Verify a specific object storage
     */
    public boolean hasObjectStorage(String namespaceName, String storageName) {
        return getNamespace(namespaceName).hasObjectStorage(storageName);
    }
    
    /**
     * Get all subject storages for a namespace
     */
    public Map<String, KVSubjectStorage> getAllSubjectStorages(String namespaceName) {
        return getNamespace(namespaceName).getAllSubjectStorages();
    }
    
    /**
     * Get all object storages for a namespace
     */
    public Map<String, KVObjectStorage> getAllObjectStorages(String namespaceName) {
        return getNamespace(namespaceName).getAllObjectStorages();
    }
    
    /**
     * Check if namespace exists
     */
    public boolean namespaceExists(String namespaceName) {
        return namespaces.containsKey(namespaceName);
    }

    /**
     * Delete a namespace and close all its storages
     */
    public void deleteNamespace(String namespaceName) {
        NamespaceStorage storage = namespaces.remove(namespaceName);
        namespaceConfigs.remove(namespaceName);
        
        if (storage != null) {
            storage.close();
            System.out.println("Namespace '" + namespaceName + "' deleted");
        }
    }
    
    /**
     * List all registered namespaces
     */
    public Set<String> listNamespaces() {
        return new HashSet<>(namespaces.keySet());
    }
    
    /**
     * Get configuration for a namespace
     */
    public NamespaceStorageConfig getNamespaceConfig(String namespaceName) {
        NamespaceStorageConfig config = namespaceConfigs.get(namespaceName);
        if (config == null) {
            throw new IllegalArgumentException("Namespace '" + namespaceName + "' not found");
        }
        return config;
    }
    
    /**
     * Close all namespaces
     */
    public void closeAll() {
        namespaces.values().forEach(NamespaceStorage::close);
        namespaces.clear();
        namespaceConfigs.clear();
        System.out.println("All namespaces closed");
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        
        JSONObject namespacesJson = new JSONObject();
        namespaces.forEach((key, value) -> 
            namespacesJson.put(key, value.toJSON())
        );
        json.put("namespaces", namespacesJson);
        
        JSONObject configsJson = new JSONObject();
        namespaceConfigs.forEach((key, value) -> 
            configsJson.put(key, value.toJSON())
        );
        json.put("namespaceConfigs", configsJson);
        
        json.put("storageConfig", storageConfig.toJSON());
        
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