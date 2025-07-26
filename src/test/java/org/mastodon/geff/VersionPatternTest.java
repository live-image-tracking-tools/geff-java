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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the version pattern works with development versions
 */
public class VersionPatternTest
{

    @Test
    @DisplayName( "Test valid version patterns are accepted" )
    public void testValidVersionPatterns()
    {
        // Test cases for different version formats that should be accepted
        String[] validVersions = {
                "0.2", // Basic major.minor
                "0.2.1", // With patch version
                "0.2.2", // Another patch version
                "0.2.2.dev20", // Development version
                "0.2.2.dev20+g611e7a2", // With git hash
                "0.2.2.dev20+g611e7a2.d20250719", // Full development version
                "0.3.0-alpha.1", // Alpha version
                "0.2.0-beta.2+build.123", // Beta with build metadata
                "0.2.5.rc1", // Release candidate
        };

        for ( String version : validVersions )
        {
            assertDoesNotThrow( () -> {
                GeffMetadata metadata = new GeffMetadata();
                metadata.setGeffVersion( version );
            }, "Version " + version + " should be accepted but was rejected" );
        }
    }

    @Test
    @DisplayName( "Test invalid version patterns are rejected" )
    public void testInvalidVersionPatterns()
    {
        // Test cases for version formats that should be rejected
        String[] invalidVersions = {
                "1.0", // Unsupported major version
                "invalid", // Not a version at all
                "0.1..x", // Invalid patch format
        };

        for ( String version : invalidVersions )
        {
            assertThrows( IllegalArgumentException.class, () -> {
                GeffMetadata metadata = new GeffMetadata();
                metadata.setGeffVersion( version );
            }, "Version " + version + " should be rejected but was accepted" );
        }
    }

    @Test
    @DisplayName( "Test specific development version format" )
    public void testDevelopmentVersionFormat()
    {
        String devVersion = "0.2.2.dev20+g611e7a2.d20250719";

        assertDoesNotThrow( () -> {
            GeffMetadata metadata = new GeffMetadata();
            metadata.setGeffVersion( devVersion );
            assertEquals( devVersion, metadata.getGeffVersion() );
        }, "Development version format should be supported" );
    }

    @Test
    @DisplayName( "Test null version is accepted" )
    public void testNullVersion()
    {
        assertDoesNotThrow( () -> {
            GeffMetadata metadata = new GeffMetadata();
            metadata.setGeffVersion( null );
            assertNull( metadata.getGeffVersion() );
        }, "Null version should be accepted" );
    }
}
