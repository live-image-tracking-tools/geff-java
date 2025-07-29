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

import static org.mastodon.geff.GeffUtils.checkSupportedVersion;
import static org.mastodon.geff.GeffUtils.verifyLength;

import java.util.ArrayList;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffUtils.FlattenedInts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an edge in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing edge data from/to Zarr format. An edge
 * connects two nodes in a tracking graph, typically representing temporal
 * connections between objects across time points.
 */
public class GeffEdge
{
	private static final Logger LOG = LoggerFactory.getLogger( GeffEdge.class );

	public static final int DEFAULT_EDGE_ID = -1; // Default ID for edges if not
	                                              // specified

	public static final double DEFAULT_SCORE = -1; // Default score for edges if
	                                               // not specified

	public static final double DEFAULT_DISTANCE = -1; // Default distance for
	                                                  // edges if not specified

	// Edge attributes
	private int sourceNodeId;

	private int targetNodeId;

	private int id; // Edge ID if available

	private double score; // Optional score for the edge

	private double distance; // Optional distance metric for the edge

	/**
	 * Default constructor
	 */
	public GeffEdge()
	{}

	/**
	 * Constructor with edge ID, source and target node IDs
	 */
	public GeffEdge( int id, int sourceNodeId, int targetNodeId, double score, double distance )
	{
		this.id = id;
		this.sourceNodeId = sourceNodeId;
		this.targetNodeId = targetNodeId;
		this.score = score;
		this.distance = distance;
	}

	// Getters and Setters
	public int getId()
	{
		return id;
	}

	public void setId( int id )
	{
		this.id = id;
	}

	public int getSourceNodeId()
	{
		return sourceNodeId;
	}

	public void setSourceNodeId( int sourceNodeId )
	{
		this.sourceNodeId = sourceNodeId;
	}

	public int getTargetNodeId()
	{
		return targetNodeId;
	}

	public void setTargetNodeId( int targetNodeId )
	{
		this.targetNodeId = targetNodeId;
	}

	public double getScore()
	{
		return score;
	}

	public void setScore( double score )
	{
		this.score = score;
	}

	public double getDistance()
	{
		return distance;
	}

	public void setDistance( double distance )
	{
		this.distance = distance;
	}

	/**
	 * Builder pattern for creating GeffEdge instances
	 */
	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private int id = DEFAULT_EDGE_ID;

		private int sourceNodeId;

		private int targetNodeId;

		private double score = DEFAULT_SCORE;

		private double distance = DEFAULT_DISTANCE;

		public Builder setId( int id )
		{
			this.id = id;
			return this;
		}

		public Builder setSourceNodeId( int sourceNodeId )
		{
			this.sourceNodeId = sourceNodeId;
			return this;
		}

		public Builder setTargetNodeId( int targetNodeId )
		{
			this.targetNodeId = targetNodeId;
			return this;
		}

		public Builder setScore( double score )
		{
			this.score = score;
			return this;
		}

		public Builder setDistance( double distance )
		{
			this.distance = distance;
			return this;
		}

