/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 - 2026 Ko Sugawara, Jean-Yves Tinevez
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.google.gson.annotations.SerializedName;
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

	private GeffDisplayHints displayHints;

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

	public void setGeffAxes( final GeffAxis[] geffAxes ) // TODO make
															// List<GeffAxis>
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

	public GeffDisplayHints getDisplayHints()
	{
		return displayHints;
	}

	public void setDisplayHints( final GeffDisplayHints displayHints )
	{
		this.displayHints = displayHints;
		validate();
	}

	/**
	 * Get the name of the axis holding the time coordinate.
	 * <p>
	 * The {@code display_time} display hint takes precedence; otherwise the
	 * first axis of type {@link GeffAxis#TYPE_TIME} is used, falling back to
	 * the standard name {@code "t"} if no axes are declared.
	 *
	 * @return the time axis name, never null
	 */
	public String getTimeAxisName()
	{
		final String hint = displayHints != null ? displayHints.getDisplayTime() : null;
		if ( hint != null && !hint.trim().isEmpty() )
			return hint;

		final String name = getAxisNameByType( GeffAxis.TYPE_TIME );
		return name != null ? name : GeffAxis.NAME_TIME;
	}

	/**
	 * Get the name of the spatial axis holding the X coordinate, i.e. the one a
	 * viewer displays horizontally.
	 *
	 * @return the axis name, never null
	 * @see #resolveSpaceAxisName(String, String, int)
	 */
	public String getHorizontalAxisName()
	{
		final String hint = displayHints != null ? displayHints.getDisplayHorizontal() : null;
		return resolveSpaceAxisName( hint, GeffAxis.NAME_SPACE_X, 0 );
	}

	/**
	 * Get the name of the spatial axis holding the Y coordinate, i.e. the one a
	 * viewer displays vertically.
	 *
	 * @return the axis name, never null
	 * @see #resolveSpaceAxisName(String, String, int)
	 */
	public String getVerticalAxisName()
	{
		final String hint = displayHints != null ? displayHints.getDisplayVertical() : null;
		return resolveSpaceAxisName( hint, GeffAxis.NAME_SPACE_Y, 1 );
	}

	/**
	 * Get the name of the spatial axis holding the Z coordinate, i.e. the one a
	 * viewer displays as depth.
	 *
	 * @return the axis name, or null for datasets without a third spatial axis
	 * @see #resolveSpaceAxisName(String, String, int)
	 */
	public String getDepthAxisName()
	{
		final String hint = displayHints != null ? displayHints.getDisplayDepth() : null;
		return resolveSpaceAxisName( hint, GeffAxis.NAME_SPACE_Z, 2 );
	}

	/**
	 * Resolve which spatial axis carries a given display dimension by combining
	 * {@code display_hints} and {@code axes}. Since {@code display_hints} is
	 * optional, the following sources are consulted in order:
	 * <ol>
	 * <li>the display hint, which names the axis explicitly,</li>
	 * <li>a spatial axis called {@code standardName} (case-insensitive), or one
	 * whose name ends in {@code "_" + standardName}, e.g.
	 * {@code "cell_x"},</li>
	 * <li>the position in the axes list, which the spec defines as image
	 * dimension order, i.e. slowest to fastest ({@code z}, {@code y},
	 * {@code x}), hence counted from the end,</li>
	 * <li>{@code standardName} itself, for datasets that declare no spatial
	 * axes at all.</li>
	 * </ol>
	 *
	 * @param hint
	 *            the corresponding display hint, or null if not given
	 * @param standardName
	 *            the conventional name of this axis ({@code x}, {@code y} or
	 *            {@code z})
	 * @param indexFromLast
	 *            position of this axis counted backwards from the end of the
	 *            spatial axes list (0 for x, 1 for y, 2 for z)
	 * @return the axis name, or null if the dataset declares spatial axes but
	 *         not the requested one
	 */
	private String resolveSpaceAxisName( final String hint, final String standardName, final int indexFromLast )
	{
		if ( hint != null && !hint.trim().isEmpty() )
			return hint;

		final String[] spaceAxes = getAxisNamesByType( GeffAxis.TYPE_SPACE );

		for ( final String name : spaceAxes )
			if ( standardName.equalsIgnoreCase( name ) )
				return name;

		for ( final String name : spaceAxes )
			if ( name != null && name.toLowerCase().endsWith( "_" + standardName ) )
				return name;

		final int index = spaceAxes.length - 1 - indexFromLast;
		if ( index >= 0 )
		{
			LOG.debug( "no display hint for {}, guessing axis '{}' from the axes order {}",
					standardName, spaceAxes[ index ], Arrays.toString( spaceAxes ) );
			return spaceAxes[ index ];
		}

		// A dataset that declares no axes at all is assumed to use the
		// standard t/x/y/z layout. Otherwise the requested axis is beyond the
		// declared ones, i.e. the dataset has no depth axis.
		return ( spaceAxes.length == 0 || indexFromLast < 2 ) ? standardName : null;
	}

	/**
	 * Get the metadata of a standard node property, as declared in
	 * {@code node_props_metadata}.
	 *
	 * @param standardName
	 *            the conventional name of the property, e.g. {@code "radius"}
	 *            or {@code "covariance3d"}
	 * @return the property metadata, or null if the property is not declared
	 * @see #findPropMetadata(Map, String)
	 */
	public PropMetadata getNodePropMetadata( final String standardName )
	{
		return findPropMetadata( nodePropsMetadata, standardName );
	}

	/**
	 * Get the metadata of a standard edge property, as declared in
	 * {@code edge_props_metadata}.
	 *
	 * @param standardName
	 *            the conventional name of the property, e.g. {@code "distance"}
	 *            or {@code "score"}
	 * @return the property metadata, or null if the property is not declared
	 * @see #findPropMetadata(Map, String)
	 */
	public PropMetadata getEdgePropMetadata( final String standardName )
	{
		return findPropMetadata( edgePropsMetadata, standardName );
	}

	/**
	 * Get the identifier of a standard node property, i.e. the name of the
	 * group holding it in {@code nodes/props}.
	 *
	 * @param standardName
	 *            the conventional name of the property, e.g. {@code "radius"}
	 *            or {@code "covariance3d"}
	 * @return the property identifier, never null
	 * @see #resolvePropIdentifier(Map, String)
	 */
	public String getNodePropIdentifier( final String standardName )
	{
		return resolvePropIdentifier( nodePropsMetadata, standardName );
	}

	/**
	 * Get the identifier of a standard edge property, i.e. the name of the
	 * group holding it in {@code edges/props}.
	 *
	 * @param standardName
	 *            the conventional name of the property, e.g. {@code "distance"}
	 *            or {@code "score"}
	 * @return the property identifier, never null
	 * @see #resolvePropIdentifier(Map, String)
	 */
	public String getEdgePropIdentifier( final String standardName )
	{
		return resolvePropIdentifier( edgePropsMetadata, standardName );
	}

	/**
	 * Look up a property in a props metadata map. The map is keyed by property
	 * identifier, so a property is matched by the {@code identifier} of its
	 * metadata first, and by the map key second, which covers maps built
	 * without setting the identifier.
	 *
	 * @param propsMetadata
	 *            {@code node_props_metadata} or {@code edge_props_metadata},
	 *            may be null
	 * @param standardName
	 *            the conventional name of the property
	 * @return the property metadata, or null if the property is not declared
	 */
	private static PropMetadata findPropMetadata( final Map< String, PropMetadata > propsMetadata, final String standardName )
	{
		if ( propsMetadata == null )
			return null;

		for ( final PropMetadata propMetadata : propsMetadata.values() )
			if ( propMetadata != null && standardName.equals( propMetadata.getIdentifier() ) )
				return propMetadata;

		return propsMetadata.get( standardName );
	}

	/**
	 * Resolve the identifier naming the group that holds a property, by looking
	 * the property up in a props metadata map.
	 *
	 * @param propsMetadata
	 *            {@code node_props_metadata} or {@code edge_props_metadata},
	 *            may be null
	 * @param standardName
	 *            the conventional name of the property
	 * @return the declared identifier, falling back to {@code standardName} for
	 *         properties that are not declared, e.g. in datasets written before
	 *         props metadata existed
	 */
	private static String resolvePropIdentifier( final Map< String, PropMetadata > propsMetadata, final String standardName )
	{
		final PropMetadata propMetadata = findPropMetadata( propsMetadata, standardName );
		final String identifier = propMetadata != null ? propMetadata.getIdentifier() : null;
		if ( identifier != null && !identifier.trim().isEmpty() )
			return identifier;

		LOG.debug( "no props metadata for '{}', assuming it is stored under that name", standardName );
		return standardName;
	}

	/**
	 * Get the positions that the x, y (and z) axes occupy in a property whose
	 * dimensions follow the declaration order of the spatial axes, such as a
	 * covariance matrix: the spec stores those "in the same coordinate system
	 * as the {@code space} type properties", and axes are declared slowest to
	 * fastest, i.e. typically {@code z, y, x}.
	 * <p>
	 * For axes declared as {@code z, y, x} this returns {@code { 2, 1, 0 }},
	 * i.e. the x axis is the last dimension of the stored matrix. For axes
	 * declared as {@code x, y, z} it returns the identity {@code { 0, 1, 2 }}.
	 *
	 * @param numDimensions
	 *            2 for a 2D property (x, y), 3 for a 3D one (x, y, z)
	 * @return the positions of the x, y (and z) axes in the stored order, or
	 *         null if the dataset does not declare the spatial axes needed to
	 *         tell, in which case the stored order is assumed to be x, y, z
	 */
	public int[] getSpaceAxisPositions( final int numDimensions )
	{
		final String[] spaceAxes = getAxisNamesByType( GeffAxis.TYPE_SPACE );
		if ( spaceAxes.length < numDimensions )
			return null;

		final String[] axisNames = numDimensions == 3
				? new String[] { getHorizontalAxisName(), getVerticalAxisName(), getDepthAxisName() }
				: new String[] { getHorizontalAxisName(), getVerticalAxisName() };

		final int[] declarationIndices = new int[ numDimensions ];
		for ( int j = 0; j < numDimensions; ++j )
		{
			declarationIndices[ j ] = Arrays.asList( spaceAxes ).indexOf( axisNames[ j ] );
			if ( declarationIndices[ j ] < 0 )
			{
				LOG.debug( "axis '{}' is not among the spatial axes {}, assuming the x, y, z order",
						axisNames[ j ], Arrays.toString( spaceAxes ) );
				return null;
			}
		}

		// The position of an axis is its rank among the axes taken into
		// account, so that a covariance2d of a z, y, x dataset is read as
		// y, x rather than as the declaration indices 1, 2.
		final int[] positions = new int[ numDimensions ];
		for ( int j = 0; j < numDimensions; ++j )
			for ( int k = 0; k < numDimensions; ++k )
				if ( declarationIndices[ k ] < declarationIndices[ j ] )
					++positions[ j ];
		return positions;
	}

	public RelatedObjects getRelatedObjects()
	{
		return relatedObjects;
	}

	public void setRelatedObjects( final RelatedObjects relatedObjects )
	{
		this.relatedObjects = relatedObjects;
		validate();
	}

	/**
	 * Get the axis name for a given axis type. Returns the name of the first
	 * axis matching the specified type, or null if no such axis exists.
	 *
	 * @param type
	 *            the axis type (e.g., "time", "space", "channel")
	 * @return the axis name, or null if no axis of the given type exists
	 */
	public String getAxisNameByType( final String type )
	{
		if ( geffAxes != null )
		{
			for ( final GeffAxis axis : geffAxes )
			{
				if ( type.equals( axis.getType() ) )
				{ return axis.getName(); }
			}
		}
		return null;
	}

	/**
	 * Get all axis names for a given axis type. Returns an array of names for
	 * all axes matching the specified type.
	 *
	 * @param type
	 *            the axis type (e.g., "space" for all spatial axes)
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
	 * Get the names of all axes, in declaration order.
	 *
	 * @return array of axis names (empty array if no axes are declared)
	 */
	public String[] getAxisNames()
	{
		if ( geffAxes == null )
			return new String[ 0 ];

		return Arrays.stream( geffAxes )
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

		// Check display hints consistency if provided
		if ( displayHints != null )
		{
			displayHints.validate();

			// Display hints must reference axes that exist
			if ( geffAxes != null )
			{
				final List< String > axisNames = Arrays.asList( getAxisNames() );
				checkDisplayHint( "display_horizontal", displayHints.getDisplayHorizontal(), axisNames );
				checkDisplayHint( "display_vertical", displayHints.getDisplayVertical(), axisNames );
				checkDisplayHint( "display_depth", displayHints.getDisplayDepth(), axisNames );
				checkDisplayHint( "display_time", displayHints.getDisplayTime(), axisNames );
			}
		}
	}

	private static void checkDisplayHint( final String hintName, final String axisName, final List< String > axisNames )
	{
		if ( axisName != null && !axisNames.contains( axisName ) )
		{ throw new IllegalArgumentException(
				"Display hint " + hintName + " name " + axisName + " not found in axes " + axisNames ); }
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
		final Map< String, String > trackNodeProps = readOptionalAttribute( reader, group, "geff/track_node_props",
				new TypeToken< Map< String, String > >()
				{}.getType() );
		LOG.debug( "found geff/track_node_props = {}", trackNodeProps );

		// displayHints may be null, so safe-read it
		final GeffDisplayHints displayHints = readOptionalAttribute( reader, group, "geff/display_hints", GeffDisplayHints.class );
		LOG.debug( "found geff/display_hints = {}", displayHints );

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
	 * Read an optional attribute. An attribute that is explicitly written as
	 * JSON {@code null}, as the Python implementation does for the optional
	 * fields it leaves unset, cannot be parsed into the target type; report it
	 * as absent instead of failing.
	 */
	private static < T > T readOptionalAttribute( final N5Reader reader, final String group, final String key, final Type type )
	{
		try
		{
			return reader.getAttribute( group, key, type );
		}
		catch ( final Exception e )
		{
			LOG.debug( "Could not parse {} as {}, setting to null: {}", key, type.getTypeName(), e.getMessage() );
			return null;
		}
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
			writer.setAttribute( group, "geff/display_hints", displayHints );
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
				"GeffMetadata{geffVersion='%s', directed=%s, geffAxes=%s, nodePropsMetadata=%s, edgePropsMetadata=%s, trackNodeProps=%s, displayHints=%s, relatedObjects=%s, extra=%s}",
				geffVersion, directed, Arrays.toString( geffAxes ), nodePropsMetadata, edgePropsMetadata, trackNodeProps, displayHints, relatedObjects, extra );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( !( o instanceof GeffMetadata ) )
			return false;
		final GeffMetadata that = ( GeffMetadata ) o;
		return directed == that.directed && Objects.equals( geffVersion, that.geffVersion ) && Objects.deepEquals( geffAxes, that.geffAxes )
				&& Objects.equals( nodePropsMetadata, that.nodePropsMetadata ) && Objects.equals( edgePropsMetadata, that.edgePropsMetadata )
				&& Objects.equals( trackNodeProps, that.trackNodeProps ) && Objects.equals( displayHints, that.displayHints )
				&& Objects.equals( relatedObjects, that.relatedObjects ) && Objects.equals( extra, that.extra );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( geffVersion, directed, Arrays.hashCode( geffAxes ), nodePropsMetadata, edgePropsMetadata, trackNodeProps, displayHints, relatedObjects, extra );
	}

	/**
	 * Display hints for GEFF
	 * 
	 * Metadata indicating how spatiotemporal axes are displayed by a viewer,
	 * corresponding to the {@code geff/display_hints} attribute of the GEFF
	 * specification.
	 * <p>
	 * The hints reference axes by {@link GeffAxis#getName() name}, e.g.
	 * 
	 * <pre>
	 * "display_hints": {
	 *     "display_horizontal": "x",
	 *     "display_vertical": "y",
	 *     "display_depth": "z",
	 *     "display_time": "t"
	 * }
	 * </pre>
	 * 
	 * {@code display_horizontal} and {@code display_vertical} are required,
	 * {@code display_depth} and {@code display_time} are optional.
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
	 *      specification</a>
	 * @author Jean-Yves Tinevez
	 * @author Ko Sugawara
	 */
	public static class GeffDisplayHints
	{

		@SerializedName( "display_horizontal" )
		private String displayHorizontal;

		@SerializedName( "display_vertical" )
		private String displayVertical;

		@SerializedName( "display_depth" )
		private String displayDepth; // Optional - can be null

		@SerializedName( "display_time" )
		private String displayTime; // Optional - can be null

		/**
		 * Default constructor
		 */
		public GeffDisplayHints()
		{}

		/**
		 * Constructor with the required fields
		 */
		public GeffDisplayHints( String displayHorizontal, String displayVertical )
		{
			this.displayHorizontal = displayHorizontal;
			this.displayVertical = displayVertical;
		}

		/**
		 * Constructor with all fields
		 */
		public GeffDisplayHints( String displayHorizontal, String displayVertical, String displayDepth, String displayTime )
		{
			this.displayHorizontal = displayHorizontal;
			this.displayVertical = displayVertical;
			this.displayDepth = displayDepth;
			this.displayTime = displayTime;
		}

		// Getters and Setters
		public String getDisplayHorizontal()
		{
			return displayHorizontal;
		}

		public void setDisplayHorizontal( String displayHorizontal )
		{
			this.displayHorizontal = displayHorizontal;
		}

		public String getDisplayVertical()
		{
			return displayVertical;
		}

		public void setDisplayVertical( String displayVertical )
		{
			this.displayVertical = displayVertical;
		}

		public String getDisplayDepth()
		{
			return displayDepth;
		}

		public void setDisplayDepth( String displayDepth )
		{
			this.displayDepth = displayDepth;
		}

		public String getDisplayTime()
		{
			return displayTime;
		}

		public void setDisplayTime( String displayTime )
		{
			this.displayTime = displayTime;
		}

		/**
		 * Validate the display hints according to GEFF rules
		 */
		public void validate()
		{
			if ( displayHorizontal == null || displayHorizontal.trim().isEmpty() )
			{ throw new IllegalArgumentException( "display_horizontal cannot be null or empty" ); }

			if ( displayVertical == null || displayVertical.trim().isEmpty() )
			{ throw new IllegalArgumentException( "display_vertical cannot be null or empty" ); }
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append( "GeffDisplayHint{" );
			sb.append( "displayHorizontal='" ).append( displayHorizontal ).append( '\'' );
			sb.append( ", displayVertical='" ).append( displayVertical ).append( '\'' );
			if ( displayDepth != null )
			{
				sb.append( ", displayDepth='" ).append( displayDepth ).append( '\'' );
			}
			if ( displayTime != null )
			{
				sb.append( ", displayTime='" ).append( displayTime ).append( '\'' );
			}
			sb.append( '}' );
			return sb.toString();
		}

		@Override
		public boolean equals( Object obj )
		{
			if ( this == obj )
				return true;
			if ( obj == null || getClass() != obj.getClass() )
				return false;

			GeffDisplayHints that = ( GeffDisplayHints ) obj;

			return Objects.equals( displayHorizontal, that.displayHorizontal ) &&
					Objects.equals( displayVertical, that.displayVertical ) &&
					Objects.equals( displayDepth, that.displayDepth ) &&
					Objects.equals( displayTime, that.displayTime );
		}

		@Override
		public int hashCode()
		{
			return Objects.hash( displayHorizontal, displayVertical, displayDepth, displayTime );
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
