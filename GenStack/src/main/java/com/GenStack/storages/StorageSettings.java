package com.GenStack.storages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StorageSettings {
    private String namespace;
    private String storageType;
    private String storageFolder;
    private Map<String, String> settings;
    
    public StorageSettings(String namespace, String storageType, String storageFolder, Map<String, String> settings) {
        this.namespace = namespace;
        this.storageType = storageType;
        this.storageFolder = storageFolder;
        this.settings = settings != null ? new HashMap<>(settings) : new HashMap<>();
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public String getStorageType() {
        return storageType;
    }

    public String getstorageFolder() {
        return storageFolder;
    }
    
    public String get(String key) {
        return settings.get(key);
    }
    
    public String get(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }
    
    public boolean containsKey(String key) {
        return settings.containsKey(key);
    }
    
    public Set<String> keySet() {
        return settings.keySet();
    }
    
    public Map<String, String> asMap() {
        return new HashMap<>(settings);
    }
    
    @Override
    public String toString() {
        return "StorageSettings{" +
                "namespace='" + namespace + '\'' +
                ", storageType='" + storageType + '\'' +
                ", settings=" + settings +
                '}';
    }
}
