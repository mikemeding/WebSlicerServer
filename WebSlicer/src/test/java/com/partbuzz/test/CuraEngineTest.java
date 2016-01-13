/*
 * Copyright (c) 2016 Michael Meding -- All Rights Reserved.
 */
package com.partbuzz.test;

import java.io.IOException;

import com.partbuzz.slicer.CuraEngine;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mike
 */
public class CuraEngineTest {

	public CuraEngineTest() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testFluidity() {

		try {
			//TODO: this argument list needs to match the one in build_env/build_tests/test.sh
			CuraEngine ce = new CuraEngine();
			ce.options()
					.verbose()
					.minusG()
					.settingsFilename("settings.json")
					.outputFilename("output.file")
					.modelFilename("model.stl");

			ce.execute();
		} catch (IOException e) {
			fail("cura failed "+e.getMessage());
		}
	}
}
