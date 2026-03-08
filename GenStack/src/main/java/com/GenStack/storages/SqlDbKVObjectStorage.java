package com.GenStack.storages;

import java.sql.Driver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.io.PrintWriter;
import java.sql.*; // Import everything from the SQL package
import java.util.*; // Import collections and other utilities

import org.json.JSONObject;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import com.GenStack.kvhandler.KVObject;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVObjectStorage;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.StringParserHelper;
import com.GenStack.helper.EncryptionUtil;
import com.GenStack.config.StorageConfig;
import com.GenStack.storages.SqlDbKVSubjectStorage;
import com.GenStack.storages.StorageSettings;

public class SqlDbKVObjectStorage implements KVObjectStorage {
    private static final String storageId = "sqldb";
    private StorageSettings dbSettings;
    // JDBC connection
    private Connection connection;

    public SqlDbKVObjectStorage(StorageSettings dbSettings) throws SQLException {
        this.dbSettings = dbSettings; // Store the dbSettings in the instance
        printDefaultConnectionDetails();

        String decryptedPassword = getDecryptedPassword(dbSettings);

        // Establish a connection to the database
        this.connection = DriverManager.getConnection(dbSettings.get("url") + "/" + dbSettings.get("database"), dbSettings.get("username"), decryptedPassword);
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

    private void printDefaultConnectionDetails() {
        String jdbcUrl = dbSettings.get("url") + "/" + dbSettings.get("database");
        String username = dbSettings.get("username");

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            System.out.println("Registered driver: " + driver.getClass().getName());
        }

        // Print the connection details
        System.out.println("Attempting to connect to the database with the following settings:");
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("Username: " + username);
        
        try {
            // Try to load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found.");
        }

        // You can also log the current thread's context class loader
        //System.out.println("Current ClassLoader: " + Thread.currentThread().getContextClassLoader());
        DriverManager.setLogWriter(new PrintWriter(System.out));
    }

    // Method to determine the database type based on the connection
    private String getDatabaseType() {
        String url = "";
        try {
            if (connection != null) {
                url = connection.getMetaData().getURL();
            }
        } catch (SQLException e) {
            //e.printStackTrace(); // Handle exception appropriately
        }

        if (url.contains("sqlserver")) {
            return "SQLSERVER";
        } else if (url.contains("mysql")) {
            return "SQLSERVER";
        } else if (url.contains("sqlite")) {
            return "SQLITE";
        } else {
            return "UNKNOWN"; // Handle unsupported types appropriately
        }
    }

