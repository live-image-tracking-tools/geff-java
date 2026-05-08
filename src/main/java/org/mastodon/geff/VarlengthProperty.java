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

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a variable-length property in the GEFF format. Each node can have
 * a different shape/length for the property value. The data is stored as a
 * single flattened array, with offset and shape information for each node.
 *
 * Format (from spec): - data: 1D array containing all flattened values
 * concatenated - values: (N, ndim+1) array where N is number of nodes, ndim is
 * dimensionality - First column: offset into the data array for that node's
 * data - Remaining columns: shape of that node's data
 */
public class VarlengthProperty
{
	private final String name;

	private final String dtype;

	private final Object[] data;

	private final long[][] offsets;

	private final boolean[] missing;

	/**
	 * Constructor for VarlengthProperty
	 *
	 * @param name
	 *            Name of the property
	 * @param dtype
	 *            Data type of the property
	 * @param data
	 *            The raw data array that holds all values flattened
	 * @param offsets
	 *            Array of shape information for each node. offsets[i][0] is the
	 *            offset, offsets[i][1:] are the dimensions
	 * @param missing
	 *            Boolean array indicating which nodes have missing values
	 */
	public VarlengthProperty( final String name, final String dtype, final Object[] data, final long[][] offsets, final boolean[] missing )
	{
		this.name = name;
		this.dtype = dtype;
		this.data = data;
		this.offsets = offsets;
		this.missing = missing;
	}

	/**
	 * Get the name of the property
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Get the data type
	 */
	public String getDtype()
	{
		return dtype;
	}

	/**
	 * Get the raw flattened data array
	 */
	public Object[] getData()
	{
		return data;
	}

	/**
	 * Get offset and shape information for each node
	 *
	 * @return 2D array where [i][0] is offset, [i][1:] are dimensions for node
	 *         i
	 */
	public long[][] getOffsets()
	{
		return offsets;
	}

	/**
	 * Get missing value indicators
	 */
	public boolean[] getMissing()
	{
		return missing;
	}

	/**
	 * Check if a specific node has a missing value
	 *
	 * @param nodeIndex
	 *            Index of the node
	 * @return true if the value is missing, false otherwise
	 */
	public boolean isMissing( final int nodeIndex )
	{
		if ( missing == null || nodeIndex < 0 || nodeIndex >= missing.length )
		{ return false; }
		return missing[ nodeIndex ];
	}

	/**
	 * Get the data for a specific node
	 *
	 * @param nodeIndex
	 *            Index of the node
	 * @return Array of data for this node, or null if missing
	 */
	public Object getNodeData( final int nodeIndex )
	{
		if ( isMissing( nodeIndex ) )
		{ return null; }

		if ( nodeIndex < 0 || nodeIndex >= offsets.length )
		{ return null; }

		final long[] shapeInfo = offsets[ nodeIndex ];
		if ( shapeInfo == null || shapeInfo.length < 1 )
		{ return null; }

		final long offset = shapeInfo[ 0 ];
		final long[] shape = Arrays.copyOfRange( shapeInfo, 1, shapeInfo.length );

		// Calculate total number of elements
		long totalElements = 1;
		for ( final long dim : shape )
		{
			totalElements *= dim;
		}

		if ( offset < 0 || offset + totalElements > data.length )
		{
			// Invalid offset/shape, return null
			return null;
		}

		// Extract the slice of data
		final Object[] nodeData = new Object[ ( int ) totalElements ];
		System.arraycopy( data, ( int ) offset, nodeData, 0, ( int ) totalElements );

		// If single element or 1D, return as-is; otherwise keep as array
		if ( shape.length == 0 )
		{ return nodeData.length > 0 ? nodeData[ 0 ] : null; }

		return nodeData;
	}

	@Override
	public String toString()
	{
		return String.format(
				"VarlengthProperty{name='%s', dtype='%s', dataLength=%d, nodeCount=%d}",
				name, dtype, data != null ? data.length : 0, offsets != null ? offsets.length : 0 );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;
		VarlengthProperty that = ( VarlengthProperty ) o;
		return Objects.equals( name, that.name ) && Objects.equals( dtype, that.dtype );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( name, dtype );
	}
}
