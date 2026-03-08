package com.GenStack.kvhandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.GenStack.callbacks.ActionCallbackInterface;
import com.GenStack.callbacks.ResponseCallbackInterface;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.storages.MultiNamespaceStorageManager;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.JSONHelper;
import com.GenStack.helper.ResponseHelper;

public class KVObjectHandler implements KVObjectHandlerInterface {
    private String DEFAULT_RESPONSE_VERSION = "1.0"; // Default version
    //private List<KVObject> kvObjects;
    private MultiNamespaceStorageManager namespaceManager; // Reference to Storages
    private List<String> uniqueEntryCheckSkippedfields = Arrays.asList("id");
    private int nextId = 0;

    public KVObjectHandler(ActionCallbackInterface callback, MultiNamespaceStorageManager namespaceManager) {
        //this.kvObjects = new ArrayList<>();
        this.namespaceManager = namespaceManager; // Initialize using injected storage
    }

    // Validator to check for duplicates in the existing collection
    private boolean isUniqueEntry(String namespace, String storage, KVObject newKVObject, List<String> fieldsToSkip) {
        // Use a validator to match the identifier and check fields, skipping those specified
        Predicate<KVObject> validator = existingObject -> {
            // Check for matching identifier
            if (!existingObject.getIdentifier().equals(newKVObject.getIdentifier())) {
                return false; // Different identifiers are not a match
            }

            // Check other fields, skipping specified ones
            for (String key : newKVObject.getFieldNames()) {
                if (fieldsToSkip.contains(key.toLowerCase())) {
                    continue; // Skip the fields specified for skipping
                }
                Object valueToCompare = existingObject.getFieldValue(key);
                Object currentValue = newKVObject.getFieldValue(key);
                if (currentValue != null && !currentValue.equals(valueToCompare)) {
                    return false; // Mismatch found
                }
            }
            return true; // All fields match
        };

        // Check if a matching object exists using the getKVObject method
        KVObject existingMatch = getKVObject(namespace , storage, newKVObject.getIdentifier(), validator);
        return existingMatch == null; // If no match, it's unique
    }

    // Add a kvObject
    public Boolean addKVObject(String namespace, String storage, KVObject kvObject) {
        if (namespaceManager.hasObjectStorage(namespace, storage) == false) {
            namespaceManager.addObjectStorage(namespace, storage);
        }
        if (isUniqueEntry(namespace, storage, kvObject, uniqueEntryCheckSkippedfields)) {
            namespaceManager.getObjectStorage(namespace, storage).addKVObject(kvObject);
            return true;
        } else {
            throw new IllegalArgumentException("A KVObject with the same non-id fields already exists.");
        }
    }

    // Remove a kvObject(s)
    public Boolean removeKVObject(String namespace, String storage, String identifier,  Predicate<KVObject> validator) {
        KVObject kvObjectToRemove = getKVObject(namespace , storage, identifier, validator);
        KVObjectStorage storageToUse = namespaceManager.getObjectStorage(namespace,storage);
        if (storageToUse != null) {
            if (kvObjectToRemove != null) {
                return storageToUse.removeKVObject(kvObjectToRemove); // Use storage's remove method
            }
        }
        return false;
    }

    // Existing method that returns a KVObject based on identifier and a validator function
    public KVObject getKVObject(String namespace, String storage, String identifier, Predicate<KVObject> validator) {
        KVObjectStorage storageToUse = namespaceManager.getObjectStorage(namespace,storage);
        if (storageToUse != null) {
            return storageToUse.getKVObjects(identifier).stream()
                .filter(kvObject -> kvObject.getIdentifier().equals(identifier) && validator.test(kvObject))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    // Existing method that returns a list of KVObjects based on identifier and a validator function
    public List<KVObject> getKVObjects(String namespace, String storage, String identifier, Predicate<KVObject> validator) {
        KVObjectStorage storageToUse = namespaceManager.getObjectStorage(namespace,storage);
        if (storageToUse != null) {
            return storageToUse.getKVObjects(identifier).stream()
                .filter(kvObject -> kvObject.getIdentifier().equals(identifier) && validator.test(kvObject))
                .collect(Collectors.toList());
        }
        return null;
    }

    // New method to get a KVObject by identifier without a validator (default to accepting all)
    public KVObject getKVObject(String namespace, String storage, String identifier) {
        return getKVObject(namespace , storage, identifier, kvObject -> true); // Default validator that accepts all
    }

    // New method to get a list of KVObjects by identifier without a validator 
    public List<KVObject> getKVObjects(String namespace, String storage, String identifier) {
        return getKVObjects(namespace , storage, identifier, kvObject -> true); // Default validator that accepts all
    }
}