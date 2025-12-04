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

/**
 * CLI tool for round-trip testing of GEFF files.
 * Reads a GEFF from an input path and writes it to an output path.
 * Used for cross-language interoperability testing with Python.
 */
public class RoundTripGeff
{
	public static void main( String[] args )
	{
		if ( args.length < 2 )
		{
			System.err.println( "Usage: RoundTripGeff <input.zarr> <output.zarr>" );
			System.err.println( "  Reads a GEFF from input path and writes to output path." );
			System.exit( 1 );
		}

		String inputPath = args[ 0 ];
		String outputPath = args[ 1 ];

		System.out.println( "Round-trip GEFF test" );
		System.out.println( "  Input:  " + inputPath );
		System.out.println( "  Output: " + outputPath );

		try
		{
			// Read metadata
			System.out.println( "\nReading metadata..." );
			GeffMetadata metadata = GeffMetadata.readFromZarr( inputPath );
			System.out.println( "  Version: " + metadata.getGeffVersion() );
			System.out.println( "  Directed: " + metadata.isDirected() );

			// Read nodes
			System.out.println( "\nReading nodes..." );
			List< GeffNode > nodes = GeffNode.readFromZarr( inputPath, metadata.getGeffVersion() );
			System.out.println( "  Read " + nodes.size() + " nodes" );

			// Read edges
			System.out.println( "\nReading edges..." );
			List< GeffEdge > edges = GeffEdge.readFromZarr( inputPath, metadata.getGeffVersion() );
			System.out.println( "  Read " + edges.size() + " edges" );

			// Write metadata
			System.out.println( "\nWriting metadata..." );
			GeffMetadata.writeToZarr( metadata, outputPath );

			// Write nodes
			System.out.println( "Writing nodes..." );
			GeffNode.writeToZarr( nodes, outputPath, metadata.getGeffVersion() );

			// Write edges
			System.out.println( "Writing edges..." );
			GeffEdge.writeToZarr( edges, outputPath, metadata.getGeffVersion() );

			System.out.println( "\nRound-trip complete!" );
			System.exit( 0 );
		}
		catch ( Exception e )
		{
			System.err.println( "\nERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
			e.printStackTrace();
			System.exit( 2 );
		}
	}
}
