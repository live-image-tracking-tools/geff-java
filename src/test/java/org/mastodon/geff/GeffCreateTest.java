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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.InvalidRangeException;

public class GeffCreateTest
{

        public static void main( String[] args ) throws IOException, InvalidRangeException
        {
                List< GeffNode > writeNodes = new ArrayList<>();
                GeffNode node0 = new GeffNode.Builder()
                                .id( 0 )
                                .timepoint( 0 )
                                .x( 10.5 )
                                .y( 20.3 )
                                .z( 5.0 )
                                .segmentId( 0 )
                                .color( new double[] { 1.0, 0.0, 0.0, 1.0 } )
                                .radius( 2.5 )
                                .covariance2d( new double[] { 1.0, 0.2, 0.2, 1.5 } )
                                .polygonX( new double[] { 0.1, 0.2, 0.3, 0.4 } )
                                .polygonY( new double[] { 0.5, 0.6, 0.7, 0.8 } )
                                .build();
                writeNodes.add( node0 );

                GeffNode node1 = new GeffNode.Builder()
                                .id( 1 )
                                .timepoint( 1 )
                                .x( 11.5 )
                                .y( 21.3 )
                                .z( 6.0 )
                                .segmentId( 1 )
                                .covariance2d( new double[] { 0.8, 0.1, 0.1, 1.2 } )
                                .polygonX( new double[] { -0.1, -0.2, -0.3, -0.4 } )
                                .polygonY( new double[] { -0.5, -0.6, -0.7, -0.8 } )
                                .build();
                writeNodes.add( node1 );

                // Write to Zarr format with version specification
                GeffNode.writeToZarr( writeNodes,
                                "src/test/resources/create_test_output.zarr/tracks",
                                "0.4.0" );

                // Create new edges using builder pattern
                List< GeffEdge > writeEdges = new ArrayList<>();
                GeffEdge edge = new GeffEdge.Builder()
                                .setId( 0 )
                                .setSourceNodeId( 0 )
                                .setTargetNodeId( 1 )
                                .setScore( 0.95 )
                                .setDistance( 1.4 )
                                .build();
                writeEdges.add( edge );

                // Write to Zarr format
                GeffEdge.writeToZarr( writeEdges,
                                "src/test/resources/create_test_output.zarr/tracks",
                                "0.4.0" );

                // Create metadata with axis information
                GeffAxis[] axes = {
                                new GeffAxis( GeffAxis.NAME_TIME, GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND, 0.0, 100.0 ),
                                new GeffAxis( GeffAxis.NAME_SPACE_X, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 1024.0 ),
                                new GeffAxis( GeffAxis.NAME_SPACE_Y, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 1024.0 ),
                                new GeffAxis( GeffAxis.NAME_SPACE_Z, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0 )
                };
                GeffMetadata writeMetadata = new GeffMetadata( "0.4.0", true, axes );
                GeffMetadata.writeToZarr( writeMetadata,
                                "src/test/resources/create_test_output.zarr/tracks" );

                GeffMetadata readMetadata = GeffMetadata.readFromZarr(
                                "src/test/resources/create_test_output.zarr/tracks" );
                List< GeffNode > readNodes = GeffNode.readFromZarr(
                                "src/test/resources/create_test_output.zarr/tracks",
                                readMetadata.getGeffVersion() );
                List< GeffEdge > readEdges = GeffEdge.readFromZarr(
                                "src/test/resources/create_test_output.zarr/tracks",
                                readMetadata.getGeffVersion() );
                // Check if read nodes and edges match written data with
                // assertions
                for ( int i = 0; i < writeNodes.size(); i++ )
                {
                        assert writeNodes.get( i ).equals( readNodes.get( i ) ): "Node mismatch at index " + i;
                }
                for ( int i = 0; i < writeEdges.size(); i++ )
                {
                        assert writeEdges.get( i ).equals( readEdges.get( i ) ): "Edge mismatch at index " + i;
                }
                assert writeMetadata.equals( readMetadata ): "Metadata mismatch";
                System.out.println( "GeffCreateTest completed successfully!" );
        }
}
