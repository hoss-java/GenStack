package com.GenStack.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.GenStack.helper.DebugUtil;

public class AppConfig {
    private String appDataFolder;

    public AppConfig(String appDataFolder) {
        this.appDataFolder = appDataFolder;
    }

    public List<File> getFiles(String folderName, String extension) {
        // Validate inputs
        if (folderName == null || folderName.isEmpty()) {
            System.err.println("Error: folderName is null or empty");
            return new ArrayList<>();
        }
        
        if (extension == null || extension.isEmpty()) {
            System.err.println("Error: extension is null or empty");
            return new ArrayList<>();
        }
        
        // Validate appDataFolder
        if (appDataFolder == null || appDataFolder.isEmpty()) {
            System.err.println("Error: appDataFolder is null or empty");
            return new ArrayList<>();
        }
        
        List<File> fileList = new ArrayList<>();
        File folder = new File(appDataFolder, folderName);
        
        // Debug: print the actual path being used
        DebugUtil.info("Looking for files in: " + folder.getAbsolutePath());
        
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith("." + extension));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileList.add(file);
                    }
                }
                DebugUtil.info("Found " + fileList.size() + " files with extension: ." + extension);
            } else {
                DebugUtil.info("No files found with extension: ." + extension);
            }
        } else {
            System.err.println("Folder does not exist or is not a directory: " + folder.getAbsolutePath());
        }
        
        return fileList;
    }

    public String getAppDataFolder() {
        return appDataFolder;
    }
}


