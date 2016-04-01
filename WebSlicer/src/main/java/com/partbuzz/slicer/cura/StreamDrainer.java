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
        log.info("starting drainer thread");
        synchronized (this) {
            log.info("drainer thread  sync");

            started = true;
            notifyAll();

            byte[] buffer = new byte[512];
            try {
                boolean reading = true;
                int n;
                do {
                    n = fp.read(buffer);
                    if (n > 0) {
                        sb.append(new String(buffer, 0, n));
                    }
                } while (n >= 0);
            } catch (IOException ex) {
                Logger.getLogger("PlatformExecutor").log(Level.SEVERE, null, ex);
            }
            log.info("done with the drainer");
        }
    }
}
