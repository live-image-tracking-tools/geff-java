/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 Ko Sugawara
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import org.mastodon.geff.function.ToDoubleArrayFunction;
import org.mastodon.geff.function.ToIntArrayFunction;

import com.bc.zarr.ArrayParams;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

public class ZarrUtils
{

	public static final int DEFAULT_CHUNK_SIZE = 1000; // Default chunk size if
														// not specified

	public static ZarrGroup openSubGroups( ZarrGroup parentGroup, String subGroupName ) throws IOException
	{
		ZarrGroup subGroup = null;
		for ( final String groupName : subGroupName.split( "/" ) )
		{
			if ( subGroup == null )
			{
				subGroup = parentGroup.openSubGroup( groupName );
			}
			else
			{
				subGroup = subGroup.openSubGroup( groupName );
			}
		}
		return subGroup;
	}

	public static ZarrGroup createSubGroups( ZarrGroup parentGroup, String subGroupName ) throws IOException
	{
		ZarrGroup subGroup = null;
		for ( final String groupName : subGroupName.split( "/" ) )
		{
			if ( subGroup == null )
			{
				subGroup = parentGroup.createSubGroup( groupName );
			}
			else
			{
				subGroup = subGroup.createSubGroup( groupName );
			}
		}
		return subGroup;
	}

	/**
	 * Helper method to read chunked int arrays
	 */
	public static int[] readChunkedIntArray( final ZarrGroup group, final String arrayPath, final String description )
			throws IOException
	{
		if ( group.getArrayKeys() == null || group.getArrayKeys().isEmpty() )
		{
			System.out.println( "No arrays found in group for " + description );
			return new int[ 0 ]; // Return empty array if no arrays found
		}
		try
		{
			// First try reading as a whole array
			final ZarrArray array = group.openArray( arrayPath );
			final Object data = array.read();
			return convertToIntArray( data, description );
		}
		catch ( final Exception e )
		{

			// Try reading individual chunks if whole array reading fails
			final List< Integer > allData = new ArrayList<>();

			// Look for numeric chunk keys (0, 1, 2, etc.)
			final ZarrGroup arrayGroup = openSubGroups( group, arrayPath );

			final String[] chunkKeys = arrayGroup.getArrayKeys().toArray( new String[ 0 ] );

			for ( final String chunkKey : chunkKeys )
			{
				try
				{
					if ( chunkKey.matches( "\\d+(\\.\\d+)?" ) )
					{ // numeric chunk key
						final ZarrArray chunkArray = arrayGroup.openArray( chunkKey );
						final Object chunkData = chunkArray.read();
						final int[] chunkValues = convertToIntArray( chunkData, description + " chunk " + chunkKey );
						for ( final int value : chunkValues )
						{
							allData.add( value );
						}
						System.out
								.println( "Read chunk " + chunkKey + " with " + chunkValues.length + " " + description );
					}
				}
				catch ( final Exception chunkException )
				{
					System.err.println( "Could not read chunk " + chunkKey + " for " + description + ": "
							+ chunkException.getMessage() );
				}
			}

			return allData.stream().mapToInt( Integer::intValue ).toArray();
		}
	}

	/**
	 * Helper method to read chunked double arrays
	 */
	public static double[] readChunkedDoubleArray( final ZarrGroup group, final String arrayPath, final String description )
			throws IOException
	{
		if ( group.getArrayKeys() == null || group.getArrayKeys().isEmpty() )
		{
			System.out.println( "No arrays found in group for " + description );
			return new double[ 0 ]; // Return empty array if no arrays found
		}
		try
		{
			// First try reading as a whole array
			final ZarrArray array = group.openArray( arrayPath );
			final Object data = array.read();
			return convertToDoubleArray( data, description );
		}
		catch ( final Exception e )
		{

			// Try reading individual chunks if whole array reading fails
			final List< Double > allData = new ArrayList<>();

			// Look for numeric chunk keys (0, 1, 2, etc.)
			final ZarrGroup arrayGroup = openSubGroups( group, arrayPath );

			final String[] chunkKeys = arrayGroup.getArrayKeys().toArray( new String[ 0 ] );

			for ( final String chunkKey : chunkKeys )
			{
				try
				{
					if ( chunkKey.matches( "\\d+(\\.\\d+)?" ) )
					{ // numeric chunk key
						final ZarrArray chunkArray = arrayGroup.openArray( chunkKey );
						final Object chunkData = chunkArray.read();
						final double[] chunkValues = convertToDoubleArray( chunkData, description + " chunk " + chunkKey );
						for ( final double value : chunkValues )
						{
							allData.add( value );
						}
						System.out
								.println( "Read chunk " + chunkKey + " with " + chunkValues.length + " " + description );
					}
				}
				catch ( final Exception chunkException )
				{
					System.err.println( "Could not read chunk " + chunkKey + " for " + description + ": "
							+ chunkException.getMessage() );
				}
			}

			return allData.stream().mapToDouble( Double::doubleValue ).toArray();
		}
	}

