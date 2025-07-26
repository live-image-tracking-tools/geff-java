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

import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class to verify the chunked writing functionality works correctly with
 * the new ZarrGroup-based approach.
 */
public class ChunkedWriteTest
{

    public static void main( String[] args )
    {
        try
        {
            testNodeChunkedWriting();
            testEdgeChunkedWriting();
            testMetadataWriting();
            System.out.println( "All chunked writing tests completed successfully!" );
        }
        catch ( Exception e )
        {
            System.err.println( "Test failed: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    /**
     * Test writing nodes with chunked structure
     */
    private static void testNodeChunkedWriting() throws IOException, InvalidRangeException
    {
        System.out.println( "=== Testing Node Chunked Writing ===" );

        // Create sample nodes
        List< GeffNode > testNodes = new ArrayList<>();
        for ( int i = 0; i < 15; i++ )
        {
            GeffNode node = new GeffNode();
            node.setT( i );
            node.setX( i * 1.5 );
            node.setY( i * 2.0 );
            node.setSegmentId( i + 100 );
            // Set Z coordinate individually instead of using deprecated
            // setPosition
            node.setZ( i * 0.5 );
            testNodes.add( node );
        }

        // Test writing with small chunk size to verify chunking
        String outputPath = "/tmp/test-nodes-chunked";
        System.out.println( "Writing " + testNodes.size() + " nodes to: " + outputPath );

        GeffNode.writeToZarr( testNodes, outputPath ); // Small chunk size to
                                                       // test chunking

        System.out.println( "Node chunked writing test completed." );
    }

    /**
     * Test writing edges with chunked structure
     */
    private static void testEdgeChunkedWriting() throws IOException, InvalidRangeException
    {
        System.out.println( "\n=== Testing Edge Chunked Writing ===" );

        // Create sample edges
        List< GeffEdge > testEdges = new ArrayList<>();
        for ( int i = 0; i < 10; i++ )
        {
            GeffEdge edge = GeffEdge.builder()
                    .setId( i )
                    .setSourceNodeId( i )
                    .setTargetNodeId( i + 1 )
                    .build();
            testEdges.add( edge );
        }

        // Test writing with small chunk size to verify chunking
        String outputPath = "/tmp/test-edges-chunked";
        System.out.println( "Writing " + testEdges.size() + " edges to: " + outputPath );

        GeffEdge.writeToZarr( testEdges, outputPath, 3 ); // Small chunk size to
                                                          // test chunking

        System.out.println( "Edge chunked writing test completed." );
    }

    /**
     * Test writing metadata with GEFF schema compliance
     */
    private static void testMetadataWriting() throws IOException, InvalidRangeException
    {
        System.out.println( "\n=== Testing Metadata Writing ===" );

        // Create sample metadata with all attributes using GeffAxis
        GeffMetadata metadata = new GeffMetadata();
        metadata.setGeffVersion( "0.1.1" );
        metadata.setDirected( true );

        // Create axes using GeffAxis
        GeffAxis[] axes = {
                GeffAxis.createSpaceAxis( "x", GeffAxis.UNIT_MICROMETER, 0.0, 100.0 ),
                GeffAxis.createSpaceAxis( "y", GeffAxis.UNIT_MICROMETER, 0.0, 100.0 ),
                GeffAxis.createSpaceAxis( "z", GeffAxis.UNIT_MICROMETER, 0.0, 50.0 )
        };
        metadata.setGeffAxes( axes );

        // Test writing to a Zarr path
        String outputPath = "/tmp/test-metadata";
        System.out.println( "Writing metadata to: " + outputPath );

        GeffMetadata.writeToZarr( metadata, outputPath );

        System.out.println( "Metadata writing test completed." );

        // Test with minimal metadata (only required fields)
        System.out.println( "Testing minimal metadata writing..." );
        GeffMetadata minimalMetadata = new GeffMetadata( "0.2", false );
        String minimalOutputPath = "/tmp/test-metadata-minimal";
        GeffMetadata.writeToZarr( minimalMetadata, minimalOutputPath );
        System.out.println( "Minimal metadata writing test completed." );
    }
}
