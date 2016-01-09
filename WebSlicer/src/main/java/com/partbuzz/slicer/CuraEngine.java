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
 *
 * @author mike
 */
public class CuraEngine extends PlatformExecutable {

    private static final Logger log = Logger.getLogger(CuraEngine.class.getName());
    private static final String CURAENGINE_PROG = "/bin/ls";

    public class Options {

        private String settingsFilename;
        private String outputFilename;
        private String modelFilename;
        private boolean verbose = false;
        private boolean p_option = false;
        private boolean g_option = false;
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
         * Set the settings file.
         *
         * @param filename the settings filename
         * @return the options
         */
        public Options settingsFilename(String filename) {
            this.settingsFilename = filename;
            return this;
        }

        /**
         * Set the output filename
         *
         * @param filename the output filename
         * @return the options
         */
        public Options outputFilename(String filename) {
            this.outputFilename = filename;
            return this;
        }

        /**
         * Set the model filename.
         *
         * @param filename
         * @return options
         */
        public Options modelFilename(String filename) {
            this.modelFilename = filename;
            return this;
        }

        /**
         * @return the options
         */
        public Options minusP() {
            this.p_option = true;
            return this;
        }

        /**
         * @return the options
         */
        public Options minusG() {
            g_option = true;
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
         * Get the options in the right sequence.
         */
        private List<String> getOptions() throws CuraEngineException {
            List<String> list = new ArrayList<>();
            list.add("slice");
            if (verbose) {
                list.add("-v");
            }
            if (p_option) {
                list.add("-p");
            }

            if (settingsFilename == null) {
                throw new CuraEngineException("no setting file defined");
            } else {
                list.add("-j");
                list.add(settingsFilename);
            }
            if (g_option) {
                list.add("-g");
            }
            if (outputFilename == null) {
                throw new CuraEngineException("no output file defined");
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

    /**
     * Run the CURA engine.
     *
     * @throws CuraEngineException
     */
    public void execute() throws IOException {
        checkPlatformExecutable(CURAENGINE_PROG);

        List<String> arguments = new ArrayList<>();
        arguments.add(CURAENGINE_PROG);
        arguments.addAll(options.getOptions());

        for (String arg : arguments) {
            log.info("cura argument: " + arg);
        }

        ProcessBuilder pb = new ProcessBuilder();
//		pb.directory(basePath);
        pb.redirectErrorStream(true);
        pb.command(arguments);

        Process p = pb.start();
        try {
            int status = p.waitFor();
            if (status == 0) {
                return;
            }

            // gather everything that is sent back from the command.
            StringBuilder sb = new StringBuilder();
            InputStream fp = p.getInputStream();
            byte[] buffer = new byte[512];
            while (fp.available() > 0) {
                int n = fp.read(buffer);
                sb.append(new String(buffer, 0, n));
            }
            if (options.haveIgnoreErrors()) {
                log.warning(sb.toString());
            } else {
                throw new CuraEngineException(sb.toString());
            }

        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            cleanupResources(p);
        }
    }

}
