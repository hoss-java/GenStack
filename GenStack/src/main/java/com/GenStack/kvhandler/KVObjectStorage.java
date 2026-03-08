package com.GenStack.kvhandler;

import java.util.List;

import com.GenStack.kvhandler.KVObject;
import com.GenStack.helper.DebugUtil;

public interface KVObjectStorage {
    void addKVObject(KVObject kvObject);
    void updateKVObject(KVObject kvObject);
    boolean removeKVObject(KVObject kvObject);
    List<KVObject> getKVObjects(String identifier);
    int countKVObjects(String identifier);
    void close();
}