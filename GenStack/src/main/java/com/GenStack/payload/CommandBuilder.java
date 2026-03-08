package com.GenStack.payload;

import org.json.JSONArray;
import org.json.JSONObject;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.util.*;

public class CommandBuilder {
    private static final String defaultCommandsJsonFile = "commands.json";
    private static final String defaultSubjectsXmlFile = "subjects.xml";
    private JSONObject commandsJson;
    private Map<String, String> actionDescriptions;
    private Map<String, String> fieldTypeCompareModes;
    private Map<String, String> actionAliases;
    
    // Configuration tag names
    private static final String CONFIG_TAG = "config";
    private static final String ACTIONS_TAG = "actions";
    private static final String ACTION_TAG = "action";
    private static final String FIELD_TYPES_TAG = "fieldTypes";
    private static final String FIELD_TYPE_TAG = "fieldType";
    private static final String SUBJECTS_TAG = "subjects";
    private static final String SUBJECT_TAG = "subject";
    private static final String FIELD_TAG = "field";
    private static final String ALIASES_TAG = "aliases";
    private static final String ALIAS_TAG = "alias";
    private static final String DEFAULTS_TAG = "defaults";
    private static final String DEFAULT_TAG = "default";
    private static final String USEDBY_TAG = "usedby";

    private String appDataFolder;
    
    public CommandBuilder(String appDataFolder) {
        this.appDataFolder = appDataFolder;
        this.actionDescriptions = new HashMap<>();
        this.fieldTypeCompareModes = new HashMap<>();
        this.actionAliases = new HashMap<>();
    }

    /**
     * Load XML file and generate commands JSON
     */
    public void generateCommands(List<File> xmlFiles) {
        // Load default resource file first
        loadFromXML(defaultSubjectsXmlFile, true);
        
        // Then load files from list
        for (File file : xmlFiles) {
            loadFromXML(file.getAbsolutePath(), false);
        }
    
    }

    /**
     * Load and generate from XML - supports both resources and files
     */
    public void loadFromXML(String xmlPath, boolean fromResources) {
        try {
            Document document = parseXML(xmlPath, fromResources);
            
            // Check if document has subjects element
            Element rootElement = document.getDocumentElement();
            NodeList subjectsNodeList = rootElement.getElementsByTagName(SUBJECTS_TAG);
            
            // Ignore XML if it doesn't have subjects element
            if (subjectsNodeList.getLength() == 0) {
                return;
            }
            
            loadConfigurationFromDocument(document);
            generateCommandsFromDocument(document);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to load and generate from XML", e);
        }
    }

