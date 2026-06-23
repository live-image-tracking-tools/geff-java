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
package org.mastodon.geff;

import static org.mastodon.geff.GeffUtils.checkSupportedVersion;
import static org.mastodon.geff.GeffUtils.verifyLength;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffUtils.FlattenedDoubles;
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

	private static final Set< String > STANDARD_EDGE_PROP_NAMES = new HashSet<>( java.util.Arrays.asList( "distance", "score" ) );

	// Edge attributes
	private int sourceNodeId;

	private int targetNodeId;

	private int id; // Edge ID if available

	private double score; // Optional score for the edge

	private double distance; // Optional distance metric for the edge

	private Map< String, Object > props;

	private Map< String, VarlengthProperty > varlengthProps;

	/**
	 * Default constructor
	 */
	public GeffEdge()
	{
		this.props = new HashMap<>();
		this.varlengthProps = new HashMap<>();
	}

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
		this.props = new HashMap<>();
		this.varlengthProps = new HashMap<>();
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
	 * Get an arbitrary edge property by name. Scalar values are stored as
	 * {@code Double} or {@code Integer}; vector values as {@code double[]} or
	 * {@code int[]}.
	 */
	public Object getProp( final String name )
	{
		return props != null ? props.get( name ) : null;
	}

	/**
	 * Set an arbitrary edge property. Supported value types: {@code Double},
	 * {@code Integer}, {@code double[]}, {@code int[]}.
	 */
	public void setProp( final String name, final Object value )
	{
		if ( props == null )
			props = new HashMap<>();
		props.put( name, value );
	}

	/**
	 * Get all arbitrary edge properties as an unmodifiable view.
	 */
	public Map< String, Object > getProps()
	{
		return props != null ? java.util.Collections.unmodifiableMap( props ) : java.util.Collections.emptyMap();
	}

	/**
	 * Get a varlength edge property by name.
	 */
	public VarlengthProperty getVarlengthProperty( final String name )
	{
		return varlengthProps != null ? varlengthProps.get( name ) : null;
	}

	/**
	 * Set a varlength edge property.
	 */
	public void setVarlengthProperty( final String name, final VarlengthProperty property )
	{
		if ( varlengthProps == null )
			varlengthProps = new HashMap<>();
		varlengthProps.put( name, property );
	}

	/**
	 * Get all varlength edge properties as an unmodifiable view.
	 */
	public Map< String, VarlengthProperty > getVarlengthProperties()
	{
		return varlengthProps != null ? java.util.Collections.unmodifiableMap( varlengthProps ) : java.util.Collections.emptyMap();
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
			return readFromN5( reader, "/", geffVersion, null );
		}
	}

	public static List< GeffEdge > readFromZarr( final String zarrPath, final GeffMetadata metadata )
	{
		LOG.debug( "Reading edges from Zarr path: {} with Geff version: {}", zarrPath, metadata != null ? metadata.getGeffVersion() : "null" );
		try ( final N5ZarrReader reader = new N5ZarrReader( zarrPath, true ) )
		{
			final String geffVersion = metadata != null ? metadata.getGeffVersion() : Geff.VERSION;
			return readFromN5( reader, "/", geffVersion, metadata );
		}
	}

	public static List< GeffEdge > readFromN5( final N5Reader reader, final String group, final String geffVersion )
	{
		return readFromN5( reader, group, geffVersion, null );
	}

	public static List< GeffEdge > readFromN5( final N5Reader reader, final String group, final String geffVersion, final GeffMetadata metadata )
	{
		checkSupportedVersion( geffVersion );
		final String path = N5URI.normalizeGroupPath( group );

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

		// Read custom non-standard, non-varlength edge props from metadata
		final Map< String, Object[] > customPropData = new HashMap<>();
		final Map< String, VarlengthProperty > varlengthPropsMap = new HashMap<>();
		if ( metadata != null && metadata.getEdgePropsMetadata() != null )
		{
			for ( final Map.Entry< String, PropMetadata > entry : metadata.getEdgePropsMetadata().entrySet() )
			{
				final String propName = entry.getKey();
				final PropMetadata propMeta = entry.getValue();
				if ( STANDARD_EDGE_PROP_NAMES.contains( propName ) )
					continue;
				if ( GeffUtils.shouldSkipProperty( propName, propMeta ) )
					continue;

				if ( propMeta != null && Boolean.TRUE.equals( propMeta.getVarlength() ) )
				{
					// Varlength edge prop
					final String propPath = path + "/edges/props/" + propName;
					final VarlengthProperty vp = GeffUtils.readVarlengthProperty( reader, propPath, numEdges, propMeta );
					if ( vp != null )
					{
						varlengthPropsMap.put( propName, vp );
						LOG.debug( "Successfully read varlength edge property: {}", propName );
					}
				}
				else
				{
					// Regular (non-varlength) edge prop
					final String valPath = path + "/edges/props/" + propName + "/values";
					if ( !reader.datasetExists( valPath ) )
						continue;
					try
					{
						final int ndim = reader.getDatasetAttributes( valPath ).getNumDimensions();
						final boolean isFloat = GeffUtils.isFloatDtype( propMeta != null ? propMeta.getDtype() : null );
						final Object[] edgeVals = new Object[ numEdges ];
						if ( ndim == 1 )
						{
							if ( isFloat )
							{
								final double[] arr = GeffUtils.readAsDoubleArray( reader, valPath, propName );
								if ( arr != null )
									for ( int i = 0; i < numEdges && i < arr.length; i++ )
										edgeVals[ i ] = arr[ i ];
							}
							else
							{
								final int[] arr = GeffUtils.readAsIntArray( reader, valPath, propName );
								if ( arr != null )
									for ( int i = 0; i < numEdges && i < arr.length; i++ )
										edgeVals[ i ] = arr[ i ];
							}
							customPropData.put( propName, edgeVals );
						}
						else if ( ndim == 2 )
						{
							if ( isFloat )
							{
								final FlattenedDoubles mat = GeffUtils.readAsDoubleMatrix( reader, valPath, propName );
								if ( mat != null )
									for ( int i = 0; i < numEdges; i++ )
										edgeVals[ i ] = mat.rowAt( i );
							}
							else
							{
								final FlattenedInts mat = GeffUtils.readAsIntMatrix( reader, valPath, propName );
								if ( mat != null )
									for ( int i = 0; i < numEdges; i++ )
										edgeVals[ i ] = mat.rowAt( i );
							}
							customPropData.put( propName, edgeVals );
						}
					}
					catch ( final Exception e )
					{
						LOG.debug( "Could not read custom edge prop {}: {}", propName, e.getMessage() );
					}
				}
			}
		}

		// Create edge objects
		final List< GeffEdge > edges = new ArrayList<>();
		for ( int i = 0; i < numEdges; i++ )
		{
			final int sourceNodeId = edgeIds.at( 0, i );
			final int targetNodeId = edgeIds.at( 1, i );
			final double score = scores != null ? scores[ i ] : DEFAULT_SCORE;
			final double distance = distances != null ? distances[ i ] : DEFAULT_DISTANCE;
			final GeffEdge edge = new GeffEdge( i, sourceNodeId, targetNodeId, score, distance );

			// Set custom props
			for ( final Map.Entry< String, Object[] > entry : customPropData.entrySet() )
			{
				final Object val = entry.getValue()[ i ];
				if ( val != null )
					edge.setProp( entry.getKey(), val );
			}

			// Set varlength props
			for ( final Map.Entry< String, VarlengthProperty > entry : varlengthPropsMap.entrySet() )
				edge.setVarlengthProperty( entry.getKey(), entry.getValue() );

			edges.add( edge );
		}
		return edges;
	}

	/**
	 * Write edges to Zarr format with chunked structure
	 */
	public static void writeToZarr( List< GeffEdge > edges, String zarrPath )
	{
		writeToZarr( edges, zarrPath, GeffUtils.computeFirstDimChunk( new long[]{ edges.size(), 2 }, Integer.BYTES ) );
	}

	public static void writeToZarr( List< GeffEdge > edges, String zarrPath, String geffVersion )
	{
		writeToZarr( edges, zarrPath, GeffUtils.computeFirstDimChunk( new long[]{ edges.size(), 2 }, Integer.BYTES ), geffVersion );
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
			writeToN5( edges, writer, "/", chunkSize, geffVersion, null );
		}
	}

	public static void writeToZarr( final List< GeffEdge > edges, final String zarrPath, final GeffMetadata metadata )
	{
		writeToZarr( edges, zarrPath, GeffUtils.computeFirstDimChunk( new long[]{ edges.size(), 2 }, Integer.BYTES ), metadata );
	}

	public static void writeToZarr( final List< GeffEdge > edges, final String zarrPath, final int chunkSize, final GeffMetadata metadata )
	{
		final String geffVersion = metadata != null ? metadata.getGeffVersion() : Geff.VERSION;
		LOG.debug( "Writing {} edges to Zarr path: {} with chunk size: {} to Geff version: {}", edges.size(), zarrPath, chunkSize, geffVersion );
		try ( final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ) )
		{
			writeToN5( edges, writer, "/", chunkSize, geffVersion, metadata );
		}
	}

	public static void writeToN5(
			final List< GeffEdge > edges,
			final N5Writer writer,
			final String group,
			final int chunkSize,
			String geffVersion )
	{
		writeToN5( edges, writer, group, chunkSize, geffVersion, null );
	}

	public static void writeToN5(
			final List< GeffEdge > edges,
			final N5Writer writer,
			final String group,
			final int chunkSize,
			String geffVersion,
			final GeffMetadata metadata )
	{
		if ( edges == null )
			throw new NullPointerException( "Edges list cannot be null" );

		if ( geffVersion == null || geffVersion.isEmpty() )
		{
			geffVersion = Geff.VERSION; // Use default version if not specified
		}
		GeffUtils.checkSupportedVersion( geffVersion );

		final String path = N5URI.normalizeGroupPath( group );
		final Map< String, PropMetadata > edgePropsMetadata = metadata != null ? metadata.getEdgePropsMetadata() : null;
		final boolean writeAllProps = edgePropsMetadata == null;

		GeffUtils.writeIntMatrix( edges, 2, e -> new int[] { e.getSourceNodeId(), e.getTargetNodeId() }, writer, path + "/edges/ids", chunkSize );

		// Always create edges/props group so the zarr structure is valid even when
		// there are no edge properties (mirrors what Python geff writes).
		writer.createGroup( path + "/edges/props" );

		// Write distances
		if ( writeAllProps || edgePropsMetadata.containsKey( "distance" ) )
			GeffUtils.writeDoubleArray( edges, GeffEdge::getDistance, writer, path + "/edges/props/distance/values", chunkSize );

		// Write scores
		if ( writeAllProps || edgePropsMetadata.containsKey( "score" ) )
			GeffUtils.writeDoubleArray( edges, GeffEdge::getScore, writer, path + "/edges/props/score/values", chunkSize );

		// When writeAllProps=true (no edgePropsMetadata provided), populate metadata
		// with the standard props so the output zarr passes Python structural
		// validation (edge_props_metadata is a required field in the Python spec).
		if ( writeAllProps && metadata != null )
		{
			final Map< String, PropMetadata > edgePropsMap = new HashMap<>();
			edgePropsMap.put( "distance", new PropMetadata( "distance", "float64", false, null, null, null ) );
			edgePropsMap.put( "score", new PropMetadata( "score", "float64", false, null, null, null ) );
			metadata.setEdgePropsMetadata( edgePropsMap );
		}

		// Write custom non-standard, non-varlength edge props
		final Set< String > customRegularPropNames = new java.util.LinkedHashSet<>();
		for ( final GeffEdge edge : edges )
			for ( final String name : edge.getProps().keySet() )
				if ( !STANDARD_EDGE_PROP_NAMES.contains( name ) )
					customRegularPropNames.add( name );

		for ( final String propName : customRegularPropNames )
		{
			Object sample = null;
			for ( final GeffEdge edge : edges )
			{
				sample = edge.getProp( propName );
				if ( sample != null )
					break;
			}
			if ( sample == null )
				continue;

			final String dtype;
			if ( sample instanceof Double )
			{
				GeffUtils.writeDoubleArray( edges, e -> {
					final Object v = e.getProp( propName );
					return v instanceof Double ? ( double ) ( Double ) v : Double.NaN;
				}, writer, path + "/edges/props/" + propName + "/values", chunkSize );
				dtype = "float64";
			}
			else if ( sample instanceof Integer )
			{
				GeffUtils.writeIntArray( edges, e -> {
					final Object v = e.getProp( propName );
					return v instanceof Integer ? ( int ) ( Integer ) v : 0;
				}, writer, path + "/edges/props/" + propName + "/values", chunkSize );
				dtype = "int32";
			}
			else if ( sample instanceof double[] )
			{
				final int cols = ( ( double[] ) sample ).length;
				GeffUtils.writeDoubleMatrix( edges, cols, e -> {
					final Object v = e.getProp( propName );
					return v instanceof double[] ? ( double[] ) v : new double[ cols ];
				}, writer, path + "/edges/props/" + propName + "/values", chunkSize );
				dtype = "float64";
			}
			else if ( sample instanceof int[] )
			{
				final int cols = ( ( int[] ) sample ).length;
				GeffUtils.writeIntMatrix( edges, cols, e -> {
					final Object v = e.getProp( propName );
					return v instanceof int[] ? ( int[] ) v : new int[ cols ];
				}, writer, path + "/edges/props/" + propName + "/values", chunkSize );
				dtype = "int32";
			}
			else
			{
				LOG.warn( "Unsupported type for custom edge prop {}: {}", propName, sample.getClass().getName() );
				continue;
			}

			final Map< String, PropMetadata > edgePropsMetadataMap = metadata != null ? metadata.getEdgePropsMetadata() : null;
			if ( edgePropsMetadataMap != null && !edgePropsMetadataMap.containsKey( propName ) )
				edgePropsMetadataMap.put( propName, new PropMetadata( propName, dtype, false, null, null, null ) );
		}

		// Write varlength edge properties
		final Set< String > varlengthPropNames = new java.util.LinkedHashSet<>();
		for ( final GeffEdge edge : edges )
			varlengthPropNames.addAll( edge.getVarlengthProperties().keySet() );

		if ( !varlengthPropNames.isEmpty() )
		{
			if ( metadata != null && metadata.getEdgePropsMetadata() == null )
				metadata.setEdgePropsMetadata( new HashMap<>() );

			final int numEdges = edges.size();
			for ( final String propName : varlengthPropNames )
			{
				final Object[][] edgeDataArrays = new Object[ numEdges ][];
				final boolean[] missing = new boolean[ numEdges ];
				String dtype = null;

				for ( int i = 0; i < numEdges; i++ )
				{
					final VarlengthProperty property = edges.get( i ).getVarlengthProperty( propName );
					if ( property == null || property.isMissing( i ) )
					{
						edgeDataArrays[ i ] = null;
						missing[ i ] = true;
						continue;
					}

					if ( dtype == null )
						dtype = property.getDtype();

					final Object edgeData = property.getNodeData( i );
					if ( edgeData == null )
					{
						edgeDataArrays[ i ] = new Object[ 0 ];
					}
					else if ( edgeData.getClass().isArray() )
					{
						if ( edgeData instanceof Object[] )
						{
							edgeDataArrays[ i ] = ( Object[] ) edgeData;
						}
						else
						{
							final int length = Array.getLength( edgeData );
							final Object[] converted = new Object[ length ];
							for ( int j = 0; j < length; j++ )
								converted[ j ] = Array.get( edgeData, j );
							edgeDataArrays[ i ] = converted;
						}
					}
					else
					{
						edgeDataArrays[ i ] = new Object[] { edgeData };
					}
				}

				if ( dtype == null )
					dtype = "float64";

				GeffUtils.writeVarlengthProperty( writer, path + "/edges/props/" + propName, edgeDataArrays, missing, chunkSize, dtype );

				final Map< String, PropMetadata > edgePropsMetadataMap = metadata != null ? metadata.getEdgePropsMetadata() : null;
				if ( edgePropsMetadataMap != null && !edgePropsMetadataMap.containsKey( propName ) )
					edgePropsMetadataMap.put( propName, new PropMetadata( propName, dtype, true, null, null, null ) );
			}
		}

		GeffUtils.patchZarrLittleEndian( writer, path + "/edges" );
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
