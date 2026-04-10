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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffUtils.FlattenedInts;
import org.mastodon.geff.geom.GeffSerializableVertex;
import org.mastodon.geff.GeffUtils.FlattenedDoubles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a node in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing node data from/to Zarr format.
 */
public class GeffNode
{
	private static final Logger LOG = LoggerFactory.getLogger( GeffNode.class );

	// Node attributes
	private int id;

	private int t;

	private double x;

	private double y;

	private double z;

	private double[] color;

	private int segmentId;

	private double radius;

	private double[] covariance2d;

	private double[] covariance3d;

	private double[] polygonX;

	private double[] polygonY;

	private Map< String, VarlengthProperty > varlengthProperties;

	private static final double[] DEFAULT_COLOR = { 1.0, 1.0, 1.0, 1.0 }; // RGBA

	public static final double DEFAULT_RADIUS = 1.0;

	public static final double[] DEFAULT_COVARIANCE_2D = { 1.0, 0.0, 0.0, 1.0 };

	public static final double[] DEFAULT_COVARIANCE_3D = { 1.0, 0.0, 0.0, 1.0, 0.0, 1.0 };

	/**
	 * Default constructor
	 */
	public GeffNode()
	{
		this.varlengthProperties = new HashMap<>();
	}

	/**
	 * Constructor with basic node parameters
	 *
	 * @param id
	 *            The unique identifier for the node.
	 * @param timepoint
	 *            The timepoint of the node.
	 * @param x
	 *            The x-coordinate of the node.
	 * @param y
	 *            The y-coordinate of the node.
	 * @param z
	 *            The z-coordinate of the node.
	 * @param color
	 *            The color of the node (RGBA).
	 * @param segmentId
	 *            The segment ID the node belongs to.
	 * @param radius
	 *            The radius of the node.
	 * @param covariance2d
	 *            The 2D covariance matrix of the node.
	 * @param covariance3d
	 *            The 3D covariance matrix of the node.
	 * @param polygonX
	 *            The x-coordinates of the polygon vertices.
	 * @param polygonY
	 *            The y-coordinates of the polygon vertices.
	 */
	public GeffNode( int id, int timepoint, double x, double y, double z, double[] color, int segmentId, double radius,
			double[] covariance2d, double[] covariance3d, double[] polygonX, double[] polygonY )
	{
		this.id = id;
		this.t = timepoint;
		this.x = x;
		this.y = y;
		this.z = z;
		this.color = color != null ? color : DEFAULT_COLOR;
		this.segmentId = segmentId;
		this.radius = radius;
		this.covariance2d = covariance2d != null ? covariance2d : DEFAULT_COVARIANCE_2D;
		this.covariance3d = covariance3d != null ? covariance3d : DEFAULT_COVARIANCE_3D;
		this.polygonX = polygonX != null ? polygonX : new double[ 0 ];
		this.polygonY = polygonY != null ? polygonY : new double[ 0 ];
		this.varlengthProperties = new HashMap<>();
	}

	/**
	 * Get the unique identifier of the node.
	 *
	 * @return The unique identifier of the node.
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Set the unique identifier of the node.
	 *
	 * @param id
	 *            The unique identifier to set.
	 */
	public void setId( int id )
	{
		this.id = id;
	}

	/**
	 * Get the timepoint of the node.
	 *
	 * @return The timepoint of the node.
	 */
	public int getT()
	{
		return t;
	}

	/**
	 * Set the timepoint of the node.
	 *
	 * @param timepoint
	 *            The timepoint to set.
	 */
	public void setT( int timepoint )
	{
		this.t = timepoint;
	}

	/**
	 * Get the x-coordinate of the node.
	 *
	 * @return The x-coordinate of the node.
	 */
	public double getX()
	{
		return x;
	}

	/**
	 * Set the x-coordinate of the node.
	 *
	 * @param x
	 *            The x-coordinate to set.
	 */
	public void setX( double x )
	{
		this.x = x;
	}

	/**
	 * Get the y-coordinate of the node.
	 *
	 * @return The y-coordinate of the node.
	 */
	public double getY()
	{
		return y;
	}

