package com.GenStack.storages;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.MongoServerException;
import org.bson.Document;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVSubjectAttribute;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.kvhandler.SerializationUtil;
import com.GenStack.config.StorageConfig;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.EncryptionUtil;
import com.GenStack.storages.StorageSettings;

public class MongoDBKVSubjectStorage implements KVSubjectStorage {
    private static final String storageId = "MongoDB";

    private StorageSettings dbSettings;

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBKVSubjectStorage(StorageSettings dbSettings) {
        this.dbSettings = dbSettings;
        printMongoDBConnectionDetails();

        try {
            String decryptedPassword = getDecryptedPassword(dbSettings);

            MongoCredential credential = MongoCredential.createCredential(
                dbSettings.get("username"), 
                dbSettings.get("database"), 
                decryptedPassword.toCharArray()
            );

            String connectionString = String.format(
                "mongodb://%s:%s@%s:%d/?authSource=%s",
                credential.getUserName(),
                new String(credential.getPassword()),
                dbSettings.get("address"),
                Integer.parseInt(dbSettings.get("port")),
                dbSettings.get("database")
            );

            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(dbSettings.get("database"));

            if (!verifyMongoDBConnection()) {
                System.err.println("Error: Connection error");
            }
            
            if (!verifyCollectionCreationPermission()) {
                System.err.println("Error: Permission denied");
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid connection parameters");
        } catch (MongoException e) {
            System.err.println("Error: Authentication failed");
        } catch (Exception e) {
            System.err.println("Error: Connection failed - " + e.getMessage());
            //e.printStackTrace();
        }
    }

    private boolean verifyMongoDBConnection() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyCollectionCreationPermission() {
        try {

            String username = dbSettings.get("username");

            Document command = new Document("usersInfo", username);
            Document result = database.runCommand(command);
            
            @SuppressWarnings("unchecked")
            List<Document> users = (List<Document>) result.get("users");
            
            if (users == null || users.isEmpty()) {
                return false;
            }

            Document userInfo = users.get(0);
            @SuppressWarnings("unchecked")
            List<Document> roles = (List<Document>) userInfo.get("roles");

            boolean hasReadWrite = roles.stream()
                .anyMatch(role -> role.getString("role").equals("readWrite"));

            return hasReadWrite;
        } catch (Exception e) {
            return false;
        }
    }

    private String getDecryptedPassword(StorageSettings dbSettings) {
        String decryptedPassword = "";
        try {
            decryptedPassword = createDecryptedPassword();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedPassword;
    }

    private SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    // Decrypt the stored encrypted password
    public String createDecryptedPassword() throws Exception {
        String keyString = dbSettings.get("secretKey");
        SecretKey dbSecretKey = stringToKey(keyString);
        String dbEncryptedPassword = dbSettings.get("password");
        if (dbEncryptedPassword == null || dbSecretKey == null) {

            return null;
        }
        return EncryptionUtil.decrypt(dbEncryptedPassword, dbSecretKey);
    }

    private void printMongoDBConnectionDetails() {
        // Print the connection details directly
        System.out.println("MongoDB Connection Details:");
        System.out.println(" MongoDB Address: " + dbSettings.get("address"));
        System.out.println(" MongoDB Port: " + dbSettings.get("port"));
        System.out.println(" Database Name: " + dbSettings.get("database")); // e.g., "myDatabase"
        System.out.println(" Username: " + dbSettings.get("username")); // e.g., "yourUsername"

        // Check if the MongoDB driver class can be loaded
        try {
            Class.forName("com.mongodb.client.MongoClient");
            System.out.println("MongoDB Java Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.out.println("MongoDB Java Driver not found.");
        }

        // Log the current thread's context class loader
        //System.out.println("Current ClassLoader: " + Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void addKVSubject(KVSubject kvSubject) {
        try {
            MongoCollection<Document> collection = database.getCollection("KVSubjects");

            Document doc = new Document("identifier", kvSubject.getIdentifier())
                    .append("description", kvSubject.getDescription())
                    .append("subjectAttribute", SerializationUtil.serialize(kvSubject.getAttribute()))
                    .append("fieldTypeMap", SerializationUtil.serializeMap(kvSubject.getFieldTypeMap()));

            collection.insertOne(doc);
        } catch (Exception e) {
            System.err.println("Error: Failed to get addKVSubject - ");
        }
    }

    @Override
    public void updateKVSubject(KVSubject kvSubject) {
        try {
            MongoCollection<Document> collection = database.getCollection("KVSubjects");

            Document doc = new Document("subjectAttribute", SerializationUtil.serialize(kvSubject.getAttribute()))
                    .append("fieldTypeMap", SerializationUtil.serializeMap(kvSubject.getFieldTypeMap()));

            collection.updateOne(Filters.eq("identifier", kvSubject.getIdentifier()), new Document("$set", doc));
        } catch (Exception e) {
            System.err.println("Error: Failed to get updateKVSubject - ");
        }
    }

    @Override
    public boolean removeKVSubject(KVSubject kvSubject) {
        try {
            // Retrieve the database
            MongoCollection<Document> collection = database.getCollection("KVSubjects");

            // Remove the document from the collection using the identifier from attributes
            long deletedCount = collection.deleteOne(Filters.eq("identifier", kvSubject.getIdentifier())).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            System.err.println("Error: Failed to get countKVSubjects - ");
            return false;
        }
    }

    @Override
    public KVSubject getKVSubject(String identifier) {
        try {
            // Retrieve the database
            MongoCollection<Document> collection = database.getCollection("KVSubjects");

            Document doc = collection.find(Filters.eq("identifier", identifier)).first();
            if (doc != null && doc.getString("subjectAttribute") != null) {
                // Deserialize the subjectAttribute
                KVSubjectAttribute subjectAttribute = SerializationUtil.deserialize(doc.getString("subjectAttribute"), KVSubjectAttribute.class);
                
                KVSubject kvSubject = new KVSubject(subjectAttribute);

                // Deserialize the fieldTypeMap from JSON
                kvSubject.setFieldTypeMap(SerializationUtil.deserializeMap(doc.getString("fieldTypeMap"), KVObjectField.class));

                return kvSubject;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error: Failed to get KVSubject - ");
            return null;
        }
    }

    @Override
    public List<KVSubject> getAllKVSubjects() {
        List<KVSubject> subjects = new ArrayList<>();

        // Retrieve the database
        MongoCollection<Document> collection = database.getCollection("KVSubjects");

        for (Document doc : collection.find()) {
            // Deserialize the subjectAttribute
            KVSubjectAttribute subjectAttribute = SerializationUtil.deserialize(doc.getString("subjectAttribute"), KVSubjectAttribute.class);
            
            KVSubject kvSubject = new KVSubject(subjectAttribute);

            // Deserialize fieldTypeMap
            kvSubject.setFieldTypeMap(SerializationUtil.deserializeMap(doc.getString("fieldTypeMap"), KVObjectField.class));

            subjects.add(kvSubject);
        }
        return subjects;
    }

    @Override
    public int countKVSubjects() {
        try {
            // Retrieve the database
            MongoCollection<Document> collection = database.getCollection("KVSubjects"); // Name of the collection

            return (int) collection.countDocuments();
        } catch (Exception e) {
            System.err.println("Error: Failed to get countKVSubjects - ");
            return 0;
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDBKVSubjectStorage connection closed.");
        }
    }

}
