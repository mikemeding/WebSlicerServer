package com.partbuzz.slicer.cura;

import com.partbuzz.slicer.util.CuraEngineException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 3/30/16.
 */

public class CuraEngineOptions {

    private String settingsFileName;
    private String outputFileName;
    private String modelFileName;
    private boolean verbose = false;
    private boolean p_option = false;
    private boolean g_option = false;
    private boolean e_option = false;
    private boolean ignoreErrors = false;

    /**
     * Set the verbose mode.
     *
     * @return the options
     */
    public CuraEngineOptions verbose() {
        this.verbose = true;
        return this;
    }

    /**
     * Set the settings file. This is a JSON formatted file which includes all needed information for slicing the file.
     *
     * @param filename the settings filename
     * @return the options
     */
    public CuraEngineOptions settingsFilename(String filename) {
        this.settingsFileName = filename;
        return this;
    }

    /**
     * Set the output filename. The resulting gcode file name/location.
     *
     * @param filename the output filename
     * @return the options
     */
    public CuraEngineOptions outputFilename(String filename) {
        this.outputFileName = filename;
        return this;
    }

    /**
     * Set the model filename. This is the model to be sliced.
     *
     * @param filename
     * @return options
     */
    public CuraEngineOptions modelFilename(String filename) {
        this.modelFileName = filename;
        return this;
    }

    /**
     * This option is similar to verbose but instead of just logging the output to the screen it logs the output to the CuraEngine log files.
     *
     * @return the options
     */
    public CuraEngineOptions logProgress() {
        this.p_option = true;
        return this;
    }

    /**
     * Switch setting focus to the current mesh group only.
     * Used for one-at-a-time printing.
     *
     * @return the options
     */
    public CuraEngineOptions currentGroupOnly() {
        g_option = true;
        return this;
    }

    /**
     * Adds a new extruder train for multi extrusion. For every time -e is included another extruder is added as on option to the slicer.
     *
     * @return the options
     */
    public CuraEngineOptions extruderTrainOption() {
        e_option = true;
        return this;
    }

    /**
     * For debugging.
     *
     * @return the options
     */
    public CuraEngineOptions ignoreErrors() {
        this.ignoreErrors = true;
        return this;
    }

    public boolean haveIgnoreErrors() {
        return ignoreErrors;
    }

    /*
     * Get the options in the right sequence. To build a correct CuraEngine command line executable.
     */
    public List<String> getOptions() throws CuraEngineException {
        List<String> list = new ArrayList<>();
        list.add("slice"); // the main parameter of the CuraEngine executable.
        if (verbose) {
            list.add("-v");
        }
        if (p_option) {
            list.add("-p");
        }

        if (settingsFileName == null) {
            throw new CuraEngineException("no setting file defined");
        } else {
            list.add("-j");
            list.add(settingsFileName);
        }
        if (g_option) {
            list.add("-g");
        }
        if (e_option) {
            list.add("-e");
        }
        if (outputFileName == null) {
            throw new CuraEngineException("no output file defined");
        } else {
            list.add("-o");
            list.add(outputFileName);
        }
        if (modelFileName == null) {
            throw new CuraEngineException("no model file defined");
        } else {
            list.add("-l");
            list.add(modelFileName);
        }

        return list;
    }
}