    /**
     * Parse XML file from resources or file system
     */
    private Document parseXML(String xmlPath, boolean fromResources) 
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        InputStream inputStream = getInputStream(xmlPath, fromResources);
        return dBuilder.parse(inputStream);
    }

    /**
     * Get input stream from resources or file system
     */
    private InputStream getInputStream(String xmlFilePath, boolean fromResources) throws IOException {
        if (fromResources) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(xmlFilePath);
            if (inputStream == null) {
                throw new RuntimeException("File not found in resources: " + xmlFilePath);
            }
            return inputStream;
        } else {
            File file = new File(xmlFilePath);
            if (file.exists()) {
                return new FileInputStream(file);
            } else {
                throw new RuntimeException("File not found: " + xmlFilePath);
            }
        }
    }

    /**
     * Load JSON file and update commands JSON
     */
    public void updateCommands(List<File> jsonFiles) {
        // Load default resource file first
        loadFromJSON(defaultCommandsJsonFile, true);
        
        // Then load files from list
        for (File file : jsonFiles) {
            loadFromJSON(file.getAbsolutePath(), false);
        }
    
    }

    /**
     * Load and merge JSON - supports both resources and files
     */
    public void loadFromJSON(String jsonPath, boolean fromResources) {
        try {
            JSONObject jsonObject = parseJSON(jsonPath, fromResources);
            mergeJsonObject(jsonObject);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load and merge from JSON", e);
        }
    }

    /**
     * Parse JSON file - supports both resources and files
     */
    private JSONObject parseJSON(String jsonPath, boolean fromResources) throws IOException {
        try (InputStream inputStream = getInputStream(jsonPath, fromResources)) {
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(jsonContent);
        }
    }

    /**
     * Merge loaded JSON object into commandsJson
     */
    private void mergeJsonObject(JSONObject loadedJson) {
        // Initialize commandsJson if null
        if (commandsJson == null) {
            commandsJson = new JSONObject();
        }
        
        // Merge all keys from loaded JSON into commandsJson
        Iterator<String> keys = loadedJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = loadedJson.get(key);
            commandsJson.put(key, value);
        }
    }

    /**
     * Load configuration (actions and field types) from XML
     */
    private void loadConfigurationFromDocument(Document document) {
        Element rootElement = document.getDocumentElement();
        
        loadAliasesFromDocument(rootElement);

        // Load actions configuration
        NodeList configNodeList = rootElement.getElementsByTagName(CONFIG_TAG);
        if (configNodeList.getLength() > 0) {
            Element configElement = (Element) configNodeList.item(0);
            
            // Load action descriptions
            NodeList actionsNodeList = configElement.getElementsByTagName(ACTIONS_TAG);
            if (actionsNodeList.getLength() > 0) {
                Element actionsElement = (Element) actionsNodeList.item(0);
                NodeList actionNodeList = actionsElement.getElementsByTagName(ACTION_TAG);
                
                for (int i = 0; i < actionNodeList.getLength(); i++) {
                    Element actionElement = (Element) actionNodeList.item(i);
                    String actionName = getAttribute(actionElement, "name", "");
                    String actionDescription = getAttribute(actionElement, "description", "");
                    
                    if (!actionName.isEmpty() && !actionDescription.isEmpty()) {
                        actionDescriptions.put(actionName, actionDescription);
                    }
                }
            }
            
            // Load field type compare modes
            NodeList fieldTypesNodeList = configElement.getElementsByTagName(FIELD_TYPES_TAG);
            if (fieldTypesNodeList.getLength() > 0) {
                Element fieldTypesElement = (Element) fieldTypesNodeList.item(0);
                NodeList fieldTypeNodeList = fieldTypesElement.getElementsByTagName(FIELD_TYPE_TAG);
                
                for (int i = 0; i < fieldTypeNodeList.getLength(); i++) {
                    Element fieldTypeElement = (Element) fieldTypeNodeList.item(i);
                    String typeName = getAttribute(fieldTypeElement, "name", "");
                    String compareMode = getAttribute(fieldTypeElement, "compareMode", "=");
                    
                    if (!typeName.isEmpty()) {
                        fieldTypeCompareModes.put(typeName, compareMode);
                    }
                }
            }
        }
    }

    /**
     * Load action aliases from XML  // ADD THIS METHOD
     */
    private void loadAliasesFromDocument(Element rootElement) {
        NodeList aliasesNodeList = rootElement.getElementsByTagName(ALIASES_TAG);
        if (aliasesNodeList.getLength() > 0) {
            Element aliasesElement = (Element) aliasesNodeList.item(0);
            NodeList aliasNodeList = aliasesElement.getElementsByTagName(ALIAS_TAG);
            
            for (int i = 0; i < aliasNodeList.getLength(); i++) {
                Element aliasElement = (Element) aliasNodeList.item(i);
                String from = getAttribute(aliasElement, "from", "");
                String to = getAttribute(aliasElement, "to", "");
                
                if (!from.isEmpty() && !to.isEmpty()) {
                    actionAliases.put(from, to);
                }
            }
        }
    }

    /**
     * Generate commands JSON from parsed XML document
     */
    private void generateCommandsFromDocument(Document document) {
        document.getDocumentElement().normalize();
        Element rootElement = document.getDocumentElement();

        // Initialize apps array if not already done
        if (commandsJson == null) {
            commandsJson = new JSONObject();
            commandsJson.put("apps", new JSONArray());
        }

        JSONArray appsArray = commandsJson.getJSONArray("apps");

        // Find the subjects container
        NodeList subjectsNodeList = rootElement.getElementsByTagName(SUBJECTS_TAG);
        Element subjectsElement = (Element) subjectsNodeList.item(0);

        if (subjectsElement != null) {
            // Get namespace and description from subjects element
            String namespace = getAttribute(subjectsElement, "namespace", "default");
            String description = getAttribute(subjectsElement, "description", "");
            
            // Create the namespace object
            JSONObject namespaceObject = new JSONObject();
            namespaceObject.put("id", namespace);
            namespaceObject.put("description", description);
            
            // Get all subject elements
            NodeList subjectNodes = subjectsElement.getElementsByTagName(SUBJECT_TAG);
            JSONArray commandsArray = new JSONArray();

            for (int i = 0; i < subjectNodes.getLength(); i++) {
                Element subjectElement = (Element) subjectNodes.item(i);
                JSONObject subjectCommand = createSubjectCommand(namespace, subjectElement);
                commandsArray.put(subjectCommand);
            }

            namespaceObject.put("commands", commandsArray);
            appsArray.put(namespaceObject);
        }
    }

    /**
     * Create a subject command object by reading all attributes dynamically
     */
    private JSONObject createSubjectCommand(String namespace, Element subjectElement) {
        JSONObject subjectCommand = new JSONObject();

        String identifier = getAttribute(subjectElement, "identifier", "");
        String description = getAttribute(subjectElement, "description", "");
        
        subjectCommand.put("id", identifier + "@" + namespace);
        subjectCommand.put("description", description);

        NodeList fieldNodes = subjectElement.getElementsByTagName(FIELD_TAG);

        JSONArray commandsArray = new JSONArray();
        
        // Get actions from <actions> element
        NodeList actionsNodeList = subjectElement.getElementsByTagName("actions");
        if (actionsNodeList.getLength() > 0) {
            Element actionsElement = (Element) actionsNodeList.item(0);
            NodeList actionNodeList = actionsElement.getElementsByTagName(ACTION_TAG);
            
            for (int i = 0; i < actionNodeList.getLength(); i++) {
                Element actionElement = (Element) actionNodeList.item(i);
                String action = getAttribute(actionElement, "name", "");
                if (!action.isEmpty()) {
                    JSONObject actionCommand = createActionCommand(namespace, identifier, subjectElement, action, fieldNodes);
                    commandsArray.put(actionCommand);
                }
            }
        }

        subjectCommand.put("commands", commandsArray);
        return subjectCommand;
    }

    /**
     * Create a field object with all its attributes
     */
    private JSONObject createFieldObject(Element fieldElement) {
        JSONObject fieldObj = new JSONObject();
        
        // Copy all attributes from field element
        NamedNodeMap attributes = fieldElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            String attrName = attr.getName();
            String attrValue = attr.getValue();
            
            // Skip the "name" attribute as it's used as the key
            if ("name".equals(attrName)) {
                continue;
            }

            if ("referencedfield".equals(attrName)) {
                JSONObject referencedField = new JSONObject();;
                referencedField.put("link", attrValue);
                fieldObj.put("referencedfield", referencedField);
                continue;
            }
            
            // Check if the value is a boolean string and convert accordingly
            if (isBooleanValue(attrValue)) {
                fieldObj.put(attrName, Boolean.parseBoolean(attrValue));
            } else if (!attrValue.isEmpty()) {
                fieldObj.put(attrName, attrValue);
            }
        }
        
        // Extract defaults from <defaults> element
        JSONObject defaultValues = extractDefaultValues(fieldElement);
        if (!defaultValues.isEmpty()) {
            fieldObj.put("defaultvalue", defaultValues);
        }

        return fieldObj;
    }

    // Helper method to check if a string value is a boolean
    private boolean isBooleanValue(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private JSONObject extractDefaultValues(Element fieldElement) {
        JSONObject defaultValues = new JSONObject();
        
        NodeList defaultsNodeList = fieldElement.getElementsByTagName(DEFAULTS_TAG);
        if (defaultsNodeList.getLength() > 0) {
            Element defaultsElement = (Element) defaultsNodeList.item(0);
            NodeList defaultNodeList = defaultsElement.getElementsByTagName(DEFAULT_TAG);
            
            for (int i = 0; i < defaultNodeList.getLength(); i++) {
                Element defaultElement = (Element) defaultNodeList.item(i);
                String action = getAttribute(defaultElement, "action", "");
                String value = getAttribute(defaultElement, "value", "");
                
                if (!action.isEmpty() && !value.isEmpty()) {
                    defaultValues.put(action, value);
                }
            }
        }
        
        return defaultValues;
    }

    /**
     * Create an action command object
     */
    private JSONObject createActionCommand(String namespace, String identifier, Element subjectElement, String action, NodeList fieldNodes) {
        JSONObject actionCommand = new JSONObject();
        
        String actionId = identifier + "." + action;
        actionCommand.put("id", actionId);
        actionCommand.put("description", getActionDescription(identifier, action));
        actionCommand.put("action", identifier + "." + resolveActionAlias(action));

        JSONObject args = new JSONObject();

        // Create args only for actions that have arguments
        if (!Boolean.parseBoolean(getMainActionOption(subjectElement, action, "noargs", "false"))){
            args = createActionArgs(fieldNodes, action);
        }

        actionCommand.put("args", args);
        return actionCommand;
    }

    /**
     * Resolve action alias to its target action  // ADD THIS METHOD
     */
    private String resolveActionAlias(String action) {
        return actionAliases.getOrDefault(action, action);
    }
    
    /**
     * Create args for any action based on action type
     */
    private JSONObject createActionArgs(NodeList fieldNodes, String action) {
        JSONObject args = new JSONObject();

        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Element fieldElement = (Element) fieldNodes.item(i);
            String fieldName = getAttribute(fieldElement, "name", "field" + i);
            
            // Skip fields not used by this action
            if (!isActionInUsedby(fieldElement, action)) {
                continue;
            }

            JSONObject fieldObj = createFieldObject(fieldElement);

            // Get mandatory option with default false
            String description = getActionOption(fieldElement, action, "description", fieldName);
            fieldObj.put("description", description);

            // Add all supported actions
            JSONArray supportsArray = extractSupportedActions(fieldElement);
            if (supportsArray.length() > 0) {
                fieldObj.put("supports", supportsArray);
            }

            // Get mandatory option with default false
            String mandatory = getActionOption(fieldElement, action, "mandatory", "false");
            fieldObj.put("mandatory", Boolean.parseBoolean(mandatory));

            // Get compareMode with default based on type
            String type = getActionOption(fieldElement, action, "type", "str");
            fieldObj.put("type", type);
            String defaultCompareMode = getCompareMode(type);
            String compareMode = getActionOption(fieldElement, action, "compareMode", defaultCompareMode);
            fieldObj.put("compareMode", compareMode);

            // Skip field if action doesn't support it (for remove action with non-id fields)
            if ("remove".equals(action) && !fieldName.equals("id")) {
                continue;
            }

            args.put(fieldName, fieldObj);

            // Break after processing id field for remove action
            if ("remove".equals(action) && fieldName.equals("id")) {
                break;
            }
        }

        return args;
    }

    private String getMainActionOption(Element fieldElement, String actionName, String optionName, String defaultValue) {
        return getElementOption(fieldElement, ACTIONS_TAG, actionName, optionName, defaultValue);
    }

    private JSONArray extractSupportedActions(Element fieldElement) {
        JSONArray supportsArray = new JSONArray();
        
        NodeList usedbyNodeList = fieldElement.getElementsByTagName(USEDBY_TAG);
        if (usedbyNodeList.getLength() > 0) {
            Element usedbyElement = (Element) usedbyNodeList.item(0);
            NodeList actionNodeList = usedbyElement.getElementsByTagName(ACTION_TAG);
            
            for (int i = 0; i < actionNodeList.getLength(); i++) {
                Element actionElement = (Element) actionNodeList.item(i);
                String actionName = getAttribute(actionElement, "name", "");
                if (!actionName.isEmpty()) {
                    supportsArray.put(actionName);
                }
            }
        }
        
        return supportsArray;
    }

    private String getActionOption(Element fieldElement, String actionName, String optionName, String defaultValue) {
        return getElementOption(fieldElement, USEDBY_TAG, actionName, optionName, defaultValue);
    }

    private String getElementOption(Element fieldElement, String rootTag, String actionName, String optionName, String defaultValue) {
        NodeList usedbyNodeList = fieldElement.getElementsByTagName(rootTag);
        if (usedbyNodeList.getLength() > 0) {
            Element usedbyElement = (Element) usedbyNodeList.item(0);
            NodeList actionNodeList = usedbyElement.getElementsByTagName(ACTION_TAG);
            
            for (int i = 0; i < actionNodeList.getLength(); i++) {
                Element actionElement = (Element) actionNodeList.item(i);
                String name = getAttribute(actionElement, "name", "");
                
                if (name.equals(actionName)) {
                    // Check if action has the option
                    String actionOption = getAttribute(actionElement, optionName, null);
                    if (actionOption != null) {
                        return actionOption;
                    }
                    
                    // Fall back to field element option
                    String fieldOption = getAttribute(fieldElement, optionName, null);
                    if (fieldOption != null) {
                        return fieldOption;
                    }
                    
                    // Return default if neither action nor field has the option
                    return defaultValue;
                }
            }
        }
        
        return defaultValue;
    }

    /**
     * Check if an action is listed in the usedby attribute
     */
    private boolean isActionInUsedby(Element fieldElement, String action) {
        NodeList usedbyNodeList = fieldElement.getElementsByTagName(USEDBY_TAG);
        if (usedbyNodeList.getLength() == 0) {
            return false;
        }
        
        Element usedbyElement = (Element) usedbyNodeList.item(0);
        NodeList actionNodeList = usedbyElement.getElementsByTagName(ACTION_TAG);
        
        for (int i = 0; i < actionNodeList.getLength(); i++) {
            Element actionElement = (Element) actionNodeList.item(i);
            String actionName = getAttribute(actionElement, "name", "");
            if (action.equals(actionName)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get attribute value from element with fallback to default
     */
    private String getAttribute(Element element, String attributeName, String defaultValue) {
        String value = element.getAttribute(attributeName);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    /**
     * Get action description from configuration with placeholder replacement
     */
    private String getActionDescription(String identifier, String action) {
        String template = actionDescriptions.getOrDefault(action, action + " {identifier}");
        return template.replace("{identifier}", identifier);
    }

    /**
     * Get compareMode from configuration based on field type
     */
    private String getCompareMode(String type) {
        return fieldTypeCompareModes.getOrDefault(type, "=");
    }

    /**
     * Get the generated commands JSON
     */
    public JSONObject getCommandsJson() {
        return commandsJson;
    }

    /**
     * Get the JSON as a string
     */
    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int... indentation) {
        int indent = indentation.length > 0 ? indentation[0] : 0;
        return commandsJson != null ? commandsJson.toString(indent) : "No commands generated";
    }
}
