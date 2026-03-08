package com.GenStack.storages;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVSubjectAttribute;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.kvhandler.SerializationUtil;
import com.GenStack.helper.DebugUtil;

public class FileKVSubjectStorage implements KVSubjectStorage {
    private static final String storageId = "File";
    private StorageSettings dbSettings;
    private File storageDirectory;

    public FileKVSubjectStorage(StorageSettings dbSettings) throws IOException {
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

    private File getFileForIdentifier(String identifier) {
        return new File(storageDirectory, identifier + ".sbj"); // Store each identifier in a separate JSON file
    }

    @Override
    public void addKVSubject(KVSubject kvSubject) {
        File file = getFileForIdentifier(dbSettings.getNamespace());
        String identifier = kvSubject.getIdentifier();
        
        // Check if identifier already exists
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                if (jsonObject.getString("identifier").equals(identifier)) {
                    System.out.println("Subject with identifier already exists: " + identifier);
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("No subject file found/Create a new on namespace '" + dbSettings.getNamespace() + "'("+file.getAbsolutePath()+"): " + identifier);
            //e.printStackTrace();
        }

        // Create new subject JSON
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("identifier", identifier);
        jsonObject.put("subjectAttribute", SerializationUtil.serialize(kvSubject.getAttribute()));
        jsonObject.put("fieldTypeMap", SerializationUtil.serializeMap(kvSubject.getFieldTypeMap()));

        // Append only the new subject to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(jsonObject.toString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateKVSubject(KVSubject kvSubject) {
        File file = getFileForIdentifier(dbSettings.getNamespace());
        String identifier = kvSubject.getIdentifier();
        List<String> lines = new ArrayList<>();
        boolean idFound = false;

        // Read all lines and update the matching identifier
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                if (jsonObject.getString("identifier").equals(identifier)) {
                    // Create updated subject JSON
                    JSONObject updatedJson = new JSONObject();
                    updatedJson.put("identifier", identifier);
                    updatedJson.put("subjectAttribute", SerializationUtil.serialize(kvSubject.getAttribute()));
                    updatedJson.put("fieldTypeMap", SerializationUtil.serializeMap(kvSubject.getFieldTypeMap()));
                    lines.add(updatedJson.toString());
                    idFound = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write back to file only if identifier found
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
            System.out.println("Subject with identifier not found: " + identifier);
        }
    }

    @Override
    public boolean removeKVSubject(KVSubject kvSubject) {
        File file = getFileForIdentifier(dbSettings.getNamespace());
        String identifier = kvSubject.getIdentifier();
        List<String> lines = new ArrayList<>();
        boolean removed = false;

        // Read all lines and skip the matching identifier
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                if (jsonObject.getString("identifier").equals(identifier)) {
                    removed = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write back to file only if subject was removed
        if (removed) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return removed;
    }

    @Override
    public KVSubject getKVSubject(String identifier) {
        File file = getFileForIdentifier(dbSettings.getNamespace());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                if (jsonObject.getString("identifier").equals(identifier)) {
                    String subjectAttributeJson = jsonObject.getString("subjectAttribute");
                    String fieldTypeMapJson = jsonObject.getString("fieldTypeMap");
                    KVSubject kvSubject = new KVSubject(SerializationUtil.deserialize(subjectAttributeJson, KVSubjectAttribute.class));

                    if (fieldTypeMapJson != null) {
                        kvSubject.setFieldTypeMap(SerializationUtil.deserializeMap(fieldTypeMapJson, KVObjectField.class));
                    }

                    return kvSubject;
                }
            }
        } catch (IOException e) {
            System.out.println("No subject file found for namespace '" + dbSettings.getNamespace() + "' : " + identifier);
            //e.printStackTrace();
        }
        return null; // Not found
    }

    @Override
    public List<KVSubject> getAllKVSubjects() {
        File file = getFileForIdentifier(dbSettings.getNamespace());
        List<KVSubject> subjects = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JSONObject jsonObject = new JSONObject(line);
                String subjectAttributeJson = jsonObject.getString("subjectAttribute");
                String fieldTypeMapJson = jsonObject.getString("fieldTypeMap");
                KVSubject kvSubject = new KVSubject(SerializationUtil.deserialize(subjectAttributeJson, KVSubjectAttribute.class));
                
                if (fieldTypeMapJson != null) {
                    kvSubject.setFieldTypeMap(SerializationUtil.deserializeMap(fieldTypeMapJson, KVObjectField.class));
                }
                subjects.add(kvSubject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return subjects.isEmpty() ? null : subjects;
    }

    @Override
    public int countKVSubjects() {
        File file = getFileForIdentifier(dbSettings.getNamespace());
        if (!file.exists() || file.length() == 0) {
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
