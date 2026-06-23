/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 - 2026 Ko Sugawara
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.geff;

import static org.junit.Assert.*;

import java.io.File;

import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for varlength property writing functionality
 */
public class VarlengthPropertyWriteTest
{
	private static final String TEST_OUTPUT_DIR = "target/test-varlength-write-output";

	@Before
	public void setUp() throws Exception
	{
		final File outDir = new File( TEST_OUTPUT_DIR );
		if ( outDir.exists() )
		{
			deleteRecursively( outDir );
		}
		outDir.mkdirs();
	}

	@After
	public void tearDown() throws Exception
	{
		final File outDir = new File( TEST_OUTPUT_DIR );
		if ( outDir.exists() )
		{
			deleteRecursively( outDir );
		}
	}

	private static void deleteRecursively( final File file ) throws Exception
	{
		if ( file.isDirectory() )
		{
			for ( final File child : file.listFiles() )
			{
				deleteRecursively( child );
			}
		}
		file.delete();
	}

	@Test
	public void testWriteSingleNode() throws Exception
	{
		final String zarrPath = TEST_OUTPUT_DIR + "/single_node.zarr";
		final Object[][] nodeData = new Object[ 1 ][];
		nodeData[ 0 ] = new Object[] { 1.0, 2.0, 3.0 };
		final boolean[] missing = { false };

		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ))
		{
			GeffUtils.writeVarlengthProperty( writer, "/props/test", nodeData, missing, 1000 );
		}

		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath ))
		{
			assertTrue( "Data should exist", reader.datasetExists( "/props/test/data" ) );
			assertTrue( "Values should exist", reader.datasetExists( "/props/test/values" ) );
			final double[] data = ( double[] ) GeffUtils.readFully( reader, "/props/test/data" );
			assertEquals( "Data length should be 3", 3, data.length );
		}
	}

	@Test
	public void testWriteMultipleNodes() throws Exception
	{
		final String zarrPath = TEST_OUTPUT_DIR + "/multi_nodes.zarr";
		final Object[][] nodeData = new Object[ 3 ][];
		nodeData[ 0 ] = new Object[] { 1.0, 2.0, 3.0 };
		nodeData[ 1 ] = new Object[] { 4.0, 5.0 };
		nodeData[ 2 ] = new Object[] { 6.0, 7.0, 8.0, 9.0 };
		final boolean[] missing = { false, false, false };

		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ))
		{
			GeffUtils.writeVarlengthProperty( writer, "/props/data", nodeData, missing, 1000 );
		}

		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath ))
		{
			final double[] data = ( double[] ) GeffUtils.readFully( reader, "/props/data/data" );
			assertEquals( "Total data should be 9 elements", 9, data.length );
		}
	}

	@Test
	public void testWriteWithMissing() throws Exception
	{
		final String zarrPath = TEST_OUTPUT_DIR + "/with_missing.zarr";
		final Object[][] nodeData = new Object[ 2 ][];
		nodeData[ 0 ] = new Object[] { 1.0, 2.0 };
		nodeData[ 1 ] = null;
		final boolean[] missing = { false, true };

		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ))
		{
			GeffUtils.writeVarlengthProperty( writer, "/props/test", nodeData, missing, 1000 );
		}

		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath ))
		{
			assertTrue( "Missing array should exist", reader.datasetExists( "/props/test/missing" ) );
			final byte[] missingArray = ( byte[] ) GeffUtils.readFully( reader, "/props/test/missing" );
			assertEquals( "First should not be missing", 0, missingArray[ 0 ] );
			assertEquals( "Second should be missing", 1, missingArray[ 1 ] );
		}
	}

	@Test
	public void testRoundTrip() throws Exception
	{
		final String zarrPath = TEST_OUTPUT_DIR + "/roundtrip.zarr";
		final Object[][] nodeData = new Object[ 2 ][];
		nodeData[ 0 ] = new Object[] { 1.5, 2.5 };
		nodeData[ 1 ] = new Object[] { 3.5 };
		final boolean[] missing = { false, false };
		final PropMetadata metadata = new PropMetadata( "test", "float64", true, null, null, null );

		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ))
		{
			GeffUtils.writeVarlengthProperty( writer, "/props/test", nodeData, missing, 1000 );
		}

		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath ))
		{
			final VarlengthProperty prop = GeffUtils.readVarlengthProperty( reader, "/props/test", 2, metadata );
			assertNotNull( "Property should not be null", prop );
			assertFalse( "Node 0 should not be missing", prop.isMissing( 0 ) );
			assertFalse( "Node 1 should not be missing", prop.isMissing( 1 ) );
		}
	}

	@Test
	public void testWriteIntArray() throws Exception
	{
		final String zarrPath = TEST_OUTPUT_DIR + "/int_array.zarr";
		final Object[][] nodeData = new Object[ 2 ][];
		nodeData[ 0 ] = new Object[] { 10, 20, 30 };
		nodeData[ 1 ] = new Object[] { 40 };
		final boolean[] missing = { false, false };

		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ))
		{
			GeffUtils.writeVarlengthProperty( writer, "/props/indices", nodeData, missing, 1000 );
		}

		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath ))
		{
			final int[] data = ( int[] ) GeffUtils.readFully( reader, "/props/indices/data" );
			assertEquals( "Total should be 4", 4, data.length );
		}
	}

}
