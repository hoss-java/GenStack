package com.GenStack.storages;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.kvhandler.KVObject;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.helper.DebugUtil;
import com.GenStack.storages.StorageSettings;

public class MemoryKVObjectStorage implements KVObjectStorage {
    private static final String storageId = "Memory";
    private Map<String, List<KVObject>> kvObjectMap;

    public MemoryKVObjectStorage(StorageSettings dbSettings) {
        this.kvObjectMap = new HashMap<>();
    }

    @Override
    public void addKVObject(KVObject kvObject) {
        String identifier = kvObject.getIdentifier();
        kvObjectMap.computeIfAbsent(identifier, k -> new ArrayList<>()).add(kvObject);
    }

    @Override
    public void updateKVObject(KVObject kvObject) {
        String identifier = kvObject.getIdentifier();
        List<KVObject> objects = kvObjectMap.get(identifier);
        
        if (objects != null) {
            for (int i = 0; i < objects.size(); i++) {
                if (objects.get(i).getFieldValue("id").equals(kvObject.getFieldValue("id"))) {
                    objects.set(i, kvObject);
                    return;
                }
            }
        }
    }

    @Override
    public boolean removeKVObject(KVObject kvObject) {
        String identifier = kvObject.getIdentifier();
        List<KVObject> objects = kvObjectMap.get(identifier);
        if (objects != null) {
            return objects.remove(kvObject); // Remove from the specific list
        }
        return false; // Return false if the list is not found
    }

    @Override
    public List<KVObject> getKVObjects(String identifier) {
        return new ArrayList<>(kvObjectMap.getOrDefault(identifier, new ArrayList<>())); // Return objects for the identifier
    }

    @Override
    public int countKVObjects(String identifier) {
        return kvObjectMap.getOrDefault(identifier, new ArrayList<>()).size(); // Count objects for the identifier
    }

    @Override
    public void close() {
    }


    /**
     * Converts the storage to a JSON object
     */
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        
        for (Map.Entry<String, List<KVObject>> entry : kvObjectMap.entrySet()) {
            JSONArray jsonArray = new JSONArray();
            
            for (KVObject kvObject : entry.getValue()) {
                jsonArray.put(kvObject.toJSON());
            }
            
            jsonObject.put(entry.getKey(), jsonArray);
        }
        
        return jsonObject;
    }

    /**
     * Returns a string representation of the storage using JSON format
     */
    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int... indentation) {
        int indent = indentation.length > 0 ? indentation[0] : 0;
        return toJSON().toString(indent);
    }
}

