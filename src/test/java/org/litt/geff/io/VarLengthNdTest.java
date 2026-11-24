package org.litt.geff.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.litt.geff.props.ElementIndex;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FunctionTypes.ToMaybeDoubleArrayFunction;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;
import org.litt.geff.props.Maybe.MaybeDoubleArray;
import org.litt.geff.props.Suppliers;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;

/**
 * Test that we can actually write and read a 2D var-length property. This is
 * important for node shapes like polygons and meshes.
 */
public class VarLengthNdTest
{
	// 2D polygons with 3, 5 and 2 points, flat interleaved XY.
	private static final double[][] POLY = {
			{ 0, 0, 1, 0, 1, 1 },
			{ 0, 0, 1, 0, 1, 1, 2, 1, 2, 0 },
			{ 4, 4, 5, 5 }, // 2 points in a polygon, lol, but ok.
			{ 3, 8 }, // 1 point in a polygon, lol too, but ok too.
			{} }; // what about a polygon with 0 points?

	record Node( int id, double[] flat, boolean missing )
	{}

	private static final ToLongFunction< Node > id = n -> n.id;

	private static final ToMaybeDoubleArrayFunction< Node > poly = n -> n.missing ? new MaybeDoubleArray() : new MaybeDoubleArray( n.flat );

	private static final org.litt.geff.props.FunctionTypes.ToLongArrayFunction< Node > dims = n -> new long[] { n.flat.length / 2, 2 };

	@Test
	@DisplayName( "2D var-length write: values rows are [offset, n_points, 2]; data is flat interleaved" )
	void testWriteLayout( @TempDir final Path dir ) throws IOException
	{
		final String path = dir.resolve( "layout.geff" ).toString();
		try (final N5Writer n5 = new N5ZarrWriter( path ))
		{
			new GeffPropertyWriter<>( nodes(), ElementType.NODE )
					.id( id, "<u8" )
					.add( "polygon", poly, 2, dims )
					.write( n5, null, 0 );
		}

		final N5Reader n5 = new N5ZarrReader( path );
		// N5 dims are zarr dims reversed: zarr (N, 3) -> N5 (3, N).
		final RandomAccessibleInterval< UnsignedLongType > values = N5Utils.open( n5, "nodes/props/polygon/values" );
		assertEquals( 2, values.numDimensions() );
		assertEquals( 3, values.dimension( 0 ) ); // offset + 2 shape columns
		assertEquals( nodes().size(), values.dimension( 1 ) ); // N nodes

		// Each present node's row is [offset, n_points, 2] with cumulative
		// offsets; a missing node keeps an all-zero dummy row.
		long total = 0;
		for ( int i = 0; i < nodes().size(); i++ )
		{
			final Node node = nodes().get( i );
			if ( node.missing() )
			{
				for ( int c = 0; c < 3; c++ )
					assertEquals( 0, values.randomAccess().setPositionAndGet( c, i ).get(),
							"dummy row of missing node " + i );
			}
			else
			{
				assertEquals( total, values.randomAccess().setPositionAndGet( 0, i ).get(), "offset of node " + i );
				assertEquals( node.flat().length / 2, values.randomAccess().setPositionAndGet( 1, i ).get(), "n_points of node " + i );
				assertEquals( 2, values.randomAccess().setPositionAndGet( 2, i ).get(), "n_dims of node " + i );
				total += node.flat().length;
			}
		}

		// data is the flat interleaved concatenation of all present entries.
		final RandomAccessibleInterval< DoubleType > data = N5Utils.open( n5, "nodes/props/polygon/data" );
		assertEquals( total, Intervals.numElements( data ) );
		final double[] expectedData = new double[ ( int ) total ];
		int pos = 0;
		for ( final Node node : nodes() )
			if ( !node.missing() )
				for ( final double v : node.flat() )
					expectedData[ pos++ ] = v;
		assertArrayEquals( expectedData, flat( data, 0, ( int ) total ), 1e-12 );
	}