	/**
	 * Helper method to read chunked integer matrix
	 */
	public static int[][] readChunkedIntMatrix( final ZarrGroup group, final String arrayPath, final String description )
			throws IOException
	{
		if ( group.getArrayKeys() == null || group.getArrayKeys().isEmpty() )
		{
			System.out.println( "No arrays found in group for " + description );
			return new int[ 0 ][]; // Return empty matrix if no arrays found
		}
		try
		{
			// First try reading as a whole array
			final ZarrArray array = group.openArray( arrayPath );
			final Object data = array.read();
			return copyToIntMatrix( data, description, array.getShape() );
		}
		catch ( final Exception e )
		{

			// Try reading individual chunks if whole array reading fails
			final List< int[] > allData = new ArrayList<>();

			// Look for numeric chunk keys (0, 1, 2, etc.)
			final ZarrGroup arrayGroup = openSubGroups( group, arrayPath );

			final String[] chunkKeys = arrayGroup.getArrayKeys().toArray( new String[ 0 ] );

			for ( final String chunkKey : chunkKeys )
			{
				try
				{
					if ( chunkKey.matches( "\\d+(\\.\\d+)?" ) )
					{ // numeric chunk key
						final ZarrArray chunkArray = arrayGroup.openArray( chunkKey );
						final Object chunkData = chunkArray.read();
						final int[][] chunkMatrix = copyToIntMatrix( chunkData, description, chunkArray.getShape() );
						for ( final int[] row : chunkMatrix )
						{
							allData.add( row );
						}
						System.out.println(
								"Read " + description + " chunk " + chunkKey + " with " + chunkMatrix.length );
					}
				}
				catch ( final Exception chunkException )
				{
					System.err
							.println( "Could not read " + description + " chunk " + chunkKey + ": "
									+ chunkException.getMessage() );
				}
			}

			return allData.toArray( new int[ 0 ][] );
		}
	}

	/**
	 * Helper method to read chunked double matrix
	 */
	public static double[][] readChunkedDoubleMatrix( final ZarrGroup group, final String arrayPath, final String description )
			throws IOException
	{
		if ( group.getArrayKeys() == null || group.getArrayKeys().isEmpty() )
		{
			System.out.println( "No arrays found in group for " + description );
			return new double[ 0 ][]; // Return empty matrix if no arrays found
		}
		try
		{
			// First try reading as a whole array
			final ZarrArray array = group.openArray( arrayPath );
			final Object data = array.read();
			return copyToDoubleMatrix( data, description, array.getShape() );
		}
		catch ( final Exception e )
		{

			// Try reading individual chunks if whole array reading fails
			final List< double[] > allData = new ArrayList<>();

			// Look for numeric chunk keys (0, 1, 2, etc.)
			final ZarrGroup arrayGroup = openSubGroups( group, arrayPath );

			final String[] chunkKeys = arrayGroup.getArrayKeys().toArray( new String[ 0 ] );

			for ( final String chunkKey : chunkKeys )
			{
				try
				{
					if ( chunkKey.matches( "\\d+(\\.\\d+)?" ) )
					{ // numeric chunk key
						final ZarrArray chunkArray = arrayGroup.openArray( chunkKey );
						final Object chunkData = chunkArray.read();
						final double[][] chunkMatrix = copyToDoubleMatrix( chunkData, description, chunkArray.getShape() );
						for ( final double[] row : chunkMatrix )
						{
							allData.add( row );
						}
						System.out.println(
								"Read " + description + " chunk " + chunkKey + " with " + chunkMatrix.length );
					}
				}
				catch ( final Exception chunkException )
				{
					System.err
							.println( "Could not read " + description + " chunk " + chunkKey + ": "
									+ chunkException.getMessage() );
				}
			}

			return allData.toArray( new double[ 0 ][] );
		}
	}

	public static int getChunkSize( final String zarrPath ) throws IOException, InvalidRangeException
	{
		try
		{
			final ZarrGroup group = ZarrGroup.open( zarrPath + "/nodes" );
			return group.openArray( "ids" ).getChunks()[ 0 ];
		}
		catch ( final IOException e )
		{
			// If the path doesn't exist, return a default chunk size
			System.out.println( "Path doesn't exist, using default chunk size: " + e.getMessage() );
			return DEFAULT_CHUNK_SIZE; // Default chunk size
		}
	}

