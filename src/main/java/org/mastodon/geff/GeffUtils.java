package org.mastodon.geff;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.blocks.BlockInterval;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

// TODO: split good parts into GeffN5Utils, move questionable parts to ZarrUtils
public class GeffUtils
{
	private static final Logger LOG = LoggerFactory.getLogger( GeffUtils.class );

	public static int[] readAsIntArray( final N5Reader reader, final String dataset, final String description )
	{
		if ( !reader.datasetExists( dataset ) )
		{
			LOG.debug( "No arrays found in group for " + description );
			return null;
		}
		if ( reader.getDatasetAttributes( dataset ).getNumDimensions() != 1 )
		{
			throw new IllegalArgumentException( "Expected 1D array" );
		}
		return convertToIntArray( readFully( reader, dataset ), description );
	}

	public static double[] readAsDoubleArray( final N5Reader reader, final String dataset, final String description )
	{
		if ( !reader.datasetExists( dataset ) )
		{
			LOG.debug( "No arrays found in group for " + description );
			return null;
		}
		if ( reader.getDatasetAttributes( dataset ).getNumDimensions() != 1 )
		{
			throw new IllegalArgumentException( "Expected 1D array" );
		}
		return convertToDoubleArray( readFully( reader, dataset ), description );
	}

	static class FlattenedDoubles
	{
		private final double[] data;

		private final int[] size;

		FlattenedDoubles( final double[] data, final int[] size )
		{
			this.data = data;
			this.size = size;
		}

		FlattenedDoubles( final double[] data, final long[] size )
		{
			this( data, Util.long2int( size ) );
		}

		int[] size()
		{
			return size;
		}

		double at(final int i0)
		{
			assert size.length == 1;
			return data[ i0 ];
		}

		double at(final int i0, final int i1)
		{
			assert size.length == 2;
			return data[ i0 + size[ 0 ] * i1 ];
		}

		double at(final int i0, final int i1, final int i2)
		{
			assert size.length == 3;
			return data[ i0 + size[ 0 ] * ( i1 * i2 * size[ 1 ] ) ];
		}

		double[] rowAt(final int i0)
		{
			assert size.length == 2;
			final double[] row = new double[ size[ 1 ] ];
			Arrays.setAll( row, i1 -> at( i0, i1 ) );
			return row;
		}
	}

	public static FlattenedDoubles readAsDoubleMatrix( final N5Reader reader, final String dataset, final String description )
	{
		if ( !reader.datasetExists( dataset ) )
		{
			LOG.debug( "No arrays found in group for " + description );
			return null;
		}
		final DatasetAttributes attributes = reader.getDatasetAttributes( dataset );
		if ( attributes.getNumDimensions() != 2 )
		{
			throw new IllegalArgumentException( "Expected 2D array" );
		}
		return new FlattenedDoubles( convertToDoubleArray( readFully( reader, dataset ), description ), attributes.getDimensions() );
	}






	@FunctionalInterface
	private interface IntValueAtIndex< T >
	{
		int apply( T data, int index );
	}

	private static < T > int[] copyToIntArray( final T data, final ToIntFunction< T > numElements, final IntValueAtIndex< T > elementAtIndex )
	{
		final int[] ints = new int[ numElements.applyAsInt( data ) ];
		Arrays.setAll( ints, i -> elementAtIndex.apply( data, i ) );
		return ints;
	}

	public static int[] convertToIntArray( final Object data, final String fieldName )
	{
		if ( data instanceof int[] )
			return ( int[] ) data;
		else if ( data instanceof long[] )
			return copyToIntArray( ( long[] ) data, a -> a.length, ( a, i ) -> ( int ) a[ i ] );
		else if ( data instanceof double[] )
			return copyToIntArray( ( double[] ) data, a -> a.length, ( a, i ) -> ( int ) a[ i ] );
		else if ( data instanceof float[] )
			return copyToIntArray( ( float[] ) data, a -> a.length, ( a, i ) -> ( int ) a[ i ] );
		else
			throw new IllegalArgumentException(
					"Unsupported data type for " + fieldName + ": " +
							( data != null ? data.getClass().getName() : "null" ) );
	}


	@FunctionalInterface
	private interface DoubleValueAtIndex< T >
	{
		double apply( T data, int index );
	}

