package org.litt.geff.bind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.litt.geff.bind.Construction.buildElements;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.litt.geff.bind.Construction.FromId;
import org.litt.geff.bind.Construction.FromProperty;
import org.litt.geff.bind.Construction.NodeConstructor;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FixedLengthProperty;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;

import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Tests for binding boxed optional types ({@code Optional<Double>} etc.) to
 * constructor parameters, and for the error messages of unsupported
 * parameter types.
 */
public class BoxedOptionalBindingTest
{

	private static GeffProperties properties(
			final double[] xValues,
			final boolean[] xMissing,
			final boolean[] bValues,
			final boolean[] bMissing )
	{
		final var elementIndex = new org.litt.geff.props.ElementIndex();
		final var id = new FixedLengthProperty<>( "id", ArrayImgs.unsignedLongs( new long[] { 10, 11, 12 }, 3 ), null, elementIndex );
		final var xImg = ArrayImgs.doubles( xValues, 3 );
		final var xMissingImg = xMissing == null ? null : new ArrayImgFactory<>( new BitType() ).create( 3 );
		if ( xMissing != null )
			for ( int i = 0; i < 3; i++ )
				xMissingImg.randomAccess().setPositionAndGet( i ).set( xMissing[ i ] );
		final GeffProperty< DoubleType > x = new FixedLengthProperty<>( "x", xImg, xMissingImg, elementIndex );
		final var bImg = new ArrayImgFactory<>( new BitType() ).create( 3 );
		final var bMissingImg = bMissing == null ? null : new ArrayImgFactory<>( new BitType() ).create( 3 );
		for ( int i = 0; i < 3; i++ )
			bImg.randomAccess().setPositionAndGet( i ).set( bValues[ i ] );
		if ( bMissing != null )
			for ( int i = 0; i < 3; i++ )
				bMissingImg.randomAccess().setPositionAndGet( i ).set( bMissing[ i ] );
		final GeffProperty< BitType > b = new FixedLengthProperty<>( "b", bImg, bMissingImg, elementIndex );
		return new GeffProperties( ElementType.NODE, id, List.of( x, b ) );
	}

	@Test
	@DisplayName( "Bind boxed Optional scalars, present and missing" )
	void testBoxedOptionalBinding() throws Throwable
	{

		final long[] ids = new long[ 3 ];
		final double[] xs = new double[ 3 ];
		final boolean[] hasX = new boolean[ 3 ];
		final boolean[] bs = new boolean[ 3 ];
		final boolean[] hasB = new boolean[ 3 ];

		final GeffProperties props = properties(
				new double[] { 1.5, 2.5, 3.5 },
				new boolean[] { false, true, false },
				new boolean[] { true, false, true },
				new boolean[] { false, false, true } );

		class Builder
		{
			@NodeConstructor
			public void addNode(
					@FromId final long id,
					@FromProperty( "x" ) final Optional< Double > x,
					@FromProperty( "b" ) final Optional< Boolean > b )
			{
				ids[ ( int ) ( id - 10 ) ] = id;
				if ( x.isPresent() )
				{
					xs[ ( int ) ( id - 10 ) ] = x.get();
					hasX[ ( int ) ( id - 10 ) ] = true;
				}
				if ( b.isPresent() )
				{
					bs[ ( int ) ( id - 10 ) ] = b.get();
					hasB[ ( int ) ( id - 10 ) ] = true;
				}
			}
		}

		buildElements( new Builder(), props );

		// x: present, missing, present
		assertTrue( hasX[ 0 ] );
		assertEquals( 1.5, xs[ 0 ], 1e-12 );
		assertFalse( hasX[ 1 ] );
		assertTrue( hasX[ 2 ] );
		assertEquals( 3.5, xs[ 2 ], 1e-12 );

		// b: true, false, missing
		assertTrue( hasB[ 0 ] && bs[ 0 ] );
		assertTrue( hasB[ 1 ] && !bs[ 1 ] );
		assertFalse( hasB[ 2 ] );

		// ids
		assertEquals( 10, ids[ 0 ] );
		assertEquals( 11, ids[ 1 ] );
		assertEquals( 12, ids[ 2 ] );
	}

	@Test
	@DisplayName( "Bare boxed scalar parameters are rejected with a helpful message" )
	void testBareBoxedScalarRejected()
	{
		final GeffProperties props = properties(
				new double[] { 1, 2, 3 }, null, new boolean[] { true, true, true }, null );

		class Builder
		{
			@NodeConstructor
			public void addNode( @FromId final long id, @FromProperty( "x" ) final Double x )
			{}
		}

		final IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> buildElements( new Builder(), props ) );
		assertTrue( e.getMessage().contains( "wrapped in Optional<...> or Maybe<...>" ), e.getMessage() );
		assertTrue( e.getMessage().contains( "double" ), e.getMessage() );
	}

	@Test
	@DisplayName( "Unsupported parameter types are rejected with a helpful message" )
	void testUnsupportedTypeRejected()
	{
		final GeffProperties props = properties(
				new double[] { 1, 2, 3 }, null, new boolean[] { true, true, true }, null );

		class Builder
		{
			@NodeConstructor
			public void addNode( @FromId final long id, @FromProperty( "x" ) final java.util.concurrent.atomic.AtomicReference< Double > x )
			{}
		}

		final IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> buildElements( new Builder(), props ) );
		assertTrue( e.getMessage().contains( "Unsupported constructor parameter type" ), e.getMessage() );
		assertTrue( e.getMessage().contains( "Supported types" ), e.getMessage() );
	}
}
