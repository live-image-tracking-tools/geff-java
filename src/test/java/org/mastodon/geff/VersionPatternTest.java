package org.mastodon.geff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify the version pattern works with development versions
 */
public class VersionPatternTest {

    @Test
    @DisplayName("Test valid version patterns are accepted")
    public void testValidVersionPatterns() {
        // Test cases for different version formats that should be accepted
        String[] validVersions = {
                "0.1", // Basic major.minor
                "0.1.1", // With patch version
                "0.2.2", // Another patch version
                "0.2.2.dev20", // Development version
                "0.2.2.dev20+g611e7a2", // With git hash
                "0.2.2.dev20+g611e7a2.d20250719", // Full development version
                "0.3.0-alpha.1", // Alpha version
                "0.1.0-beta.2+build.123", // Beta with build metadata
                "0.0.5.rc1", // Release candidate
        };

        for (String version : validVersions) {
            assertDoesNotThrow(() -> {
                GeffMetadata metadata = new GeffMetadata();
                metadata.setGeffVersion(version);
            }, "Version " + version + " should be accepted but was rejected");
        }
    }

    @Test
    @DisplayName("Test invalid version patterns are rejected")
    public void testInvalidVersionPatterns() {
        // Test cases for version formats that should be rejected
        String[] invalidVersions = {
                "1.0", // Unsupported major version
                "0.4", // Unsupported minor version
                "invalid", // Not a version at all
                "0.1..x", // Invalid patch format
        };

        for (String version : invalidVersions) {
            assertThrows(IllegalArgumentException.class, () -> {
                GeffMetadata metadata = new GeffMetadata();
                metadata.setGeffVersion(version);
            }, "Version " + version + " should be rejected but was accepted");
        }
    }

    @Test
    @DisplayName("Test specific development version format")
    public void testDevelopmentVersionFormat() {
        String devVersion = "0.2.2.dev20+g611e7a2.d20250719";

        assertDoesNotThrow(() -> {
            GeffMetadata metadata = new GeffMetadata();
            metadata.setGeffVersion(devVersion);
            assertEquals(devVersion, metadata.getGeffVersion());
        }, "Development version format should be supported");
    }

    @Test
    @DisplayName("Test null version is accepted")
    public void testNullVersion() {
        assertDoesNotThrow(() -> {
            GeffMetadata metadata = new GeffMetadata();
            metadata.setGeffVersion(null);
            assertNull(metadata.getGeffVersion());
        }, "Null version should be accepted");
    }
}
