/*
 * Copyright (c) 2016 Michael Meding -- All Rights Reserved.
 */
package com.partbuzz.slicer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Invoke the CURA slicing engine.
 * <p>
 * SAMPLE BASH
 * ../../CuraEngine-master/build/CuraEngine slice -v -j ultimaker2.json -g -e -o "output/test.gcode" -l ../../models/ControlPanel.stl
 *
 * @author mike
 */
public class CuraEngine extends PlatformExecutable {

    private static final Logger log = Logger.getLogger(CuraEngine.class.getName());
    //TODO: this needs to be a relative file path
    private static final String CURAENGINE_PROG = "/home/mike/Documents/WebSlicerServer/CuraEngine-master/build/CuraEngine";

    public class Options {

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
        public Options verbose() {
            this.verbose = true;
            return this;
        }

        /**
         * Set the settings file. This is a JSON formatted file which includes all needed information for slicing the file.
         *
         * @param filename the settings filename
         * @return the options
         */
        public Options settingsFilename(String filename) {
            this.settingsFileName = filename;
            return this;
        }

        /**
         * Set the output filename. The resulting gcode file name/location.
         *
         * @param filename the output filename
         * @return the options
         */
        public Options outputFilename(String filename) {
            this.outputFileName = filename;
            return this;
        }

        /**
         * Set the model filename. This is the model to be sliced.
         *
         * @param filename
         * @return options
         */
        public Options modelFilename(String filename) {
            this.modelFileName = filename;
            return this;
        }

        /**
         * This option is similar to verbose but instead of just logging the output to the screen it logs the output to the CuraEngine log files.
         *
         * @return the options
         */
        public Options logProgress() {
            this.p_option = true;
            return this;
        }

        /**
         * Switch setting focus to the current mesh group only.
         * Used for one-at-a-time printing.
         *
         * @return the options
         */
        public Options currentGroupOnly() {
            g_option = true;
            return this;
        }

        /**
         * Adds a new extruder train for multi extrusion. For every time -e is included another extruder is added as on option to the slicer.
         *
         * @return the options
         */
        public Options extruderTrainOption() {
            e_option = true;
            return this;
        }

        /**
         * For debugging.
         *
         * @return the options
         */
        public Options ignoreErrors() {
            this.ignoreErrors = true;
            return this;
        }

        public boolean haveIgnoreErrors() {
            return ignoreErrors;
        }

        /*
         * Get the options in the right sequence. To build a correct CuraEngine command line executable.
         */
        private List<String> getOptions() throws CuraEngineException {
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

    private final Options options;

    public CuraEngine() {
        this.options = new Options();
    }

    public Options options() {
        return options;
    }

    public String output = "";

    /**
     * Run the CURA engine.
     *
     * @throws CuraEngineException
     */
    public void execute() throws IOException {
        checkPlatformExecutable(CURAENGINE_PROG);

        List<String> arguments = new ArrayList<>();
        arguments.add(CURAENGINE_PROG); // add base executable
        arguments.addAll(options.getOptions()); // add all options in the correct order

        for (String arg : arguments) {
            log.info("cura argument: " + arg);
        }

        // Process builder options for command line execution
        ProcessBuilder pb = new ProcessBuilder();
//		pb.directory(basePath);
        pb.redirectErrorStream(true);
        pb.command(arguments);

        // Create new thread and start execution
        Process p = pb.start();
        try {
            int status = p.waitFor(); // wait for slice to finish

            if (status == 0) {

                // gather everything that is sent back from the command.
                StringBuilder sb = new StringBuilder();
                InputStream fp = p.getInputStream();
                byte[] buffer = new byte[512];
                while (fp.available() > 0) {
                    int n = fp.read(buffer);
                    sb.append(new String(buffer, 0, n));
                }
                output = sb.toString();
                log.info(output); // just log the output for now
                return;
            } else {
                throw new CuraEngineException("unable to parse gcode file");
            }

//            if (options.haveIgnoreErrors()) {
//                log.warning(sb.toString());
//            } else {
//                throw new CuraEngineException(sb.toString());
//            }


        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            cleanupResources(p);
        }
    }

    /**
     * After execute is called the standard out of the call is concatenated into output.
     *
     * @return
     */
    public String getOutput() {
        return output;
    }
}
