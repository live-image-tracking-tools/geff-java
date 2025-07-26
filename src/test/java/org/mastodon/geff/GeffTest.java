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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Test class for the main Geff functionality
 */
public class GeffTest
{

    private GeffMetadata testMetadata;

    private List< GeffNode > testNodes;

    private List< GeffEdge > testEdges;

    @BeforeEach
    void setUp()
    {
        // Create test metadata with GeffAxis
        testMetadata = new GeffMetadata( "0.2.2", true );

        // Create axes using GeffAxis
        GeffAxis[] axes = {
                GeffAxis.createSpaceAxis( "x", GeffAxis.UNIT_MICROMETERS, 0.0, 100.0 ),
                GeffAxis.createSpaceAxis( "y", GeffAxis.UNIT_MICROMETERS, 0.0, 100.0 ),
                GeffAxis.createSpaceAxis( "z", GeffAxis.UNIT_MICROMETERS, 0.0, 50.0 )
        };
        testMetadata.setGeffAxes( axes );

        // Create test nodes
        testNodes = new ArrayList<>();
        for ( int i = 0; i < 5; i++ )
        {
            GeffNode node = new GeffNode();
            node.setT( i ); // Use setT instead of setTimepoint
            node.setX( i * 10.0 );
            node.setY( i * 15.0 );
            node.setSegmentId( i + 100 );
            // Note: setPosition is deprecated, using individual coordinates instead
            testNodes.add( node );
        }

        // Create test edges
        testEdges = new ArrayList<>();
        for ( int i = 0; i < 4; i++ )
        {
            GeffEdge edge = new GeffEdge( i, i, i + 1, 0.5, 10.0 ); // id,
                                                                    // source,
                                                                    // target,
                                                                    // score,
                                                                    // distance
            testEdges.add( edge );
        }
    }

    @Test
    @DisplayName( "Test Geff version constant" )
    void testVersionConstant()
    {
        assertNotNull( Geff.VERSION );
        assertFalse( Geff.VERSION.isEmpty() );
        assertEquals( "0.3.0", Geff.VERSION );
    }

    @Test
    @DisplayName( "Test Geff object creation and getters" )
    void testGeffObjectCreation()
    {
        // Create Geff object using reflection since constructor is private
        // We'll test the functionality through the main method behavior
        assertDoesNotThrow( () -> {
            // Test that we can create the components that would go into a Geff
            // object
            assertNotNull( testMetadata );
            assertNotNull( testNodes );
            assertNotNull( testEdges );

            // Verify the test data is properly set up
            assertEquals( 5, testNodes.size() );
            assertEquals( 4, testEdges.size() );
            assertEquals( "0.2.2", testMetadata.getGeffVersion() );
        } );
    }

    @Test
    @DisplayName( "Test node data structure" )
    void testNodeDataStructure()
    {
        GeffNode node = testNodes.get( 0 );

        assertEquals( 0, node.getT() ); // Use getT instead of getTimepoint
        assertEquals( 0.0, node.getX(), 0.001 );
        assertEquals( 0.0, node.getY(), 0.001 );
        assertEquals( 100, node.getSegmentId() );
        // Note: getPosition is deprecated, testing individual coordinates
        // instead
    }

    @Test
    @DisplayName( "Test edge data structure" )
    void testEdgeDataStructure()
    {
        GeffEdge edge = testEdges.get( 0 );

        assertEquals( 0, edge.getId() ); // Use getId for edge ID
        assertEquals( 0, edge.getSourceNodeId() ); // Use getSourceNodeId
        assertEquals( 1, edge.getTargetNodeId() ); // Use getTargetNodeId
        assertEquals( 0.5, edge.getScore(), 0.001 );
        assertEquals( 10.0, edge.getDistance(), 0.001 );
    }

