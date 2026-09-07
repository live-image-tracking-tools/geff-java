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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test class for resolving axis names from display_hints and axes.
 */
public class GeffDisplayHintTest
{

	private static GeffMetadata metadata( final GeffAxis... axes )
	{
		return new GeffMetadata( Geff.VERSION, true, axes );
	}

	/**
	 * Read the .zattrs of a group with all whitespace removed, so that
	 * assertions on the JSON do not depend on formatting.
	 */
	private static String readZattrs( final String groupPath ) throws IOException
	{
		return new String( Files.readAllBytes( Paths.get( groupPath, ".zattrs" ) ) ).replaceAll( "\\s", "" );
	}

	@Test
	@DisplayName( "Test display hints take precedence over axis names and order" )
	void testDisplayHintsWin()
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "frame", GeffAxis.TYPE_TIME, "frame" ),
				new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_PIXEL ),
				new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_PIXEL ),
				new GeffAxis( "z", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_PIXEL ) );
		// Deliberately unconventional: display x as vertical and y as horizontal
		meta.setDisplayHints( new GeffDisplayHint( "y", "x", "z", "frame" ) );

		assertEquals( "y", meta.getHorizontalAxisName() );
		assertEquals( "x", meta.getVerticalAxisName() );
		assertEquals( "z", meta.getDepthAxisName() );
		assertEquals( "frame", meta.getTimeAxisName() );
	}

	@Test
	@DisplayName( "Test axes in image dimension order without display hints" )
	void testImageOrderAxesWithoutHints()
	{
		// The order used by the spec example and by the Python implementation
		final GeffMetadata meta = metadata(
				new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND ),
				new GeffAxis( "z", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );

		assertEquals( "x", meta.getHorizontalAxisName() );
		assertEquals( "y", meta.getVerticalAxisName() );
		assertEquals( "z", meta.getDepthAxisName() );
		assertEquals( "t", meta.getTimeAxisName() );
	}

	@Test
	@DisplayName( "Test axes in x, y, z order without display hints" )
	void testXyzOrderAxesWithoutHints()
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND ),
				new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "z", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );

		// Names are matched before positions, so the order does not matter here
		assertEquals( "x", meta.getHorizontalAxisName() );
		assertEquals( "y", meta.getVerticalAxisName() );
		assertEquals( "z", meta.getDepthAxisName() );
	}

	@Test
	@DisplayName( "Test suffixed axis names without display hints" )
	void testSuffixedAxisNamesWithoutHints()
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "frame", GeffAxis.TYPE_TIME, "frame" ),
				new GeffAxis( "cell_x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_PIXEL ),
				new GeffAxis( "cell_y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_PIXEL ) );

		assertEquals( "cell_x", meta.getHorizontalAxisName() );
		assertEquals( "cell_y", meta.getVerticalAxisName() );
		assertNull( meta.getDepthAxisName(), "2D dataset must not have a depth axis" );
		assertEquals( "frame", meta.getTimeAxisName() );
	}

	@Test
	@DisplayName( "Test unrecognizable axis names fall back to the axes order" )
	void testUnrecognizableAxisNamesWithoutHints()
	{
		// Nothing but the position is left to go by, so image dimension order
		// (slowest to fastest) is assumed
		final GeffMetadata meta = metadata(
				new GeffAxis( "depth", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "row", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "column", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );

		assertEquals( "column", meta.getHorizontalAxisName() );
		assertEquals( "row", meta.getVerticalAxisName() );
		assertEquals( "depth", meta.getDepthAxisName() );
		assertEquals( "t", meta.getTimeAxisName(), "no time axis declared, standard name expected" );
	}

	@Test
	@DisplayName( "Test standard names are used when no axes are declared" )
	void testNoAxes()
	{
		final GeffMetadata meta = new GeffMetadata( Geff.VERSION, true );

		assertEquals( "t", meta.getTimeAxisName() );
		assertEquals( "x", meta.getHorizontalAxisName() );
		assertEquals( "y", meta.getVerticalAxisName() );
		assertEquals( "z", meta.getDepthAxisName() );
	}

	@Test
	@DisplayName( "Test channel axes are ignored when resolving spatial axes" )
	void testChannelAxisIgnored()
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND ),
				new GeffAxis( "c", GeffAxis.TYPE_CHANNEL, "channel" ),
				new GeffAxis( "row", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "column", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );

		assertEquals( "column", meta.getHorizontalAxisName() );
		assertEquals( "row", meta.getVerticalAxisName() );
		assertNull( meta.getDepthAxisName() );
	}

	@Test
	@DisplayName( "Test display hints must reference declared axes" )
	void testDisplayHintsValidation()
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "t", GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND ),
				new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );

		assertThrows( IllegalArgumentException.class,
				() -> meta.setDisplayHints( new GeffDisplayHint( "column", "y" ) ),
				"display_horizontal naming an undeclared axis should be rejected" );
		assertThrows( IllegalArgumentException.class,
				() -> meta.setDisplayHints( new GeffDisplayHint( "x", "y", "z", null ) ),
				"display_depth naming an undeclared axis should be rejected" );
		assertThrows( IllegalArgumentException.class,
				() -> meta.setDisplayHints( new GeffDisplayHint( "x", "y", null, "frame" ) ),
				"display_time naming an undeclared axis should be rejected" );
		assertThrows( IllegalArgumentException.class,
				() -> meta.setDisplayHints( new GeffDisplayHint( "x", null ) ),
				"display_vertical is required" );
	}

	@Test
	@DisplayName( "Test display hints round trip through Zarr" )
	void testDisplayHintsRoundTrip( @TempDir Path tempDir ) throws IOException
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "frame", GeffAxis.TYPE_TIME, "frame" ),
				new GeffAxis( "depth", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "row", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "column", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );
		meta.setDisplayHints( new GeffDisplayHint( "column", "row", "depth", "frame" ) );

		final String tempPath = tempDir.toString() + "/test-display-hints.zarr/tracks";
		GeffMetadata.writeToZarr( meta, tempPath );

		// The attribute must be written with the snake_case keys of the spec
		final String zattrs = readZattrs( tempPath );
		assertTrue( zattrs.contains( "\"display_hints\":" ), zattrs );
		assertTrue( zattrs.contains( "\"display_horizontal\":\"column\"" ), zattrs );
		assertTrue( zattrs.contains( "\"display_vertical\":\"row\"" ), zattrs );
		assertTrue( zattrs.contains( "\"display_depth\":\"depth\"" ), zattrs );
		assertTrue( zattrs.contains( "\"display_time\":\"frame\"" ), zattrs );

		final GeffMetadata read = GeffMetadata.readFromZarr( tempPath );
		assertEquals( meta.getDisplayHints(), read.getDisplayHints() );
		assertEquals( "column", read.getHorizontalAxisName() );
		assertEquals( "row", read.getVerticalAxisName() );
		assertEquals( "depth", read.getDepthAxisName() );
		assertEquals( "frame", read.getTimeAxisName() );
	}

	@Test
	@DisplayName( "Test unset optional display hint fields stay null" )
	void testOptionalDisplayHintsUnset( @TempDir Path tempDir ) throws IOException
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "y", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );
		meta.setDisplayHints( new GeffDisplayHint( "x", "y" ) );

		final String tempPath = tempDir.toString() + "/test-partial-hints.zarr/tracks";
		GeffMetadata.writeToZarr( meta, tempPath );

		final String zattrs = readZattrs( tempPath );
		assertTrue( zattrs.contains( "\"display_horizontal\":\"x\"" ), zattrs );
		assertTrue( zattrs.contains( "\"display_depth\":null" ), zattrs );
		assertTrue( zattrs.contains( "\"display_time\":null" ), zattrs );

		final GeffDisplayHint read = GeffMetadata.readFromZarr( tempPath ).getDisplayHints();
		assertNotNull( read );
		assertEquals( "x", read.getDisplayHorizontal() );
		assertEquals( "y", read.getDisplayVertical() );
		assertNull( read.getDisplayDepth() );
		assertNull( read.getDisplayTime() );
	}

	@Test
	@DisplayName( "Test nodes are stored under the axes named by the display hints" )
	void testNodesUseDisplayHintedAxes( @TempDir Path tempDir ) throws IOException
	{
		final GeffMetadata meta = metadata(
				new GeffAxis( "frame", GeffAxis.TYPE_TIME, "frame" ),
				new GeffAxis( "depth", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "row", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ),
				new GeffAxis( "column", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER ) );
		meta.setDisplayHints( new GeffDisplayHint( "column", "row", "depth", "frame" ) );

		final List< GeffNode > nodes = new ArrayList<>();
		for ( int i = 0; i < 3; i++ )
		{
			final GeffNode node = new GeffNode();
			node.setId( i );
			node.setT( i );
			node.setX( i * 10.0 );
			node.setY( i * 20.0 );
			node.setZ( i * 30.0 );
			nodes.add( node );
		}

		final String tempPath = tempDir.toString() + "/test-hinted-nodes.zarr/tracks";
		GeffNode.writeToZarr( nodes, tempPath, meta );
		GeffMetadata.writeToZarr( meta, tempPath );

		// Coordinates must land in the props named by the display hints
		for ( final String propName : new String[] { "frame", "depth", "row", "column" } )
			assertTrue( Files.isDirectory( Paths.get( tempPath, "nodes", "props", propName, "values" ) ),
					"missing node property " + propName );

		final GeffMetadata readMeta = GeffMetadata.readFromZarr( tempPath );
		assertEquals( meta.getDisplayHints(), readMeta.getDisplayHints() );

		final List< GeffNode > readNodes = GeffNode.readFromZarr( tempPath, readMeta );
		assertEquals( nodes.size(), readNodes.size() );
		for ( int i = 0; i < nodes.size(); i++ )
		{
			final GeffNode expected = nodes.get( i );
			final GeffNode actual = readNodes.get( i );
			assertEquals( expected.getT(), actual.getT(), "timepoint mismatch at node " + i );
			assertEquals( expected.getX(), actual.getX(), 1e-9, "x mismatch at node " + i );
			assertEquals( expected.getY(), actual.getY(), 1e-9, "y mismatch at node " + i );
			assertEquals( expected.getZ(), actual.getZ(), 1e-9, "z mismatch at node " + i );
		}
	}
}