	/**
	 * Helper method to write chunked int attributes
	 */
	public static < T extends ZarrEntity > void writeChunkedIntAttribute( final List< T > nodes, final ZarrGroup attrsGroup,
			final String subGroupName,
			final int chunkSize, final ToIntFunction< T > extractor )
			throws IOException, InvalidRangeException
	{

		final int totalNodes = nodes.size();

		// Create the attribute subgroup
		final ZarrGroup valuesGroup = createSubGroups( attrsGroup, subGroupName );

		// Create a single ZarrArray for all values with proper chunking
		final ZarrArray valuesArray = valuesGroup.createArray( "", new ArrayParams()
				.shape( totalNodes )
				.chunks( chunkSize )
				.dataType( DataType.i4 ) );

		// Write data in chunks
		int chunkIndex = 0;
		for ( int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize )
		{
			final int endIdx = Math.min( startIdx + chunkSize, totalNodes );
			final int currentChunkSize = endIdx - startIdx;

			// Prepare chunk data array
			final int[] chunkData = new int[ currentChunkSize ];

			// Fill chunk data array
			for ( int i = 0; i < currentChunkSize; i++ )
			{
				chunkData[ i ] = extractor.applyAsInt( nodes.get( startIdx + i ) );
			}

			// Write chunk at specific offset
			valuesArray.write( chunkData, new int[] { currentChunkSize }, new int[] { startIdx } );

			System.out.println( "- Wrote " + subGroupName + " chunk " + chunkIndex + ": " + currentChunkSize + " values" );
			chunkIndex++;
		}
	}

	/**
	 * Helper method to write chunked double attributes
	 */
	public static < T extends ZarrEntity > void writeChunkedDoubleAttribute( final List< T > nodes, final ZarrGroup attrsGroup,
			final String subGroupName,
			final int chunkSize, final java.util.function.ToDoubleFunction< T > extractor )
			throws IOException, InvalidRangeException
	{

		final int totalNodes = nodes.size();

		// Create the attribute subgroup
		final ZarrGroup valuesGroup = createSubGroups( attrsGroup, subGroupName );

		// Create a single ZarrArray for all values with proper chunking
		final ZarrArray valuesArray = valuesGroup.createArray( "", new ArrayParams()
				.shape( totalNodes )
				.chunks( chunkSize )
				.dataType( DataType.f8 ) );

		// Write data in chunks
		int chunkIndex = 0;
		for ( int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize )
		{
			final int endIdx = Math.min( startIdx + chunkSize, totalNodes );
			final int currentChunkSize = endIdx - startIdx;

			// Prepare chunk data array
			final double[] chunkData = new double[ currentChunkSize ];

			// Fill chunk data array
			for ( int i = 0; i < currentChunkSize; i++ )
			{
				chunkData[ i ] = extractor.applyAsDouble( nodes.get( startIdx + i ) );
			}

			// Write chunk at specific offset
			valuesArray.write( chunkData, new int[] { currentChunkSize }, new int[] { startIdx } );

			System.out.println( "- Wrote " + subGroupName + " chunk " + chunkIndex + ": " + currentChunkSize + " values" );
			chunkIndex++;
		}
	}

	/**
	 * Helper method to write chunked integer matrices
	 */
	public static < T extends ZarrEntity > void writeChunkedIntMatrix( final List< T > nodes, final ZarrGroup attrsGroup,
			final String subGroupName,
			final int chunkSize, final ToIntArrayFunction< T > extractor, final int numColumns )
			throws IOException, InvalidRangeException
	{
		final int totalNodes = nodes.size();

		// Create the attribute subgroup
		final ZarrGroup valuesGroup = createSubGroups( attrsGroup, subGroupName );

		// Create a single ZarrArray for all data with proper chunking
		final ZarrArray array2d = valuesGroup.createArray( "", new ArrayParams()
				.shape( totalNodes, numColumns )
				.chunks( new int[] { chunkSize, numColumns } )
				.dataType( DataType.i8 ) );

		// Write data in chunks
		int chunkIndex = 0;
		for ( int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize )
		{
			final int endIdx = Math.min( startIdx + chunkSize, totalNodes );
			final int currentChunkSize = endIdx - startIdx;

			// Prepare chunk data array
			final int[] chunkData = new int[ currentChunkSize * numColumns ];

			// Fill chunk data array
			for ( int i = 0; i < currentChunkSize; i++ )
			{
				final T node = nodes.get( startIdx + i );
				final int[] values = extractor.applyAsIntArray( node );
				if ( values != null && values.length == numColumns )
				{
					for ( int j = 0; j < numColumns; j++ )
					{
						chunkData[ i * numColumns + j ] = values[ j ];
					}
				}
				else
				{
					for ( int j = 0; j < numColumns; j++ )
					{
						chunkData[ i * numColumns + j ] = 0; // Default to zero
																// if not set
					}
				}
			}

			// Write chunk at specific offset
			array2d.write( chunkData, new int[] { currentChunkSize, numColumns },
					new int[] { startIdx, 0 } );

			System.out.println( "- Wrote " + subGroupName + " chunk " + chunkIndex + ": " + currentChunkSize + " values" );
			chunkIndex++;
		}
	}

