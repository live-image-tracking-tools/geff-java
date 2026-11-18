/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 - 2026 Ko Sugawara, Jean-Yves Tinevez
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
package org.litt.geff.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@code geffGroup} path normalization in {@link IoUtils}.
 */
public class IoUtilsPathTest
{

	@Test
	@DisplayName( "Null and empty group paths normalize to the empty string" )
	void testEmpty()
	{
		assertEquals( "", IoUtils.normalizeGroupPath( null ) );
		assertEquals( "", IoUtils.normalizeGroupPath( "" ) );
		assertEquals( "", IoUtils.normalizeGroupPath( "/" ) );
		assertEquals( "", IoUtils.normalizeGroupPath( "///" ) );
		assertEquals( "", IoUtils.normalizeGroupPath( "." ) );
	}

	@Test
	@DisplayName( "Simple group paths get a trailing slash" )
	void testSimple()
	{
		assertEquals( "tracks/", IoUtils.normalizeGroupPath( "tracks" ) );
		assertEquals( "tracks/", IoUtils.normalizeGroupPath( "tracks/" ) );
		assertEquals( "a/b/", IoUtils.normalizeGroupPath( "a/b" ) );
		assertEquals( "a/b/", IoUtils.normalizeGroupPath( "a/b/" ) );
	}

	@Test
	@DisplayName( "Leading and duplicated slashes are dropped" )
	void testSlashes()
	{
		assertEquals( "tracks/", IoUtils.normalizeGroupPath( "/tracks" ) );
		assertEquals( "tracks/", IoUtils.normalizeGroupPath( "/tracks/" ) );
		assertEquals( "a/b/", IoUtils.normalizeGroupPath( "a//b" ) );
		assertEquals( "a/b/", IoUtils.normalizeGroupPath( "//a//b//" ) );
	}

	@Test
	@DisplayName( "Current-directory components are dropped" )
	void testDot()
	{
		assertEquals( "tracks/", IoUtils.normalizeGroupPath( "./tracks" ) );
		assertEquals( "a/b/", IoUtils.normalizeGroupPath( "a/./b" ) );
		assertEquals( "a/", IoUtils.normalizeGroupPath( "a/." ) );
	}

	@Test
	@DisplayName( "Parent-directory components are rejected" )
	void testDotDot()
	{
		assertThrows( IllegalArgumentException.class, () -> IoUtils.normalizeGroupPath( ".." ) );
		assertThrows( IllegalArgumentException.class, () -> IoUtils.normalizeGroupPath( "a/../b" ) );
		assertThrows( IllegalArgumentException.class, () -> IoUtils.normalizeGroupPath( "a/b/../.." ) );
	}

	@Test
	@DisplayName( "Group names containing dots as part of their name are kept" )
	void testDotsInNames()
	{
		// ".geff"-style names are legal group names; only exact "." and ".."
		// components are special.
		assertEquals( "tracks.v2/", IoUtils.normalizeGroupPath( "tracks.v2" ) );
		assertEquals( "my.geff/", IoUtils.normalizeGroupPath( "my.geff" ) );
	}
}
