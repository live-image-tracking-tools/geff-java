package org.litt.geff.props;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedLongType;

public class VarLengthWriteProperty< T extends Type< T > > extends VarLengthProperty< T >
{

	private final VarLengthData< T > propertyData;

	/*
	 * Reusable cursors for set(). They are only valid for a row of exactly the
	 * dimensions they were created for, because RandomAccessibleIntervalCursor
	 * snapshots the interval bounds at construction. This is not just a
	 * concern for adapters that return the same RAI instance with changing
	 * dimensions (e.g. StringAsVarLengthPropertyAdapter): the target values()
	 * RAI itself has dynamic dimensions. Therefore set() reuses a cursor only
	 * if the dimensions of its source match those it was created for, and
	 * re-creates it otherwise. (Constant-size rows, the common case, reuse
	 * throughout; varying-size rows pay one cursor allocation per element,
	 * like before the optimization.)
	 */

	private Cursor< T > sourceCursor;

	private final long[] sourceCursorDims;

	private Cursor< T > targetCursor;

	private final long[] targetCursorDims;

	public VarLengthWriteProperty(
			final String identifier,
			final RandomAccessibleInterval< UnsignedLongType > propertyValues,
			final VarLengthData< T > propertyData,
			final RandomAccessibleInterval< ? extends BooleanType< ? > > propertyMissing, // optional
			final ElementIndex sharedElementIndex )
	{
		super( identifier, propertyValues, propertyData, propertyMissing, sharedElementIndex );
		this.propertyData = propertyData;
		final int n = numDimensions();
		sourceCursorDims = new long[ n ];
		targetCursorDims = new long[ n ];
	}

	@Override
	public void set( final GeffProperty< T > property )
	{
		if ( isOptional() )
		{
			final boolean missing = property.isMissing();
			missingAccess.get().set( missing );
			if ( missing )
				return;
		}

		final long offset = propertyData.size();
		valuesAccess.setPositionAndGet( 0 ).set( offset );
		final Dimensions dims = property.dimensions();
		final int n = numDimensions();
		for ( int i = 0; i < n; i++ )
			valuesAccess.setPositionAndGet( n - i ).set( dims.dimension( i ) );

		updateDataOffset( true );

		final RandomAccessibleInterval< T > source = property.values();
		final int size = ( int ) source.size();
		propertyData.growBy( size );
		targetCursor = cursorFor( values(), targetCursor, targetCursorDims );
		sourceCursor = cursorFor( source, sourceCursor, sourceCursorDims );
		while ( sourceCursor.hasNext() )
			targetCursor.next().set( sourceCursor.next() );
	}

	/**
	 * Returns a cursor over {@code rai}, reusing {@code cursor} if it was
	 * created for exactly the same dimensions (the current ones are compared
	 * with and stored in {@code dims}), and creating a new one otherwise.
	 */
	private Cursor< T > cursorFor(
			final RandomAccessibleInterval< T > rai,
			final Cursor< T > cursor,
			final long[] dims )
	{
		boolean matches = ( cursor != null );
		for ( int i = 0; matches && i < dims.length; i++ )
			matches = ( rai.dimension( i ) == dims[ i ] );
		if ( matches )
		{
			cursor.reset();
			return cursor;
		}
		for ( int i = 0; i < dims.length; i++ )
			dims[ i ] = rai.dimension( i );
		return rai.cursor();
	}
}
