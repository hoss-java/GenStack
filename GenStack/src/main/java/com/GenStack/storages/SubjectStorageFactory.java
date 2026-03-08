package com.GenStack.storages;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.storages.MongoDBKVSubjectStorage;
import com.GenStack.storages.SqlDbKVSubjectStorage;
import com.GenStack.storages.FileKVSubjectStorage;
import com.GenStack.storages.MemoryKVSubjectStorage;
import com.GenStack.helper.DebugUtil;

public class SubjectStorageFactory {
    public static KVSubjectStorage createKVSubjectStorage(String type, StorageSettings storageSettings) {
        switch (type.toLowerCase()) {
            case "memory":
                return new MemoryKVSubjectStorage(storageSettings);
            case "file":
                try {
                    return new FileKVSubjectStorage(storageSettings);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create FileKVSubjectStorage: " + e.getMessage(), e);
                }
            case "sqldb":
                try {
                    return new SqlDbKVSubjectStorage(storageSettings); // Handle SQLException
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create DatabaseKVSubjectStorage: " + e.getMessage(), e);
                }
            case "mongodb":
                try {
                    return new MongoDBKVSubjectStorage(storageSettings);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create MongoDBKVSubjectStorage: " + e.getMessage(), e);
                }
            default:
                throw new IllegalArgumentException("Unknown storage type: " + type);
        }
    }
}