	/**
	 * Helper method to write chunked double matrices
	 */
	public static < T extends ZarrEntity > void writeChunkedDoubleMatrix( final List< T > nodes, final ZarrGroup attrsGroup,
			final String subGroupName,
			final int chunkSize, final ToDoubleArrayFunction< T > extractor, final int numColumns )
			throws IOException, InvalidRangeException
	{
		final int totalNodes = nodes.size();

		// Create the attribute subgroup
		final ZarrGroup valuesGroup = createSubGroups( attrsGroup, subGroupName );

		// Create a single ZarrArray for all data with proper chunking
		final ZarrArray array2d = valuesGroup.createArray( "", new ArrayParams()
				.shape( totalNodes, numColumns )
				.chunks( new int[] { chunkSize, numColumns } )
				.dataType( DataType.f4 ) );

		// Write data in chunks
		int chunkIndex = 0;
		for ( int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize )
		{
			final int endIdx = Math.min( startIdx + chunkSize, totalNodes );
			final int currentChunkSize = endIdx - startIdx;

			// Prepare chunk data array
			final double[] chunkData = new double[ currentChunkSize * numColumns ];

			// Fill chunk data array
			for ( int i = 0; i < currentChunkSize; i++ )
			{
				final T node = nodes.get( startIdx + i );
				final double[] values = extractor.applyAsDoubleArray( node );
				if ( values != null && values.length == numColumns )
				{
					for ( int j = 0; j < numColumns; j++ )
					{
						chunkData[ i * numColumns + j ] = values[ j ];
					}
				}
				else
				{
					for ( int j = 0; j < numColumns; j++ )
					{
						chunkData[ i * numColumns + j ] = 0.0; // Default to
																// zero if not
																// set
					}
				}
			}

			// Write chunk at specific offset
			array2d.write( chunkData, new int[] { currentChunkSize, numColumns },
					new int[] { startIdx, 0 } );

			System.out.println( "- Wrote " + subGroupName + " chunk " + chunkIndex + ": " + currentChunkSize + " values" );
			chunkIndex++;
		}
	}

	// Helper methods for type conversion
	public static int[] convertToIntArray( final Object data, final String fieldName )
	{
		if ( data instanceof int[] )
		{
			return ( int[] ) data;
		}
		else if ( data instanceof long[] )
		{
			final long[] longArray = ( long[] ) data;
			final int[] intArray = new int[ longArray.length ];
			for ( int i = 0; i < longArray.length; i++ )
			{
				intArray[ i ] = ( int ) longArray[ i ];
			}
			return intArray;
		}
		else if ( data instanceof double[] )
		{
			final double[] doubleArray = ( double[] ) data;
			final int[] intArray = new int[ doubleArray.length ];
			for ( int i = 0; i < doubleArray.length; i++ )
			{
				intArray[ i ] = ( int ) doubleArray[ i ];
			}
			return intArray;
		}
		else if ( data instanceof float[] )
		{
			final float[] floatArray = ( float[] ) data;
			final int[] intArray = new int[ floatArray.length ];
			for ( int i = 0; i < floatArray.length; i++ )
			{
				intArray[ i ] = ( int ) floatArray[ i ];
			}
			return intArray;
		}
		else
		{
			throw new IllegalArgumentException(
					"Unsupported data type for " + fieldName + ": " +
							( data != null ? data.getClass().getName() : "null" ) );
		}
	}

