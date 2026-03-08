package com.GenStack.storages;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.helper.DebugUtil;
import com.GenStack.storages.StorageSettings;

public class MemoryKVSubjectStorage implements KVSubjectStorage {
    private static final String storageId = "Memory";
    private Map<String, KVSubject> kvSubjectsMap;

    public MemoryKVSubjectStorage(StorageSettings dbSettings) {
        this.kvSubjectsMap =  new HashMap<>();
    }

    @Override
    public void addKVSubject(KVSubject kvSubject) {
        kvSubjectsMap.put(kvSubject.getIdentifier(), kvSubject); // Store with identifier as key
    }

    @Override
    public void updateKVSubject(KVSubject kvSubject) {
        kvSubjectsMap.put(kvSubject.getIdentifier(), kvSubject); // Update or insert with identifier as key
    }

    @Override
    public boolean removeKVSubject(KVSubject kvSubject) {
        return kvSubjectsMap.remove(kvSubject.getIdentifier()) != null; // Use identifier for removal
    }

    @Override
    public KVSubject getKVSubject(String identifier) {
        return kvSubjectsMap.get(identifier); // Get by identifier
    }

    @Override
    public List<KVSubject> getAllKVSubjects() {
        return new ArrayList<>(kvSubjectsMap.values()); // Return all values
    }

    @Override
    public int countKVSubjects() {
        return kvSubjectsMap.size(); // Return the number of subjects stored in memory
    }

    @Override
    public void close() {
    }

    /**
     * Converts the storage to a JSON object
     */
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        
        for (Map.Entry<String, KVSubject> entry : kvSubjectsMap.entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue().toJSON());
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