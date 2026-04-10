package org.mastodon.geff;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Exception;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.blocks.BlockInterval;
import net.imglib2.blocks.SubArrayCopy;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.iterator.IntervalIterator;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class GeffUtils
{
	private static final Logger LOG = LoggerFactory.getLogger( GeffUtils.class );

	private static final Pattern VERSION_PATTERN = Pattern
			.compile( "^\\d+\\.\\d+(?:\\.\\d+)?(?:\\.dev\\d+)?(?:[.-][a-zA-Z0-9-]+(?:[.-][a-zA-Z0-9-]+)*)?(?:\\+[a-zA-Z0-9.-]+)?$" );

	public static void checkSupportedVersion( final String version ) throws IllegalArgumentException
	{
		if ( !VERSION_PATTERN.matcher( version ).matches() )
		{ throw new IllegalArgumentException( "geff_version " + version + " does not match semver pattern." ); }
	}

	/**
	 * Check if a property should be skipped based on metadata. Returns true if
	 * the property should be skipped due to: - String/bytes data type
	 * 
	 * NOTE: Variable-length properties are now supported and are NOT skipped.
	 * 
	 * Logs appropriate warnings for each case.
	 */
	public static boolean shouldSkipProperty( final String propName, final PropMetadata metadata )
	{
		if ( metadata == null )
		{ return false; }

		// Variable-length properties are now supported - don't skip!
		// They are handled by readVarlengthProperty()

		// Check for string/bytes properties
		if ( metadata.getDtype() != null )
		{
			final String dtype = metadata.getDtype().toLowerCase();
			if ( dtype.equals( "str" ) || dtype.equals( "string" ) || dtype.equals( "bytes" ) )
			{
				LOG.warn( "Skipping property '{}' with dtype '{}' because string properties are not supported", propName, metadata.getDtype() );
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if a property has missing values array and log a warning. Java
	 * doesn't support sparse/missing data, so we read all values.
	 */
	public static void checkForMissingValues( final N5Reader reader, final String propertyPath )
	{
		try
		{
			final String missingPath = propertyPath + "/missing";
			if ( reader.exists( missingPath ) )
			{
				LOG.warn( "Property '{}' has missing value indicators, but Java does not support sparse data. All values will be read as present.", propertyPath );
			}
		}
		catch ( final Exception e )
		{
			// Ignore errors checking for missing values
		}
	}

	// Default chunk size if not specified
	public static final int DEFAULT_CHUNK_SIZE = 1000;

	public static int getChunkSize( final String zarrPath )
	{
		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath, true ))
		{
			final int[] chunkSize = reader.getDatasetAttributes( "/nodes/ids" ).getBlockSize();
			return chunkSize[ 0 ];
		}
		catch ( final N5Exception.N5IOException e )
		{
			// If the path doesn't exist, return a default chunk size
			System.out.println( "Path doesn't exist, using default chunk size: " + e.getMessage() );
			return DEFAULT_CHUNK_SIZE; // Default chunk size
		}
	}

	public static < T > void writeIntArray(
			final List< T > elements,
			final ToIntFunction< T > extractor,
			final N5Writer writer,
			final String dataset,
			final int chunkSize )
	{
		final int size = elements.size();
		final int[] data = new int[ size ];
		Arrays.setAll( data, i -> extractor.applyAsInt( elements.get( i ) ) );
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { size },
				new int[] { chunkSize },
				DataType.INT32,
				new BloscCompression() );
		writer.createDataset( dataset, attributes );
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
		writeIntMatrix( elements.size(), numColumns,
				i -> extractor.apply( elements.get( i ) ),
				writer, dataset, chunkSize );
	}

	/**
	 * @param extractor
	 *            function from row index to int[] with column data
	 */
	public static void writeIntMatrix(
			final int numRows,
			final int numColumns,
			final IntFunction< int[] > extractor,
			final N5Writer writer,
			final String dataset,
			final int chunkSize )
	{
		final int[] data = new int[ numColumns * numRows ];
		for ( int i = 0; i < numRows; ++i )
		{
			final int[] row = extractor.apply( i );
			if ( row == null || row.length < numColumns )
				continue;
			System.arraycopy( row, 0, data, numColumns * i, numColumns );
		}
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { numColumns, numRows },
				new int[] { numColumns, chunkSize },
				DataType.INT32,
				new BloscCompression() );
		writer.createDataset( dataset, attributes );
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
		Arrays.setAll( data, i -> extractor.applyAsDouble( elements.get( i ) ) );
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { size },
				new int[] { chunkSize },
				DataType.FLOAT64,
				new BloscCompression() );
		writer.createDataset( dataset, attributes );
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
		for ( int i = 0; i < size; ++i )
		{
			final double[] row = extractor.apply( elements.get( i ) );
			if ( row == null || row.length < numColumns )
				continue;
			System.arraycopy( row, 0, data, numColumns * i, numColumns );
		}
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { numColumns, size },
				new int[] { numColumns, chunkSize },
				DataType.FLOAT64,
				new BloscCompression() );
		writer.createDataset( dataset, attributes );
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
		{ throw new IllegalArgumentException( "Expected 1D array" ); }
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
		{ throw new IllegalArgumentException( "Expected 1D array" ); }
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
		{ throw new IllegalArgumentException( "Expected 2D array" ); }
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
		{ throw new IllegalArgumentException( "Expected 2D array" ); }
		return new FlattenedInts( convertToIntArray( readFully( reader, dataset ), description ), attributes.getDimensions() );
	}

	public static int[] convertToIntArray( final Object array, final String fieldName )
	{
		if ( array == null )
			return null;
		else if ( array instanceof int[] )
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
		if ( array == null )
			return null;
		else if ( array instanceof double[] )
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
		{ throw new IllegalArgumentException( "property " + name + " does not have expected length (" + array.size()[ array.size().length - 1 ] + " vs " + expectedLength + ")" ); }
	}

	public static void verifyLength( final FlattenedInts array, final int expectedLength, final String name )
	{
		if ( array != null && array.size()[ array.size().length - 1 ] != expectedLength )
		{ throw new IllegalArgumentException( "property " + name + " does not have expected length (" + array.size()[ array.size().length - 1 ] + " vs " + expectedLength + ")" ); }
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

	/**
	 * Read a variable-length property from the zarr format
	 * 
	 * @param reader
	 *            N5Reader to read from
	 * @param propPath
	 *            Path to the property (e.g., /nodes/props/polygon)
	 * @param numNodes
	 *            Number of nodes in the graph
	 * @param metadata
	 *            PropMetadata for the property (optional, for dtype info)
	 * @return VarlengthProperty containing the varlength data, or null if
	 *         property doesn't exist
	 */
	public static VarlengthProperty readVarlengthProperty(
			final N5Reader reader,
			final String propPath,
			final int numNodes,
			final PropMetadata metadata )
	{
		// Check if the property exists
		final String dataPath = propPath + "/data";
		final String valuesPath = propPath + "/values";

		if ( !reader.datasetExists( dataPath ) || !reader.datasetExists( valuesPath ) )
		{
			LOG.debug( "Varlength property {} does not exist or missing data/values arrays", propPath );
			return null;
		}

		try
		{
			// Read the data array (flattened values)
			final Object dataArray = readFully( reader, dataPath );
			if ( dataArray == null )
			{
				LOG.warn( "Failed to read data array for varlength property {}", propPath );
				return null;
			}

			// Read the values array (offset and shape information)
			// values shape is (numNodes, ndim+1) where first column is offset,
			// rest are dims
			final FlattenedInts valuesArray = readAsIntMatrix( reader, valuesPath, "varlength property values" );
			if ( valuesArray == null )
			{
				LOG.warn( "Failed to read values array for varlength property {}", propPath );
				return null;
			}

			// Convert values array to long[][] for VarlengthProperty
			final int[] valuesDims = valuesArray.size();
			final int numColumns = valuesDims[ 0 ]; // ndim + 1
			final int numRowsFromValues = valuesDims[ 1 ]; // should equal
															// numNodes

			if ( numRowsFromValues != numNodes )
			{
				LOG.warn( "Varlength property {} values array has {} rows but expected {}", propPath, numRowsFromValues, numNodes );
				return null;
			}

			final long[][] offsets = new long[ numNodes ][];
			for ( int i = 0; i < numNodes; i++ )
			{
				offsets[ i ] = new long[ numColumns ];
				for ( int j = 0; j < numColumns; j++ )
				{
					offsets[ i ][ j ] = valuesArray.at( j, i );
				}
			}

			// Read missing array if present
			boolean[] missing = null;
			final String missingPath = propPath + "/missing";
			if ( reader.datasetExists( missingPath ) )
			{
				try
				{
					final boolean[] missingArray = ( boolean[] ) readFully( reader, missingPath );
					if ( missingArray != null && missingArray.length == numNodes )
					{
						missing = missingArray;
						LOG.debug( "Varlength property {} has missing indicators", propPath );
					}
				}
				catch ( final Exception e )
				{
					LOG.debug( "Could not read missing array for varlength property {}: {}", propPath, e.getMessage() );
				}
			}

			// Determine dtype from metadata if available
			final String dtype = metadata != null ? metadata.getDtype() : "unknown";

			// Create and return VarlengthProperty
			// Convert Object array to handle different data types properly
			final Object[] convertedData = convertVarlengthData( dataArray, dtype );
			return new VarlengthProperty( propPath, dtype, convertedData, offsets, missing );
		}
		catch ( final Exception e )
		{
			LOG.warn( "Error reading varlength property {}: {}", propPath, e.getMessage() );
			return null;
		}
	}

	/**
	 * Convert raw varlength data to Object array with proper type handling
	 */
	private static Object[] convertVarlengthData( final Object dataArray, final String dtype )
	{
		if ( dataArray == null )
		{ return null; }

		if ( dataArray instanceof Object[] )
		{
			return ( Object[] ) dataArray;
		}
		else if ( dataArray instanceof double[] )
		{
			final double[] dArray = ( double[] ) dataArray;
			final Object[] result = new Object[ dArray.length ];
			for ( int i = 0; i < dArray.length; i++ )
			{
				result[ i ] = dArray[ i ];
			}
			return result;
		}
		else if ( dataArray instanceof int[] )
		{
			final int[] iArray = ( int[] ) dataArray;
			final Object[] result = new Object[ iArray.length ];
			for ( int i = 0; i < iArray.length; i++ )
			{
				result[ i ] = iArray[ i ];
			}
			return result;
		}
		else if ( dataArray instanceof long[] )
		{
			final long[] lArray = ( long[] ) dataArray;
			final Object[] result = new Object[ lArray.length ];
			for ( int i = 0; i < lArray.length; i++ )
			{
				result[ i ] = lArray[ i ];
			}
			return result;
		}
		else if ( dataArray instanceof float[] )
		{
			final float[] fArray = ( float[] ) dataArray;
			final Object[] result = new Object[ fArray.length ];
			for ( int i = 0; i < fArray.length; i++ )
			{
				result[ i ] = fArray[ i ];
			}
			return result;
		}
		else
		{
			// Unknown type, return as Object array if possible
			LOG.warn( "Unknown data type for varlength data: {}", dataArray.getClass().getName() );
			if ( dataArray instanceof Object[] )
			{
				return ( Object[] ) dataArray;
			}
			else
			{
				return new Object[] { dataArray };
			}
		}
	}

	/**
	 * Write a variable-length property to zarr format. The property data is
	 * flattened with offset and shape information recorded in the values array.
	 *
	 * @param writer
	 *            N5Writer to write to
	 * @param propPath
	 *            Path where property will be stored (e.g.,
	 *            /nodes/props/polygon)
	 * @param nodeDataArrays
	 *            Array of Object[] where each element represents one node's
	 *            data (can have varying sizes/dimensions)
	 * @param missing
	 *            Optional boolean array indicating missing values for each node
	 * @param chunkSize
	 *            Chunk size for zarr storage
	 */
	public static void writeVarlengthProperty(
			final N5Writer writer,
			final String propPath,
			final Object[][] nodeDataArrays,
			final boolean[] missing,
			final int chunkSize )
	{
		if ( nodeDataArrays == null || nodeDataArrays.length == 0 )
		{
			LOG.warn( "Cannot write empty varlength property at {}", propPath );
			return;
		}

		final int numNodes = nodeDataArrays.length;

		// Step 1: Flatten all data and build offset/shape information
		final Object[] flattenedData = new Object[ calculateTotalElements( nodeDataArrays ) ];
		final long[][] offsetsAndShapes = new long[ numNodes ][];

		long currentOffset = 0;
		Object elementType = null;

		for ( int i = 0; i < numNodes; i++ )
		{
			final Object[] nodeData = nodeDataArrays[ i ];

			if ( nodeData != null && nodeData.length > 0 )
			{
				if ( elementType == null && nodeData.length > 0 )
				{
					elementType = nodeData[ 0 ];
				}

				// Calculate dimensions of this node's data
				// For now, store as 1D flat offset with total length
				final long[] shapeInfo = new long[ 2 ]; // [offset, length]
				shapeInfo[ 0 ] = currentOffset;
				shapeInfo[ 1 ] = nodeData.length;
				offsetsAndShapes[ i ] = shapeInfo;

				// Copy data into flattened array
				System.arraycopy( nodeData, 0, flattenedData, ( int ) currentOffset, nodeData.length );
				currentOffset += nodeData.length;
			}
			else
			{
				// Node has empty or null data
				offsetsAndShapes[ i ] = new long[] { currentOffset, 0 };
			}
		}

		try
		{
			// Step 2: Write data array
			final Object dataToWrite = convertObjectArrayToNativeArray( flattenedData, elementType, ( int ) currentOffset );
			DataType dataType = inferDataType( dataToWrite );
			writeDataArray( writer, propPath + "/data", dataToWrite, dataType, chunkSize );

			// Step 3: Write values array (offset and length information)
			writeOffsetsArray( writer, propPath + "/values", offsetsAndShapes, chunkSize );

			// Step 4: Write missing array if needed
			if ( missing != null && missing.length == numNodes )
			{
				writeMissingArray( writer, propPath + "/missing", missing, chunkSize );
			}

			LOG.debug( "Successfully wrote varlength property: {}", propPath );
		}
		catch ( final Exception e )
		{
			LOG.warn( "Error writing varlength property {}: {}", propPath, e.getMessage() );
		}
	}

	/**
	 * Calculate total number of elements across all node data arrays
	 */
	private static int calculateTotalElements( final Object[][] nodeDataArrays )
	{
		int total = 0;
		for ( final Object[] nodeData : nodeDataArrays )
		{
			if ( nodeData != null )
			{
				total += nodeData.length;
			}
		}
		return total;
	}

	/**
	 * Convert Object array to native array type (double[], int[], long[], etc.)
	 */
	private static Object convertObjectArrayToNativeArray( final Object[] objectArray, final Object elementType, final int size )
	{
		if ( elementType == null )
		{ return new double[ size ]; } // Default

		if ( elementType instanceof Double )
		{
			final double[] result = new double[ size ];
			for ( int i = 0; i < size; i++ )
			{
				result[ i ] = ( Double ) objectArray[ i ];
			}
			return result;
		}
		else if ( elementType instanceof Integer )
		{
			final int[] result = new int[ size ];
			for ( int i = 0; i < size; i++ )
			{
				result[ i ] = ( Integer ) objectArray[ i ];
			}
			return result;
		}
		else if ( elementType instanceof Long )
		{
			final long[] result = new long[ size ];
			for ( int i = 0; i < size; i++ )
			{
				result[ i ] = ( Long ) objectArray[ i ];
			}
			return result;
		}
		else if ( elementType instanceof Float )
		{
			final float[] result = new float[ size ];
			for ( int i = 0; i < size; i++ )
			{
				result[ i ] = ( Float ) objectArray[ i ];
			}
			return result;
		}
		else
		{
			// Default to double for unknown types
			final double[] result = new double[ size ];
			for ( int i = 0; i < size; i++ )
			{
				if ( objectArray[ i ] instanceof Number )
				{
					result[ i ] = ( ( Number ) objectArray[ i ] ).doubleValue();
				}
			}
			return result;
		}
	}

	/**
	 * Infer data type from native array
	 */
	private static DataType inferDataType( final Object array )
	{
		if ( array instanceof double[] )
		{
			return DataType.FLOAT64;
		}
		else if ( array instanceof float[] )
		{
			return DataType.FLOAT32;
		}
		else if ( array instanceof int[] )
		{
			return DataType.INT32;
		}
		else if ( array instanceof long[] )
		{
			return DataType.INT64;
		}
		else
		{
			return DataType.FLOAT64;
		} // Default
	}

	/**
	 * Write a native data array to zarr dataset
	 */
	private static void writeDataArray(
			final N5Writer writer,
			final String dataset,
			final Object data,
			final DataType dataType,
			final int chunkSize ) throws Exception
	{
		final long numElements = Array.getLength( data );
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { numElements },
				new int[] { Math.min( chunkSize, ( int ) numElements ) },
				dataType,
				new BloscCompression() );
		writer.createDataset( dataset, attributes );
		write( data, writer, dataset, attributes );
	}

	/**
	 * Write offset and shape information as int64 2D array
	 */
	private static void writeOffsetsArray(
			final N5Writer writer,
			final String dataset,
			final long[][] offsetsAndShapes,
			final int chunkSize ) throws Exception
	{
		final int numNodes = offsetsAndShapes.length;
		final int numColumns = offsetsAndShapes.length > 0 ? offsetsAndShapes[ 0 ].length : 2;

		// Convert long[][] to long[] array in column-major order
		final long[] flatOffsets = new long[ numNodes * numColumns ];
		for ( int i = 0; i < numNodes; i++ )
		{
			for ( int j = 0; j < numColumns; j++ )
			{
				flatOffsets[ i + j * numNodes ] = offsetsAndShapes[ i ][ j ];
			}
		}

		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { numColumns, numNodes },
				new int[] { numColumns, Math.min( chunkSize, numNodes ) },
				DataType.INT64,
				new BloscCompression() );
		writer.createDataset( dataset, attributes );
		write( flatOffsets, writer, dataset, attributes );
	}

	/**
	 * Write missing value indicators as boolean array
	 */
	private static void writeMissingArray(
			final N5Writer writer,
			final String dataset,
			final boolean[] missing,
			final int chunkSize ) throws Exception
	{
		final DatasetAttributes attributes = new DatasetAttributes(
				new long[] { missing.length },
				new int[] { Math.min( chunkSize, missing.length ) },
				DataType.UINT8, // N5 uses UINT8 for booleans
				new BloscCompression() );
		writer.createDataset( dataset, attributes );

		// Convert boolean[] to byte[] for storage
		final byte[] boolAsBytes = new byte[ missing.length ];
		for ( int i = 0; i < missing.length; i++ )
		{
			boolAsBytes[ i ] = ( byte ) ( missing[ i ] ? 1 : 0 );
		}

		write( boolAsBytes, writer, dataset, attributes );
	}

	private GeffUtils()
	{
		// static utility methods. don't instantiate.
	}
}
