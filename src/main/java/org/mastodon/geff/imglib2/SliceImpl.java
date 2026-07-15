package org.mastodon.geff.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.Views;

class SliceImpl< T > implements Slice< T >
{
	private final RandomAccessibleInterval< T > delegate;

	private final long[] slicePos;

	private final int n;

	private final SP slicePosition;

	SliceImpl( final RandomAccessibleInterval< T > parent, final int... slicedDimensions )
	{
		if ( slicedDimensions.length == 0 )
			throw new IllegalArgumentException( "must slice at least along one one dimension" );

		final int pn = parent.numDimensions();
		final int sn = slicedDimensions.length;
		n = pn - sn;

		for ( int d : slicedDimensions )
			if ( d >= pn )
				throw new IllegalArgumentException( "slicing along non-existent dimension" );

		final int[] axes = new int[ pn ];
		int j = 0;
		for ( int i = 0; i < pn; i++ )
			if ( !contains( slicedDimensions, i ) )
				axes[ j++ ] = i;
		for ( int d : slicedDimensions )
			axes[ j++ ] = d;

		// Reorder parent axes such that the first n dimensions of delegate
		// are the dimensions of this Slice and the trailing dimension are
		// the slice position.
		delegate = Views.selectAxes( parent, axes );
		slicePos = new long[ sn ];
		slicePosition = new SP( slicePos );
	}

	private SliceImpl( final SliceImpl< T > slice )
	{
		delegate = slice.delegate;
		slicePos = slice.slicePos.clone();
		n = slice.n;
		slicePosition = new SP( slicePos );
	}

	@Override
	public Slice< T > copy()
	{
		return new SliceImpl<>( this );
	}

	@Override
	public SlicePosition slicePosition()
	{
		return slicePosition;
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	@Override
	public RandomAccess< T > randomAccess()
	{
		return new RA<>( delegate.randomAccess(), slicePos );
	}

	@Override
	public RandomAccess< T > randomAccess( final Interval interval )
	{
		// TODO: extract delegate part of interval and use that to construct the delegate RA
		return new RA<>( delegate.randomAccess(), slicePos );
	}

	@Override
	public long min( final int d )
	{
		return delegate.min( d );
	}

	@Override
	public long max( final int d )
	{
		return delegate.max( d );
	}

	private boolean contains( final int[] values, final int v )
	{
		for ( final int value : values )
			if ( value == v )
				return true;
		return false;
	}

	private static class SP extends Point implements SlicePosition
	{
		SP( final long[] pos )
		{
			super( pos, false ); // wrap pos
		}
	}

	private static class RA< T > extends Point implements RandomAccess< T >
	{
		private final RandomAccess< T > delegateAccess;

		private final long[] delegatePos;

		private final long[] slicePos;

		RA( final RandomAccess< T > delegateAccess, final long[] slicePos )
		{
			super( delegateAccess.numDimensions() - slicePos.length );
			this.delegateAccess = delegateAccess;
			this.delegatePos = new long[ delegateAccess.numDimensions() ];
			this.slicePos = slicePos;
		}

		@Override
		public T get()
		{
			System.arraycopy( position, 0, delegatePos, 0, position.length );
			System.arraycopy( slicePos, 0, delegatePos, position.length, slicePos.length );
			return delegateAccess.setPositionAndGet( delegatePos );
		}

		@Override
		public RandomAccess< T > copy()
		{
			return new RA<>( delegateAccess.copy(), slicePos );
		}
	}
}
