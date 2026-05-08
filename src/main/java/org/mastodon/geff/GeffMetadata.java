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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Represents metadata for a Geff (Graph Exchange Format for Features) dataset.
 * This class handles reading and writing metadata from/to Zarr format.
 *
 * This is the Java equivalent of the Python GeffMetadata schema from:
 * https://github.com/live-image-tracking-tools/geff/blob/main/src/geff/metadata_schema.py
 */
public class GeffMetadata
{
	private static final Logger LOG = LoggerFactory.getLogger( GeffMetadata.class );

	// Supported GEFF versions
	public static final List< String > SUPPORTED_VERSIONS = Arrays.asList( "0.2", "0.3", "0.4", "1.0", "1.1" );

	// Pattern to match major.minor versions, allowing for patch versions and
	// development versions
	// Examples: 0.1.1, 0.2.2.dev20+g611e7a2.d20250719, 0.2.0-alpha.1, etc.
	private static final Pattern SUPPORTED_VERSIONS_PATTERN = Pattern
			.compile( "^\\d+\\.\\d+(?:\\.\\d+)?(?:\\.dev\\d+)?(?:[.-][a-zA-Z0-9-]+(?:[.-][a-zA-Z0-9-]+)*)?(?:\\+[a-zA-Z0-9.-]+)?$" );

	// Metadata attributes - matching the Python schema
	private String geffVersion;

	private boolean directed;

	private GeffAxis[] geffAxes; // TODO make List<GeffAxis>

	private Map< String, PropMetadata > nodePropsMetadata;

	private Map< String, PropMetadata > edgePropsMetadata;

	private Map< String, String > trackNodeProps;

	/**
	 * Default constructor
	 */
	public GeffMetadata()
	{}

	/**
	 * Constructor with basic parameters
	 */
	public GeffMetadata( String geffVersion, boolean directed )
	{
		setGeffVersion( geffVersion );
		this.directed = directed;
	}

	/**
	 * Constructor with all parameters
	 */
	public GeffMetadata( String geffVersion, boolean directed, GeffAxis[] geffAxes )
	{
		setGeffVersion( geffVersion );
		this.directed = directed;
		setGeffAxes( geffAxes );
	}

	/**
	 * Constructor with all parameters
	 */
	public GeffMetadata( String geffVersion, boolean directed, List< GeffAxis > geffAxes )
	{
		setGeffVersion( geffVersion );
		this.directed = directed;
		setGeffAxes( geffAxes );
	}

	// Getters and Setters
	public String getGeffVersion()
	{
		return geffVersion;
	}

	public void setGeffVersion( String geffVersion )
	{
		if ( geffVersion != null && !SUPPORTED_VERSIONS_PATTERN.matcher( geffVersion ).matches() )
		{ throw new IllegalArgumentException(
				"Unsupported Geff version: " + geffVersion +
						". Supported major.minor versions are: " + SUPPORTED_VERSIONS +
						" (patch versions, development versions, and metadata are also supported, " +
						"e.g., 0.1.1, 0.2.2.dev20+g611e7a2.d20250719)" ); }
		this.geffVersion = geffVersion;
	}

	public boolean isDirected()
	{
		return directed;
	}

	public void setDirected( boolean directed )
	{
		this.directed = directed;
	}

	public GeffAxis[] getGeffAxes() // TODO make List<GeffAxis>
	{
		return geffAxes;
	}

	public List< GeffAxis > getGeffAxesList() // TODO rename getGeffAxes()
	{
		return ( geffAxes != null ) ? Arrays.asList( geffAxes ) : null;
	}

	public void setGeffAxes( GeffAxis[] geffAxes ) // TODO make List<GeffAxis>
	{
		this.geffAxes = geffAxes != null ? geffAxes.clone() : null;
		validate();
	}

	public void setGeffAxes( final List< GeffAxis > geffAxes )
	{
		this.geffAxes = ( geffAxes != null ) ? geffAxes.toArray( new GeffAxis[ 0 ] ) : null;
		validate();
	}

	public Map< String, PropMetadata > getNodePropsMetadata()
	{
		return nodePropsMetadata;
	}

	public void setNodePropsMetadata( Map< String, PropMetadata > nodePropsMetadata )
	{
		this.nodePropsMetadata = nodePropsMetadata;
	}

	public Map< String, PropMetadata > getEdgePropsMetadata()
	{
		return edgePropsMetadata;
	}

	public void setEdgePropsMetadata( Map< String, PropMetadata > edgePropsMetadata )
	{
		this.edgePropsMetadata = edgePropsMetadata;
	}

	public Map< String, String > getTrackNodeProps()
	{
		return trackNodeProps;
	}

	public void setTrackNodeProps( Map< String, String > trackNodeProps )
	{
		this.trackNodeProps = trackNodeProps;
	}

	/**
	 * Validates the metadata according to the GEFF schema rules
	 */
	public void validate()
	{
		if ( geffVersion == null )
		{ throw new IllegalArgumentException( "geff_version is missing." ); }

		// Check spatial metadata consistency if position is provided
		if ( geffAxes != null )
		{
			for ( GeffAxis axis : geffAxes )
			{
				if ( !Arrays.asList( GeffAxis.TYPE_TIME, GeffAxis.TYPE_SPACE, GeffAxis.TYPE_CHANNEL ).contains( axis.getType() ) )
				{ throw new IllegalArgumentException(
						"Invalid axis type: " + axis.getType() + ". Supported types are: " +
								GeffAxis.TYPE_TIME + ", " + GeffAxis.TYPE_SPACE + ", " + GeffAxis.TYPE_CHANNEL ); }
				if ( axis.getMin() != null && axis.getMax() != null && axis.getMin() > axis.getMax() )
				{ throw new IllegalArgumentException(
						"Roi min " + axis.getMin() + " is greater than " +
								"max " + axis.getMax() + " in dimension " + axis.getName() ); }
			}
		}
	}