	/**
	 * Set the y-coordinate of the node.
	 *
	 * @param y
	 *            The y-coordinate to set.
	 */
	public void setY( double y )
	{
		this.y = y;
	}

	/**
	 * Get the z-coordinate of the node.
	 *
	 * @return The z-coordinate of the node.
	 */
	public double getZ()
	{
		return z;
	}

	/**
	 * Set the z-coordinate of the node.
	 *
	 * @param z
	 *            The z-coordinate to set.
	 */
	public void setZ( double z )
	{
		this.z = z;
	}

	/**
	 * Get the color of the node.
	 *
	 * @return The color of the node as an RGBA array.
	 */
	public double[] getColor()
	{
		return color;
	}

	/**
	 * Set the color of the node.
	 *
	 * @param color
	 *            The color to set as an RGBA array.
	 */
	public void setColor( double[] color )
	{
		if ( color != null && color.length == 4 )
		{
			this.color = color;
		}
		else
		{
			throw new IllegalArgumentException( "Color must be a 4-element array" );
		}
	}

	/**
	 * Get the segment ID of the node.
	 *
	 * @return The segment ID of the node.
	 */
	public int getSegmentId()
	{
		return segmentId;
	}

	/**
	 * Set the segment ID of the node.
	 *
	 * @param segmentId
	 *            The segment ID to set.
	 */
	public void setSegmentId( int segmentId )
	{
		this.segmentId = segmentId;
	}

	/**
	 * Get the radius of the node.
	 *
	 * @return The radius of the node.
	 */
	public double getRadius()
	{
		return radius;
	}

	/**
	 * Set the radius of the node.
	 *
	 * @param radius
	 *            The radius to set.
	 */
	public void setRadius( double radius )
	{
		this.radius = radius;
	}

	/**
	 * Get the 2D covariance matrix of the node.
	 *
	 * @return The 2D covariance matrix as a 4-element array.
	 */
	public double[] getCovariance2d()
	{
		return covariance2d;
	}

	/**
	 * Set the 2D covariance matrix of the node.
	 *
	 * @param covariance2d
	 *            The 2D covariance matrix to set as a 4-element array.
	 *
	 * @throws IllegalArgumentException
	 *             if the covariance2d array is not of length 4.
	 */
	public void setCovariance2d( double[] covariance2d )
	{
		if ( covariance2d != null && covariance2d.length == 4 )
		{
			this.covariance2d = covariance2d;
		}
		else
		{
			throw new IllegalArgumentException( "Covariance2D must be a 4-element array" );
		}
	}

	/**
	 * Get the 3D covariance matrix of the node.
	 *
	 * @return The 3D covariance matrix as a 6-element array.
	 */
	public double[] getCovariance3d()
	{
		return covariance3d;
	}

	/**
	 * Set the 3D covariance matrix of the node.
	 *
	 * @param covariance3d
	 *            The 3D covariance matrix to set as a 6-element array.
	 *
	 * @throws IllegalArgumentException
	 *             if the covariance3d array is not of length 6.
	 */
	public void setCovariance3d( double[] covariance3d )
	{
		if ( covariance3d != null && covariance3d.length == 6 )
		{
			this.covariance3d = covariance3d;
		}
		else
		{
			throw new IllegalArgumentException( "Covariance3D must be a 6-element array" );
		}
	}

	/**
	 * Get the x-coordinates of the polygon vertices.
	 *
	 * @return The x-coordinates of the polygon vertices.
	 */
	public double[] getPolygonX()
	{
		return polygonX;
	}

	/**
	 * Get the y-coordinates of the polygon vertices.
	 *
	 * @return The y-coordinates of the polygon vertices.
	 */
	public double[] getPolygonY()
	{
		return polygonY;
	}

	/**
	 * Set the x-coordinates of the polygon vertices.
	 *
	 * @param polygonX
	 *            The x-coordinates to set.
	 */
	public void setPolygonX( double[] polygonX )
	{
		this.polygonX = polygonX != null ? polygonX : new double[ 0 ];
	}

