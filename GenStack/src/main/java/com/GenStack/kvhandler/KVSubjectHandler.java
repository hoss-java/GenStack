package com.GenStack.kvhandler;

import java.io.File;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.FileInputStream;

import com.GenStack.kvhandler.KVSubject;
import com.GenStack.kvhandler.KVSubjectHandlerInterface;
import com.GenStack.kvhandler.KVSubjectStorage;
import com.GenStack.storages.MultiNamespaceStorageManager;
import com.GenStack.helper.DebugUtil;

public class KVSubjectHandler implements KVSubjectHandlerInterface {
    private static final String subjectsXMLRootStr = "subjects";
    private static final String subjectXMLRootStr = "subject";
    private static final String defaultNamespace = "default";
    private static final String defaultDescription = "";
    private static final String defaultStorage = "memory";
    private static final String defaultActions = "add,remove,gets,getsall";
    private static final String defaultSubjectsXmlFile = "subjects.xml";

    private MultiNamespaceStorageManager namespaceManager;

    public KVSubjectHandler(List<File> xmlFiles, MultiNamespaceStorageManager namespaceManager) {
        this.namespaceManager = namespaceManager; // Initialize using injected storage

        loadXMLFileIfNotExists(defaultSubjectsXmlFile, true);
        for (File file : xmlFiles) {
            loadXMLFileIfNotExists(file.getAbsolutePath(), false);
        }
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
     * Get all root element attributes from XML file
     */
    private Map<String, String> getRootAttributesFromXML(String xmlFilePath, boolean fromResources) {
        Map<String, String> attributes = new HashMap<>();
        
        try {
            InputStream inputStream = getInputStream(xmlFilePath, fromResources);
            
            try (InputStream stream = inputStream) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document document = dBuilder.parse(stream);
                document.getDocumentElement().normalize();
                
                Element rootElement = document.getDocumentElement();
                
                // Get the first <subjects> element under <root>
                NodeList subjectsNodeList = rootElement.getElementsByTagName(subjectsXMLRootStr);
                
                if (subjectsNodeList.getLength() == 0) {
                    throw new RuntimeException("Invalid XML: No '" + subjectsXMLRootStr + "' element found under root");
                }
                
                Element subjectsElement = (Element) subjectsNodeList.item(0);
                
                // Get all attributes from the <subjects> element
                NamedNodeMap attrMap = subjectsElement.getAttributes();
                for (int i = 0; i < attrMap.getLength(); i++) {
                    Attr attr = (Attr) attrMap.item(i);
                    attributes.put(attr.getName(), attr.getValue());
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading root attributes from XML: " + xmlFilePath);
            //e.printStackTrace();
        }
        
        return attributes;
    }

    /**
     * Load XML file only if namespace and storage are not already imported
     */
    private void loadXMLFileIfNotExists(String xmlFilePath, boolean fromResources) {
        DebugUtil.info(xmlFilePath, fromResources);
        try {
            // Get all root attributes in one call
            Map<String, String> rootAttributes = getRootAttributesFromXML(xmlFilePath, fromResources);

            // Check if XML is empty
            if (rootAttributes.isEmpty()) {
               System.out.println("XML file is empty, skipping: " + xmlFilePath);
                return;
            }

            String namespace = rootAttributes.getOrDefault("namespace", defaultNamespace);
            String storage = rootAttributes.getOrDefault("storage", defaultStorage);

            if (!namespaceManager.namespaceExists(namespace)){
                namespaceManager.registerNamespace(namespace, storage);
            }

            if (namespaceManager.hasSubjectStorage(namespace, storage) == true && 
                namespaceManager.getSubjectStorage(namespace, storage).countKVSubjects() > 0) {
                System.out.println("Namespace '" + namespace + "' with storage '" + storage + "' already loaded. Skipping: " + xmlFilePath);
                return;
            }
            
            // If not loaded, proceed with full XML loading
            DebugUtil.info("Loading XML file: " + xmlFilePath + " (namespace: " + namespace + ", storage: " + storage + ")");
            loadDataFromXML(namespace, storage, xmlFilePath, fromResources);
            
        } catch (Exception e) {
            System.out.println("Error processing XML file: " + xmlFilePath);
            //e.printStackTrace();
        }
    }


    /**
     * Load full XML data (only called if not already loaded)
     */
    private void loadDataFromXML(String namespace, String storage, String xmlFilePath, boolean fromResources) {
        try {
            InputStream inputStream = getInputStream(xmlFilePath, fromResources);
            
            try (InputStream stream = inputStream) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document document = dBuilder.parse(stream);
                document.getDocumentElement().normalize();

                Element rootElement = document.getDocumentElement();
                
                // Get the <subjects> element directly (there's only one)
                Element subjectsElement = (Element) rootElement.getElementsByTagName("subjects").item(0);
                
                if (subjectsElement != null) {
                    // Get all <subject> children under <subjects>
                    NodeList subjectNodes = subjectsElement.getElementsByTagName("subject");
                    
                    for (int i = 0; i < subjectNodes.getLength(); i++) {
                        Element subjectElement = (Element) subjectNodes.item(i);
                        addKVSubject(namespace, storage, subjectElement);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addKVSubject(String namespace, String storage, Element subjectElement) {
        if ( namespaceManager.hasSubjectStorage(namespace,storage) == false){
            namespaceManager.addSubjectStorage(namespace,storage);
        }
        // Check if the storage already contains the identifier
        KVSubjectAttribute kvSubjectAttribute = initializeSubjectAttributes(subjectElement);        
        KVSubjectStorage storageToUse = namespaceManager.getSubjectStorage(namespace,storage);
        if (storageToUse != null && storageToUse.getKVSubject(kvSubjectAttribute.getIdentifier()) == null) {
            KVSubject subject = new KVSubject(kvSubjectAttribute);
            if (subject != null){
                initializeFieldTypeMap(subject, subjectElement);
                storageToUse.addKVSubject(subject); // Use storage method to add
            }
        }
    }

    @Override
    public void removeKVSubject(String namespace, String storage, String identifier) {
        KVSubjectStorage storageToUse = namespaceManager.getSubjectStorage(namespace,storage);
        if (storageToUse != null) {
            KVSubject subject = storageToUse.getKVSubject(identifier);
            if (subject != null) {
                storageToUse.removeKVSubject(subject); // Use storage method to remove
            }
        }
    }

    @Override
    public KVSubject getKVSubject(String namespace, String storage, String identifier) {
        KVSubjectStorage storageToUse = namespaceManager.getSubjectStorage(namespace,storage);
        if (storageToUse != null) {
            return namespaceManager.getSubjectStorage(namespace,storage).getKVSubject(identifier); // Use storage method to get
        }
        return null;
    }

    // New method to get fieldTypeMap for a given identifier
    public Map<String, KVObjectField> getFieldTypeMapByIdentifier(String namespace, String storage, String identifier) {
        KVSubject subject = getKVSubject(namespace, storage, identifier);
        return (subject != null) ? subject.getFieldTypeMap() : null;
    }
    
    private KVSubjectAttribute initializeSubjectAttributes(Element subjectElement) {
        String namespace = subjectElement.getAttribute("namespace").isEmpty() ? defaultNamespace : subjectElement.getAttribute("namespace");
        String identifier = subjectElement.getAttribute("identifier");
        String description = subjectElement.getAttribute("description").isEmpty() ? defaultDescription : subjectElement.getAttribute("description");
        String storage = subjectElement.getAttribute("storage").isEmpty() ? defaultStorage : subjectElement.getAttribute("storage");
        String actions = subjectElement.getAttribute("actions").isEmpty() ? defaultActions : subjectElement.getAttribute("actions");
        return new KVSubjectAttribute(namespace,identifier,description,storage,actions);
    }

    private void initializeFieldTypeMap(KVSubject subject, Element subjectElement) {
        NodeList fieldNodes = subjectElement.getElementsByTagName("field");
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Element fieldElement = (Element) fieldNodes.item(i);
            String field = fieldElement.getAttribute("field");
            String name = fieldElement.getAttribute("name");
            String type = fieldElement.getAttribute("type");
            boolean mandatory = Boolean.parseBoolean(fieldElement.getAttribute("mandatory"));
            String modifier = fieldElement.getAttribute("modifier");
            
            // Read the default value from the <defaults><default action="init"> element
            String defaultValue = getDefaultValueForAction(fieldElement, "init");

            subject.getFieldTypeMap().put(name, new KVObjectField(field, type, mandatory, modifier, defaultValue));
        }
    }

    private String getDefaultValueForAction(Element fieldElement, String action) {
        NodeList defaultNodes = fieldElement.getElementsByTagName("default");
        for (int i = 0; i < defaultNodes.getLength(); i++) {
            Element defaultElement = (Element) defaultNodes.item(i);
            String defaultAction = defaultElement.getAttribute("action");
            if (action.equals(defaultAction)) {
                return defaultElement.getAttribute("value");
            }
        }
        return null; // Return null if no matching default is found
    }
}