    // Check if a table with the specified identifier exists
    private boolean tableExists(String tableName) {
        boolean exists = false; // Variable to store if the table exists
        String dbType = getDatabaseType(); // Implement this method to return the DB type
        String checkTableSQL;

        try {
            if ("SQLSERVER".equalsIgnoreCase(dbType)) {
                checkTableSQL = "SELECT COUNT(*) FROM information_schema.tables "
                              + "WHERE table_schema = ? AND table_name = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(checkTableSQL)) {
                    pstmt.setString(1, dbSettings.get("database"));
                    pstmt.setString(2, tableName);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        exists = rs.getInt(1) > 0; // If count is greater than 0, the table exists
                    }
                }
            } else if ("SQLITE".equalsIgnoreCase(dbType)) {
                checkTableSQL = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?";
                try (PreparedStatement pstmt = connection.prepareStatement(checkTableSQL)) {
                    pstmt.setString(1, tableName);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        exists = rs.getInt(1) > 0; // If count is greater than 0, the table exists
                    }
                }
            } else {
                throw new UnsupportedOperationException("Unsupported database type: " + dbType);
            }
        } catch (SQLException e) {
            //System.err.println("Error: Failed to get tableExists - ");
            //e.printStackTrace(); // Handle exceptions
        }

        return exists; // Return whether the table exists
    }

    @Override
    public void addKVObject(KVObject kvObject) {
        // Check if a table with the same identifier exists
        if (!tableExists(kvObject.getIdentifier())) {
            System.out.println("Error (" + storageId + "): No table found for identifier " + kvObject.getIdentifier() + ". Cannot insert KVObject.");
            return; // Exit the method if the table does not exist
        }

        // Prepare the insert statement based on fieldTypeMap
        StringBuilder insertSQL = new StringBuilder("INSERT INTO " + kvObject.getIdentifier() + " (");
        StringBuilder valuesPlaceholder = new StringBuilder();

        // Use a list to store the field values for the prepared statement
        List<Object> values = new ArrayList<>();

        // Iterate through the fieldTypeMap to create the insert query
        for (Map.Entry<String, KVObjectField> entry : kvObject.getFieldTypeMap().entrySet()) {
            String fieldName = entry.getKey();
            KVObjectField fieldType = entry.getValue();

            // Get the value from fields
            Object fieldValue = kvObject.getFieldValue(fieldName);
            insertSQL.append(fieldName).append(", ");
            valuesPlaceholder.append("?, ");
            values.add(fieldValue);
        }

        // Remove the last comma and space
        insertSQL.setLength(insertSQL.length() - 2);
        valuesPlaceholder.setLength(valuesPlaceholder.length() - 2);
        insertSQL.append(") VALUES (").append(valuesPlaceholder).append(")");

        // Execute the insert statement
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i)); // Set each value
            }
            pstmt.executeUpdate(); // Execute the insert
        } catch (SQLException e) {
            System.err.println("Error: Failed to get addKVObject - ");
            //e.printStackTrace(); // Handle exceptions
        }
    }

    @Override
    public void updateKVObject(KVObject kvObject) {
        // Check if a table with the same identifier exists
        if (!tableExists(kvObject.getIdentifier())) {
            System.out.println("Error (" + storageId + "): No table found for identifier " + kvObject.getIdentifier() + ". Cannot update KVObject.");
            return;
        }

        // Check if id field exists
        Object idValue = kvObject.getFieldValue("id");
        if (idValue == null) {
            System.out.println("Error (" + storageId + "): No id field found. Cannot update KVObject.");
            return;
        }

        // Prepare the update statement based on fieldTypeMap
        StringBuilder updateSQL = new StringBuilder("UPDATE " + kvObject.getIdentifier() + " SET ");
        List<Object> values = new ArrayList<>();

        // Iterate through the fieldTypeMap to create the update query
        for (Map.Entry<String, KVObjectField> entry : kvObject.getFieldTypeMap().entrySet()) {
            String fieldName = entry.getKey();
            if (!fieldName.equals("id")) { // Skip the id field in SET clause
                Object fieldValue = kvObject.getFieldValue(fieldName);
                updateSQL.append(fieldName).append(" = ?, ");
                values.add(fieldValue);
            }
        }

        // Remove the last comma and space
        updateSQL.setLength(updateSQL.length() - 2);
        updateSQL.append(" WHERE id = ?");
        values.add(idValue); // Add id value for WHERE clause

        // Execute the update statement
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error: Failed to get updateKVObject - ");
            //e.printStackTrace(); // Handle exceptions
        }
    }

    @Override
    public boolean removeKVObject(KVObject kvObject) {
        String identifier = kvObject.getIdentifier(); // Get the identifier (table name)

        // Check if a table with the same identifier exists
        if (!tableExists(identifier)) {
            System.out.println("Error (" + storageId + "): No table found for identifier " + identifier + ". Cannot remove KVObject.");
            return false; // Exit if the table does not exist
        }

        // Prepare the delete statement based on the identifier
        String deleteSQL = "DELETE FROM " + identifier + " WHERE id = ?"; // Assuming you have a primary key 'id'
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setInt(1, (int) kvObject.getFieldValue("id")); // Assuming getId() returns the primary key value
            return pstmt.executeUpdate() > 0; // Return true if a row was deleted
        } catch (SQLException e) {
            System.err.println("Error: Failed to get removeKVObject - ");
            //e.printStackTrace(); // Handle exceptions
            return false; // Return false in case of exception
        }
    }

    // Helper method to retrieve values based on internal types
    private Object retrieveValueByInternalType(ResultSet rs, String fieldName, String internalType) throws SQLException {
        switch (internalType) {
            case "str":
                return rs.getString(fieldName);
            case "int":
                return rs.getInt(fieldName);
            case "unsigned":
                return rs.getInt(fieldName); // Treat as int for simplicity; handle unsigned logic as needed
            case "date":
                return rs.getDate(fieldName);
            case "time":
                return rs.getTime(fieldName);
            case "duration":
                return StringParserHelper.parseDuration(rs.getString(fieldName)); // Example: treat duration as a string
            default:
                System.out.println("Unknown internal type: " + internalType);
                return null; // Handle unknown types as needed
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
            
            if (!(subjectStorage instanceof SqlDbKVSubjectStorage)) {
                throw new IllegalArgumentException(
                    "Subject storage '" + storageType + "' in namespace '" + namespace + 
                    "' is not a SqlDbKVSubjectStorage instance"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Check if the table with the given identifier exists
        if (!tableExists(identifier)) {
            System.out.println("Error (" + storageId + "): No table found for identifier " + identifier + ". Cannot retrieve KVObjects.");
            return Collections.emptyList();
        }

        // Get the KVSubject associated with the identifier
        KVSubject kvSubject = subjectStorage.getKVSubject(identifier);
        if (kvSubject == null) {
            System.out.println("Error (" + storageId + "): No KVSubject found for identifier " + identifier + ". Cannot retrieve KVObjects.");
            return Collections.emptyList();
        }

        // Get the field type map from the KVSubject
        Map<String, KVObjectField> fieldTypeMap = kvSubject.getFieldTypeMap();
        if (fieldTypeMap == null || fieldTypeMap.isEmpty()) {
            System.out.println("Error (" + storageId + "): FieldTypeMap is empty for identifier " + identifier + ". Cannot retrieve KVObjects.");
            return Collections.emptyList();
        }

        // Prepare to read records from the specified table
        List<KVObject> kvObjects = new ArrayList<>();
        String selectSQL = "SELECT * FROM " + identifier; // Select all records from the table

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                // Create a Map to hold field data
                Map<String, String> jsonFields = new HashMap<>();

                // Populate the Map based on the FieldTypeMap
                for (Map.Entry<String, KVObjectField> entry : fieldTypeMap.entrySet()) {
                    String fieldName = entry.getKey();
                    KVObjectField fieldType = entry.getValue();

                    // Get the internal type and retrieve the value from ResultSet
                    String internalType = fieldType.getType().toLowerCase();
                    Object value = retrieveValueByInternalType(rs, fieldName, internalType);

                    // Convert value to String and put it in the Map
                    jsonFields.put(fieldName, value != null ? value.toString() : null);
                }

                // Create a new KVObject using the constructor
                KVObject kvObject = new KVObject(namespace, identifier, fieldTypeMap, jsonFields);
                kvObjects.add(kvObject); // Add the populated KVObject to the list
            }
        } catch (SQLException e) {
            System.err.println("Error: Failed to get getKVObjects - ");
            //e.printStackTrace(); // Handle exceptions
        }

        return kvObjects; // Return the list of KVObjects
    }

    @Override
    public int countKVObjects(String identifier) {
        // Check if a table with the same identifier exists
        if (!tableExists(identifier)) {
            System.out.println("Error (" + storageId + "): No table found for identifier " + identifier + ". Cannot count KVObjects.");
            return 0; // Return 0 if the table does not exist
        }

        String countSQL = "SELECT COUNT(*) AS total FROM " + identifier; // Use the identifier in the query
        try (PreparedStatement pstmt = connection.prepareStatement(countSQL);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total"); // Return the count of records
            }
        } catch (SQLException e) {
            System.err.println("Error: Failed to get countKVObjects - ");
            //e.printStackTrace(); // Handle exceptions
        }
        return 0; // Return 0 if count fails or no records found
    }

    // Close the database connection when done
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("SQL client closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle exceptions
        } finally {
            // Optional: nullify the object to free memory
            connection = null;
        }
    }

    // Getter for the connection if needed
    public Connection getConnection() {
        return connection;
    }
}

