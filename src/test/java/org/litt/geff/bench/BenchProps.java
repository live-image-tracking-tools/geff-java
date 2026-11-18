package org.litt.geff.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.litt.geff.io.GeffPropertyWriter;
import org.litt.geff.props.ElementIndex;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FixedLengthProperty;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;
import org.litt.geff.props.Maybe.MaybeDouble;
import org.litt.geff.props.Maybe.MaybeDoubleArray;
import org.litt.geff.props.Suppliers;

import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Micro-benchmarks for the hot paths of the props package. Run with:
 *
 * <pre>
 * mvn -o test-compile
 * java -cp target/classes:target/test-classes:$(cat target/classpath.txt) \
 *   org.litt.geff.bench.BenchProps
 * </pre>
 *
 * (Build {@code target/classpath.txt} once with
 * {@code mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt}.)
 */
public class BenchProps
{

	record Node( long id, double x, double y, double z )
	{
		double[] pos()
		{
			return new double[] { x, y, z };
		}

		MaybeDouble radius()
		{
			return new MaybeDouble( x * 0.1 );
		}
	}

	public static void main( final String[] args ) throws Throwable
	{
		final int n = 1_000_000;

		// ---- benchmark 1: read path, fixed-length [3] property,
		// MaybeDoubleArray supplier
		{
			final ElementIndex elementIndex = new ElementIndex();
			final double[] data = new double[ 3 * n ];
			for ( int i = 0; i < data.length; i++ )
				data[ i ] = i;
			final boolean[] missing = new boolean[ n ];
			missing[ 5 ] = true;
			final var missingImg = new net.imglib2.img.array.ArrayImgFactory<>( new net.imglib2.type.logic.BitType() ).create( n );
			missingImg.randomAccess().setPositionAndGet( 5 ).set( true );
			final GeffProperty< DoubleType > prop = new FixedLengthProperty<>( "pos", ArrayImgs.doubles( data, 3, n ), missingImg, elementIndex );
			final Supplier< MaybeDoubleArray > s = Suppliers.asMaybeDoubleArraySupplier( prop );
			System.out.println( "B1: asMaybeDoubleArraySupplier over fixed-length [3], " + n + " elements:" );
			for ( int r = 0; r < 3; r++ ) // warmup
				runB1( s, elementIndex, n );
			for ( int r = 0; r < 5; r++ )
			{
				final long t0 = System.nanoTime();
				runB1( s, elementIndex, n );
				final long dt = System.nanoTime() - t0;
				System.out.printf( "  rep %d: %.1f ms (%.0f ns/element)%n", r, dt / 1e6, ( double ) dt / n );
			}
		}

		// ---- benchmark 2: write path, GeffPropertyWriter with var-length and
		// optional accessors
		{
			final List< Node > nodes = new ArrayList<>( n );
			for ( long i = 0; i < n; i++ )
				nodes.add( new Node( i, i * 0.5, i * 0.7, i * 0.11 ) );
			System.out.println( "B2: GeffPropertyWriter harvest, var-length pos[3] + optional radius, " + n + " elements:" );
			for ( int r = 0; r < 2; r++ ) // warmup
				runB2( nodes );
			for ( int r = 0; r < 5; r++ )
			{
				final long t0 = System.nanoTime();
				final GeffProperties props = runB2( nodes );
				final long dt = System.nanoTime() - t0;
				System.out.printf( "  rep %d: %.1f ms (%.0f ns/element), %d properties%n", r, dt / 1e6, ( double ) dt / n, props.properties().size() );
			}
		}

		// ---- benchmark 3: write path, fixed-length accessors only
		{
			final List< Node > nodes = new ArrayList<>( n );
			for ( long i = 0; i < n; i++ )
				nodes.add( new Node( i, i * 0.5, i * 0.7, i * 0.11 ) );
			System.out.println( "B3: GeffPropertyWriter harvest, fixed-length pos[3] + optional radius, " + n + " elements:" );
			for ( int r = 0; r < 2; r++ ) // warmup
				runB3( nodes );
			for ( int r = 0; r < 5; r++ )
			{
				final long t0 = System.nanoTime();
				final GeffProperties props = runB3( nodes );
				final long dt = System.nanoTime() - t0;
				System.out.printf( "  rep %d: %.1f ms (%.0f ns/element), %d properties%n", r, dt / 1e6, ( double ) dt / n, props.properties().size() );
			}
		}
	}

	private static void runB1( final Supplier< MaybeDoubleArray > s, final ElementIndex elementIndex, final int n )
	{
		long sink = 0;
		for ( int i = 0; i < n; i++ )
		{
			elementIndex.set( i );
			final MaybeDoubleArray v = s.get();
			if ( v.isPresent() && v.get() != null )
				sink += v.get().length;
		}
		if ( sink == -1 )
			System.out.println( sink );
	}

	private static GeffProperties runB2( final List< Node > nodes ) throws Throwable
	{
		return new GeffPropertyWriter<>( nodes, ElementType.NODE )
				.id( Node::id, "<u8" )
				.add( "pos", Node::pos ) // var-length (no length argument)
				.add( "radius", Node::radius ) // optional scalar
				.createGeffProperties();
	}

	private static GeffProperties runB3( final List< Node > nodes ) throws Throwable
	{
		return new GeffPropertyWriter<>( nodes, ElementType.NODE )
				.id( Node::id, "<u8" )
				.add( "pos", Node::pos, 3 ) // fixed-length
				.add( "radius", Node::radius ) // optional scalar
				.createGeffProperties();
	}
}