	@Test
	@DisplayName( "2D var-length read: suppliers deliver flat interleaved arrays per node" )
	void testRead( @TempDir final Path dir ) throws IOException
	{
		final String path = dir.resolve( "read.geff" ).toString();
		write( path, nodes() );

		final N5Reader n5 = new N5ZarrReader( path );
		final ElementIndex index = new ElementIndex();
		final GeffProperty< DoubleType > polygon = IoUtils.loadVarLengthProperty( n5, "polygon", index,
				"nodes/props/polygon/values", null, "nodes/props/polygon/data" );
		final Supplier< double[] > supplier = Suppliers.asDoubleArraySupplier( polygon );
		for ( int i = 0; i < POLY.length; i++ )
		{
			index.set( i );
			assertArrayEquals( POLY[ i ], supplier.get(), 1e-12, "node " + i );
		}
	}

	@Test
	@DisplayName( "2D var-length with missing entries: missing flag set, dummy values row, present data intact" )
	void testMissing( @TempDir final Path dir ) throws IOException
	{
		final String path = dir.resolve( "missing.geff" ).toString();
		write( path, List.of( new Node( 0, POLY[ 0 ], false ), new Node( 1, POLY[ 2 ], true ), new Node( 2, POLY[ 1 ], false ) ) );

		final N5Reader n5 = new N5ZarrReader( path );
		final ElementIndex index = new ElementIndex();
		final GeffProperty< DoubleType > polygon = IoUtils.loadVarLengthProperty( n5, "polygon", index,
				"nodes/props/polygon/values", "nodes/props/polygon/missing", "nodes/props/polygon/data" );
		final Supplier< MaybeDoubleArray > supplier = Suppliers.asMaybeDoubleArraySupplier( polygon );

		index.set( 0 );
		assertArrayEquals( POLY[ 0 ], supplier.get().get(), 1e-12 );
		index.set( 1 );
		assertEquals( false, supplier.get().isPresent() );
		// dummy values row for the missing node: all zeros
		final RandomAccessibleInterval< UnsignedLongType > values = N5Utils.open( n5, "nodes/props/polygon/values" );
		for ( int c = 0; c < 3; c++ )
			assertEquals( 0, values.randomAccess().setPositionAndGet( c, 1 ).get() );
		index.set( 2 );
		assertArrayEquals( POLY[ 1 ], supplier.get().get(), 1e-12 );
	}

	@Test
	@DisplayName( "Empty graph with a 2D var-length property: write does not throw, values is zero-length" )
	void testEmpty( @TempDir final Path dir ) throws IOException
	{
		final String path = dir.resolve( "empty.geff" ).toString();
		assertDoesNotThrow( () -> write( path, List.of() ) );

		try (final N5Reader n5 = new N5ZarrReader( path ))
		{
			final ZarrDatasetAttributes attrs = ( ZarrDatasetAttributes ) n5.getDatasetAttributes( "nodes/props/polygon/values" );
			assertEquals( 2, attrs.getDimensions().length );
			assertEquals( 0, Intervals.numElements( attrs.getDimensions() ) );
		}
	}

	// ---------------------------------------------------------------------

	private static List< Node > nodes()
	{
		final int nPolys = POLY.length;
		final List< Node > nodes = new ArrayList<>( nPolys );
		for ( int i = 0; i < nPolys; i++ )
			nodes.add( new Node( i, POLY[ i ], false ) );
		// Add a missing node to test the missing flag.
		nodes.add( new Node( nPolys, new double[ 0 ], true ) );
		return nodes;
	}

	private static void write( final String path, final List< Node > nodes ) throws IOException
	{
		final GeffProperties props = new GeffPropertyWriter<>( nodes, ElementType.NODE )
				.id( id, "<u8" )
				.add( "polygon", poly, 2, dims )
				.createGeffProperties();
		try (final N5Writer n5 = new N5ZarrWriter( path ))
		{
			IoUtils.writeProperties( n5, props, null, 0, null );
		}
	}

	private static double[] flat( final RandomAccessibleInterval< DoubleType > data, final int from, final int len )
	{
		final double[] out = new double[ len ];
		for ( int i = 0; i < len; i++ )
			out[ i ] = data.randomAccess().setPositionAndGet( from + i ).get();
		return out;
	}
}
