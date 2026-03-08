package com.GenStack.storages;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.kvhandler.KVObject;
import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.helper.DebugUtil;
import com.GenStack.storages.StorageSettings;
import com.GenStack.storages.FileKVSubjectStorage;

public class FileKVObjectStorage implements KVObjectStorage {
    private static final String storageId = "File";
    private StorageSettings dbSettings;
    private File storageDirectory;

    public FileKVObjectStorage(StorageSettings dbSettings) throws IOException {
        this.dbSettings = dbSettings;

        this.storageDirectory = new File(
            dbSettings.getstorageFolder(), 
            dbSettings.getNamespace()
        );

        if (!storageDirectory.exists()) {
            boolean created = storageDirectory.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + storageDirectory.getAbsolutePath());
            }
        }
    }

    private boolean ensureFileAndDirectoryExists(File file) {
        // Ensure the directory exists
        File directory = file.getParentFile();
        if (directory != null && !directory.exists()) {
            boolean dirCreated = directory.mkdirs(); // Create the directory, including any necessary parent directories
            if (!dirCreated) {
                System.err.println("Failed to create directory: " + directory.getAbsolutePath());
                return false; // Return false if the directory could not be created
            }
        }

        // Ensure the file exists
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                return created; // Return true if the file was created, false otherwise
            } catch (IOException e) {
                e.printStackTrace();
                return false; // Return false if an exception occurred
            }
        }
        return true; // Both directory and file already exist
    }

    private File getFileForIdentifier(String identifier) {
        return new File(storageDirectory, identifier + ".obj"); // Store each identifier in a separate JSON file
    }
    
    @Override
    public void addKVObject(KVObject kvObject) {
        File file = getFileForIdentifier(kvObject.getIdentifier());

        JSONObject jsonObject = new JSONObject();
        kvObject.getFieldTypeMap().forEach((fieldName, fieldType) -> {
            jsonObject.put(fieldName, kvObject.getFieldValue(fieldName).toString());
        });

        if (!ensureFileAndDirectoryExists(file)) {
            System.err.println("Error (" + storageId + "): Could not create file: " + file.getAbsolutePath());
            return;
        }

        // Check if id already exists
        boolean idExists = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentId = jsonObject.getString("id");
            while ((line = reader.readLine()) != null) {
                if (new JSONObject(line).getString("id").equals(currentId)) {
                    idExists = true;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write only if id doesn't exist
        if (!idExists) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(jsonObject.toString());
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Duplicate id: " + jsonObject.getString("id"));
        }
    }

    @Override
    public void updateKVObject(KVObject kvObject) {
        File file = getFileForIdentifier(kvObject.getIdentifier());

        JSONObject jsonObject = new JSONObject();
        kvObject.getFieldTypeMap().forEach((fieldName, fieldType) -> {
            jsonObject.put(fieldName, kvObject.getFieldValue(fieldName).toString());
        });

        if (!ensureFileAndDirectoryExists(file)) {
            System.err.println("Error (" + storageId + "): Could not create file: " + file.getAbsolutePath());
            return;
        }

        // Read all lines and update the matching id
        List<String> lines = new ArrayList<>();
        boolean idFound = false;
        String currentId = jsonObject.getString("id");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject existingJson = new JSONObject(line);
                if (existingJson.getString("id").equals(currentId)) {
                    lines.add(jsonObject.toString());
                    idFound = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write back to file
        if (idFound) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Id not found: " + currentId);
        }
    }

    @Override
    public boolean removeKVObject(KVObject kvObject) {
        File file = getFileForIdentifier(kvObject.getIdentifier());
        List<KVObject> kvObjects = getKVObjects(kvObject.getIdentifier());
        boolean removed = kvObjects.removeIf(existingKVObject -> existingKVObject.getIdentifier().equals(kvObject.getIdentifier()));

        // If something was removed, rewrite the file
        if (removed) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (KVObject obj : kvObjects) {
                    writer.write(obj.toString());
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return removed;
    }

    @Override
    public List<KVObject> getKVObjects(String identifier) {
        // Get the MultiNamespaceStorageManager instance
        MultiNamespaceStorageManager manager = MultiNamespaceStorageManager.getInstance(null);

        // Get the namespace and storage type from dbSettings
        String namespace = dbSettings.getNamespace();
        String storageType = dbSettings.getStorageType();

        // Get the subject storage from the manager
        KVSubjectStorage subjectStorage;
        try {
            subjectStorage = manager.getSubjectStorage(namespace, storageType);
            
            if (!(subjectStorage instanceof FileKVSubjectStorage)) {
                throw new IllegalArgumentException(
                    "Subject storage '" + storageType + "' in namespace '" + namespace + 
                    "' is not a FileDBKVSubjectStorage instance"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Get the KVSubject associated with the identifier
        KVSubject kvSubject = subjectStorage.getKVSubject(identifier);
        if (kvSubject == null) {
            System.out.println("Error (" + storageId + "): No KVSubject found for identifier " + identifier + 
                             ". Cannot retrieve KVObjects.");
            return new ArrayList<>();
        }

        // Get the field type map from the KVSubject
        Map<String, KVObjectField> fieldTypeMap = kvSubject.getFieldTypeMap();
        if (fieldTypeMap == null || fieldTypeMap.isEmpty()) {
            System.out.println("Error (" + storageId + "): FieldTypeMap is empty for identifier " + identifier + 
                             ". Cannot retrieve KVObjects.");
            return new ArrayList<>();
        }

        File file = getFileForIdentifier(identifier);

        // Ensure the file exists before proceeding
        if (!ensureFileAndDirectoryExists(file)) {
            return null;
        }

        List<KVObject> kvObjects = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                Map<String, String> jsonFields = new HashMap<>();        
                for (String fieldName : jsonObject.keySet()) {
                    jsonFields.put(fieldName, jsonObject.get(fieldName).toString());
                }
                
                KVObject kvObject = new KVObject(namespace, identifier, fieldTypeMap, jsonFields);
                kvObjects.add(kvObject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return kvObjects;
    }

    @Override
    public int countKVObjects(String identifier) {
        File file = getFileForIdentifier(identifier);

        if (!ensureFileAndDirectoryExists(file) || file.length() == 0) {
            return 0; // Return 0 if the file does not exist or is empty
        }

        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null) {
                lineCount++; // Increment the line count for each line read
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle exceptions
        }
        return lineCount; // Return the total line count
    }

    @Override
    public void close() {
    }
}