	/**
	 * Set the y-coordinates of the polygon vertices.
	 *
	 * @param polygonY
	 *            The y-coordinates to set.
	 */
	public void setPolygonY( double[] polygonY )
	{
		this.polygonY = polygonY != null ? polygonY : new double[ 0 ];
	}

	/**
	 * Get a varlength property by name
	 *
	 * @param propName
	 *            Name of the property
	 * @return VarlengthProperty if exists, null otherwise
	 */
	public VarlengthProperty getVarlengthProperty( final String propName )
	{
		return varlengthProperties != null ? varlengthProperties.get( propName ) : null;
	}

	/**
	 * Add or update a varlength property
	 *
	 * @param propName
	 *            Name of the property
	 * @param property
	 *            VarlengthProperty to store
	 */
	public void setVarlengthProperty( final String propName, final VarlengthProperty property )
	{
		if ( varlengthProperties == null )
		{
			varlengthProperties = new HashMap<>();
		}
		varlengthProperties.put( propName, property );
	}

	/**
	 * Get all varlength properties
	 *
	 * @return Map of varlength properties
	 */
	public Map< String, VarlengthProperty > getVarlengthProperties()
	{
		return varlengthProperties != null ? varlengthProperties : new HashMap<>();
	}

	/**
	 * Returns the position of the node as a 3D array.
	 *
	 * @return The position of the node as a 3D array.
	 *
	 * @deprecated Use {@link #getX()}, {@link #getY()}, {@link #getZ()}
	 *             instead.
	 */
	@Deprecated
	public double[] getPosition()
	{
		return new double[] { x, y, z };
	}

	/**
	 * Set the position of the node.
	 *
	 * @param position
	 *            The position of the node as a 3D array.
	 *
	 * @deprecated Use {@link #setX(double)}, {@link #setY(double)},
	 *             {@link #setZ(double)} instead.
	 */
	@Deprecated
	public void setPosition( double[] position )
	{
		if ( position != null && position.length == 2 )
		{
			this.x = position[ 0 ];
			this.y = position[ 1 ];
			this.z = 0.0; // Default Z to 0
		}
		else if ( position != null && position.length == 3 )
		{
			this.x = position[ 0 ];
			this.y = position[ 1 ];
			this.z = position[ 2 ];
		}
		else
		{
			throw new IllegalArgumentException( "Position must be a 2D or 3D array" );
		}
	}

	/**
	 * Builder for creating GeffNode instance.
	 *
	 * @return A new Builder instance for GeffNode.
	 */
	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private int id;

		private int timepoint;

		private double x;

		private double y;

		private double z;

		private double[] color = DEFAULT_COLOR;

		private int segmentId;

		private double radius = DEFAULT_RADIUS;

		private double[] covariance2d = DEFAULT_COVARIANCE_2D;

		private double[] covariance3d = DEFAULT_COVARIANCE_3D;

		private double[] polygonX;

		private double[] polygonY;

		public Builder id( int id )
		{
			this.id = id;
			return this;
		}

		public Builder timepoint( int timepoint )
		{
			this.timepoint = timepoint;
			return this;
		}

		public Builder x( double x )
		{
			this.x = x;
			return this;
		}

		public Builder y( double y )
		{
			this.y = y;
			return this;
		}

		public Builder z( double z )
		{
			this.z = z;
			return this;
		}

		public Builder color( double[] color )
		{
			if ( color != null && color.length == 4 )
			{
				this.color = color;
			}
			else
			{
				throw new IllegalArgumentException( "Color must be a 4-element array" );
			}
			return this;
		}

		public Builder segmentId( int segmentId )
		{
			this.segmentId = segmentId;
			return this;
		}

		public Builder radius( double radius )
		{
			this.radius = radius;
			return this;
		}

		public Builder covariance2d( double[] covariance2d )
		{
			if ( covariance2d != null && covariance2d.length == 4 )
			{
				this.covariance2d = covariance2d;
			}
			else
			{
				throw new IllegalArgumentException( "Covariance2D must be a 4-element array" );
			}
			return this;
		}

