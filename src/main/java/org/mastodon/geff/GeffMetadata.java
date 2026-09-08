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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	 * The optional extra object is a free-form dictionary that can hold any
	 * additional, application-specific metadata that is not covered by the core
	 * geff schema. Users may place arbitrary keys and values inside extra
	 * without fear of clashing with future reserved fields. Although the core
	 * geff reader makes these attributes available, their meaning and use are
	 * left entirely to downstream applications.
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/specification/#geff_spec.GeffMetadata">GEFF
	 *      Specification: extra</a>
	 */
	private Map< String, Object > extra;

	private DisplayHints displayHints;

	private RelatedObjects relatedObjects;

	/**
	 * Default constructor
	 */
	public GeffMetadata()
	{}

	/**
	 * Constructor with basic parameters
	 */
	public GeffMetadata( final String geffVersion, final boolean directed )
	{
		setGeffVersion( geffVersion );
		this.directed = directed;
	}

	/**
	 * Constructor with all parameters
	 */
	public GeffMetadata( final String geffVersion, final boolean directed, final GeffAxis[] geffAxes )
	{
		setGeffVersion( geffVersion );
		this.directed = directed;
		setGeffAxes( geffAxes );
	}

	/**
	 * Constructor with all parameters
	 */
	public GeffMetadata( final String geffVersion, final boolean directed, final List< GeffAxis > geffAxes )
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

	public void setGeffVersion( final String geffVersion )
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

	public void setDirected( final boolean directed )
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

	public void setGeffAxes( final GeffAxis[] geffAxes ) // TODO make List<GeffAxis>
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

	public void setNodePropsMetadata( final Map< String, PropMetadata > nodePropsMetadata )
	{
		this.nodePropsMetadata = nodePropsMetadata;
	}

	public Map< String, PropMetadata > getEdgePropsMetadata()
	{
		return edgePropsMetadata;
	}

	public void setEdgePropsMetadata( final Map< String, PropMetadata > edgePropsMetadata )
	{
		this.edgePropsMetadata = edgePropsMetadata;
	}

	public Map< String, String > getTrackNodeProps()
	{
		return trackNodeProps;
	}

	public void setTrackNodeProps( final Map< String, String > trackNodeProps )
	{
		this.trackNodeProps = trackNodeProps;
	}

	public Map< String, Object > getExtra()
	{
		return extra;
	}

	public void setExtra( final Map< String, Object > extra )
	{
		this.extra = extra;
	}

	public DisplayHints getDisplayHints()
	{
		return displayHints;
	}

	public void setDisplayHints( final DisplayHints displayHints )
	{
		this.displayHints = displayHints;
	}

	public void setRelatedObjects( final RelatedObjects relatedObjects )
	{
		this.relatedObjects = relatedObjects;
	}

	public RelatedObjects getRelatedObjects()
	{
		return relatedObjects;
	}

	/**
	 * Get the axis name for a given axis type.
	 * Returns the name of the first axis matching the specified type, or null if no such axis exists.
	 *
	 * @param type the axis type (e.g., "time", "space", "channel")
	 * @return the axis name, or null if no axis of the given type exists
	 */
	public String getAxisNameByType( final String type )
	{
		if ( geffAxes != null )
		{
			for ( final GeffAxis axis : geffAxes )
			{
				if ( type.equals( axis.getType() ) )
				{
					return axis.getName();
				}
			}
		}
		return null;
	}

	/**
	 * Get all axis names for a given axis type.
	 * Returns an array of names for all axes matching the specified type.
	 *
	 * @param type the axis type (e.g., "space" for all spatial axes)
	 * @return array of axis names (empty array if no matching axes)
	 */
	public String[] getAxisNamesByType( final String type )
	{
		if ( geffAxes == null )
			return new String[ 0 ];

		return Arrays.stream( geffAxes )
				.filter( axis -> type.equals( axis.getType() ) )
				.map( GeffAxis::getName )
				.toArray( String[]::new );
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
			for ( final GeffAxis axis : geffAxes )
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

		// DisplayHints
		DisplayHints displayHints = null;
		try
		{
			final Map< String, String > dhMap = reader.getAttribute( group, "geff/display_hints", Map.class );
			if ( dhMap != null )
			{
				displayHints = new DisplayHints();
				displayHints.hints.putAll( dhMap );
			}
		}
		catch ( final Exception e )
		{
			LOG.debug( "Could not parse geff/display_hints as DisplayHints, setting to null: {}", e.getMessage() );
		}

		// RelatedObjects
		RelatedObjects relatedObjects = null;
		try
		{
			final List< Map< String, String > > roMap = reader.getAttribute( group, "geff/related_objects", List.class );
			relatedObjects = new RelatedObjects();
			relatedObjects.relatedObjects.addAll( roMap );
		}
		catch ( final Exception e )
		{
			LOG.debug( "Could not parse geff/related_objects as RelatedObjects, setting to null: {}", e.getMessage() );
		}

		// Extra may be null, so safe-read it
		Map< String, Object > extra = null;
		try
		{
			extra = reader.getAttribute( group, "geff/extra",
					new TypeToken< Map< String, Object > >()
					{}.getType() );
		}
		catch ( final Exception e )
		{
			// If the attribute cannot be parsed as Map<String, String> (e.g.,
			// if it's null in JSON), just leave it as null
			LOG.debug( "Could not parse geff/extra as Map<String,Object>, setting to null: {}", e.getMessage() );
		}
		LOG.debug( "found geff/extra = {}", extra );

		final GeffMetadata metadata = new GeffMetadata( geffVersion, directed, axes );
		metadata.setNodePropsMetadata( nodePropsMetadata );
		metadata.setEdgePropsMetadata( edgePropsMetadata );
		metadata.setTrackNodeProps( trackNodeProps );
		metadata.setDisplayHints( displayHints );
		metadata.setRelatedObjects( relatedObjects );
		metadata.setExtra( extra );
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

		final Map< String, PropMetadata > nodeMeta = nodePropsMetadata != null ? nodePropsMetadata : new java.util.HashMap<>();
		LOG.debug( "writing geff/node_props_metadata {}", nodeMeta );
		writer.setAttribute( group, "geff/node_props_metadata", nodeMeta );

		final Map< String, PropMetadata > edgeMeta = edgePropsMetadata != null ? edgePropsMetadata : new java.util.HashMap<>();
		LOG.debug( "writing geff/edge_props_metadata {}", edgeMeta );
		writer.setAttribute( group, "geff/edge_props_metadata", edgeMeta );

		if ( trackNodeProps != null )
		{
			LOG.debug( "writing geff/track_node_props {}", trackNodeProps );
			writer.setAttribute( group, "geff/track_node_props", trackNodeProps );
		}

		if ( displayHints != null )
		{
			LOG.debug( "writing geff/display_hints {}", displayHints );
			writer.setAttribute( group, "geff/display_hints", displayHints.hints );
		}

		if ( relatedObjects != null )
		{
			LOG.debug( "writing geff/related_objects {}", relatedObjects.relatedObjects );
			writer.setAttribute( group, "geff/related_objects", relatedObjects.relatedObjects );
		}

		if ( extra != null )
		{
			LOG.debug( "writing geff/extra {}", extra );
			writer.setAttribute( group, "geff/extra", extra );
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
		final GeffMetadata that = ( GeffMetadata ) o;
		return directed == that.directed && Objects.equals( geffVersion, that.geffVersion ) && Objects.deepEquals( geffAxes, that.geffAxes )
				&& Objects.equals( nodePropsMetadata, that.nodePropsMetadata ) && Objects.equals( edgePropsMetadata, that.edgePropsMetadata )
				&& Objects.equals( trackNodeProps, that.trackNodeProps );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( geffVersion, directed, Arrays.hashCode( geffAxes ), nodePropsMetadata, edgePropsMetadata, trackNodeProps );
	}

	/**
	 * Display hints for GEFF
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/reference/geff_spec/#geff_spec.DisplayHint">GEFF
	 *      Specification: DisplayHint</a>
	 * @author Jean-Yves Tinevez
	 */
	public static class DisplayHints
	{

		private final Map< String, String > hints = new HashMap<>();


		/**
		 * Which spatial axis to use for horizontal display.
		 *
		 * @param propName
		 *            the name of the property to use for horizontal display.
		 * @return
		 */
		public DisplayHints displayHorizontal( final String propName )
		{
			hints.put( "display_horizontal", propName );
			return this;
		}

		public DisplayHints displayVertical( final String propName )
		{
			hints.put( "display_vertical", propName );
			return this;
		}

		public DisplayHints displayDepth( final String propName )
		{
			hints.put( "display_depth", propName );
			return this;
		}

		public DisplayHints displayTime( final String propName )
		{
			hints.put( "display_time", propName );
			return this;
		}
	}

	/**
	 * A set of metadata for data that is associated with the graph. The types
	 * 'labels' and 'image' should be used for label and image objects,
	 * respectively. Other types are also allowed.
	 *
	 * @see <a
	 *      href=https://liveimagetrackingtools.org/geff/latest/reference/geff_spec/#geff_spec.RelatedObject>GEFF
	 *      Specification: RelatedObject</a>
	 * @author Jean-Yves Tinevez
	 */
	public static class RelatedObjects
	{

		private final List< Map< String, String > > relatedObjects = new ArrayList<>();

		/**
		 * Add a related object of type 'labels' with the specified path and
		 * label property.
		 *
		 * @param path
		 *            Path of the labels within the zarr group, relative to the
		 *            geff zarr-attributes file. It is strongly recommended all
		 *            related objects are stored as siblings of the geff group
		 *            within the top-level zarr group.
		 * @param labelProp
		 *            Property name for label objects. This is the node property
		 *            that will be used to identify the labels in the related
		 *            object.
		 * @return this RelatedObject instance for method chaining.
		 */
		public RelatedObjects labels( final String path, final String labelProp )
		{
			relatedObjects.add( Map.of( "type", "labels", "path", path, "label_prop", labelProp ) );
			return this;
		}

		/**
		 * Add a related object of type 'image' with the specified path.
		 *
		 * @param path
		 *            Path of the image within the zarr group, relative to the
		 *            geff zarr-attributes file. It is strongly recommended all
		 *            related objects are stored as siblings of the geff group
		 *            within the top-level zarr group.
		 * @return this RelatedObject instance for method chaining.
		 */
		public RelatedObjects image( final String path )
		{
			relatedObjects.add( Map.of( "type", "image", "path", path ) );
			return this;
		}

		public List< String > getImagePaths()
		{
			return relatedObjects.stream()
					.filter( obj -> "image".equals( obj.get( "type" ) ) )
					.map( obj -> obj.get( "path" ) )
					.collect( Collectors.toList() );
		}
	}
}
