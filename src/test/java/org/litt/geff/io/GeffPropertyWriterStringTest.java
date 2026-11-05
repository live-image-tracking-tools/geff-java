package org.litt.geff.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.Maybe.MaybeString;
import org.litt.geff.props.Suppliers;

/**
 * Regression test for the cursor reuse in
 * {@link org.litt.geff.props.VarLengthWriteProperty}.
 * <p>
 * Cursors snapshot the interval bounds at construction, so they may only be
 * reused across elements if the row dimensions match. When the writer harvests
 * properties with <em>varying</em> row sizes (for example strings of different
 * lengths), a stale cursor reads past the end of shorter rows (or, worse,
 * silently truncates writes on the target side).
 */
public class GeffPropertyWriterStringTest
{

	@Test
	@DisplayName( "Writer harvests strings of varying lengths correctly" )
	void testVaryingLengthStrings()
	{
		// Deliberately mixes empty, short, long, and re-growing lengths.
		final List< String > names = List.of(
				"a",
				"abcdef",
				"",
				"xy",
				"this is a much longer name",
				"z",
				"another quite long name indeed" );

		final long[] counter = { 0 };
		final org.litt.geff.props.FunctionTypes.ToStringFunction< String > nameAccessor = o -> o;
		final GeffProperties props = new GeffPropertyWriter<>( names, ElementType.NODE )
				.id( ( java.util.function.ToLongFunction< String > ) o -> counter[ 0 ]++, "<u8" )
				.add( "name", nameAccessor )
				.createGeffProperties();

		final Supplier< MaybeString > supplier = Suppliers.asMaybeStringSupplier( props.property( "name" ) );
		for ( int i = 0; i < names.size(); i++ )
		{
			props.property( "name" ).elementIndex().set( i );
			final MaybeString v = supplier.get();
			assertEquals( names.get( i ), v.get(), "Round-trip mismatch at element " + i );
		}
	}
}