	public static double[] convertToDoubleArray( final Object data, final String fieldName )
	{
		if ( data instanceof double[] )
		{
			return ( double[] ) data;
		}
		else if ( data instanceof float[] )
		{
			final float[] floatArray = ( float[] ) data;
			final double[] doubleArray = new double[ floatArray.length ];
			for ( int i = 0; i < floatArray.length; i++ )
			{
				doubleArray[ i ] = floatArray[ i ];
			}
			return doubleArray;
		}
		else if ( data instanceof int[] )
		{
			final int[] intArray = ( int[] ) data;
			final double[] doubleArray = new double[ intArray.length ];
			for ( int i = 0; i < intArray.length; i++ )
			{
				doubleArray[ i ] = intArray[ i ];
			}
			return doubleArray;
		}
		else if ( data instanceof long[] )
		{
			final long[] longArray = ( long[] ) data;
			final double[] doubleArray = new double[ longArray.length ];
			for ( int i = 0; i < longArray.length; i++ )
			{
				doubleArray[ i ] = longArray[ i ];
			}
			return doubleArray;
		}
		else
		{
			throw new IllegalArgumentException(
					"Unsupported data type for " + fieldName + ": " +
							( data != null ? data.getClass().getName() : "null" ) );
		}
	}

	public static int[][] copyToIntMatrix( final Object data, final String description, final int[] shape )
	{
		if ( shape.length != 2 )
			throw new IllegalArgumentException( "Shape must have exactly 2 dimensions for a matrix, but had " + shape.length );

		final int N = shape[ 0 ];
		final int nel = shape[ 1 ];
		if ( data.getClass().isArray() )
		{
			final Class< ? > componentType = data.getClass().getComponentType();
			if ( componentType.isPrimitive() )
			{
				if ( componentType == int.class )
				{
					final int[] arr = ( int[] ) data;
					final int[][] matrix = new int[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = arr[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == byte.class )
				{
					final byte[] byteArray = ( byte[] ) data;
					final int[][] matrix = new int[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = byteArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == short.class )
				{
					final short[] shortArray = ( short[] ) data;
					final int[][] matrix = new int[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = shortArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == long.class )
				{
					final long[] longArray = ( long[] ) data;
					final int[][] matrix = new int[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = ( int ) longArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == float.class )
				{
					final float[] floatArray = ( float[] ) data;
					final int[][] matrix = new int[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = ( int ) floatArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == double.class )
				{
					final double[] doubleArray = ( double[] ) data;
					final int[][] matrix = new int[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = ( int ) doubleArray[ i * nel + j ];

					return matrix;
				}
				else
				{
					throw new IllegalArgumentException(
							"Unsupported primitive type for " + description + ": " + componentType.getName() );
				}
			}
			else
			{
				throw new IllegalArgumentException( "The array is not of a primitive type." );
			}
		}
		else
		{
			throw new IllegalArgumentException( "The object is not an array." );
		}
	}

	public static double[][] copyToDoubleMatrix( final Object data, final String description, final int[] shape )
	{
		if ( shape.length != 2 )
			throw new IllegalArgumentException( "Shape must have exactly 2 dimensions for a matrix, but had " + shape.length );

		final int N = shape[ 0 ];
		final int nel = shape[ 1 ];
		if ( data.getClass().isArray() )
		{
			final Class< ? > componentType = data.getClass().getComponentType();
			if ( componentType.isPrimitive() )
			{
				if ( componentType == int.class )
				{
					final int[] arr = ( int[] ) data;
					final double[][] matrix = new double[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = arr[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == byte.class )
				{
					final byte[] byteArray = ( byte[] ) data;
					final double[][] matrix = new double[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = byteArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == short.class )
				{
					final short[] shortArray = ( short[] ) data;
					final double[][] matrix = new double[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = shortArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == long.class )
				{
					final long[] longArray = ( long[] ) data;
					final double[][] matrix = new double[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = longArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == float.class )
				{
					final float[] floatArray = ( float[] ) data;
					final double[][] matrix = new double[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = floatArray[ i * nel + j ];

					return matrix;
				}
				else if ( componentType == double.class )
				{
					final double[] doubleArray = ( double[] ) data;
					final double[][] matrix = new double[ N ][ nel ];
					for ( int i = 0; i < N; i++ )
						for ( int j = 0; j < nel; j++ )
							matrix[ i ][ j ] = doubleArray[ i * nel + j ];

					return matrix;
				}
				else
				{
					throw new IllegalArgumentException(
							"Unsupported primitive type for " + description + ": " + componentType.getName() );
				}
			}
			else
			{
				throw new IllegalArgumentException( "The array is not of a primitive type." );
			}
		}
		else
		{
			throw new IllegalArgumentException( "The object is not an array." );
		}
	}
}
