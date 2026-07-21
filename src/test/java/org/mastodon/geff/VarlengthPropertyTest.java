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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for VarlengthProperty (per-node design).
 */
public class VarlengthPropertyTest
{
	private VarlengthProperty property;

	@Before
	public void setUp()
	{
		// A VarlengthProperty holds data for a single node
		property = new VarlengthProperty( "test_polygon", "float64",
				new Object[] { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 } );
	}

	@Test
	public void testBasicProperties()
	{
		assertEquals( "test_polygon", property.getName() );
		assertEquals( "float64", property.getDtype() );
		assertNotNull( property.getData() );
		assertEquals( 6, property.getData().length );
		assertFalse( property.isMissing() );
	}

	@Test
	public void testGetData()
	{
		final Object[] data = property.getData();
		assertNotNull( data );
		assertEquals( 6, data.length );
		assertEquals( 0.0, data[ 0 ] );
		assertEquals( 5.0, data[ 5 ] );
	}

	@Test
	public void testNonMissingDefault()
	{
		assertFalse( property.isMissing() );
	}

	@Test
	public void testMissingFlag()
	{
		final VarlengthProperty missing = new VarlengthProperty( "test", "float64", null, true );
		assertTrue( missing.isMissing() );
		assertNull( missing.getData() );
	}

	@Test
	public void testToString()
	{
		final String str = property.toString();
		assertTrue( str.contains( "test_polygon" ) );
		assertTrue( str.contains( "float64" ) );
		assertTrue( str.contains( "length=6" ) );
		assertTrue( str.contains( "missing=false" ) );
	}

	@Test
	public void testEquals()
	{
		final VarlengthProperty same = new VarlengthProperty( "test_polygon", "float64",
				new Object[] { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 } );
		assertEquals( property, same );

		final VarlengthProperty diffName = new VarlengthProperty( "other", "float64",
				new Object[] { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 } );
		assertNotEquals( property, diffName );

		final VarlengthProperty diffData = new VarlengthProperty( "test_polygon", "float64",
				new Object[] { 9.0 } );
		assertNotEquals( property, diffData );
	}

	@Test
	public void testHashCode()
	{
		final VarlengthProperty same = new VarlengthProperty( "test_polygon", "float64",
				new Object[] { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 } );
		assertEquals( property.hashCode(), same.hashCode() );
	}
}