    @Test
    @DisplayName( "Test metadata data structure" )
    void testMetadataDataStructure()
    {
        assertEquals( "0.2.2", testMetadata.getGeffVersion() );
        assertTrue( testMetadata.isDirected() );

        // Test GeffAxis array
        GeffAxis[] axes = testMetadata.getGeffAxes();
        assertNotNull( axes );
        assertEquals( 3, axes.length );

        // Test individual axes
        assertEquals( "x", axes[ 0 ].getName() );
        assertEquals( GeffAxis.TYPE_SPACE, axes[ 0 ].getType() );
        assertEquals( GeffAxis.UNIT_MICROMETERS, axes[ 0 ].getUnit() );
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
        GeffMetadata invalidMetadata = new GeffMetadata();
        invalidMetadata.setGeffVersion( "0.2" );
        invalidMetadata.setDirected( false );

        // Create invalid axes (min > max)
        GeffAxis[] invalidAxes = {
                new GeffAxis( "x", GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETERS, 100.0, 50.0 ) // min
                                                                                                 // >
                                                                                                 // max
        };

        assertThrows( IllegalArgumentException.class, () -> {
            invalidMetadata.setGeffAxes( invalidAxes );
        } );
    }

    @Test
    @DisplayName( "Test node list operations" )
    void testNodeListOperations()
    {
        assertFalse( testNodes.isEmpty() );
        assertEquals( 5, testNodes.size() );

        // Test that nodes can be accessed and modified
        GeffNode firstNode = testNodes.get( 0 );
        assertNotNull( firstNode );

        // Test adding a new node
        GeffNode newNode = new GeffNode();
        newNode.setT( 10 ); // Use setT instead of setTimepoint
        newNode.setSegmentId( 200 );
        testNodes.add( newNode );
        assertEquals( 6, testNodes.size() );
    }

    @Test
    @DisplayName( "Test edge list operations" )
    void testEdgeListOperations()
    {
        assertFalse( testEdges.isEmpty() );
        assertEquals( 4, testEdges.size() );

        // Test that edges can be accessed and modified
        GeffEdge firstEdge = testEdges.get( 0 );
        assertNotNull( firstEdge );

        // Test adding a new edge
        GeffEdge newEdge = new GeffEdge( 10, 100, 101, 0.8, 15.0 ); // id,
                                                                    // source,
                                                                    // target,
                                                                    // score,
                                                                    // distance
        testEdges.add( newEdge );
        assertEquals( 5, testEdges.size() );
    }

    @Test
    @DisplayName( "Test write operations work correctly" )
    void testWriteOperations( @TempDir Path tempDir )
    {
        String tempPath = tempDir.toString() + "/test.zarr/tracks";

        // Test that write operations complete without throwing exceptions
        assertDoesNotThrow( () -> {
            try
            {
                GeffNode.writeToZarr( testNodes, tempPath, 1000 );
                GeffEdge.writeToZarr( testEdges, tempPath, 1000 );
                GeffMetadata.writeToZarr( testMetadata, tempPath );
            }
            catch ( Exception e )
            {
                // If any exception occurs, fail with details
				e.printStackTrace();
                fail( "Write operations should not throw exceptions: " + e.getMessage() );
            }
        } );
    }

    @Test
    @DisplayName( "Test development version format support" )
    void testDevelopmentVersionSupport()
    {
        assertDoesNotThrow( () -> {
            GeffMetadata devMetadata = new GeffMetadata();
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
            GeffMetadata metadata = new GeffMetadata();
            metadata.setGeffVersion( null );
        } );

        // Test various valid version formats
        String[] validVersions = {
                "0.2", "0.3",
                "0.2.0", "0.3.5",
                "0.2.2.dev20", "0.2.0-alpha.1", "0.3.0-beta.2+build.123"
        };

        for ( String version : validVersions )
        {
            assertDoesNotThrow( () -> {
                GeffMetadata metadata = new GeffMetadata();
                metadata.setGeffVersion( version );
            }, "Version " + version + " should be valid" );
        }

        // Test invalid versions
        String[] invalidVersions = { "1.0", "0.4", "invalid", "0.1..x" };

        for ( String version : invalidVersions )
        {
            assertThrows( IllegalArgumentException.class, () -> {
                GeffMetadata metadata = new GeffMetadata();
                metadata.setGeffVersion( version );
            }, "Version " + version + " should be invalid" );
        }
    }
}
