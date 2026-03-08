package com.GenStack.storages;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.storages.MongoDBKVObjectStorage;
import com.GenStack.storages.SqlDbKVObjectStorage;
import com.GenStack.storages.FileKVObjectStorage;
import com.GenStack.storages.MemoryKVObjectStorage;

public class ObjectStorageFactory {
    public static KVObjectStorage createKVObjectStorage(String type, StorageSettings storageSettings) {
        switch (type.toLowerCase()) {
            case "memory":
                return new MemoryKVObjectStorage(storageSettings);
            case "file":
                try {
                    return new FileKVObjectStorage(storageSettings);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create FileKVObjectStorage: " + e.getMessage(), e);
                }
            case "sqldb":
                try {
                    return new SqlDbKVObjectStorage(storageSettings); // Handle SQLException
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create DatabaseKVObjectStorage: " + e.getMessage(), e);
                }
            case "mongodb":
                try {
                    return new MongoDBKVObjectStorage(storageSettings);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create MongoDBKVObjectStorage: " + e.getMessage(), e);
                }

            default:
                throw new IllegalArgumentException("Unknown storage type: " + type);
        }
    }
}
