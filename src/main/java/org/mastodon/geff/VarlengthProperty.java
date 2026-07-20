/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 - 2026 Ko Sugawara
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
 * Represents a variable-length property value for a single node or edge. Each
 * instance holds the data for exactly one element; the length may differ
 * between elements, which is the defining feature of a varlength property.
 * <p>
 * On-disk (Zarr), varlength data is stored as a flattened array with an offset
 * table. That layout is an I/O implementation detail handled by
 * {@link GeffUtils#readVarlengthProperty} and
 * {@link GeffUtils#writeVarlengthProperty}; users of this class do not need to
 * know about it.
 */
public class VarlengthProperty
{
	private final String name;

	private final String dtype;

	private final Object[] data;

	private final boolean missing;

	/**
	 * Creates a non-missing varlength property for one node/edge.
	 *
	 * @param name
	 *            property name (e.g. {@code "polygon"})
	 * @param dtype
	 *            data type string (e.g. {@code "float64"})
	 * @param data
	 *            the values for this node/edge
	 */
	public VarlengthProperty( final String name, final String dtype, final Object[] data )
	{
		this( name, dtype, data, false );
	}

	/**
	 * Creates a varlength property for one node/edge with an explicit missing
	 * flag.
	 *
	 * @param name
	 *            property name
	 * @param dtype
	 *            data type string
	 * @param data
	 *            the values for this node/edge; ignored when {@code missing} is
	 *            {@code true}
	 * @param missing
	 *            {@code true} if this entry has no value
	 */
	public VarlengthProperty( final String name, final String dtype, final Object[] data, final boolean missing )
	{
		this.name = name;
		this.dtype = dtype;
		this.data = data;
		this.missing = missing;
	}

	/**
	 * Returns the property name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the data type string (e.g. {@code "float64"}, {@code "int32"}).
	 */
	public String getDtype()
	{
		return dtype;
	}

	/**
	 * Returns the data values for this node/edge, or {@code null} when
	 * {@link #isMissing()} is {@code true}.
	 */
	public Object[] getData()
	{
		return data;
	}

	/**
	 * Returns {@code true} when this entry has no value (missing-value
	 * indicator).
	 */
	public boolean isMissing()
	{
		return missing;
	}

	@Override
	public String toString()
	{
		return String.format(
				"VarlengthProperty{name='%s', dtype='%s', length=%d, missing=%b}",
				name, dtype, data != null ? data.length : 0, missing );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;
		final VarlengthProperty that = ( VarlengthProperty ) o;
		return missing == that.missing
				&& Objects.equals( name, that.name )
				&& Objects.equals( dtype, that.dtype )
				&& Arrays.equals( data, that.data );
	}

	@Override
	public int hashCode()
	{
		int result = Objects.hash( name, dtype, missing );
		result = 31 * result + Arrays.hashCode( data );
		return result;
	}
}
