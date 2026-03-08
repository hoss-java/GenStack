package com.GenStack.storages;

import java.sql.Driver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.kvhandler.KVSubjectAttribute;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.kvhandler.SerializationUtil;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.EncryptionUtil;
import com.GenStack.config.StorageConfig;
import com.GenStack.storages.StorageSettings;

public class SqlDbKVSubjectStorage implements KVSubjectStorage {
    private static final String storageId = "sqldb";
    private StorageSettings dbSettings; // Add this field
    // JDBC connection
    private Connection connection;

    public SqlDbKVSubjectStorage(StorageSettings dbSettings) throws SQLException {
        this.dbSettings = dbSettings;
        printDefaultConnectionDetails();

        try {
            String decryptedPassword = getDecryptedPassword(dbSettings);

            // Establish a connection to the database
            this.connection = DriverManager.getConnection(
                dbSettings.get("url") + "/" + dbSettings.get("database"), 
                dbSettings.get("username"), 
                decryptedPassword
            );

            if (!verifyDatabaseConnection()) {
                System.err.println("Error: Connection error");
                return;
            }
            
            //if (!verifyTableCreationPermission()) {
            //    System.err.println("Error: Permission denied");
            //    return;
            //}

            // Create the table for KVSubjects if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS KVSubjects (" +
                    "identifier VARCHAR(255) PRIMARY KEY, " +
                    "subjectAttribute TEXT, " +
                    "fieldTypeMap TEXT)";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }

        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            //throw e;
        }
    }

    private boolean verifyDatabaseConnection() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            metaData.getDatabaseProductName();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean verifyTableCreationPermission() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String username = dbSettings.get("username");
            
            // Check if user has CREATE permission
            ResultSet privileges = metaData.getTablePrivileges(null, null, "%");
            
            while (privileges.next()) {
                String grantee = privileges.getString("GRANTEE");
                String privilege = privileges.getString("PRIVILEGE");
                
                if (grantee.contains(username) && privilege.equals("CREATE")) {
                    return true;
                }
            }
            privileges.close();
            
            return false;
        } catch (SQLException e) {
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

    private boolean tableExists(String tableName) {
        boolean exists = false; // Variable to store if the table exists
        String dbType = getDatabaseType(); // Implement this method to return the DB type
        String checkTableSQL;

        try {
            if ("SQLSERVER".equalsIgnoreCase(dbType)) {
                checkTableSQL = "SELECT COUNT(*) FROM information_schema.tables "
                              + "WHERE table_schema = ? AND table_name = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(checkTableSQL)) {
                    pstmt.setString(1,dbSettings.get("database"));
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

    private void createTableFromFieldTypeMap(KVSubject kvSubject) {
        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE " + kvSubject.getIdentifier() + " (");
        
        // Iterate over the fieldTypeMap to create columns
        for (KVObjectField field : kvSubject.getFieldTypeMap().values()) {
            createTableSQL.append(field.getField()).append(" ").append(field.getSqlType()); // Use new getSqlType()
            if (field.isMandatory()) {
                createTableSQL.append(" NOT NULL"); // Set mandatory constraint
            }
            createTableSQL.append(", "); // Commas between columns
        }
        
        // Remove the last comma and space
        createTableSQL.setLength(createTableSQL.length() - 2);
        createTableSQL.append(")");

        // Execute the create table SQL
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL.toString());
        } catch (SQLException e) {
            //System.err.println("Error: Failed to get createTableFromFieldTypeMap - ");
            //e.printStackTrace(); // Handle exceptions
        }
    }

    @Override 
    public void addKVSubject(KVSubject kvSubject) {
        if (tableExists(kvSubject.getIdentifier())) {
            System.out.println("Error: A table with the name " + kvSubject.getIdentifier() + " already exists. Please delete the table to proceed.");
            return;
        }

        String insertSQL = "INSERT INTO KVSubjects (identifier, subjectAttribute, fieldTypeMap) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, kvSubject.getIdentifier());
            pstmt.setString(2, SerializationUtil.serialize(kvSubject.getAttribute()));
            pstmt.setString(3, SerializationUtil.serializeMap(kvSubject.getFieldTypeMap()));
            pstmt.executeUpdate();
            
            createTableFromFieldTypeMap(kvSubject);
        } catch (SQLException e) {
            System.err.println("Error: Failed to get addKVSubject - ");
            //e.printStackTrace(); // Handle exceptions
        }
    }

    @Override
    public void updateKVSubject(KVSubject kvSubject) {
        if (!tableExists("KVSubjects")) {
            System.out.println("Error: KVSubjects table does not exist. Cannot update KVSubject.");
            return;
        }

        String updateSQL = "UPDATE KVSubjects SET subjectAttribute = ?, fieldTypeMap = ? WHERE identifier = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, SerializationUtil.serialize(kvSubject.getAttribute()));
            pstmt.setString(2, SerializationUtil.serializeMap(kvSubject.getFieldTypeMap()));
            pstmt.setString(3, kvSubject.getIdentifier());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error: Failed to get updateKVSubject - ");
            //e.printStackTrace(); // Handle exceptions
        }
    }

    private String getCurrentDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return LocalDateTime.now().format(formatter); // Format date and time
    }

    @Override
    public boolean removeKVSubject(KVSubject kvSubject) {
        // Check if the table with the same name exists
        if (tableExists(kvSubject.getIdentifier())) {
            // Prepare the new table name with date and time
            String newTableName = "removed_" + kvSubject.getIdentifier() + "_" + getCurrentDateTime();
            
            // Rename the table
            String renameTableSQL = "ALTER TABLE " + kvSubject.getIdentifier() + " RENAME TO " + newTableName;
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(renameTableSQL);
                System.out.println("Table " + kvSubject.getIdentifier() + " renamed to " + newTableName);
            } catch (SQLException e) {
                e.printStackTrace(); // Handle exceptions
                return false; // Return false if there's an error
            }
        } 

        // Now, remove the subject from KVSubjects
        String deleteSQL = "DELETE FROM KVSubjects WHERE identifier = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, kvSubject.getIdentifier());
            return pstmt.executeUpdate() > 0; // Return true if a row was deleted
        } catch (SQLException e) {
            System.err.println("Error: Failed to get removeKVSubject - ");
            //e.printStackTrace(); // Handle exceptions
            return false; // Return false in case of exception
        }
    }

    @Override
    public KVSubject getKVSubject(String identifier) {
        String selectSQL = "SELECT subjectAttribute, fieldTypeMap FROM KVSubjects WHERE identifier = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, identifier);
            try(ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String subjectAttributeJson = rs.getString("subjectAttribute");
                    String fieldTypeMapJson = rs.getString("fieldTypeMap");

                    KVSubject kvSubject = new KVSubject(SerializationUtil.deserialize(subjectAttributeJson, KVSubjectAttribute.class));

                    if (fieldTypeMapJson != null) {
                        kvSubject.setFieldTypeMap(SerializationUtil.deserializeMap(fieldTypeMapJson, KVObjectField.class));
                    }

                    return kvSubject;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Failed to get getKVSubject - ");
            //e.printStackTrace(); // Handle exceptions
        }
        return null;
    }

    @Override
    public List<KVSubject> getAllKVSubjects() {
        List<KVSubject> subjects = new ArrayList<>();
        String selectAllSQL = "SELECT identifier, subjectAttribute, fieldTypeMap FROM KVSubjects";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectAllSQL)) {
            while (rs.next()) {
                String identifier = rs.getString("identifier");
                String subjectAttributeJson = rs.getString("subjectAttribute");
                String fieldTypeMapJson = rs.getString("fieldTypeMap");

                KVSubject kvSubject = new KVSubject(SerializationUtil.deserialize(subjectAttributeJson, KVSubjectAttribute.class));

                if (fieldTypeMapJson != null) {
                    kvSubject.setFieldTypeMap(SerializationUtil.deserializeMap(fieldTypeMapJson, KVObjectField.class));
                }

                subjects.add(kvSubject);
            }
        } catch (SQLException e) {
            System.err.println("Error: Failed to get getAllKVSubjects - ");
            //e.printStackTrace(); // Handle exceptions
        }
        return subjects;
    }

    @Override
    public int countKVSubjects() {
        String countSQL = "SELECT COUNT(*) AS total FROM KVSubjects";
        try (PreparedStatement pstmt = connection.prepareStatement(countSQL);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total"); // Return the count of records
            }
        } catch (SQLException e) {
            System.err.println("Error: Failed to get countKVSubjects - ");
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
