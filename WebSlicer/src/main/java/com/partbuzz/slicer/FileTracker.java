package com.partbuzz.slicer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * For tracking the associated files with a particular client id.
 * <p>
 * FORMAT IS
 * map<String clientId, String fileName>
 * <p>
 * Created by mike on 1/14/16.
 */
public class FileTracker {
    private Map<String, String> modelFileMap = new HashMap<>();
    private Map<String, String> settingsFileMap = new HashMap<>();

    public FileTracker() {
    }

    /**
     * Get the settings file id associated with a particular client id.
     *
     * @param id
     * @return
     */
    public String getSettingFileByClientId(String id) {
        return settingsFileMap.get(id);
    }

    /**
     * Same as above only for the model file.
     *
     * @param id
     * @return
     */
    public String getModelFileByClientId(String id) {
        return modelFileMap.get(id);
    }


    public void removeClient(String id) {
        settingsFileMap.remove(id);
        modelFileMap.remove(id);
    }

    /**
     * Issue new filename. These names are untracked unless registered using the register function.
     *
     * @return
     */
    public String getSettingsFileName() {
        return UUID.randomUUID().toString() + "-settings.json";
    }

    public String getModelFileName() {
        return UUID.randomUUID().toString() + "-model.stl";
    }

    /**
     * Register a new settings file with our registry
     *
     * @param fileName
     * @return
     */
    public String registerSettingsFile(String fileName) {
        String uuid = UUID.randomUUID().toString();
        settingsFileMap.put(uuid, fileName);
        return uuid;
    }

    /**
     * Register a new model file with our registry
     *
     * @param fileName
     * @return
     */
    public String registerModelfile(String fileName) {
        String uuid = UUID.randomUUID().toString();
        modelFileMap.put(uuid, fileName);
        return uuid;
    }

}
