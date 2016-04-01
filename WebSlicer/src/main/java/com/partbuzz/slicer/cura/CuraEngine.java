/*
 * Copyright (c) 2016 Michael Meding -- All Rights Reserved.
 */
package com.partbuzz.slicer.cura;

import com.partbuzz.slicer.util.CuraEngineException;
import com.partbuzz.slicer.util.PlatformExecutable;

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
    private static final String CURAENGINE_PROG = "/home/mike/Documents/WebSlicerServer/CuraEngine-master/build/CuraEngine"; //TODO: relative path???
    private final CuraEngineOptions options;

    public CuraEngine() {
        this.options = new CuraEngineOptions();
    }

    public CuraEngineOptions options() {
        return options;
    }

    public String output = "";

    /**
     * Setup a stream drainer.
     *
     * @param fp the input stream
     * @return the stream drainer
     * @throws java.io.IOException
     */
    protected static StreamDrainer setupStreamDrain(InputStream fp) throws IOException {
        final StreamDrainer drainer = new StreamDrainer(fp);
        // wait until we are draining
        synchronized (drainer) {

            log.info("starting thread");
            Thread t = new Thread(drainer);
            t.setDaemon(true);
            t.start();

            while (!drainer.hasStarted()) {
                try {
                    log.info("waiting for drainer thread");
                    drainer.wait(500);
                    log.info("drainer wait expired");
                } catch (InterruptedException ex) {
                    Thread.interrupted();
                    throw new IOException("Unable to setup stream drainer");
                }
            }

            log.info("drainer started");
        }

        return drainer;
    }

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

            // Setup drains for both output and error streams
            StreamDrainer stdout = setupStreamDrain(p.getInputStream());
//            StreamDrainer stderr = setupStreamDrain(p.getErrorStream());

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
                log.info("STDOUT:" + stdout.getText());
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
