/*
 * Copyright (c) 2016 Michael Meding -- All Right Reserved.
 */
package com.partbuzz.slicer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * See if an executable is present and runnable.
 *
 * @author mike
 */
public abstract class PlatformExecutable {

	/**
	 * Check if a particular executable exists
	 *
	 * @param path is the path
	 */
	protected static void checkPlatformExecutable(String path) {
		File file = new File(path);
		if (!(file.exists() && file.canExecute())) {
			throw new UnsupportedOperationException(path + " is not supported on this platform");
		}
	}

	/**
	 * See if a port is in the valid range 0-65535
	 *
	 * @param port the port number
	 */
	protected static void checkPortNumberLegal(int port) {
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("TCP/IP Port " + port + " illegal");
		}
	}

	/**
	 * Gather all the output a command may have sent back. Used if a system
	 * command did not finish successfully.
	 *
	 * @param p is the process
	 * @return the string
	 */
	protected static String gatherCommandOutput(Process p) throws IOException {
		StringBuilder sb = new StringBuilder();
		InputStream fp = p.getInputStream();
		byte[] buffer = new byte[512];
		while (fp.available() > 0) {
			int n = fp.read(buffer);
			sb.append(new String(buffer, 0, n));
		}

		return sb.toString();
	}

	/**
	 * Cleanup the external process resources
	 *
	 * @param p is the process
	 */
	protected static void cleanupResources(Process p) {
		if (p == null) {
			return;
		}

		close(p.getOutputStream());
		close(p.getInputStream());
		close(p.getErrorStream());
		p.destroy();
	}

	private static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable t) {
				// ignore
			}
		}
	}
}
