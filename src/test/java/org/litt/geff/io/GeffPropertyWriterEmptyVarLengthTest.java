package org.litt.geff.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.litt.geff.GeffMetadata;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.Maybe.MaybeString;
import org.litt.geff.props.Suppliers;

/**
 * Test writing empty properties.
 */
public class GeffPropertyWriterEmptyVarLengthTest
{

	@Test
	@DisplayName( "Writing an all-empty var-length string property does not throw" )
	void testAllEmptyVarLengthStrings( @TempDir final Path tempDir ) throws IOException, org.litt.geff.GeffException
	{
		// Every name is the empty string: the var-length store stays empty.
		final List< String > names = List.of( "", "", "", "" );

		final long[] counter = { 0 };
		final org.litt.geff.props.FunctionTypes.ToStringFunction< String > nameAccessor = o -> o;
		final GeffProperties props = new GeffPropertyWriter<>( names, ElementType.NODE )
				.id( ( java.util.function.ToLongFunction< String > ) o -> counter[ 0 ]++, "<u8" )
				.add( "name", nameAccessor )
				.createGeffProperties();

		final String geffPath = tempDir.resolve( "empty_names.geff" ).toString();
		try (final N5Writer n5 = new N5ZarrWriter( geffPath ))
		{
			assertDoesNotThrow( () -> IoUtils.writeProperties( n5, props, null, 0, null ) );
		}

		// The metadata must still be writable (and the reader must find it).
		// Property discovery on read is driven by the node_props_metadata
		// attribute, which the writer does not write: it must be registered
		// here, as a client would.
		final GeffMetadata metadata = new GeffMetadata( "1.3", false );
		metadata.setNodePropsMetadata( java.util.Map.of( "name",
				new org.litt.geff.PropMetadata( "name", "uint8", true, null, null, null ) ) );
		GeffMetadata.writeToZarr( metadata, geffPath );
		final GeffMetadata read = GeffMetadata.readFromZarr( geffPath );
		assertEquals( "1.3", read.getGeffVersion() );

		// The values must round-trip as empty strings.
		final GeffProperties readProps = IoUtils.loadProperties( new org.janelia.saalfeldlab.n5.zarr.N5ZarrReader( geffPath, true ), ElementType.NODE );
		final Supplier< MaybeString > supplier = Suppliers.asMaybeStringSupplier( readProps.property( "name" ) );
		for ( int i = 0; i < names.size(); i++ )
		{
			readProps.property( "name" ).elementIndex().set( i );
			assertEquals( "", supplier.get().get(), "Round-trip mismatch at element " + i );
		}
	}
}