		public Builder covariance3d( double[] covariance3d )
		{
			if ( covariance3d != null && covariance3d.length == 6 )
			{
				this.covariance3d = covariance3d;
			}
			else
			{
				throw new IllegalArgumentException( "Covariance3D must be a 6-element array" );
			}
			return this;
		}

		public Builder polygonX( double[] polygonX )
		{
			this.polygonX = polygonX;
			return this;
		}

		public Builder polygonY( double[] polygonY )
		{
			this.polygonY = polygonY;
			return this;
		}

		public GeffNode build()
		{
			return new GeffNode( id, timepoint, x, y, z, color, segmentId, radius, covariance2d, covariance3d, polygonX, polygonY );
		}
	}

	/**
	 * Read nodes from Zarr format with default version and chunked structure
	 *
	 * @param zarrPath
	 *            The path to the Zarr directory containing nodes.
	 *
	 * @return List of GeffNode objects read from the Zarr path.
	 */
	public static List< GeffNode > readFromZarr( String zarrPath ) throws IOException
	{
		final GeffMetadata metadata = GeffMetadata.readFromZarr( zarrPath );
		return readFromZarr( zarrPath, metadata );
	}

	/**
	 * Read nodes from Zarr format with specified version and chunked structure
	 *
	 * @param zarrPath
	 *            The path to the Zarr directory containing nodes.
	 * @param metadata
	 *            The GeffMetadata for the dataset.
	 *
	 * @return List of GeffNode objects read from the Zarr path.
	 */
	public static List< GeffNode > readFromZarr( final String zarrPath, final GeffMetadata metadata )
	{
		LOG.debug( "Reading nodes from Zarr path: " + zarrPath + " with Geff version: " + metadata.getGeffVersion() );
		try (final N5ZarrReader reader = new N5ZarrReader( zarrPath, true ))
		{
			return readFromN5( reader, "/", metadata );
		}
	}

	public static List< GeffNode > readFromN5( final N5Reader reader, final String group, final GeffMetadata metadata )
	{
		final String geffVersion = metadata.getGeffVersion();
		checkSupportedVersion( geffVersion );
		final String path = N5URI.normalizeGroupPath( group );

		// GRACEFUL HANDLING FOR v1 SPEC COMPATIBILITY:
		// - Variable-length properties (varlength: true): Will be skipped with
		// a warning
		// - String properties (dtype: "str" or "bytes"): Will be skipped with a
		// warning
		// - Missing value arrays: Will log a warning; values are read as
		// present (no sparse support)
		// See GeffUtils.shouldSkipProperty() and checkForMissingValues() for
		// implementation

		// Read node IDs from chunks
		final int[] nodeIds = GeffUtils.readAsIntArray( reader, path + "/nodes/ids", "node IDs" );
		if ( nodeIds == null )
		{ throw new IllegalArgumentException( "required property '/nodes/ids' not found" ); }
		final int numNodes = nodeIds.length;

		// Read time points from chunks
		final int[] timepoints = GeffUtils.readAsIntArray( reader, path + "/nodes/props/t/values", "timepoints" );
		verifyLength( timepoints, numNodes, "/nodes/props/t/values" );

		// Read X coordinates from chunks
		final double[] xCoords = GeffUtils.readAsDoubleArray( reader, path + "/nodes/props/x/values", "X coordinates" );
		verifyLength( xCoords, numNodes, "/nodes/props/x/values" );

		// Read Y coordinates from chunks
		final double[] yCoords = GeffUtils.readAsDoubleArray( reader, path + "/nodes/props/y/values", "Y coordinates" );
		verifyLength( yCoords, numNodes, "/nodes/props/y/values" );

		// Read Z coordinates from chunks
		final double[] zCoords = GeffUtils.readAsDoubleArray( reader, path + "/nodes/props/z/values", "Z coordinates" );
		verifyLength( zCoords, numNodes, "/nodes/props/z/values" );

		// Read color from chunks
		final FlattenedDoubles colors = GeffUtils.readAsDoubleMatrix( reader, path + "/nodes/props/color/values", "color" );
		verifyLength( colors, numNodes, "/nodes/props/color/values" );

		// Read track IDs from chunks
		final String trackletProp = metadata.getTrackNodeProps() != null && metadata.getTrackNodeProps().containsKey( "tracklet" )
				? metadata.getTrackNodeProps().get( "tracklet" ) : "track_id";
		final int[] trackIds = GeffUtils.readAsIntArray( reader, path + "/nodes/props/" + trackletProp + "/values", "track IDs" );
		verifyLength( trackIds, numNodes, "/nodes/props/" + trackletProp + "/values" );

		// Check for missing values and property metadata warnings (graceful
		// handling)
		GeffUtils.checkForMissingValues( reader, path + "/nodes/props/" + trackletProp + "/values" );
		if ( metadata.getNodePropsMetadata() != null && metadata.getNodePropsMetadata().containsKey( trackletProp ) )
		{
			PropMetadata propMeta = metadata.getNodePropsMetadata().get( trackletProp );
			if ( GeffUtils.shouldSkipProperty( trackletProp, propMeta ) )
			{
				// Log warning already done in shouldSkipProperty, continue
				// reading with default values
			}
		}

		// Read radius from chunks
		double[] radius = GeffUtils.readAsDoubleArray( reader, path + "/nodes/props/radius/values", "radius" );
		verifyLength( radius, numNodes, "/nodes/props/radius/values" );

		// Read covariance2d from chunks
		final FlattenedDoubles covariance2ds = GeffUtils.readAsDoubleMatrix( reader, path + "/nodes/props/covariance2d/values", "covariance2d" );
		verifyLength( covariance2ds, numNodes, "/nodes/props/covariance2d/values" );

		// Read covariance3d from chunks
		final FlattenedDoubles covariance3ds = GeffUtils.readAsDoubleMatrix( reader, path + "/nodes/props/covariance3d/values", "covariance3d" );
		verifyLength( covariance3ds, numNodes, "/nodes/props/covariance3d/values" );

		// Read polygon from chunks
		double[][] polygonsX = null;
		double[][] polygonsY = null;
		if ( geffVersion.startsWith( "0.4" ) )
		{
			try
			{
				final FlattenedInts polygonSlices = GeffUtils.readAsIntMatrix( reader, path + "/nodes/serialized_props/polygon/slices", "polygon slices" );
				verifyLength( polygonSlices, numNodes, "/nodes/serialized_props/polygon/slices" );

				final FlattenedDoubles polygonValues = GeffUtils.readAsDoubleMatrix( reader, path + "/nodes/serialized_props/polygon/values", "polygon values" );

				polygonsX = new double[ numNodes ][];
				polygonsY = new double[ numNodes ][];
				for ( int i = 0; i < numNodes; i++ )
				{
					int start = polygonSlices.at( i, 0 );
					int length = polygonSlices.at( i, 1 );
					final int numVertices = polygonValues.size()[ 0 ];
					if ( start >= 0 && start + length < numVertices )
					{
						final double[] xPoints = new double[ length ];
						final double[] yPoints = new double[ length ];
						for ( int j = 0; j < length; j++ )
						{
							xPoints[ j ] = polygonValues.at( start + j, 0 );
							yPoints[ j ] = polygonValues.at( start + j, 1 );
						}
						polygonsX[ i ] = xPoints;
						polygonsY[ i ] = yPoints;
					}
					else
					{
						LOG.warn( "Warning: Invalid polygon slice at index {}, skipping...", i );
					}
				}
			}
			catch ( Exception e )
			{
				LOG.warn( "Warning: Could not read polygon: {}, skipping...", e.getMessage() );
			}
		}

		// Read varlength properties
		final Map< String, VarlengthProperty > varlengthPropsMap = new HashMap<>();
		if ( metadata.getNodePropsMetadata() != null )
		{
			for ( final String propName : metadata.getNodePropsMetadata().keySet() )
			{
				final PropMetadata propMeta = metadata.getNodePropsMetadata().get( propName );
				if ( propMeta != null && propMeta.getVarlength() != null && propMeta.getVarlength() )
				{
					final String propPath = path + "/nodes/props/" + propName;
					final VarlengthProperty varlengthProp = GeffUtils.readVarlengthProperty( reader, propPath, numNodes, propMeta );
					if ( varlengthProp != null )
					{
						varlengthPropsMap.put( propName, varlengthProp );
						LOG.debug( "Successfully read varlength property: {}", propName );
					}
				}
			}
		}

		// Create node objects
		final List< GeffNode > nodes = new ArrayList<>( numNodes );
		for ( int i = 0; i < numNodes; i++ )
		{
			final int id = nodeIds[ i ];
			final int t = timepoints != null ? timepoints[ i ] : -1;
			final double x = xCoords != null ? xCoords[ i ] : Double.NaN;
			final double y = yCoords != null ? yCoords[ i ] : Double.NaN;
			final double z = zCoords != null ? zCoords[ i ] : Double.NaN;
			final double[] color = colors != null ? colors.rowAt( i ) : DEFAULT_COLOR;
			final int segmentId = trackIds != null ? trackIds[ i ] : -1;
			final double r = radius != null ? radius[ i ] : Double.NaN;
			final double[] covariance2d = DEFAULT_COVARIANCE_2D;
			final double[] covariance3d = DEFAULT_COVARIANCE_2D;
			final double[] polygonX = polygonsX != null ? polygonsX[ i ] : null;
			final double[] polygonY = polygonsY != null ? polygonsY[ i ] : null;
			final GeffNode node = new GeffNode( id, t, x, y, z, color, segmentId, r, covariance2d, covariance3d, polygonX, polygonY );

			// Add varlength properties to the node
			for ( final String propName : varlengthPropsMap.keySet() )
			{
				final VarlengthProperty varlengthProp = varlengthPropsMap.get( propName );
				node.setVarlengthProperty( propName, varlengthProp );
			}

			nodes.add( node );
		}
		return nodes;
	}

	/**
	 * Write nodes to Zarr format with chunked structure
	 */
	public static void writeToZarr( List< GeffNode > nodes, String zarrPath )
	{
		writeToZarr( nodes, zarrPath, GeffUtils.DEFAULT_CHUNK_SIZE );
	}

	/**
	 * Write nodes to Zarr format with specified chunk size
	 */
	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, int chunkSize )
	{
		// Create minimal metadata for backward compatibility
		GeffMetadata metadata = new GeffMetadata( Geff.VERSION, true ); // Assume
																		// directed
																		// for
																		// now
		writeToZarr( nodes, zarrPath, chunkSize, metadata );
	}

	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, String geffVersion )
	{
		// Create minimal metadata for backward compatibility
		GeffMetadata metadata = new GeffMetadata( geffVersion, true ); // Assume
																		// directed
																		// for
																		// now
		writeToZarr( nodes, zarrPath, GeffUtils.DEFAULT_CHUNK_SIZE, metadata );
	}

	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, GeffMetadata metadata )
	{
		writeToZarr( nodes, zarrPath, GeffUtils.DEFAULT_CHUNK_SIZE, metadata );
	}

	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, int chunkSize, GeffMetadata metadata )
	{
		LOG.debug( "Writing {} nodes to Zarr path: {} with chunk size: {} to Geff version: {}", nodes.size(), zarrPath, chunkSize, metadata.getGeffVersion() );
		try (final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ))
		{
			writeToN5( nodes, writer, "/", chunkSize, metadata );
		}
	}

	public static void writeToN5(
			final List< GeffNode > nodes,
			final N5Writer writer,
			final String group,
			final int chunkSize,
			final GeffMetadata metadata )
	{
		if ( nodes == null )
			throw new NullPointerException( "Nodes list cannot be null" );

		final String geffVersion = metadata.getGeffVersion();
		if ( geffVersion == null || geffVersion.isEmpty() )
		{ throw new IllegalArgumentException( "Geff version cannot be null or empty" ); }
		GeffUtils.checkSupportedVersion( geffVersion );

		final String path = N5URI.normalizeGroupPath( group );
		final int numNodes = nodes.size();

		// Write node IDs in chunks
		GeffUtils.writeIntArray( nodes, GeffNode::getId, writer, path + "/nodes/ids", chunkSize );

		// Write timepoints in chunks
		GeffUtils.writeIntArray( nodes, GeffNode::getT, writer, path + "/nodes/props/t/values", chunkSize );

		// Write X coordinates in chunks
		GeffUtils.writeDoubleArray( nodes, GeffNode::getX, writer, path + "/nodes/props/x/values", chunkSize );

		// Write Y coordinates in chunks
		GeffUtils.writeDoubleArray( nodes, GeffNode::getY, writer, path + "/nodes/props/y/values", chunkSize );

		// Write Z coordinates in chunks
		GeffUtils.writeDoubleArray( nodes, GeffNode::getZ, writer, path + "/nodes/props/z/values", chunkSize );

		// Write color in chunks
		GeffUtils.writeDoubleMatrix( nodes, 4, GeffNode::getColor, writer, path + "/nodes/props/color/values", chunkSize );

		// Write segment IDs in chunks
		final String trackletProp = metadata.getTrackNodeProps() != null && metadata.getTrackNodeProps().containsKey( "tracklet" )
				? metadata.getTrackNodeProps().get( "tracklet" ) : "track_id";
		GeffUtils.writeIntArray( nodes, GeffNode::getSegmentId, writer, path + "/nodes/props/" + trackletProp + "/values", chunkSize );

		// Write radius and covariance attributes if available
		GeffUtils.writeDoubleArray( nodes, GeffNode::getRadius, writer, path + "/nodes/props/radius/values", chunkSize );

		// Write covariance2d in chunks
		GeffUtils.writeDoubleMatrix( nodes, 4, GeffNode::getCovariance2d, writer, path + "/nodes/props/covariance2d/values", chunkSize );

		// Write covariance3d in chunks
		GeffUtils.writeDoubleMatrix( nodes, 6, GeffNode::getCovariance3d, writer, path + "/nodes/props/covariance3d/values", chunkSize );

		// Write variable-length node properties if available
		final Set< String > varlengthPropertyNames = new HashSet<>();
		for ( final GeffNode node : nodes )
		{
			varlengthPropertyNames.addAll( node.getVarlengthProperties().keySet() );
		}
		if ( !varlengthPropertyNames.isEmpty() )
		{
			if ( metadata.getNodePropsMetadata() == null )
			{
				metadata.setNodePropsMetadata( new HashMap<>() );
			}
			for ( final String propName : varlengthPropertyNames )
			{
				final Object[][] nodeDataArrays = new Object[ numNodes ][];
				final boolean[] missing = new boolean[ numNodes ];
				String dtype = null;

				for ( int i = 0; i < numNodes; i++ )
				{
					final VarlengthProperty property = nodes.get( i ).getVarlengthProperty( propName );
					if ( property == null || property.isMissing( i ) )
					{
						nodeDataArrays[ i ] = null;
						missing[ i ] = true;
						continue;
					}

					if ( dtype == null )
					{
						dtype = property.getDtype();
					}

					final Object nodeData = property.getNodeData( i );
					if ( nodeData == null )
					{
						nodeDataArrays[ i ] = new Object[ 0 ];
					}
					else if ( nodeData.getClass().isArray() )
					{
						if ( nodeData instanceof Object[] )
						{
							nodeDataArrays[ i ] = ( Object[] ) nodeData;
						}
						else
						{
							final int length = Array.getLength( nodeData );
							final Object[] converted = new Object[ length ];
							for ( int j = 0; j < length; j++ )
							{
								converted[ j ] = Array.get( nodeData, j );
							}
							nodeDataArrays[ i ] = converted;
						}
					}
					else
					{
						nodeDataArrays[ i ] = new Object[] { nodeData };
					}
				}

				if ( dtype == null )
				{
					dtype = "float64";
				}

				GeffUtils.writeVarlengthProperty( writer, path + "/nodes/props/" + propName, nodeDataArrays, missing, chunkSize );

				final Map< String, PropMetadata > nodePropsMetadata = metadata.getNodePropsMetadata();
				if ( !nodePropsMetadata.containsKey( propName ) )
				{
					nodePropsMetadata.put( propName, new PropMetadata( propName, dtype, true, null, null, null ) );
				}
			}
		}

		if ( geffVersion.startsWith( "0.4" ) )
		{
			// Write polygon slices and values if available
			final List< GeffSerializableVertex > vertices = new ArrayList<>();
			final List< int[] > slices = new ArrayList<>();
			int polygonOffset = 0;
			for ( final GeffNode node : nodes )
			{
				if ( node.polygonX == null || node.polygonY == null )
					throw new IllegalArgumentException( "Polygon coordinates cannot be null" );
				if ( node.getPolygonX().length != node.getPolygonY().length )
					throw new IllegalArgumentException( "Polygon X and Y coordinates must have the same length" );
				final int numVertices = node.getPolygonX().length;
				for ( int j = 0; j < numVertices; j++ )
					vertices.add( new GeffSerializableVertex(
							node.getPolygonX()[ j ],
							node.getPolygonY()[ j ] ) );
				slices.add( new int[] { polygonOffset, numVertices } );
				polygonOffset += numVertices;
			}
			GeffUtils.writeIntMatrix( slices, 2, Function.identity(), writer, path + "/nodes/serialized_props/polygon/slices", chunkSize );
			GeffUtils.writeDoubleMatrix( vertices, 2, GeffSerializableVertex::getCoordinates, writer, path + "/nodes/serialized_props/polygon/values", chunkSize );
		}

		LOG.debug( "Successfully wrote nodes to Zarr format with chunked structure" );
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder( "GeffNode{" )
				.append( "id=" ).append( id )
				.append( ", t=" ).append( t )
				.append( ", x=" ).append( String.format( "%.2f", x ) )
				.append( ", y=" ).append( String.format( "%.2f", y ) )
				.append( ", z=" ).append( String.format( "%.2f", z ) )
				.append( color != null ? ", color=" + java.util.Arrays.toString( color ) : "" )
				.append( ", segId=" ).append( segmentId )
				.append( "radius=" ).append( String.format( "%.2f", radius ) )
				.append( covariance2d != null ? ", covariance2d=" + java.util.Arrays.toString( covariance2d ) : "" )
				.append( covariance3d != null ? ", covariance3d=" + java.util.Arrays.toString( covariance3d ) : "" )
				.append( "}" );
		return sb.toString();
	}

	@Override
	public boolean equals( Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null || getClass() != obj.getClass() )
			return false;

		GeffNode geffNode = ( GeffNode ) obj;
		return id == geffNode.id &&
				t == geffNode.t &&
				Double.compare( geffNode.x, x ) == 0 &&
				Double.compare( geffNode.y, y ) == 0 &&
				Double.compare( geffNode.z, z ) == 0 &&
				java.util.Arrays.equals( color, geffNode.color ) &&
				segmentId == geffNode.segmentId &&
				Double.compare( geffNode.radius, radius ) == 0 &&
				java.util.Arrays.equals( covariance2d, geffNode.covariance2d ) &&
				java.util.Arrays.equals( covariance3d, geffNode.covariance3d ) &&
				java.util.Arrays.equals( polygonX, geffNode.polygonX ) &&
				java.util.Arrays.equals( polygonY, geffNode.polygonY );
	}

	@Override
	public int hashCode()
	{
		int result = id;
		result = 31 * result + t;
		result = 31 * result + Double.hashCode( x );
		result = 31 * result + Double.hashCode( y );
		result = 31 * result + Double.hashCode( z );
		result = 31 * result + Arrays.hashCode( color );
		result = 31 * result + segmentId;
		result = 31 * result + Double.hashCode( radius );
		result = 31 * result + Arrays.hashCode( covariance2d );
		result = 31 * result + Arrays.hashCode( covariance3d );
		return result;
	}
}