	/**
	 * Read metadata from a Zarr group
	 */
	public static GeffMetadata readFromZarr( final String zarrPath )
	{
		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath, true ))
		{
			return readFromN5( reader, "/" );
		}
	}

	public static GeffMetadata readFromN5( final N5Reader reader, final String group )
	{
		final String geffVersion = reader.getAttribute( group, "geff/geff_version", String.class );
		LOG.debug( "found geff/geff_version = {}", geffVersion );
		if ( geffVersion == null )
		{ throw new IllegalArgumentException(
				"No geff_version found in " + group + ". This may indicate the path is incorrect or " +
						"zarr group name is not specified (e.g. /dataset.zarr/tracks/ instead of " +
						"/dataset.zarr/)." ); }
		checkSupportedVersion( geffVersion );

		final Boolean directed = reader.getAttribute( group, "geff/directed", Boolean.class );
		LOG.debug( "found geff/directed = {}", directed );
		if ( directed == null )
		{ throw new IllegalArgumentException( "required attribute 'geff/directed' is missing." ); }

		final List< GeffAxis > axes = reader.getAttribute( group, "geff/axes",
				new TypeToken< List< GeffAxis > >()
				{}.getType() );
		LOG.debug( "found geff/axes = {}", axes );

		final Map< String, PropMetadata > nodePropsMetadata = reader.getAttribute( group, "geff/node_props_metadata",
				new TypeToken< Map< String, PropMetadata > >()
				{}.getType() );
		LOG.debug( "found geff/node_props_metadata = {}", nodePropsMetadata );

		final Map< String, PropMetadata > edgePropsMetadata = reader.getAttribute( group, "geff/edge_props_metadata",
				new TypeToken< Map< String, PropMetadata > >()
				{}.getType() );
		LOG.debug( "found geff/edge_props_metadata = {}", edgePropsMetadata );

		// trackNodeProps may be null, so safe-read it
		Map< String, String > trackNodeProps = null;
		try
		{
			trackNodeProps = reader.getAttribute( group, "geff/track_node_props",
					new TypeToken< Map< String, String > >()
					{}.getType() );
		}
		catch ( final Exception e )
		{
			// If the attribute cannot be parsed as Map<String, String> (e.g.,
			// if it's null in JSON),
			// just leave it as null
			LOG.debug( "Could not parse geff/track_node_props as Map<String,String>, setting to null: {}", e.getMessage() );
		}
		LOG.debug( "found geff/track_node_props = {}", trackNodeProps );

		final GeffMetadata metadata = new GeffMetadata( geffVersion, directed, axes );
		metadata.setNodePropsMetadata( nodePropsMetadata );
		metadata.setEdgePropsMetadata( edgePropsMetadata );
		metadata.setTrackNodeProps( trackNodeProps );
		metadata.validate();

		return metadata;
	}

	/**
	 * Write metadata to Zarr format at specified path
	 */
	public static void writeToZarr( final GeffMetadata metadata, final String zarrPath ) throws IOException
	{
		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, new GsonBuilder().setPrettyPrinting(), true ))
		{
			metadata.writeToN5( writer, "/" );
		}
	}

	public void writeToN5( final N5Writer writer, final String group )
	{
		// Validate before writing
		validate();

		checkSupportedVersion( geffVersion );

		// required
		LOG.debug( "writing geff/geff_version {}", getGeffVersion() );
		writer.setAttribute( group, "geff/geff_version", getGeffVersion() );
		LOG.debug( "writing geff/directed {}", isDirected() );
		writer.setAttribute( group, "geff/directed", isDirected() );

		// optional
		final List< GeffAxis > axes = getGeffAxesList();
		if ( axes != null )
		{
			LOG.debug( "writing geff/axes {}", axes );
			writer.setAttribute( group, "geff/axes", axes );
		}

		if ( nodePropsMetadata != null )
		{
			LOG.debug( "writing geff/node_props_metadata {}", nodePropsMetadata );
			writer.setAttribute( group, "geff/node_props_metadata", nodePropsMetadata );
		}

		if ( edgePropsMetadata != null )
		{
			LOG.debug( "writing geff/edge_props_metadata {}", edgePropsMetadata );
			writer.setAttribute( group, "geff/edge_props_metadata", edgePropsMetadata );
		}

		if ( trackNodeProps != null )
		{
			LOG.debug( "writing geff/track_node_props {}", trackNodeProps );
			writer.setAttribute( group, "geff/track_node_props", trackNodeProps );
		}
	}

	@Override
	public String toString()
	{
		return String.format(
				"GeffMetadata{geffVersion='%s', directed=%s, geffAxes=%s, nodePropsMetadata=%s, edgePropsMetadata=%s, trackNodeProps=%s}",
				geffVersion, directed, Arrays.toString( geffAxes ), nodePropsMetadata, edgePropsMetadata, trackNodeProps );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( !( o instanceof GeffMetadata ) )
			return false;
		GeffMetadata that = ( GeffMetadata ) o;
		return directed == that.directed && Objects.equals( geffVersion, that.geffVersion ) && Objects.deepEquals( geffAxes, that.geffAxes )
				&& Objects.equals( nodePropsMetadata, that.nodePropsMetadata ) && Objects.equals( edgePropsMetadata, that.edgePropsMetadata )
				&& Objects.equals( trackNodeProps, that.trackNodeProps );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( geffVersion, directed, Arrays.hashCode( geffAxes ), nodePropsMetadata, edgePropsMetadata, trackNodeProps );
	}
}
