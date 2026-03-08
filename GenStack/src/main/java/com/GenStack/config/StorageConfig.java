package com.GenStack.config;

import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.Properties;
import java.util.Base64;
import java.util.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.GenStack.storages.StorageSettings;
import com.GenStack.helper.EncryptionUtil;

public class StorageConfig {
    private static final String dataFolderName = "data";
    private Properties properties;
    private Map<String, Map<String, Map<String, String>>> namespaceStorageConfig;
    private Map<String, SecretKey> secretKeys;
    private String globalSecretKeyString;
    private String appDataFolder;
    
    public StorageConfig(String appDataFolder, String storagePropertiesFile) {
        this.appDataFolder = appDataFolder;
        this.secretKeys = new HashMap<>();
        loadProperties(storagePropertiesFile);
    }
    
    private void loadProperties(String configFile) {
        properties = new Properties();
        try (InputStream input = getConfigInputStream(appDataFolder, configFile)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + configFile);
                return;
            }
            
            properties.load(input);
            
            // Load global secret key
            this.globalSecretKeyString = properties.getProperty("storage.secretKey");
            
            // Initialize namespace-storage configuration
            initializeNamespaceStorageConfig();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Parse properties into nested structure: namespace -> storage -> settings
     * Dynamically discovers storage types from property keys
     */
    private void initializeNamespaceStorageConfig() {
        namespaceStorageConfig = new HashMap<>();
        
        // Group properties by namespace
        Set<String> namespaces = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.contains(".storage.")) {
                String namespace = key.split("\\.storage\\.")[0];
                namespaces.add(namespace);
            }
        }
        
        // For each namespace, dynamically discover storage types
        for (String namespace : namespaces) {
            Map<String, Map<String, String>> storageMap = new HashMap<>();
            Set<String> storageTypes = new HashSet<>();
            
            // Discover all storage types for this namespace
            String namespacePrefix = namespace + ".storage.";
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith(namespacePrefix)) {
                    // Extract storage type: "namespace.storage.TYPE.setting"
                    String remainder = key.substring(namespacePrefix.length());
                    String storageType = remainder.split("\\.")[0];
                    storageTypes.add(storageType);
                }
            }
            
            // For each discovered storage type, collect its settings
            for (String storageType : storageTypes) {
                Map<String, String> storageSettings = new HashMap<>();
                String prefix = namespace + ".storage." + storageType + ".";
                
                for (String key : properties.stringPropertyNames()) {
                    if (key.startsWith(prefix)) {
                        String settingKey = key.substring(prefix.length());
                        storageSettings.put(settingKey, properties.getProperty(key));
                    }
                }
                
                if (!storageSettings.isEmpty()) {
                    storageMap.put(storageType, storageSettings);
                }
            }
            
            if (!storageMap.isEmpty()) {
                namespaceStorageConfig.put(namespace, storageMap);
            }
        }
    }

    
    /**
     * Get all namespaces configured
     */
    public Set<String> getNamespaces() {
        return namespaceStorageConfig.keySet();
    }
    
    /**
     * Get all enabled storage types for a namespace
     */
    public List<String> getEnabledStorageTypes(String namespace) {
        List<String> enabledStorages = new ArrayList<>();
        Map<String, Map<String, String>> storageMap = namespaceStorageConfig.get(namespace);
        
        if (storageMap != null) {
            for (String storageType : storageMap.keySet()) {
                Map<String, String> settings = storageMap.get(storageType);
                if ("true".equalsIgnoreCase(settings.getOrDefault("enabled", "false"))) {
                    enabledStorages.add(storageType);
                }
            }
        }
        
        return enabledStorages;
    }

    /**
     * Get configuration for specific namespace and storage type
     */
    public StorageSettings getStorageConfig(String namespace, String storageType) {
        Map<String, Map<String, String>> storageMap = namespaceStorageConfig.get(namespace);
        if (storageMap != null) {
            Map<String, String> settings = storageMap.get(storageType);
            if (settings != null) {
                return new StorageSettings(namespace, storageType, appDataFolder + "/" + dataFolderName, settings);
            }
        }
        return null;
    }

    
    /**
     * Get configuration for specific namespace and storage type
     */
    public Map<String, String> getStorageConfigAsMap(String namespace, String storageType) {
        StorageSettings settings = getStorageConfig(namespace, storageType);
        return settings != null ? settings.asMap() : null;
    }

    
    /**
     * Get specific setting value
     */
    public String getSetting(String namespace, String storageType, String settingKey) {
        StorageSettings settings = getStorageConfig(namespace, storageType);
        if (settings != null) {
            return settings.asMap().get(settingKey);
        }
        return null;
    }
    
    /**
     * Check if storage is enabled for namespace
     */
    public boolean isStorageEnabled(String namespace, String storageType) {
        String enabled = getSetting(namespace, storageType, "enabled");
        return "true".equalsIgnoreCase(enabled);
    }
    
    /**
     * Get decrypted password for specific namespace and storage
     */
    public String getDecryptedPassword(String namespace, String storageType) throws Exception {
        String encryptedPassword = getSetting(namespace, storageType, "password");
        
        if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
            return null;
        }
        
        // Get secret key for this namespace+storage combination
        SecretKey secretKey = getSecretKey(namespace, storageType);
        
        if (secretKey == null) {
            return null;
        }
        
        return EncryptionUtil.decrypt(encryptedPassword, secretKey);
    }
    
    /**
     * Get secret key for namespace and storage type
     */
    private SecretKey getSecretKey(String namespace, String storageType) {
        String keyId = namespace + "." + storageType;
        
        // Return cached key if available
        if (secretKeys.containsKey(keyId)) {
            return secretKeys.get(keyId);
        }
        
        // Try to get namespace+storage specific secret key
        String keyString = getSetting(namespace, storageType, "secretKey");
        
        // Fall back to global secret key if not found
        if (keyString == null || keyString.trim().isEmpty()) {
            keyString = globalSecretKeyString;
        }
        
        if (keyString != null && !keyString.trim().isEmpty()) {
            SecretKey secretKey = stringToKey(keyString);
            secretKeys.put(keyId, secretKey);
            return secretKey;
        }
        
        return null;
    }
    
    /**
     * Convert Base64 string to SecretKey
     */
    private SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }
    
    /**
     * Convert configuration to JSONObject (masks sensitive data)
     */
    public JSONObject toJSON() {
        JSONObject jsonConfig = new JSONObject();
        
        for (String namespace : namespaceStorageConfig.keySet()) {
            JSONObject namespaceObj = new JSONObject();
            Map<String, Map<String, String>> storageMap = namespaceStorageConfig.get(namespace);
            
            for (String storageType : storageMap.keySet()) {
                JSONObject storageObj = new JSONObject();
                Map<String, String> settings = storageMap.get(storageType);
                
                for (String key : settings.keySet()) {
                    if (key.equals("password") || key.equals("secretKey")) {
                        storageObj.put(key, "***MASKED***");
                    } else {
                        storageObj.put(key, settings.get(key));
                    }
                }
                
                namespaceObj.put(storageType, storageObj);
            }
            
            jsonConfig.put(namespace, namespaceObj);
        }
        
        return jsonConfig;
    }

    /**
     * Return string representation using JSON format
     */
    @Override
    public String toString() {
        return toString(0);
    }
    public String toString(int... indentation) {
        int indent = indentation.length > 0 ? indentation[0] : 0;
        return toJSON().toString(indent);
    }

    /**
     * Check if namespace exists
     */
    public boolean hasNamespace(String namespace) {
        return namespaceStorageConfig.containsKey(namespace);
    }
    
    /**
     * Check if storage type exists for namespace
     */
    public boolean hasStorageType(String namespace, String storageType) {
        Map<String, Map<String, String>> storageMap = namespaceStorageConfig.get(namespace);
        return storageMap != null && storageMap.containsKey(storageType);
    }

    /**
     * Helper method to get the correct InputStream based on appDataFolder availability.
     * If appDataFolder is set, reads from the file system; otherwise reads from resources.
     * 
     * @return InputStream for the config file, or null if file not found
     * @throws IOException if an I/O error occurs
     */
    protected InputStream getConfigInputStream(String appDataFolder, String configFile) throws IOException {
        if (appDataFolder != null && !appDataFolder.isEmpty()) {
            // Read from folder
            Path configPath = Paths.get(appDataFolder, configFile);
            if (Files.exists(configPath)) {
                return Files.newInputStream(configPath);
            } else {
                System.out.println("Config file not found at: " + configPath);
                return null;
            }
        } else {
            // Read from resources
            return getClass().getClassLoader().getResourceAsStream(configFile);
        }
    }
}
