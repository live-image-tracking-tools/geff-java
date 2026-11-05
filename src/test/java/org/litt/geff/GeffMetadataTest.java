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
package org.litt.geff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.litt.geff.GeffAxis;
import org.litt.geff.GeffMetadata;

/**
 * Tests for {@link GeffMetadata} and {@link GeffAxis}, salvaged from the old
 * {@code GeffTest} when the old-API classes were removed.
 */
public class GeffMetadataTest
{

	private GeffMetadata testMetadata;

	@BeforeEach
	void setUp()
	{
		// Create test metadata with GeffAxis
		testMetadata = new GeffMetadata( "0.2.2", true );

		// Create axes using GeffAxis
		final GeffAxis[] axes = {
				GeffAxis.createSpaceAxis( "x", GeffAxis.UNIT_MICROMETER, 0.0, 100.0 ),
				GeffAxis.createSpaceAxis( "y", GeffAxis.UNIT_MICROMETER, 0.0, 100.0 ),
				GeffAxis.createSpaceAxis( "z", GeffAxis.UNIT_MICROMETER, 0.0, 50.0 )
		};
		testMetadata.setGeffAxes( axes );
	}

	@Test
	@DisplayName( "Test version constant" )
	void testVersionConstant()
	{
		// The library version is read from the build metadata (pom), so we
		// cannot assert a fixed value; just that it is a non-empty string.
		assertNotNull( GeffMetadata.VERSION );
		assertFalse( GeffMetadata.VERSION.isEmpty() );
	}

	@Test
	@DisplayName( "Test metadata data structure" )
	void testMetadataDataStructure()
	{
		assertEquals( "0.2.2", testMetadata.getGeffVersion() );
		assertTrue( testMetadata.isDirected() );

		// Test GeffAxis array
		final GeffAxis[] axes = testMetadata.getGeffAxes();
		assertNotNull( axes );
		assertEquals( 3, axes.length );

		// Test individual axes
		assertEquals( "x", axes[ 0 ].getName() );
		assertEquals( GeffAxis.TYPE_SPACE, axes[ 0 ].getType() );
		assertEquals( GeffAxis.UNIT_MICROMETER, axes[ 0 ].getUnit() );
		assertEquals( 0.0, axes[ 0 ].getMin(), 0.001 );
		assertEquals( 100.0, axes[ 0 ].getMax(), 0.001 );

		assertEquals( "y", axes[ 1 ].getName() );
		assertEquals( "z", axes[ 2 ].getName() );
	}

	@Test
	@DisplayName( "Test metadata validation" )
	void testMetadataValidation()
	{
		assertDoesNotThrow( () -> testMetadata.validate() );

		// Test invalid metadata - create axes with invalid bounds
		final GeffMetadata invalidMetadata = new GeffMetadata();
		invalidMetadata.setGeffVersion( "0.2" );
		invalidMetadata.setDirected( false );

		// Create invalid axes (min > max)
		final GeffAxis[] invalidAxes = {
				new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 100.0, 50.0 ) // min
					// >
					// max
		};

		assertThrows( IllegalArgumentException.class, () -> {
			invalidMetadata.setGeffAxes( invalidAxes );
		} );
	}

	@Test
	@DisplayName( "Test development version format support" )
	void testDevelopmentVersionSupport()
	{
		assertDoesNotThrow( () -> {
			final GeffMetadata devMetadata = new GeffMetadata();
			devMetadata.setGeffVersion( "0.2.2.dev20+g611e7a2.d20250719" );
			devMetadata.setDirected( true );
			devMetadata.validate();
		} );
	}

	@Test
	@DisplayName( "Test version validation edge cases" )
	void testVersionValidationEdgeCases()
	{
		// Test null version (should be allowed)
		assertDoesNotThrow( () -> {
			final GeffMetadata metadata = new GeffMetadata();
			metadata.setGeffVersion( null );
		} );

		// Test various valid version formats
		final String[] validVersions = {
				"0.2", "0.3", "0.4",
				"0.2.0", "0.3.5",
				"0.2.2.dev20", "0.2.0-alpha.1", "0.3.0-beta.2+build.123"
		};

		for ( final String version : validVersions )
		{
			assertDoesNotThrow( () -> {
				final GeffMetadata metadata = new GeffMetadata();
				metadata.setGeffVersion( version );
			}, "Version " + version + " should be valid" );
		}

		// Test invalid versions
		final String[] invalidVersions = { "invalid", "0.1..x" };

		for ( final String version : invalidVersions )
		{
			assertThrows( IllegalArgumentException.class, () -> {
				final GeffMetadata metadata = new GeffMetadata();
				metadata.setGeffVersion( version );
			}, "Version " + version + " should be invalid" );
		}
	}
}