	private static < T > double[] copyToDoubleArray( final T data, final ToIntFunction< T > numElements, final DoubleValueAtIndex< T > elementAtIndex )
	{
		final double[] doubles = new double[ numElements.applyAsInt( data ) ];
		Arrays.setAll( doubles, i -> elementAtIndex.apply( data, i ) );
		return doubles;
	}

	public static double[] convertToDoubleArray( final Object data, final String fieldName )
	{
		if ( data instanceof double[] )
			return ( double[] ) data;
		else if ( data instanceof int[] )
			return copyToDoubleArray( ( int[] ) data, a -> a.length, ( a, i ) -> a[ i ] );
		else if ( data instanceof long[] )
			return copyToDoubleArray( ( long[] ) data, a -> a.length, ( a, i ) -> a[ i ] );
		else if ( data instanceof float[] )
			return copyToDoubleArray( ( float[] ) data, a -> a.length, ( a, i ) -> a[ i ] );
		else
			throw new IllegalArgumentException(
					"Unsupported data type for " + fieldName + ": " +
							( data != null ? data.getClass().getName() : "null" ) );
	}










	public static Object readFully( final N5Reader reader, final String dataset )
	{
		final DatasetAttributes attributes = reader.getDatasetAttributes( dataset );
		final DataType dataType = attributes.getDataType();
		final int numElements = Util.safeInt( Intervals.numElements( attributes.getDimensions() ) );
		final Object dest = createArray( dataType ).apply( numElements );
		copy( dest, new FinalInterval( attributes.getDimensions() ), reader, dataset );
		return dest;
	}

	private static IntFunction< ? > createArray( final DataType dataType )
	{
		switch ( dataType )
		{
		case INT8:
		case UINT8:
			return byte[]::new;
		case INT16:
		case UINT16:
			return short[]::new;
		case INT32:
		case UINT32:
			return int[]::new;
		case INT64:
		case UINT64:
			return long[]::new;
		case FLOAT32:
			return float[]::new;
		case FLOAT64:
			return double[]::new;
		case STRING:
			return String[]::new;
		case OBJECT:
			return Object[]::new;
		}
		throw new IllegalArgumentException();
	}

	private static void copy(
			final Object dest,
			final Interval destInterval,
			final N5Reader reader,
			final String dataset )
	{
		final DatasetAttributes attributes = reader.getDatasetAttributes( dataset );
		final int[] blockSize = attributes.getBlockSize();
		final int n = attributes.getNumDimensions();

		final long[] gridMin = new long[ n ];
		final long[] gridMax = new long[ n ];
		final long[] gridSize = new long[ n ];
		for ( int d = 0; d < n; d++ )
		{
			gridMin[ d ] = destInterval.min( d ) / blockSize[ d ];
			gridMax[ d ] = destInterval.max( d ) / blockSize[ d ];
			gridSize[ d ] = gridMax[ d ] + 1 - gridMin[ d ];
		}

		final long[] gridPos = new long[ n ];
		final long[] blockMin = new long[ n ];
		final int[] srcPos = new int[ n ];
		final int[] destSize = Util.long2int( destInterval.dimensionsAsLongArray() );
		final int[] destPos = new int[ n ];
		final IntervalIterator gridIter = new IntervalIterator( gridSize );
		while ( gridIter.hasNext() )
		{
			gridIter.fwd();
			gridIter.localize( gridPos );
			Arrays.setAll( blockMin, d -> gridPos[ d ] * blockSize[ d ] );
			final DataBlock< ? > block = reader.readBlock( dataset, attributes, gridPos );
			final BlockInterval blockInterval = BlockInterval.wrap( blockMin, block.getSize() );
			final FinalInterval intersection = Intervals.intersect( blockInterval, destInterval );
			Arrays.setAll( srcPos, d -> ( int ) ( intersection.min( d ) - blockMin[ d ] ) );
			Arrays.setAll( destPos, d -> ( int ) ( intersection.min( d ) - destInterval.min( d ) ) );
			SubArrayCopy.copy( block.getData(), blockInterval.size(), srcPos, dest, destSize, destPos, Util.long2int( intersection.dimensionsAsLongArray() ) );
		}
	}
}
