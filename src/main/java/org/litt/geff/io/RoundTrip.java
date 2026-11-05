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
package org.litt.geff.io;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.litt.geff.GeffMetadata;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.GeffProperties;

/**
 * CLI tool that reads a GEFF file and writes it back unchanged, using only
 * the generic {@link IoUtils} API (no application-specific data model).
 * <p>
 * It is used by the cross-language round-trip tests (see the {@code
 * cross-language-tests/} folder) to validate interoperability with the
 * Python geff implementation: Python creates a GEFF file, this tool reads it
 * and rewrites it, and the two files are then compared.
 */
public class RoundTrip
{
	public static void main( final String[] args ) throws IOException
	{
		if ( args.length != 2 )
		{
			System.err.println( "Usage: RoundTrip <input.zarr> <output.zarr>" );
			System.err.println( "  Reads a GEFF from input path and writes it to output path." );
			System.exit( 1 );
		}

		final String inputPath = args[ 0 ];
		final String outputPath = args[ 1 ];

		System.out.println( "Round-trip GEFF test" );
		System.out.println( "  Input:  " + inputPath );
		System.out.println( "  Output: " + outputPath );

		try
		{
			// Read metadata.
			System.out.println( "Reading metadata..." );
			final GeffMetadata metadata = GeffMetadata.readFromZarr( inputPath );
			System.out.println( "  Version:  " + metadata.getGeffVersion() );
			System.out.println( "  Directed: " + metadata.isDirected() );

			// Read nodes and edges as generic GeffProperties.
			final N5Reader n5 = new N5ZarrReader( inputPath );
			System.out.println( "Reading nodes..." );
			final GeffProperties nodes = IoUtils.loadProperties( n5, ElementType.NODE );
			System.out.println( "  Read " + nodes.numElements() + " nodes" );
			final GeffProperties edges;
			if ( n5.exists( "edges/ids" ) )
			{
				System.out.println( "Reading edges..." );
				edges = IoUtils.loadProperties( n5, ElementType.EDGE );
				System.out.println( "  Read " + edges.numElements() + " edges" );
			}
			else
			{
				edges = null;
				System.out.println( "  No edges group found." );
			}

			// Write properties first, metadata last so that all data is in
			// place when the metadata is written.
			try (final N5ZarrWriter out = new N5ZarrWriter( outputPath ))
			{
				System.out.println( "Writing nodes..." );
				IoUtils.writeProperties( out, nodes, null, 0, null );
				if ( edges != null )
				{
					System.out.println( "Writing edges..." );
					IoUtils.writeProperties( out, edges, null, 0, null );
				}
			}
			System.out.println( "Writing metadata..." );
			GeffMetadata.writeToZarr( metadata, outputPath );

			System.out.println( "\nRound-trip complete!" );
			System.exit( 0 );
		}
		catch ( final Exception e )
		{
			System.err.println( "\nERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage() );
			e.printStackTrace();
			System.exit( 2 );
		}
	}
}