		public GeffEdge build()
		{
			return new GeffEdge( id, sourceNodeId, targetNodeId, score, distance );
		}
	}

	/**
	 * Read edges from a Zarr group
	 */
	public static List< GeffEdge > readFromZarr( String zarrPath )
	{
		return readFromZarr( zarrPath, Geff.VERSION );
	}

	public static List< GeffEdge > readFromZarr( String zarrPath, String geffVersion )
	{
		LOG.debug( "Reading edges from Zarr path: " + zarrPath + " with Geff version: " + geffVersion );
		try ( final N5ZarrReader reader = new N5ZarrReader( zarrPath, true ) )
		{
			return readFromN5( reader, "/", geffVersion );
		}
	}

	public static List< GeffEdge > readFromN5( final N5Reader reader, final String group, final String geffVersion )
	{
		checkSupportedVersion( geffVersion );
		final String path = N5URI.normalizeGroupPath( group );

//		final DatasetAttributes attributes = reader.getDatasetAttributes( path + "/edges/ids" );
//		System.out.println( "attributes.getNumDimensions() = " + attributes.getNumDimensions() );
//		System.out.println( "attributes.getDimensions() = " + Arrays.toString( attributes.getDimensions() ) );
//		System.out.println( "attributes.getBlockSize() = " + Arrays.toString( attributes.getBlockSize() ) );

		final FlattenedInts edgeIds = GeffUtils.readAsIntMatrix( reader, path + "/edges/ids", "edge IDs" );
		if ( edgeIds == null )
		{
			throw new IllegalArgumentException( "required property '/edges/ids' not found" );
		}
		final int numEdges = edgeIds.size()[ 1 ];

		// Read distances from chunks
		final double[] distances = GeffUtils.readAsDoubleArray( reader, path + "/edges/props/distance/values", "distances" );
		verifyLength( distances, numEdges, "/edges/props/distance/values" );

		// Read scores from chunks
		final double[] scores = GeffUtils.readAsDoubleArray( reader, path + "/edges/props/score/values", "scores" );
		verifyLength( scores, numEdges, "/edges/props/score/values" );

		// Create edge objects
		final List< GeffEdge > edges = new ArrayList<>();
		for ( int i = 0; i < numEdges; i++ )
		{
			final int sourceNodeId = edgeIds.at( 0, i );
			final int targetNodeId = edgeIds.at( 1, i );
			final double score = scores != null ? scores[ i ] : DEFAULT_SCORE;
			final double distance = distances != null ? distances[ i ] : DEFAULT_DISTANCE;
			final GeffEdge edge = new GeffEdge( i, sourceNodeId, targetNodeId, score, distance );
			edges.add( edge );
		}
		return edges;
	}

	/**
	 * Write edges to Zarr format with chunked structure
	 */
	public static void writeToZarr( List< GeffEdge > edges, String zarrPath )
	{
		writeToZarr( edges, zarrPath, GeffUtils.DEFAULT_CHUNK_SIZE );
	}

	public static void writeToZarr( List< GeffEdge > edges, String zarrPath, String geffVersion )
	{
		writeToZarr( edges, zarrPath, GeffUtils.DEFAULT_CHUNK_SIZE, geffVersion );
	}

	/**
	 * Write edges to Zarr format with specified chunk size
	 */
	public static void writeToZarr( List< GeffEdge > edges, String zarrPath, int chunkSize )
	{
		writeToZarr( edges, zarrPath, chunkSize, Geff.VERSION );
	}

	public static void writeToZarr( List< GeffEdge > edges, String zarrPath, int chunkSize, String geffVersion )
	{
		LOG.debug( "Writing {} edges to Zarr path: {} with chunk size: {} to Geff version: {}", edges.size(), zarrPath, chunkSize, geffVersion );
		try ( final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ) )
		{
			writeToN5( edges, writer, "/", chunkSize, geffVersion );
		}
	}

	public static void writeToN5(
			final List< GeffEdge > edges,
			final N5Writer writer,
			final String group,
			final int chunkSize,
			String geffVersion )
	{
		if ( edges == null )
			throw new NullPointerException( "Edges list cannot be null" );

		if ( geffVersion == null || geffVersion.isEmpty() )
		{
			geffVersion = Geff.VERSION; // Use default version if not specified
		}
		GeffUtils.checkSupportedVersion( geffVersion );

		final String path = N5URI.normalizeGroupPath( group );

		GeffUtils.writeIntMatrix( edges, 2, e -> new int[] { e.getSourceNodeId(), e.getTargetNodeId() }, writer, path + "/edges/ids", chunkSize );

		// Write distances
		GeffUtils.writeDoubleArray( edges, GeffEdge::getDistance, writer, path + "/edges/props/distance/values", chunkSize );

		// Write scores
		GeffUtils.writeDoubleArray( edges, GeffEdge::getScore, writer, path + "/edges/props/score/values", chunkSize );
	}

	private static void printEdgeIdStuff( List< GeffEdge > edges )
	{
		// Write edges in chunks
		int totalEdges = edges.size();

		// Analyze edge data format
		long validEdges = edges.stream().filter( GeffEdge::isValid ).count();
		long selfLoops = edges.stream().filter( GeffEdge::isSelfLoop ).count();

		System.out.println( "Edge analysis:" );
		System.out.println( "- Valid edges: " + validEdges + "/" + edges.size() );
		if ( selfLoops > 0 )
		{
			System.out.println( "- Self-loops detected: " + selfLoops );
		}
		System.out.println( "- Format: Chunked 2D arrays [[source1, target1], [source2, target2], ...]" );

		// Log summary
		int uniqueSourceNodes = ( int ) edges.stream().mapToInt( GeffEdge::getSourceNodeId ).distinct().count();
		int uniqueTargetNodes = ( int ) edges.stream().mapToInt( GeffEdge::getTargetNodeId ).distinct().count();

		System.out.println( "Successfully wrote edges to Zarr format:" );
		System.out.println( "- " + totalEdges + " edges written" );
		System.out.println( "- Source nodes: " + uniqueSourceNodes + " unique" );
		System.out.println( "- Target nodes: " + uniqueTargetNodes + " unique" );

		// Sample verification
		if ( !edges.isEmpty() )
		{
			System.out.println( "Sample written edge data:" );
			for ( int i = 0; i < Math.min( 3, edges.size() ); i++ )
			{
				GeffEdge edge = edges.get( i );
				System.out.println( "  [" + edge.getSourceNodeId() + ", " + edge.getTargetNodeId() + "] - " + edge );
			}
		}
	}

	/**
	 * Check if this edge is valid (has valid source and target node IDs)
	 */
	public boolean isValid()
	{
		return sourceNodeId >= 0 && targetNodeId >= 0;
	}

	/**
	 * Check if this edge represents a self-loop (source == target)
	 */
	public boolean isSelfLoop()
	{
		return sourceNodeId == targetNodeId;
	}

	@Override
	public String toString()
	{
		return String.format( "GeffEdge{id=%d, source=%d, target=%d}",
				id, sourceNodeId, targetNodeId );
	}

	@Override
	public boolean equals( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null || getClass() != obj.getClass() )
			return false;

		GeffEdge geffEdge = ( GeffEdge ) obj;
		return sourceNodeId == geffEdge.sourceNodeId &&
				targetNodeId == geffEdge.targetNodeId &&
				id == geffEdge.id &&
				Double.compare( geffEdge.score, score ) == 0 &&
				Double.compare( geffEdge.distance, distance ) == 0;
	}

	@Override
	public int hashCode()
	{
		int result = sourceNodeId;
		result = 31 * result + targetNodeId;
		result = 31 * result + id;
		result = 31 * result + Double.hashCode( score );
		return result;
	}
}
