package com.GenStack.payload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.GenStack.kvhandler.KVObjectHandler;
import com.GenStack.kvhandler.KVSubjectHandler;
import com.GenStack.kvhandler.KVObject;
import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVObjectField;
import com.GenStack.payload.Payload;
import com.GenStack.payload.PayloadCommand;
import com.GenStack.payload.CommandHandler;
import com.GenStack.payload.ValueComparator;
import com.GenStack.payload.CommandManager;
import com.GenStack.helper.DebugUtil;
import com.GenStack.helper.JSONHelper;
import com.GenStack.helper.ResponseHelper;
import com.GenStack.helper.StringParserHelper;
import com.GenStack.helper.TokenizedString;

public class PayloadHandler extends CommandHandler{
    private static final String RESPONSE_DEFAULT_VERSION = "1.0"; // Default version
    private ObjectMapper objectMapper;
    private KVObjectHandler kvObjectHandler;
    private KVSubjectHandler kvSubjectHandler;
    private CommandManager commandManager;

    public PayloadHandler(KVObjectHandler kvObjectHandler, KVSubjectHandler kvSubjectHandler, CommandManager commandManager) {
        super();
        //DebugUtil.debug();

        this.kvObjectHandler = kvObjectHandler;
        this.kvSubjectHandler = kvSubjectHandler;
        this.commandManager = commandManager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Check if a command is valid
    public boolean isValidCommand(String namespace, String storage, String subjectId, String commandId) {
        //return commandMap.containsKey(commandId);
        return kvSubjectHandler.getKVSubject(namespace, storage, subjectId) != null;
    }

    private String[] extractParts(String input, String separator) {
        // Return null parts if input is null or empty
        if (input == null || input.isEmpty()) {
            return new String[]{null, null};
        }

        String[] parts = input.split(Pattern.quote(separator));
        String lastPart = parts[parts.length - 1];
        String firstParts = (parts.length > 1) ? String.join(separator, Arrays.copyOf(parts, parts.length - 1)) : null;

        return new String[]{firstParts, lastPart};
    }

    public JSONObject parsePayload(String jsonPayload) {
        String storage = null;
        try {
            Payload payload = objectMapper.readValue(jsonPayload, Payload.class);

            String identifier = payload.getIdentifier();
            String[] extractedNamespace = extractParts(identifier,"@"); // Assuming payload has an Id
            String namespace = extractedNamespace[1];
            String subjectId = extractedNamespace[0];
            if (namespace != null){
                for (PayloadCommand command : payload.getCommands()) {
                    if (command.getArgs().isEmpty()){
                        command.setArgsFromJson(commandManager.getCommandArgs(namespace,command.getId()));
                    }
                }
                if (subjectId != null && 
                    refineCommands(namespace, storage, subjectId, payload.getCommands())) {
                    return executeCommands(namespace, storage, payload); // Return the output of executeCommands
                }
                return ResponseHelper.createResponse("Invalid payload: Missing identifier.", null);
            }
            return ResponseHelper.createResponse("Invalid command or Refinement of commands failed; not executing ("+subjectId+")..", null); // Command not found
        } catch (Exception e) {
            return ResponseHelper.createResponse("Error processing payload: " + e.getMessage(), null);
        }
    }

   private boolean checkResponseMessage(JSONObject item, String key) {
        if (item.has(key)) {
            String responseMessage = item.getString(key);

            // Check if the message contains "Successfully"
            if (responseMessage.contains("Successfully")) {
                return true; // Return true if it's successful
            } else {
                System.out.println("Error: " + responseMessage); // Print error message
                return false;
            }
        } else {
            System.out.println("Error: Item has no '" + key + "' key.");
            return false; // Return false if the key does not exist
        }
    }

    private boolean checkResponse(JSONObject jsonResponse) {
        boolean allSuccess = true; // Variable to track success

        // Check if 'responses' key exists
        if (jsonResponse.has("responses")) {
            JSONArray responses = jsonResponse.getJSONArray("responses");

            // Loop through each item in the responses array
            for (int i = 0; i < responses.length(); i++) {
                JSONObject responseItem = responses.getJSONObject(i);
                allSuccess &= checkResponseMessage(responseItem, "message"); // Call the helper method
            }
            return allSuccess; // Return true if all messages indicate success
        } else {
            // If 'responses' key does not exist, check for a direct "message"
            allSuccess &= checkResponseMessage(jsonResponse, "message"); // Call the helper method
            return allSuccess;
        }
    }

    private String refineValueFromChaine(String namespace, String fieldChaineStr, String typeChaineStr, String defaultSubject, String currentValue){
        //DebugUtil.debug(fieldChaineStr,typeChaineStr, defaultSubject, currentValue);
        //"field" : "eventid@id:event.title",
        //"type": "int@str",
        TokenizedString tokenizedfieldChaine = new TokenizedString(fieldChaineStr,"@");
        int currentFieldChaineCount = tokenizedfieldChaine.getCount();
        if ( currentFieldChaineCount < 2 ) {
            return currentValue;
        }

        String newCurrentValue = null;

        // get last token
        String fieldChaineLastItem = tokenizedfieldChaine.getPart(currentFieldChaineCount - 1);

        // section tokens [<subject>.]<field>
        // Assume it doesn't have a specified subject (and then update it if it does)
        // The last item containes only the field name
        String currentFieldName = fieldChaineLastItem;
        String currentSubject = defaultSubject;
        String currentIdentifier = currentSubject;
        String currentReturnFieldName = "id";

        TokenizedString currentTokenizedField = new TokenizedString(fieldChaineLastItem);
        if (currentTokenizedField.getCount() > 1){
            currentFieldName = currentTokenizedField.getPart(1);
            //Assume it doesn't have a specified field name (and then update it if it does)
            currentSubject = currentTokenizedField.getPart(0);
            TokenizedString currentTokenizedSubject = new TokenizedString(currentSubject, ":");
            if (currentTokenizedSubject.getCount() > 1){
                currentSubject = currentTokenizedSubject.getPart(1);            
                currentReturnFieldName = currentTokenizedSubject.getPart(0);
            }
            currentIdentifier = currentSubject+"@"+namespace;
        }

        TokenizedString tokenizedTypeChaine = new TokenizedString(typeChaineStr,"@");
        // Look for a type in the same level of 'currentFieldChaineCount', if no type in the same level found, 'str' is used
        String currentType = tokenizedTypeChaine.getPart(currentFieldChaineCount - 1, "str");

        JSONObject getPayloadTemplate = JSONHelper.loadJsonFromFile("templates/payloads/simpleget.json");

        // Using custom replacements
        Map<String, String> customReplacements = new HashMap<>();
        customReplacements.put("%IDENTIFIER%",currentIdentifier != null ? currentIdentifier : "");
        customReplacements.put("%FIELD%",currentFieldName != null ? currentFieldName : "");
        customReplacements.put("%TYPE%",currentType != null ? currentType : "");
        customReplacements.put("%VALUE%",currentValue != null ? currentValue : "");
        customReplacements.put("%SUBJECT%",currentSubject != null ? currentSubject : "");
        customReplacements.put("%COMPAREMODE%","=");
        String getPayloadStr = StringParserHelper.parseString(getPayloadTemplate.toString(), customReplacements);
        
        JSONObject jsonResponse = null;
        jsonResponse = parsePayload(getPayloadStr);
        // Check if message contains "Successfully retrieved"
        if (!checkResponse(jsonResponse)){
            return null;
        }

        //System.out.println(jsonResponse.toString());
        if (jsonResponse != null ){
            try {
                JSONArray responsesArray = jsonResponse.getJSONArray("responses");
                // Check if the responses array is not empty
                if (responsesArray.length() > 0) {
                    JSONObject firstResponse = responsesArray.getJSONObject(0);
                    String message = firstResponse.getString("message");

                    // Check if message contains "Successfully retrieved"
                    if (message.contains("Successfully retrieved")) {
                        JSONObject dataObject = firstResponse.getJSONObject("data");
                        JSONObject dataDataObject = dataObject.getJSONObject("data");

                        try {
                            newCurrentValue = dataDataObject.get(currentReturnFieldName).toString();
                            if ( newCurrentValue == null || newCurrentValue.isEmpty()){
                                System.out.println("No " +
                                    "('" + currentValue + "'@'" + currentSubject + "') found.");
                            }
                        } catch (JSONException e) {
                            // Handle the exception if the key does not exist
                            System.out.println("Error: " +
                                "('" + fieldChaineStr + "','" + typeChaineStr + "')." +
                                e.getMessage());
                        }

                        if ( currentFieldChaineCount > 2 ){
                            return refineValueFromChaine(namespace,tokenizedfieldChaine.removePart(currentFieldChaineCount - 1), 
                                tokenizedTypeChaine.removePart(currentFieldChaineCount - 1),
                                defaultSubject, newCurrentValue);
                        }
                    }
                } else {
                    System.out.println("Error: Responses array is empty" +
                        "('"+fieldChaineStr+"','"+typeChaineStr+"'').");
                }
            } catch (JSONException e) {
                System.out.println("Error: " +
                    "('" + fieldChaineStr + "','" + typeChaineStr + "')." +
                    e.getMessage());
            }
        }
        return newCurrentValue;
    }

    private boolean refineCommands(String namespace, String storage, String identifier, List<PayloadCommand> commands) {
        boolean success = true;  // Assume success initially
        for (PayloadCommand command : commands) {
            String subjectId = identifier;

            String[] extractedCommand = extractParts(subjectId,"."); // Assuming payload has an Id
            String commandId = extractedCommand[1];
            if (extractedCommand[0] != null){
                subjectId = extractedCommand[0];
            }

            String defaultValueType = extractParts(command.getId(),".")[1];

            if (commandId != null && isValidCommand(namespace, storage, subjectId, commandId)) {
                Map<String, PayloadDetail> args = command.getArgs();
                Map<String, Object> data = command.getData();

                KVSubject subject = kvSubjectHandler.getKVSubject(namespace, storage, subjectId);
                if (subject == null) {
                    System.out.println("Error: Subject not found for identifier " + subjectId);
                    success = false;  // Set success to false if subject not found
                    continue; // Skip to the next command
                }

                if (args != null) {
                    for (Map.Entry<String, PayloadDetail> entry : args.entrySet()) {
                        String fieldName = entry.getKey();
                        PayloadDetail fieldDetail = entry.getValue();

                        String value = null;

                        if ("auto".equals(fieldDetail.getModifier())) {
                            value = String.valueOf(subject.getNextId());
                            //System.out.println("auto value " + value);
                        } else {
                            String fieldStr = fieldDetail.getField();
                            String typeStr = fieldDetail.getType();
                            if ( fieldDetail.getReferencedFieldByKey("link") != null && !fieldDetail.getReferencedFieldByKey("link").equals("")){
                                fieldStr = fieldDetail.getField() + "@" + fieldDetail.getReferencedFieldByKey("link");
                                //fieldStr = fieldDetail.getReferencedFieldByKey("link");
                                typeStr = commandManager.getFieldProperty(namespace, identifier+"@"+namespace,fieldDetail.getField(),"type").toString();
                                typeStr = typeStr + "@" + fieldDetail.getReferencedFieldByKey("type");
                                //typeStr = fieldDetail.getReferencedFieldByKey("type");

                                value = (String) data.get(fieldName);
                                value = refineValueFromChaine(namespace,fieldStr,typeStr,identifier,value);
                            }
                            else{
                                value = (String) data.get(fieldStr);
                            }
                            data.put(fieldName, value != null ? value : "");  // Put an empty string as a fallback

                            // Check for mandatory fields
                            if (fieldDetail.isMandatory() && (value == null || value.isEmpty())) {
                                System.out.println("Error: " + fieldName + " is a mandatory field.");
                                success = false;  // Set success to false if mandatory fields are missing
                                continue; // Skip further processing for this field
                            }

                            // Set value to default if it's still null and field is not mandatory and not auto-modified
                            if (value == null || "".equals(value)) {
                                value = fieldDetail.getDefaultValueByKey(defaultValueType);
                            }
                        }
                        // Make sure not to put a null value into the data map
                        data.put(fieldName, value != null ? value : "");  // Put an empty string as a fallback
                    }
                }
            }
        }
        return success;  // Return the overall success status
    }

    private JSONObject executeCommands(String namespace, String storage, Payload payload) {
        List<PayloadCommand> commands = payload.getCommands();
        JSONArray resultsArray = new JSONArray();  // Array to hold individual command responses

        if (commands == null || commands.isEmpty()) {
            System.out.println("No commands to execute.");
            return ResponseHelper.createResponse("No commands to execute.", null);  // Return appropriate response
        }

        for (PayloadCommand command : commands) {
            JSONObject commandResponse = executeCommand(namespace, storage, command);
            resultsArray.put(commandResponse);  // Add the response to the results array
        }

        // Combine all results into a single JSON object with a list of responses
        JSONObject combinedResponse = new JSONObject();
        combinedResponse.put("responses", resultsArray);

        return combinedResponse; // Return the combined JSON response
    }

    public JSONObject executeCommand(String namespace, String storage, PayloadCommand command) {
        // Logic to execute the command would go here
        System.out.println("Executing command payload: " + command.toString());

        if (namespace != null){
            String[] extractedCommand = extractParts(command.getId(), ".");
            String commandId = extractedCommand[1];
            String subjectId = extractedCommand[0];

            if (isValidCommand(namespace, storage, subjectId,commandId)){
                try {
                    Method method = commandMap.get(commandId);
                    if (method != null) {
                        return (JSONObject) method.invoke(this, namespace, storage, subjectId, command);
                    }
                } catch (Exception e) {
                    return ResponseHelper.createResponse("Error executing command: " + e.getMessage(), null);
                }
            }
        }
        return ResponseHelper.createResponse("Command execution failed", null);
    }

    private Map<String, String> convertObjectToMap(Object data) {
        Map<String, String> jsonFields = new HashMap<>();
        
        if (data != null) {
            // Convert the Object data to a Map using ObjectMapper
            try {
                jsonFields = objectMapper.convertValue(data, new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                System.out.println("Error converting object to map: " + e.getMessage());
            }
        }
        
        return jsonFields; // Return the map, or an empty map if data is null
    }

    private KVObject createKVObject(String namespace, String storage, String identifier, Object data) {
        // Retrieve the field type map for KVObject
        Map<String, KVObjectField> fieldTypeMap = kvSubjectHandler.getFieldTypeMapByIdentifier(namespace, storage, identifier);
        // Convert Object data to a Map if necessary
        Map<String, String> jsonFields = convertObjectToMap(data); // This method needs to be implemented

        try {
            // Instantiate and return the KVObject
            return new KVObject(namespace, identifier, fieldTypeMap, jsonFields);
        } catch (IllegalArgumentException e) {
            System.out.println("Error creating KVObject: " + e.getMessage());
            return null; // Indicate failure in KVObject creation
        }
    }

    @Override
    public JSONObject addRow(String namespace, String storage, String identifier, PayloadCommand command) {
        if (command.getData() != null) {
            try {
                // Create the KVObject from the command's data
                KVObject kvObject = createKVObject(namespace, storage, identifier, command.getData());

                if (kvObject != null) {
                    // Check if the KVObject was successfully added
                    boolean addedSuccessfully = kvObjectHandler.addKVObject(namespace, storage, kvObject);
                    
                    if (addedSuccessfully) {
                        // Use the passed identifier directly in the success response message
                        return ResponseHelper.createResponse(
                            "A new row as " + identifier + " successfully added.",
                            new JSONObject().put("id", identifier)
                        ); 
                    } else {
                        return ResponseHelper.createResponse("Failed to add a new row as " + identifier + ".", null);
                    }
                } else {
                    return ResponseHelper.createResponse(CONSTANT_ERROR_EXECUTING_COMMAND, null);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return ResponseHelper.createResponse(CONSTANT_ERROR_EXECUTING_COMMAND + ": " + e.getMessage(), null);
            }
        }
        return ResponseHelper.createResponse(CONSTANT_COMMAND_EXECUTION_FAILED, null); // Use constant for error
    }

    // Private helper method to create the validator predicate
    private Predicate<KVObject> createValidator(PayloadCommand command) {
        String defaultValueType = extractParts(command.getId(),".")[1]; // Assuming payload has an Id

        return kvObject -> {
            boolean matched = true;

            // Get the field names from the KVObject
            List<String> fieldNames = kvObject.getFieldNames();
            Map<String, Object> data = command.getData(); // Assuming command.getData() provides a Map
            Map<String, PayloadDetail> args = command.getArgs(); // Assuming command.getArgs() provides a Map

            for (String fieldName : fieldNames) {
                // Get the value from the KVObject
                String valueStr = kvObject.getFieldValue(fieldName).toString();
                String compareStr = (data != null && data.containsKey(fieldName)) 
                    ? data.get(fieldName).toString() // Use the value from data first
                    : (args.containsKey(fieldName) ? args.get(fieldName).getDefaultValueByKey(defaultValueType) : null);

                if (compareStr != null && !compareStr.isEmpty()) {
                    compareStr = StringParserHelper.parseString(compareStr);
                    String compareMode = "="; // Default comparison mode

                    // Use getFieldType to retrieve the expected type dynamically
                    String expectedType = kvObject.getFieldType(fieldName);
                    if (expectedType == null) {
                        matched = false; // If type is null, set matched to false
                        break; // No need to check further fields
                    }

                    if (args.containsKey(fieldName)) {
                        PayloadDetail fieldDetail = args.get(fieldName);
                        if (fieldDetail != null) {
                            compareMode = fieldDetail.getCompareMode(); // Retrieve compare mode from PayloadDetail
                        }
                    }

                    // Compare values using ValueComparator
                    if (!ValueComparator.validateValue(valueStr, compareStr, expectedType, compareMode)) {
                        matched = false; // If any field does not match, set to false
                        break; // No need to check further fields
                    }
                }
            }
            return matched; // Return whether all fields matched
        };
    }

    @Override
    public JSONObject removeRow(String namespace, String storage, String identifier, PayloadCommand command) {
        //DebugUtil.debug(identifier,command.toString());

        try {
            // Use the existing createValidator method to create the validator
            Predicate<KVObject> validator = createValidator(command);

            // Use the removeKVObject method with the identifier and validator
            boolean removed = kvObjectHandler.removeKVObject(namespace, storage, identifier, validator);
            
            if (removed) {
                return ResponseHelper.createResponse("Successfully removed the KVObject.", null);
            } else {
                return ResponseHelper.createResponse("KVObject not found for identifier: " + identifier, null);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseHelper.createResponse("Error removing KVObject: " + e.getMessage(), null);
        }
    }

    @Override
    public JSONObject getRow(String namespace, String storage, String identifier, PayloadCommand command) {
        try {
            Predicate<KVObject> validator = createValidator(command);

            // Use the getKVObject method to search with the identifier and validator
            KVObject kvObject = kvObjectHandler.getKVObject(namespace, storage, identifier, validator);
            
            if (kvObject != null) {
                // Return a success response with the found KVObject's details
                return ResponseHelper.createResponse("Successfully retrieved the KVObject.", 
                    new JSONObject().put("id", kvObject.getIdentifier())
                                    .put("data", kvObject.getKVObjectData())); // Adjust the data as needed
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseHelper.createResponse("Error retrieving KVObject: " + e.getMessage(), null);
        }
        return ResponseHelper.createResponse("KVObject not found for identifier: " + identifier, null);
    }

    @Override
    public JSONObject getRows(String namespace, String storage, String identifier, PayloadCommand command) {
        //DebugUtil.debug(identifier, command.toString());
        try {
            Predicate<KVObject> validator = createValidator(command);

            // Use the getKVObjects method to search with the identifier and validator
            List<KVObject> kvObjects = kvObjectHandler.getKVObjects(namespace, storage, identifier, validator);
            
            // Create the JSONArray to hold the KVObject details
            JSONArray jsonArray = new JSONArray();

            if (!kvObjects.isEmpty()) {
                for (KVObject kvObject : kvObjects) {
                    jsonArray.put(new JSONObject()
                        .put("id", kvObject.getIdentifier())
                        .put("data", kvObject.getKVObjectData())); // Adjust the data as needed
                }
                // Return a success response containing the JSONArray inside a JSONObject
                return ResponseHelper.createResponse("Successfully retrieved the KVObjects.", 
                    new JSONObject().put("results", jsonArray));
            } else {
                return ResponseHelper.createResponse("No KVObjects found for identifier: " + identifier, null);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseHelper.createResponse("Error retrieving KVObjects: " + e.getMessage(), null);
        }
    }
}

