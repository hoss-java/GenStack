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

import com.GenStack.kvhandler.KVObject;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.StringParserHelper;
import com.GenStack.helper.EncryptionUtil;
import com.GenStack.config.StorageConfig;
import com.GenStack.storages.MongoDBKVSubjectStorage;
import com.GenStack.storages.StorageSettings;

public class MongoDBKVObjectStorage implements KVObjectStorage {
    private static final String storageId = "MongoDB";
    private StorageSettings dbSettings; 

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBKVObjectStorage(StorageSettings dbSettings) {
        this.dbSettings = dbSettings; // Store the dbSettings in the instance

        String decryptedPassword = getDecryptedPassword(dbSettings);

        // Create credentials
        MongoCredential credential = MongoCredential.createCredential(dbSettings.get("username"), dbSettings.get("database"), decryptedPassword.toCharArray());

        // Create a new MongoClient with corrected formatting
        String connectionString = String.format("mongodb://%s:%s@%s:%d/?authSource=%s",
            credential.getUserName(),
            new String(credential.getPassword()),
            dbSettings.get("address"),
            Integer.parseInt(dbSettings.get("port")),
            dbSettings.get("database") // Ensure the auth source is set to your db
        );

        // Create a new MongoClient
        mongoClient = MongoClients.create(connectionString);
        database = mongoClient.getDatabase(dbSettings.get("database"));
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

    @Override
    public void addKVObject(KVObject kvObject) {
        try {
            String collectionName = kvObject.getIdentifier();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document document = new Document();
            kvObject.getFieldTypeMap().forEach((fieldName, fieldType) -> {
                document.append(fieldName, kvObject.getFieldValue(fieldName).toString());
            });

            collection.insertOne(document);
            System.out.println("KVObject added to collection: " + collectionName);
        } catch (Exception e) {
            System.err.println("Error: Failed to get addKVObject - ");
        }
    }

    @Override
    public void updateKVObject(KVObject kvObject) {
        try {
            String collectionName = kvObject.getIdentifier();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document document = new Document();
            kvObject.getFieldTypeMap().forEach((fieldName, fieldType) -> {
                document.append(fieldName, kvObject.getFieldValue(fieldName).toString());
            });

            String id = document.getString("id");
            collection.replaceOne(Filters.eq("id", id), document);
            System.out.println("KVObject updated in collection: " + collectionName);
        } catch (Exception e) {
            System.err.println("Error: Failed to get updateKVObject - ");
        }
    }

    @Override
    public boolean removeKVObject(KVObject kvObject) {
        try {
            String collectionName = kvObject.getIdentifier();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            // Create a Document object to hold the query criteria, excluding the id
            Document query = new Document();
            
            kvObject.getFieldTypeMap().forEach((fieldName, fieldType) -> {
                if (!fieldName.equals("id")) { // Exclude the id field from the query
                    query.append(fieldName, kvObject.getFieldValue(fieldName));
                }
            });

            long deletedCount = collection.deleteOne(query).getDeletedCount();
            System.out.println("Removed KVObject from collection: " + collectionName);
            return deletedCount > 0;
        } catch (Exception e) {
            System.err.println("Error: Failed to get removeKVObject - ");
            return false;
        }
    }

    private boolean collectionExists(String collectionName) {
        boolean exists = false;
        try {
            // Get the list of existing collections in the database
            for (String name : this.database.listCollectionNames()) {
                if (name.equals(collectionName)) {
                    exists = true; // Collection exists
                    break;
                }
            }

            return exists; // Return whether the collection exists
        } catch (Exception e) {
            System.err.println("Error: Failed to get collectionExists - ");
            return false;
        }
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
            
            if (!(subjectStorage instanceof MongoDBKVSubjectStorage)) {
                throw new IllegalArgumentException(
                    "Subject storage '" + storageType + "' in namespace '" + namespace + 
                    "' is not a MongoDBKVSubjectStorage instance"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        // Check if the collection with the given identifier exists
        if (!collectionExists(identifier)) {
            System.out.println("Error (" + storageId + "): No collection found for identifier " + identifier + 
                             ". Cannot retrieve KVObjects.");
            return new ArrayList<>();
        }

        // Get the KVSubject associated with the identifier
        KVSubject kvSubject = subjectStorage.getKVSubject(identifier);
        if (kvSubject == null) {
            System.out.println("Error(" + storageId + "): No KVSubject found for identifier " + identifier + 
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

        try {
            // Prepare to read records from the specified collection
            MongoCollection<Document> collection = database.getCollection(identifier);
            List<KVObject> kvObjects = new ArrayList<>();

            for (Document doc : collection.find()) {
                Map<String, String> jsonFields = new HashMap<>();
                for (String fieldName : doc.keySet()) {
                    jsonFields.put(fieldName, doc.get(fieldName).toString());
                }
                KVObject kvObject = new KVObject(namespace, identifier, fieldTypeMap, jsonFields);
                kvObjects.add(kvObject);
            }
            return kvObjects;
        } catch (Exception e) {
            System.err.println("Error: Failed to get getKVObjects - ");
            return null;
        }
    }

    @Override
    public int countKVObjects(String identifier) {
        try {
            MongoCollection<Document> collection = database.getCollection(identifier);
            long count = collection.countDocuments();
            return (int) count;
        } catch (Exception e) {
            System.err.println("Error: Failed to get getKVObjects - ");
            return 0;
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDBKVObjectStorage connection closed.");
        }
    }
}
