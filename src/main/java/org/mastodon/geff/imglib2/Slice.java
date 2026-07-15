package org.mastodon.geff.imglib2;

import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.RandomAccessibleInterval;

/**
 * Re-positionable slice.
 * <p>
 * Not thread-safe. Use {@link #copy()} to create independent copies for multithreaded use.
 *
 * @param <T>
 */
public interface Slice< T > extends RandomAccessibleInterval< T >
{
	/**
	 * @param parent
	 * @param slicedDimensions
	 * 		indices of the dimensions in which to slice
	 * @param <T>
	 *
	 * @return
	 */
	static < T > Slice< T > slice( final RandomAccessibleInterval< T > parent, final int... slicedDimensions )
	{
		return new SliceImpl<>( parent, slicedDimensions );
	}

	interface SlicePosition extends Positionable, Localizable
	{
	}

	/**
	 * A handle to move the slice along the sliced dimensions
	 */
	SlicePosition slicePosition();

	/**
	 * Independent copy of this slice.
	 */
	Slice< T > copy();
}
