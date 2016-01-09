/*
 * Copyright (c) 2016 Michael Meding -- All Rights Reserved.
 */
package com.partbuzz.slicer;

import java.io.IOException;

/**
 * A CURA engine exception.
 *
 * @author mike
 */
public class CuraEngineException extends IOException {

	public CuraEngineException() {
	}

	public CuraEngineException(String message) {
		super(message);
	}

	public CuraEngineException(String message, Throwable cause) {
		super(message, cause);
	}

	public CuraEngineException(Throwable cause) {
		super(cause);
	}

}
