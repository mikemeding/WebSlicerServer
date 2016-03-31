package com.partbuzz.slicer.cura;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drain an input stream into a buffer.
 */
class StreamDrainer implements Runnable {

    private static final Logger log = Logger.getLogger(StreamDrainer.class.getName());
    private final InputStream fp;
    private final StringBuilder sb;
    private boolean started;

    public StreamDrainer(InputStream fp) {
        this.fp = fp;
        this.sb = new StringBuilder();
        this.started = false;
    }

    public String getText() {
        return sb.toString();
    }

    boolean hasStarted() {
        return started;
    }

    @Override
    public void run() {

        // tell the invoker that we are draining
        synchronized (this) {

            byte[] buffer = new byte[512];
            try {
                while (fp.available() > 0) {
                    log.info("thread available!");
                    started = true;
                    notifyAll();
                    int n = fp.read(buffer);
                    sb.append(new String(buffer, 0, n));
                }
            } catch (IOException ex) {
                Logger.getLogger("PlatformExecutor").log(Level.SEVERE, null, ex);
            }
        }
    }
}
