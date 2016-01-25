package com.partbuzz.slicer.util;

import os.io.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * For tracking the associated files with a particular client id.
 * FILE STRUCTURE
 * /main---/(clientUUID)---output.gcode
 * -settings.json
 * -/models---(uuid-filename.stl)
 * -(...stl)
 * -...
 * <p>
 * -/common---fdmprinter.json
 * -/presets---ultimaker.json
 * -prusai3.json
 * -...
 * <p>
 * Structure dictates file limit and will throw exceptions if # of files goes above limit.
 * <p>
 * Created by mike on 1/14/16.
 */
public class FileTracker {
    //TODO: This really needs to come from a properties file (JNDI path to properties file)
    public static String delimiter = File.separator;
    public static String basePath = delimiter + "tmp" + delimiter + "webslicer";
    public static String common = basePath + delimiter + "common";
    public static String modelDir = "models";
    public static String settingsFileName = "settings.json";
    public static String fdmprinterFile = "fdmprinter.json";
    public static String outputFile = "output.gcode";
    private static Map<String, HashMap<String, String>> clientFilesMap = new HashMap<>();

    /**
     * Same as above only for the model file.
     *
     * @param clientId The id of the client in question
     * @param fileId   The tracked filename which must be managed by client
     * @return
     */
    public static String getModelFileName(String clientId, String fileId) {
        return clientFilesMap.get(clientId).get(fileId);
    }

    /**
     * Get the full path to a model file
     *
     * @param clientId
     * @param fileId
     * @return
     */
    public static String getModelFullPath(String clientId, String fileId) {
        return basePath + delimiter + clientId + delimiter + modelDir + delimiter + getModelFileName(clientId, fileId);
    }

    /**
     * Get the path to the model file directory
     *
     * @return
     */
    public static String getModelPathById(String clientId) {
        return basePath + delimiter + clientId + delimiter + modelDir;
    }

    /**
     * Return the entire hashmap of model files associated with a client
     *
     * @param clientId
     * @return
     */
    public static HashMap<String, String> getAllModelFiles(String clientId) {
        return clientFilesMap.get(clientId);
    }

    public static String getOutputFilePath(String clientId) {
        return basePath + delimiter + clientId + delimiter + outputFile;
    }

    /**
     * Register a new model file with our registry
     *
     * @param fileName
     * @return
     */
    public static String registerModelFile(String clientId, String fileName) {
        String uuid = UUID.randomUUID().toString(); // generate new tracking uuid
        clientFilesMap.get(clientId).put(uuid, fileName); // store this filename using that new tracking uuid for that client
        return uuid;
    }

    /**
     * Gets the fully qualified path to the settings file for a specified client
     *
     * @param clientId
     * @return
     */
    public static String getSettingsFullPath(String clientId) {
        return basePath + delimiter + clientId + delimiter + settingsFileName;
    }

    /**
     * Remove all files associated with a client and remove its hashmap tracking in our server
     *
     * @param clientId
     */
    public static void removeClient(String clientId) {
        try {
            FileHelper.delete(new File(basePath + delimiter + clientId));
            clientFilesMap.remove(clientId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Completely remove model file from our disk
     *
     * @param clientId
     * @param id
     */
    public static void removeModelFile(String clientId, String id) {
        try {
            FileHelper.delete(new File(getModelFullPath(clientId, id)));
            clientFilesMap.get(clientId).remove(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Register and setup a new client with our registry.
     *
     * @return the new clients UUID.
     */
    public static String setupNewClient() throws CuraEngineException {

        // generate new client uuid
        String uuid = UUID.randomUUID().toString();

        // Create empty file as clients home directory
        if (!new File(basePath + delimiter + uuid + delimiter + modelDir).mkdirs()) {
            throw new CuraEngineException("Could not create file path correctly");
        }

        // Symbolically link fdmprinter.json from common to this clients home directory
        try {
            Path targetPath = Paths.get(basePath + delimiter + uuid + delimiter + fdmprinterFile);
            Path sourceFile = Paths.get(common + delimiter + fdmprinterFile);
            Files.createSymbolicLink(targetPath, sourceFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Give our new client a file tracking hashmap
        clientFilesMap.put(uuid, new HashMap<String, String>()); // initalize client with new empty hashmap

        return uuid;
    }


/**
 * PRIVATE METHODS
 */
}
