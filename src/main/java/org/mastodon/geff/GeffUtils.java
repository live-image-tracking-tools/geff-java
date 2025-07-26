package org.mastodon.geff;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.blocks.BlockInterval;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

// TODO: split good parts into GeffN5Utils, move questionable parts to ZarrUtils
public class GeffUtils
{
	private static final Logger LOG = LoggerFactory.getLogger( GeffUtils.class );



	public static < T > void writeIntArray(
			final List< T > elements,
			final ToIntFunction< T > extractor,
			final N5Writer writer,
			final String dataset,
			final int chunkSize )
	{
		final int size = elements.size();
		final int[] data = new int[ size ];
		Arrays.setAll(data, i -> extractor.applyAsInt(elements.get(i)));
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { size },
				new int[] { chunkSize },
				DataType.INT32,
				new BloscCompression() );
		writer.createDataset(dataset, attributes);
		write( data, writer, dataset, attributes );
	}

	public static < T > void writeIntMatrix(
			final List< T > elements,
			final int numColumns,
			final Function< T, int[] > extractor,
			final N5Writer writer,
			final String dataset,
			final int chunkSize )
	{
		final int size = elements.size();
		final int[] data = new int[ numColumns * size ];
		for ( int i = 0; i < size; ++i ) {
			final int[] row = extractor.apply( elements.get( i ) );
			System.arraycopy( row, 0, data, numColumns * i, numColumns );
		}
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { numColumns, size },
				new int[] { numColumns, chunkSize },
				DataType.INT32,
				new BloscCompression() );
		writer.createDataset(dataset, attributes);
		write( data, writer, dataset, attributes );
	}

	public static < T > void writeDoubleArray(
			final List< T > elements,
			final ToDoubleFunction< T > extractor,
			final N5Writer writer,
			final String dataset,
			final int chunkSize )
	{
		final int size = elements.size();
		final double[] data = new double[ size ];
		Arrays.setAll(data, i -> extractor.applyAsDouble(elements.get(i)));
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { size },
				new int[] { chunkSize },
				DataType.FLOAT64,
				new BloscCompression() );
		writer.createDataset(dataset, attributes);
		write( data, writer, dataset, attributes );
	}

	public static < T > void writeDoubleMatrix(
			final List< T > elements,
			final int numColumns,
			final Function< T, double[] > extractor,
			final N5Writer writer,
			final String dataset,
			final int chunkSize )
	{
		final int size = elements.size();
		final double[] data = new double[ numColumns * size ];
		for ( int i = 0; i < size; ++i ) {
			final double[] row = extractor.apply( elements.get( i ) );
			System.arraycopy( row, 0, data, numColumns * i, numColumns );
		}
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { numColumns, size },
				new int[] { numColumns, chunkSize },
				DataType.FLOAT64,
				new BloscCompression() );
		writer.createDataset(dataset, attributes);
		write( data, writer, dataset, attributes );
	}







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

	public static class FlattenedDoubles
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

		double at( final int i0, final int i1 )
		{
			assert size.length == 2;
			return data[ i0 + size[ 0 ] * i1 ];
		}

		// TODO: remove until needed
		double at( final int i0, final int i1, final int i2 )
		{
			assert size.length == 3;
			return data[ i0 + size[ 0 ] * ( i1 * i2 * size[ 1 ] ) ];
		}

		double[] rowAt( final int i1 )
		{
			assert size.length == 2;
			final double[] row = new double[ size[ 0 ] ];
			Arrays.setAll( row, i0 -> at( i0, i1 ) );
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

	public static class FlattenedInts
	{
		private final int[] data;

		private final int[] size;

		FlattenedInts( final int[] data, final int[] size )
		{
			this.data = data;
			this.size = size;
		}

		FlattenedInts( final int[] data, final long[] size )
		{
			this( data, Util.long2int( size ) );
		}

		int[] size()
		{
			return size;
		}

		int at( final int i0, final int i1 )
		{
			assert size.length == 2;
			return data[ i0 + size[ 0 ] * i1 ];
		}

		int[] rowAt( final int i0 )
		{
			assert size.length == 2;
			final int[] row = new int[ size[ 1 ] ];
			Arrays.setAll( row, i1 -> at( i0, i1 ) );
			return row;
		}
	}

	public static FlattenedInts readAsIntMatrix( final N5Reader reader, final String dataset, final String description )
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
		return new FlattenedInts( convertToIntArray( readFully( reader, dataset ), description ), attributes.getDimensions() );
	}


	public static int[] convertToIntArray( final Object array, final String fieldName )
	{
		if ( array instanceof int[] )
			return ( int[] ) array;
		else if ( array instanceof long[] )
			return copyToIntArray( ( long[] ) array, a -> a.length, ( a, i ) -> ( int ) a[ i ] );
		else if ( array instanceof double[] )
			return copyToIntArray( ( double[] ) array, a -> a.length, ( a, i ) -> ( int ) a[ i ] );
		else if ( array instanceof float[] )
			return copyToIntArray( ( float[] ) array, a -> a.length, ( a, i ) -> ( int ) a[ i ] );
		else
			throw new IllegalArgumentException(
					"Unsupported data type for " + fieldName + ": " +
							( array != null ? array.getClass().getName() : "null" ) );
	}

	@FunctionalInterface
	private interface IntValueAtIndex< T >
	{
		int apply( T array, int index );
	}

	private static < T > int[] copyToIntArray( final T array, final ToIntFunction< T > numElements, final IntValueAtIndex< T > elementAtIndex )
	{
		final int[] ints = new int[ numElements.applyAsInt( array ) ];
		Arrays.setAll( ints, i -> elementAtIndex.apply( array, i ) );
		return ints;
	}

	public static double[] convertToDoubleArray( final Object array, final String fieldName )
	{
		if ( array instanceof double[] )
			return ( double[] ) array;
		else if ( array instanceof int[] )
			return copyToDoubleArray( ( int[] ) array, a -> a.length, ( a, i ) -> a[ i ] );
		else if ( array instanceof long[] )
			return copyToDoubleArray( ( long[] ) array, a -> a.length, ( a, i ) -> a[ i ] );
		else if ( array instanceof float[] )
			return copyToDoubleArray( ( float[] ) array, a -> a.length, ( a, i ) -> a[ i ] );
		else
			throw new IllegalArgumentException(
					"Unsupported data type for " + fieldName + ": " +
							( array != null ? array.getClass().getName() : "null" ) );
	}

	@FunctionalInterface
	private interface DoubleValueAtIndex< T >
	{
		double apply( T array, int index );
	}

	private static < T > double[] copyToDoubleArray( final T array, final ToIntFunction< T > numElements, final DoubleValueAtIndex< T > elementAtIndex )
	{
		final double[] doubles = new double[ numElements.applyAsInt( array ) ];
		Arrays.setAll( doubles, i -> elementAtIndex.apply( array, i ) );
		return doubles;
	}

	public static void verifyLength( final int[] array, final int expectedLength, final String name )
	{
		if ( array != null && array.length != expectedLength )
			throw new IllegalArgumentException( "property " + name + " does not have expected length (" + array.length + " vs " + expectedLength + ")" );
	}

	public static void verifyLength( final double[] array, final int expectedLength, final String name )
	{
		if ( array != null && array.length != expectedLength )
			throw new IllegalArgumentException( "property " + name + " does not have expected length (" + array.length + " vs " + expectedLength + ")" );
	}

	public static void verifyLength( final FlattenedDoubles array, final int expectedLength, final String name )
	{
		if ( array != null && array.size()[ array.size().length - 1 ] != expectedLength )
		{
			throw new IllegalArgumentException( "property " + name + " does not have expected length (" + array.size()[ array.size().length - 1 ] + " vs " + expectedLength + ")" );
		}
	}

	public static void verifyLength( final FlattenedInts array, final int expectedLength, final String name )
	{
		if ( array != null && array.size()[ array.size().length - 1 ] != expectedLength )
		{
			throw new IllegalArgumentException( "property " + name + " does not have expected length (" + array.size()[ array.size().length - 1 ] + " vs " + expectedLength + ")" );
		}
	}


	// -- write dataset fully --

	public static void write(
			final Object src,
			final N5Writer writer,
			final String dataset,
			final DatasetAttributes attributes )
	{
		final int[] blockSize = attributes.getBlockSize();
		final long[] size = attributes.getDimensions();
		final int n = attributes.getNumDimensions();
		final DataType dataType = attributes.getDataType();

		final CellGrid grid = new CellGrid( size, blockSize );

		final int[] srcSize = Util.long2int( size );
		final long[] srcPos = new long[ n ];
		final int[] destSize = new int[ n ];
		final int[] destPos = new int[ n ];

		final long[] gridPos = new long[ n ];
		final IntervalIterator gridIter = new IntervalIterator( grid.getGridDimensions() );
		while ( gridIter.hasNext() )
		{
			gridIter.fwd();
			gridIter.localize( gridPos );
			grid.getCellDimensions( gridPos, srcPos, destSize );
			final DataBlock< ? > block = dataType.createDataBlock( destSize, gridPos );
			SubArrayCopy.copy( src, srcSize, Util.long2int( srcPos ), block.getData(), destSize, destPos, destSize );
			writer.writeBlock( dataset, attributes, block );
		}
	}



	// -- read dataset fully --

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
		final long[] gridSize = new long[ n ];
		for ( int d = 0; d < n; d++ )
		{
			gridMin[ d ] = destInterval.min( d ) / blockSize[ d ];
			final long gridMax = destInterval.max( d ) / blockSize[ d ];
			gridSize[ d ] = gridMax + 1 - gridMin[ d ];
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
