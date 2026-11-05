/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 - 2026 Ko Sugawara, Jean-Yves Tinevez
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
package org.litt.geff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;

/**
 * Round-trip tests for {@link GeffMetadata}: write metadata to a Zarr
 * container, read it back, and check that every component survives.
 */
public class GeffMetadataRoundTripTest
{

	/**
	 * Read the .zattrs of a group and parse it into a map, so that assertions
	 * can be made on the raw JSON content.
	 */
	@SuppressWarnings( "unchecked" )
	private static Map< String, Object > readZattrs( final String groupPath ) throws IOException
	{
		final String json = new String( Files.readAllBytes( Path.of( groupPath, ".zattrs" ) ) );
		return new Gson().fromJson( json, Map.class );
	}

	@Test
	@DisplayName( "Round trip all metadata components through Zarr" )
	void testFullRoundTrip( @TempDir final Path tempDir ) throws IOException
	{
		final String path = tempDir.resolve( "full.zarr" ).toString();

		final GeffMetadata metadata = new GeffMetadata( "1.3", true );
		metadata.setGeffAxes( List.of(
				GeffAxis.createTimeAxis( "t", GeffAxis.UNIT_SECOND, 0.0, 10.0 ),
				GeffAxis.createSpaceAxis( "x", GeffAxis.UNIT_MICROMETER, 0.0, 100.0 ),
				GeffAxis.createSpaceAxis( "y", GeffAxis.UNIT_MICROMETER, 0.0, 100.0 ) ) );
		final GeffAxis tAxis = metadata.getGeffAxes().get( 0 );
		tAxis.setScale( 0.5 );
		tAxis.setScaledUnit( "s" );
		tAxis.setOffset( 2.0 );

		metadata.setNodePropsMetadata( Map.of(
				"x", new PropMetadata( "x", "float64", false, GeffAxis.UNIT_MICROMETER, null, null ),
				"seg_id", new PropMetadata( "seg_id", "uint16", false, null, null, null ) ) );
		metadata.setEdgePropsMetadata( new HashMap<>() );
		metadata.setTrackNodeProps( Map.of( "lineage", "track_id", "tracklet", "tracklet_id" ) );
		metadata.setSphere( "radius" );
		metadata.setEllipsoid( "covariance" );
		metadata.setPolygon( "outline" );
		metadata.setMesh( new MeshMetadata( "mesh_vertices", "mesh_triangles", "mesh_vertex_normals", null ) );
		metadata.setDisplayHints( new GeffDisplayHints( "x", "y", null, "t" ) );
		metadata.setRelatedObjects( new RelatedObjects()
				.image( "../raw/" )
				.labels( "../segmentation/", "seg_id" )
				.geff( "../subgraph" ) );
		metadata.setExtra( Map.of( "source", "TrackMate" ) );

		GeffMetadata.writeToZarr( metadata, path );
		final GeffMetadata read = GeffMetadata.readFromZarr( path );

		assertEquals( metadata, read );
		assertEquals( 0.5, read.getGeffAxes().get( 0 ).getScale(), 1e-12 );
		assertEquals( "s", read.getGeffAxes().get( 0 ).getScaledUnit() );
		assertEquals( 2.0, read.getGeffAxes().get( 0 ).getOffset(), 1e-12 );
		assertEquals( "covariance", read.getEllipsoid() );
		assertEquals( "outline", read.getPolygon() );

		final RelatedObjects related = read.getRelatedObjects();
		assertNotNull( related );
		assertEquals( 3, related.size() );
		assertEquals( "seg_id", related.getLabels().get( 0 ).getNodeProp() );
		assertEquals( List.of( "../raw/" ), related.getImagePaths() );
		assertEquals( 1, related.getGeffObjects().size() );

		// Raw JSON content checks
		final Map< String, Object > zattrs = readZattrs( path );
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > geff = ( Map< String, Object > ) zattrs.get( "geff" );
		assertEquals( List.of( "geff_version", "directed", "axes", "node_props_metadata", "edge_props_metadata",
				"track_node_props", "sphere", "ellipsoid", "polygon", "mesh", "display_hints", "related_objects", "extra" ),
				List.copyOf( geff.keySet() ), "keys should be written in specification order" );
		assertTrue( geff.containsKey( "ellipsoid" ) && geff.containsKey( "polygon" ) );
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > labelObject = ( Map< String, Object > ) ( ( List< ? > ) geff.get( "related_objects" ) ).get( 1 );
		assertEquals( "seg_id", labelObject.get( "node_prop" ), "node_prop must be written, not the deprecated label_prop" );
	}

