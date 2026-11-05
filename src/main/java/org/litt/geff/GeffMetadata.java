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
package org.litt.geff;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.scijava.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Represents metadata for a GEFF (Graph Exchange File Format) dataset,
 * corresponding to the {@code geff} key of the zarr attributes file.
 * <p>
 * This class handles reading and writing metadata from/to Zarr format, and is
 * the Java equivalent of the Python {@code GeffMetadata} schema from:
 * https://github.com/live-image-tracking-tools/geff/blob/main/src/geff/metadata_schema.py
 * <p>
 * Reading and writing are performed on the raw {@code geff} attribute map:
 * known keys are parsed into the typed fields of this class, and unknown keys
 * are preserved as-is (see {@link #getUnknownAttributes()}) so that files
 * written by future versions of the specification round-trip losslessly.
 *
 * @see <a href=
 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
 *      specification</a>
 */
public class GeffMetadata
{

	private static final Logger LOG = LoggerFactory.getLogger( GeffMetadata.class );

	private static final Gson GSON = new Gson();

	/** The GEFF-Java library version. */
	public static final String VERSION = VersionUtils.getVersion( GeffMetadata.class );

	/**
	 * The type of the raw {@code geff} attribute map.
	 */
	private static final Type MAP_TYPE = new TypeToken< Map< String, Object > >()
	{}.getType();

	/**
	 * The keys of the {@code geff} attribute map that are parsed into typed
	 * fields of this class. All other keys found on read are kept as-is in
	 * {@link #getUnknownAttributes() unknownAttributes} and written back
	 * unchanged, so that attributes introduced by future versions of the
	 * specification round-trip losslessly.
	 */
	private static final Set< String > KNOWN_KEYS = Set.of(
			"geff_version", "directed", "axes",
			"node_props_metadata", "edge_props_metadata",
			"track_node_props", "sphere", "ellipsoid", "polygon", "mesh",
			"related_objects", "display_hints", "extra" );

	/*
	 * Pattern to match semantic versions, allowing for patch versions and
	 * development versions. Examples: 0.1.1, 0.2.2.dev20+g611e7a2.d20250719,
	 * 0.2.0-alpha.1, etc.
	 */
	private static final Pattern SEMVER_PATTERN = Pattern
			.compile( "^\\d+\\.\\d+(?:\\.\\d+)?(?:\\.dev\\d+)?(?:[.-][a-zA-Z0-9-]+(?:[.-][a-zA-Z0-9-]+)*)?(?:\\+[a-zA-Z0-9.-]+)?$" );

	/**
	 * Checks that the specified version string matches the semantic versioning
	 * pattern, throws an {@link IllegalArgumentException} otherwise.
	 */
	private static void checkSemver( final String version )
	{
		if ( !SEMVER_PATTERN.matcher( version ).matches() )
			throw new IllegalArgumentException(
					"geff_version " + version + " does not follow semantic versioning (e.g. 1.3, 1.3.1, 0.3.1.dev6+g61d5f18)." );
	}

	// Metadata attributes - matching the Python schema

	private String geffVersion;

	private boolean directed;

	private List< GeffAxis > geffAxes;

	private Map< String, PropMetadata > nodePropsMetadata;

	private Map< String, PropMetadata > edgePropsMetadata;

	private Map< String, String > trackNodeProps;

	/**
	 * Identifier of the node property that defines the sphere shape of nodes,
	 * or {@code null} if the graph does not have sphere shapes. Corresponds to
	 * the {@code geff/sphere} attribute of the GEFF specification. The sphere
	 * property is a scalar property storing the radius of the hypersphere, in
	 * the units of the spatial axes.
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/specification/#geff_spec.GeffMetadata">GEFF
	 *      Specification: sphere</a>
	 */
	private String sphere;

	/**
	 * Identifier of the node property that defines the ellipsoid shape of
	 * nodes, or {@code null} if the graph does not have ellipsoid shapes.
	 * Corresponds to the {@code geff/ellipsoid} attribute of the GEFF
	 * specification. The ellipsoid property is a matrix property storing a
	 * symmetric positive-definite covariance matrix, as a {@code 2x2} or
	 * {@code 3x3} array; its center is given by the spatial axes properties.
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/specification/#geff_spec.GeffMetadata">GEFF
	 *      Specification: ellipsoid</a>
	 */
	private String ellipsoid;

	/**
	 * Identifier of the node property that defines the polygon shape of nodes,
	 * or {@code null} if the graph does not have polygon shapes. Corresponds to
	 * the {@code geff/polygon} attribute of the GEFF specification. The polygon
	 * property is a property storing {@code (n_points, n_dimensions)} arrays of
	 * points relative to the node position; since the number of points may
	 * differ for each node, it is typically a var-length property.
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/specification/#geff_spec.GeffMetadata">GEFF
	 *      Specification: polygon</a>
	 */
	private String polygon;

	/**
	 * The mesh shape specification of nodes, or {@code null} if the graph does
	 * not have mesh shapes. Corresponds to the {@code geff/mesh} attribute of
	 * the GEFF specification.
	 *
	 * @see <a href=
	 *      "https://liveimagetrackingtools.org/geff/latest/specification/#geff_spec.GeffMetadata">GEFF
	 *      Specification: mesh</a>
	 */
	private MeshMetadata mesh;

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
	 * The attributes of the {@code geff} map that this class does not know
	 * about (i.e. attributes introduced by a version of the specification newer
	 * than this library). They are preserved on read and written back
	 * unchanged, so that files round-trip losslessly. Never {@code null} after
	 * {@link #readFromN5(N5Reader, String)}; may be modified through
	 * {@link #getUnknownAttributes()}.
	 */
	private final Map< String, Object > unknownAttributes = new LinkedHashMap<>();

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
		if ( geffVersion != null && !SEMVER_PATTERN.matcher( geffVersion ).matches() )
		{ throw new IllegalArgumentException(
				"Unsupported Geff version: " + geffVersion +
						". The version must follow semantic versioning, e.g. 1.3, 1.3.1, 0.3.1.dev6+g61d5f18." ); }
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

	/**
	 * Returns the axes of this dataset, in declaration order. The order is
	 * meaningful: it defines the dimension order of array-valued properties,
	 * and is used as a fallback when aligning a graph with image data that do
	 * not declare axes of their own.
	 *
	 * @return the axes, or {@code null} if the dataset declares no axes.
	 */
	public List< GeffAxis > getGeffAxes()
	{
		return geffAxes;
	}

	/**
	 * Sets the axes of this dataset, validating them. The order of the axes is
	 * meaningful; see {@link #getGeffAxes()}.
	 *
	 * @param geffAxes
	 *            the axes, or {@code null} if the dataset declares no axes.
	 */
	public void setGeffAxes( final List< GeffAxis > geffAxes )
	{
		this.geffAxes = geffAxes;
		if ( geffAxes != null )
		{
			for ( final GeffAxis axis : geffAxes )
				axis.validate();
		}
	}

	/**
	 * Alias for {@link #getGeffAxes()}.
	 *
	 * @return the axes, or {@code null} if the dataset declares no axes.
	 * @deprecated use {@link #getGeffAxes()} instead.
	 */
	@Deprecated
	public List< GeffAxis > getGeffAxesList()
	{
		return getGeffAxes();
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

	/**
	 * Sets the track node properties, mapping the special keys {@code
	 * lineage} and {@code tracklet} to the identifiers of the node properties
	 * that store the corresponding IDs. Both keys are optional, and no other
	 * key is defined by the specification.
	 *
	 * @param trackNodeProps
	 *            the track node properties, or {@code null} to unset.
	 */
	public void setTrackNodeProps( final Map< String, String > trackNodeProps )
	{
		this.trackNodeProps = trackNodeProps;
	}

	/**
	 * Returns the identifier of the node property that defines the sphere shape
	 * of nodes, or {@code null} if the graph does not have sphere shapes.
	 *
	 * @return the sphere property identifier, or {@code null}.
	 */
	public String getSphere()
	{
		return sphere;
	}

	/**
	 * Sets the identifier of the node property that defines the sphere shape of
	 * nodes.
	 *
	 * @param sphere
	 *            the sphere property identifier, or {@code null} to unset.
	 */
	public void setSphere( final String sphere )
	{
		this.sphere = sphere;
	}

	/**
	 * Returns the identifier of the node property that defines the ellipsoid
	 * shape of nodes, or {@code null} if the graph does not have ellipsoid
	 * shapes.
	 *
	 * @return the ellipsoid property identifier, or {@code null}.
	 */
	public String getEllipsoid()
	{
		return ellipsoid;
	}

	/**
	 * Sets the identifier of the node property that defines the ellipsoid shape
	 * of nodes. The property must store a symmetric positive-definite
	 * covariance matrix as a {@code 2x2} or {@code 3x3} array.
	 *
	 * @param ellipsoid
	 *            the ellipsoid property identifier, or {@code null} to unset.
	 */
	public void setEllipsoid( final String ellipsoid )
	{
		this.ellipsoid = ellipsoid;
	}

	/**
	 * Returns the identifier of the node property that defines the polygon
	 * shape of nodes, or {@code null} if the graph does not have polygon
	 * shapes.
	 *
	 * @return the polygon property identifier, or {@code null}.
	 */
	public String getPolygon()
	{
		return polygon;
	}

	/**
	 * Sets the identifier of the node property that defines the polygon shape
	 * of nodes. The property must store {@code (n_points, n_dimensions)} arrays
	 * of points relative to the node position, and is typically a var-length
	 * property.
	 *
	 * @param polygon
	 *            the polygon property identifier, or {@code null} to unset.
	 */
	public void setPolygon( final String polygon )
	{
		this.polygon = polygon;
	}

	/**
	 * Returns the mesh shape specification of nodes, or {@code null} if the
	 * graph does not have mesh shapes.
	 *
	 * @return the mesh shape specification, or {@code null}.
	 */
	public MeshMetadata getMesh()
	{
		return mesh;
	}

	/**
	 * Sets the mesh shape specification of nodes.
	 *
	 * @param mesh
	 *            the mesh shape specification, or {@code null} to unset.
	 */
	public void setMesh( final MeshMetadata mesh )
	{
		this.mesh = mesh;
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

	/**
	 * Sets the display hints, validating them. If axes are declared, the hints
	 * must name declared axes; see {@link #validate()}.
	 *
	 * @param displayHints
	 *            the display hints, or {@code null} to unset.
	 */
	public void setDisplayHints( final GeffDisplayHints displayHints )
	{
		this.displayHints = displayHints;
		if ( displayHints != null )
			displayHints.validate();
		checkDisplayHints();
	}

	/**
	 * Checks that the display hints, if any, name axes that are declared in
	 * {@link #getGeffAxes()}. Does nothing when there are no hints or no axes.
	 */
	private void checkDisplayHints()
	{
		if ( displayHints == null || geffAxes == null )
			return;

		final List< String > axisNames = Arrays.asList( getAxisNames() );
		checkDisplayHint( "display_horizontal", displayHints.getDisplayHorizontal(), axisNames );
		checkDisplayHint( "display_vertical", displayHints.getDisplayVertical(), axisNames );
		checkDisplayHint( "display_depth", displayHints.getDisplayDepth(), axisNames );
		checkDisplayHint( "display_time", displayHints.getDisplayTime(), axisNames );
	}

	/**
	 * Returns the related objects of this dataset, or {@code null} if there are
	 * none.
	 *
	 * @return the related objects, or {@code null}.
	 */
	public RelatedObjects getRelatedObjects()
	{
		return relatedObjects;
	}

	/**
	 * Sets the related objects of this dataset.
	 *
	 * @param relatedObjects
	 *            the related objects, or {@code null} to unset.
	 */
	public void setRelatedObjects( final RelatedObjects relatedObjects )
	{
		this.relatedObjects = relatedObjects;
	}

	/**
	 * Returns the attributes of the {@code geff} map that this class does not
	 * know about (i.e. attributes introduced by a version of the specification
	 * newer than this library). They are preserved on read and written back
	 * unchanged by {@link #writeToN5(N5Writer, String)}, so that files
	 * round-trip losslessly. The returned map is writable: entries added there
	 * are written as {@code geff/&lt;key&gt;} attributes.
	 *
	 * @return the unknown attributes, never {@code null}.
	 */
	public Map< String, Object > getUnknownAttributes()
	{
		return unknownAttributes;
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
					return axis.getName();
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

		return geffAxes.stream()
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

		return geffAxes.stream()
				.map( GeffAxis::getName )
				.toArray( String[]::new );
	}

	/**
	 * Validates the metadata according to the GEFF schema rules.
	 * <p>
	 * This is called once by {@link #readFromN5(N5Reader, String)} after all
	 * fields are parsed, and by {@link #writeToN5(N5Writer, String)} before
	 * anything is written. Individual fields are validated by their own
	 * setters; this method only checks cross-field consistency.
	 */
	public void validate()
	{
		if ( geffVersion == null )
		{ throw new IllegalArgumentException( "geff_version is missing." ); }

		// Check display hints consistency if provided
		if ( displayHints != null )
			displayHints.validate();
		checkDisplayHints();
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

	/**
	 * Read metadata from an N5/Zarr reader. All known keys of the raw {@code
	 * geff} attribute map are parsed into the typed fields of this class;
	 * unknown keys are preserved in {@link #getUnknownAttributes()}. Unknown or
	 * unparseable values are reported as {@code null} with a debug log message.
	 *
	 * @param reader
	 *            the {@code N5Reader}
	 * @param group
	 *            path to the geff group (relative to container root)
	 * @return the metadata
	 * @throws IllegalArgumentException
	 *             if the {@code geff} attribute map is missing, or if the
	 *             required {@code geff_version} or {@code geff/directed}
	 *             attributes are missing or invalid.
	 */
	public static GeffMetadata readFromN5( final N5Reader reader, final String group )
	{
		final Map< String, Object > geff = reader.getAttribute( group, "geff", MAP_TYPE );
		LOG.debug( "found geff = {}", geff );
		if ( geff == null || geff.get( "geff_version" ) == null )
		{ throw new IllegalArgumentException(
				"No geff/geff_version found in " + group + ". This may indicate the path is incorrect or " +
						"zarr group name is not specified (e.g. /dataset.zarr/tracks/ instead of " +
						"/dataset.zarr/)." ); }

		final GeffMetadata metadata = new GeffMetadata();

		// Required fields.
		metadata.geffVersion = attribute( geff.get( "geff_version" ), String.class, "geff_version" );
		checkSemver( metadata.geffVersion );
		final Boolean directed = attribute( geff.get( "directed" ), Boolean.class, "directed" );
		if ( directed == null )
		{ throw new IllegalArgumentException( "required attribute 'geff/directed' is missing." ); }
		metadata.directed = directed.booleanValue();

		// Optional fields.
		metadata.geffAxes = attribute( geff.get( "axes" ), new TypeToken< List< GeffAxis > >()
		{}, "axes" );
		metadata.nodePropsMetadata = attribute( geff.get( "node_props_metadata" ),
				new TypeToken< Map< String, PropMetadata > >()
				{}, "node_props_metadata" );
		metadata.edgePropsMetadata = attribute( geff.get( "edge_props_metadata" ),
				new TypeToken< Map< String, PropMetadata > >()
				{}, "edge_props_metadata" );
		metadata.trackNodeProps = attribute( geff.get( "track_node_props" ), new TypeToken< Map< String, String > >()
		{}, "track_node_props" );
		metadata.sphere = attribute( geff.get( "sphere" ), String.class, "sphere" );
		metadata.ellipsoid = attribute( geff.get( "ellipsoid" ), String.class, "ellipsoid" );
		metadata.polygon = attribute( geff.get( "polygon" ), String.class, "polygon" );
		metadata.mesh = attribute( geff.get( "mesh" ), MeshMetadata.class, "mesh" );
		metadata.displayHints = attribute( geff.get( "display_hints" ), GeffDisplayHints.class, "display_hints" );
		metadata.relatedObjects = attribute( geff.get( "related_objects" ), RelatedObjects.class, "related_objects" );
		metadata.extra = attribute( geff.get( "extra" ), new TypeToken< Map< String, Object > >()
		{}, "extra" );

		// Preserve attributes that this class does not know about.
		for ( final Map.Entry< String, Object > entry : geff.entrySet() )
			if ( !KNOWN_KEYS.contains( entry.getKey() ) )
				metadata.unknownAttributes.put( entry.getKey(), entry.getValue() );

		metadata.validate();
		return metadata;
	}

	/**
	 * Convert a raw attribute value to the given type with Gson, returning
	 * {@code null} and logging if the value is not convertible. A {@code
	 * null} input yields a {@code null} output: attributes explicitly written
	 * as JSON {@code null}, as the Python implementation does for the optional
	 * fields it leaves unset, are reported as absent.
	 */
	private static < T > T attribute( final Object raw, final Type type, final String key )
	{
		if ( raw == null )
			return null;
		try
		{
			return GSON.fromJson( GSON.toJsonTree( raw ), type );
		}
		catch ( final Exception e )
		{
			LOG.debug( "Could not parse geff/{} as {}, setting to null: {}", key, type.getTypeName(), e.getMessage() );
			return null;
		}
	}

	private static < T > T attribute( final Object raw, final TypeToken< T > type, final String key )
	{
		return attribute( raw, type.getType(), key );
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

	/**
	 * Write this metadata to an N5/Zarr writer, as the {@code geff} attribute
	 * map. Known fields are written from the typed fields of this class, in
	 * specification order; the attributes in {@link #getUnknownAttributes()
	 * unknownAttributes} are written back unchanged. Fields that are unset are
	 * omitted, except for the required {@code node_props_metadata} and
	 * {@code edge_props_metadata} maps, which are written as empty objects if
	 * unset.
	 *
	 * @param writer
	 *            the {@code N5Writer}
	 * @param group
	 *            path to the geff group (relative to container root)
	 */
	public void writeToN5( final N5Writer writer, final String group )
	{
		// Validate before writing
		validate();
		checkSemver( geffVersion );

		final Map< String, Object > geff = new LinkedHashMap<>();

		// required
		LOG.debug( "writing geff/geff_version {}", geffVersion );
		geff.put( "geff_version", geffVersion );
		LOG.debug( "writing geff/directed {}", directed );
		geff.put( "directed", directed );

		// optional
		if ( geffAxes != null )
		{
			LOG.debug( "writing geff/axes {}", geffAxes );
			geff.put( "axes", geffAxes );
		}

		final Map< String, PropMetadata > nodeMeta = nodePropsMetadata != null ? nodePropsMetadata : new HashMap<>();
		LOG.debug( "writing geff/node_props_metadata {}", nodeMeta );
		geff.put( "node_props_metadata", nodeMeta );

		final Map< String, PropMetadata > edgeMeta = edgePropsMetadata != null ? edgePropsMetadata : new HashMap<>();
		LOG.debug( "writing geff/edge_props_metadata {}", edgeMeta );
		geff.put( "edge_props_metadata", edgeMeta );

		if ( trackNodeProps != null )
		{
			LOG.debug( "writing geff/track_node_props {}", trackNodeProps );
			geff.put( "track_node_props", trackNodeProps );
		}

		if ( sphere != null )
		{
			LOG.debug( "writing geff/sphere {}", sphere );
			geff.put( "sphere", sphere );
		}

		if ( ellipsoid != null )
		{
			LOG.debug( "writing geff/ellipsoid {}", ellipsoid );
			geff.put( "ellipsoid", ellipsoid );
		}

		if ( polygon != null )
		{
			LOG.debug( "writing geff/polygon {}", polygon );
			geff.put( "polygon", polygon );
		}

		if ( mesh != null )
		{
			LOG.debug( "writing geff/mesh {}", mesh );
			geff.put( "mesh", mesh );
		}

		if ( displayHints != null )
		{
			LOG.debug( "writing geff/display_hints {}", displayHints );
			geff.put( "display_hints", displayHints );
		}

		if ( relatedObjects != null && !relatedObjects.isEmpty() )
		{
			LOG.debug( "writing geff/related_objects {}", relatedObjects );
			geff.put( "related_objects", relatedObjects );
		}

		if ( extra != null )
		{
			LOG.debug( "writing geff/extra {}", extra );
			geff.put( "extra", extra );
		}

		// Write back attributes that this class does not know about.
		geff.putAll( unknownAttributes );

		writer.setAttribute( group, "geff", geff );
	}

	@Override
	public String toString()
	{
		return String.format(
				"GeffMetadata{geffVersion='%s', directed=%s, geffAxes=%s, nodePropsMetadata=%s, edgePropsMetadata=%s, trackNodeProps=%s, sphere=%s, ellipsoid=%s, polygon=%s, mesh=%s, displayHints=%s, relatedObjects=%s, extra=%s, unknownAttributes=%s}",
				geffVersion, directed, geffAxes, nodePropsMetadata, edgePropsMetadata, trackNodeProps, sphere, ellipsoid, polygon, mesh, displayHints, relatedObjects, extra, unknownAttributes );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( !( o instanceof GeffMetadata ) )
			return false;
		final GeffMetadata that = ( GeffMetadata ) o;
		return directed == that.directed && Objects.equals( geffVersion, that.geffVersion ) && Objects.equals( geffAxes, that.geffAxes )
				&& Objects.equals( nodePropsMetadata, that.nodePropsMetadata ) && Objects.equals( edgePropsMetadata, that.edgePropsMetadata )
				&& Objects.equals( trackNodeProps, that.trackNodeProps ) && Objects.equals( sphere, that.sphere )
				&& Objects.equals( ellipsoid, that.ellipsoid ) && Objects.equals( polygon, that.polygon )
				&& Objects.equals( mesh, that.mesh ) && Objects.equals( displayHints, that.displayHints )
				&& Objects.equals( relatedObjects, that.relatedObjects ) && Objects.equals( extra, that.extra )
				&& Objects.equals( unknownAttributes, that.unknownAttributes );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( geffVersion, directed, geffAxes, nodePropsMetadata, edgePropsMetadata, trackNodeProps,
				sphere, ellipsoid, polygon, mesh, displayHints, relatedObjects, extra, unknownAttributes );
	}
}