	@Test
	@DisplayName( "Read legacy label_prop and write node_prop" )
	void testLegacyLabelProp( @TempDir final Path tempDir ) throws IOException
	{
		final String path = tempDir.resolve( "legacy.zarr" ).toString();
		GeffMetadata.writeToZarr( new GeffMetadata( "1.3", false ), path );

		// Hand-write a related_objects entry using the deprecated key.
		final Path zattrs = Path.of( path, ".zattrs" );
		final String json = new String( Files.readAllBytes( zattrs ) );
		final String withLegacy = json.replace( "}}", "},\"related_objects\":[{\"type\":\"labels\",\"path\":\"../seg/\",\"label_prop\":\"cell_id\"}]}" );
		Files.write( zattrs, withLegacy.getBytes() );

		final GeffMetadata read = GeffMetadata.readFromZarr( path );
		assertNotNull( read.getRelatedObjects() );
		assertEquals( "cell_id", read.getRelatedObjects().getLabels().get( 0 ).getNodeProp(),
				"the deprecated label_prop key must be read as node_prop" );

		// Re-writing must emit node_prop.
		final String out = tempDir.resolve( "rewritten.zarr" ).toString();
		GeffMetadata.writeToZarr( read, out );
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > geff = ( Map< String, Object > ) readZattrs( out ).get( "geff" );
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > labelObject = ( Map< String, Object > ) ( ( List< ? > ) geff.get( "related_objects" ) ).get( 0 );
		assertEquals( "cell_id", labelObject.get( "node_prop" ) );
		assertTrue( !labelObject.containsKey( "label_prop" ), "the deprecated label_prop key must not be written" );
	}

	@SuppressWarnings( "unchecked" )
	@Test
	@DisplayName( "Unknown geff attributes round-trip losslessly" )
	void testUnknownAttributes( @TempDir final Path tempDir ) throws IOException
	{
		final String path = tempDir.resolve( "unknown.zarr" ).toString();
		GeffMetadata.writeToZarr( new GeffMetadata( "1.3", true ), path );

		// Hand-write an attribute that this version of the library does not
		// know about, as a future version of the spec would.
		final Path zattrs = Path.of( path, ".zattrs" );
		final String json = new String( Files.readAllBytes( zattrs ) );
		final String withUnknown = json.replace( "}}",
				"},\"future_thing\":{\"a\":[1,2,3],\"b\":\"yes\"},\"another\":42}" );
		Files.write( zattrs, withUnknown.getBytes() );

		final GeffMetadata read = GeffMetadata.readFromZarr( path );
		assertEquals( 2, read.getUnknownAttributes().size() );
		final Map< String, Object > future = ( Map< String, Object > ) read.getUnknownAttributes().get( "future_thing" );
		assertEquals( List.of( 1.0, 2.0, 3.0 ), future.get( "a" ) );
		assertEquals( 42.0, read.getUnknownAttributes().get( "another" ) );

		// And they must be written back unchanged.
		final String out = tempDir.resolve( "rewritten.zarr" ).toString();
		GeffMetadata.writeToZarr( read, out );
		final Map< String, Object > geff = ( Map< String, Object > ) readZattrs( out ).get( "geff" );
		assertEquals( List.of( 1.0, 2.0, 3.0 ), ( ( Map< String, Object > ) geff.get( "future_thing" ) ).get( "a" ) );
		assertEquals( 42.0, geff.get( "another" ) );
	}

	@Test
	@DisplayName( "Null optional attributes are reported as absent" )
	void testNullOptionalAttributes( @TempDir final Path tempDir ) throws IOException
	{
		final String path = tempDir.resolve( "nulls.zarr" ).toString();
		GeffMetadata.writeToZarr( new GeffMetadata( "1.3", true ), path );

		// Hand-write explicit nulls, as the Python implementation does for
		// optional fields it leaves unset.
		final Path zattrs = Path.of( path, ".zattrs" );
		final String json = new String( Files.readAllBytes( zattrs ) );
		final String withNulls = json.replace( "}}", "},\"axes\":null,\"sphere\":null}" );
		Files.write( zattrs, withNulls.getBytes() );

		final GeffMetadata read = GeffMetadata.readFromZarr( path );
		assertNull( read.getGeffAxes() );
		assertNull( read.getSphere() );
	}

	@Test
	@DisplayName( "Display hints referencing undeclared axes are rejected on read" )
	void testInvalidDisplayHintsOnRead( @TempDir final Path tempDir ) throws IOException
	{
		final String path = tempDir.resolve( "badhints.zarr" ).toString();
		final GeffMetadata metadata = new GeffMetadata( "1.3", true );
		metadata.setGeffAxes( List.of(
				GeffAxis.createSpaceAxis( "x", GeffAxis.UNIT_MICROMETER ),
				GeffAxis.createSpaceAxis( "y", GeffAxis.UNIT_MICROMETER ) ) );
		GeffMetadata.writeToZarr( metadata, path );

		final Path zattrs = Path.of( path, ".zattrs" );
		final String json = new String( Files.readAllBytes( zattrs ) );
		final String withHints = json.replace( "}}", "},\"display_hints\":{\"display_horizontal\":\"nope\",\"display_vertical\":\"nada\"}}" );
		Files.write( zattrs, withHints.getBytes() );

		assertThrows( IllegalArgumentException.class, () -> GeffMetadata.readFromZarr( path ),
				"display hints naming undeclared axes must be rejected" );
	}
}
