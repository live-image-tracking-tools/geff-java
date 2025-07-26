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

import static org.mastodon.geff.GeffUtil.checkSupportedVersion;
import static org.mastodon.geff.GeffUtils.verifyLength;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffUtils.FlattenedDoubles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a node in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing node data from/to Zarr format.
 */
public class GeffNode implements ZarrEntity
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

    public static final double[] DEFAULT_COLOR = { 1.0, 1.0, 1.0, 1.0 }; // Default white color

    public static final double DEFAULT_RADIUS = 1.0;

    public static final double[] DEFAULT_COVARIANCE_2D = { 1.0, 0.0, 0.0, 1.0 };

    public static final double[] DEFAULT_COVARIANCE_3D = { 1.0, 0.0, 0.0, 1.0, 0.0, 1.0 };

    /**
     * Default constructor
     */
    public GeffNode()
    {}

    /**
     * Constructor with basic node parameters
     */
    public GeffNode( int id, int timepoint, double x, double y, double z, double[] color, int segmentId, double radius,
            double[] covariance2d, double[] covariance3d )
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

    public int getT()
    {
        return t;
    }

    public void setT( int timepoint )
    {
        this.t = timepoint;
    }

    public double getX()
    {
        return x;
    }

    public void setX( double x )
    {
        this.x = x;
    }

    public double getY()
    {
        return y;
    }

    public void setY( double y )
    {
        this.y = y;
    }

    public double getZ()
    {
        return z;
    }

    public void setZ( double z )
    {
        this.z = z;
    }

    public double[] getColor()
    {
        return color;
    }

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

    public int getSegmentId()
    {
        return segmentId;
    }

    public void setSegmentId( int segmentId )
    {
        this.segmentId = segmentId;
    }

    public double getRadius()
    {
        return radius;
    }

    public void setRadius( double radius )
    {
        this.radius = radius;
    }

    public double[] getCovariance2d()
    {
        return covariance2d;
    }

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

    public double[] getCovariance3d()
    {
        return covariance3d;
    }

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
     * Returns the position of the node as a 3D array.
     *
     * @deprecated Use {@link #getX()}, {@link #getY()}, {@link #getZ()}
     *             instead.
     * @return The position of the node as a 3D array.
     */
    @Deprecated
    public double[] getPosition()
    {
        return new double[] { x, y, z };
    }

    /**
     * Set the position of the node.
     *
     * @deprecated Use {@link #setX(double)}, {@link #setY(double)},
     *             {@link #setZ(double)} instead.
     * @param position
     *            The position of the node as a 3D array.
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

        public GeffNode build()
        {
            return new GeffNode( id, timepoint, x, y, z, color, segmentId, radius, covariance2d, covariance3d );
        }
    }

	/**
	 * Write nodes to Zarr format with chunked structure
	 */
	public static void writeToZarr( List< GeffNode > nodes, String zarrPath )
	{
		writeToZarr( nodes, zarrPath, ZarrUtils.DEFAULT_CHUNK_SIZE );
	}
	/**
	 * Write nodes to Zarr format with specified chunk size
	 */
	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, int chunkSize )
	{
		writeToZarr( nodes, zarrPath, chunkSize, Geff.VERSION );
	}

	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, String geffVersion )
	{
		writeToZarr( nodes, zarrPath, ZarrUtils.DEFAULT_CHUNK_SIZE, geffVersion );
	}

	public static void writeToZarr( List< GeffNode > nodes, String zarrPath, int chunkSize, String geffVersion )
	{
		LOG.debug( "Writing {} nodes to Zarr path: {} with chunk size: {} to Geff version: {}", nodes.size(), zarrPath, chunkSize, geffVersion );
		try ( final N5ZarrWriter writer = new N5ZarrWriter( zarrPath, true ) )
		{
			writeToN5( nodes, writer, "/", chunkSize, geffVersion );
		}
	}

	public static void writeToN5(
			final List< GeffNode > nodes,
			final N5Writer writer,
			final String group,
			final int chunkSize,
			String geffVersion )
	{
		if ( nodes == null )
			throw new NullPointerException( "Nodes list cannot be null" );

		if ( geffVersion == null || geffVersion.isEmpty() )
		{
			geffVersion = Geff.VERSION; // Use default version if not specified
		}
		GeffUtil.checkSupportedVersion( geffVersion );

		final String path = N5URI.normalizeGroupPath( group );

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
		GeffUtils.writeIntArray( nodes, GeffNode::getSegmentId, writer, path + "/nodes/props/track_id/values", chunkSize );

		// Write radius and covariance attributes if available
		GeffUtils.writeDoubleArray( nodes, GeffNode::getRadius, writer, path + "/nodes/props/radius/values", chunkSize );

		// TODO: ellipsoid etc

		LOG.debug( "Successfully wrote nodes to Zarr format with chunked structure" );
	}

	/**
	 * Read nodes from Zarr format with default version and chunked structure
	 *
	 * @param zarrPath
	 *            The path to the Zarr directory containing nodes.
	 * @return List of GeffNode objects read from the Zarr path.
	 */
	public static List< GeffNode > readFromZarr( String zarrPath ) throws IOException
	{
		return readFromZarr( zarrPath, Geff.VERSION );
	}

	/**
	 * Read nodes from Zarr format with specified version and chunked structure
	 *
	 * @param zarrPath
	 *            The path to the Zarr directory containing nodes.
	 * @param geffVersion
	 *            The version of the GEFF format to read.
	 * @return List of GeffNode objects read from the Zarr path.
	 */
	public static List< GeffNode > readFromZarr( final String zarrPath, final String geffVersion )
	{
		LOG.debug( "Reading nodes from Zarr path: " + zarrPath + " with Geff version: " + geffVersion );
		try ( final N5ZarrReader reader = new N5ZarrReader( zarrPath, true ) )
		{
			return readFromN5( reader, "/", geffVersion );
		}
	}

	public static List< GeffNode > readFromN5( final N5Reader reader, final String group, final String geffVersion )
	{
		checkSupportedVersion( geffVersion );
		final String path = N5URI.normalizeGroupPath( group );

		// Read node IDs from chunks
		final int[] nodeIds = GeffUtils.readAsIntArray( reader, path + "/nodes/ids", "node IDs" );
		if ( nodeIds == null )
		{
			throw new IllegalArgumentException( "required property '/nodes/ids' not found" );
		}
		final int numNodes = nodeIds.length;

		// Read time points from chunks
		final int[] timepoints = GeffUtils.readAsIntArray( reader, "/nodes/props/t/values", "timepoints" );
		verifyLength( timepoints, numNodes, "/nodes/props/t/values" );

		// Read X coordinates from chunks
		final double[] xCoords = GeffUtils.readAsDoubleArray( reader, "/nodes/props/x/values", "X coordinates" );
		verifyLength( xCoords, numNodes, "/nodes/props/x/values" );

		// Read Y coordinates from chunks
		final double[] yCoords = GeffUtils.readAsDoubleArray( reader, "/nodes/props/y/values", "Y coordinates" );
		verifyLength( yCoords, numNodes, "/nodes/props/y/values" );

		// Read Z coordinates from chunks
		final double[] zCoords = GeffUtils.readAsDoubleArray( reader, "/nodes/props/z/values", "Z coordinates" );
		verifyLength( zCoords, numNodes, "/nodes/props/z/values" );

		// Read color from chunks
		final FlattenedDoubles colors = GeffUtils.readAsDoubleMatrix( reader, "/nodes/props/color/values", "color" );
		verifyLength( colors, numNodes, "/nodes/props/color/values" );

		// Read track IDs from chunks
		final int[] trackIds = GeffUtils.readAsIntArray( reader, "/nodes/props/track_id/values", "track IDs" );
		verifyLength( trackIds, numNodes, "/nodes/props/track_id/values" );

		// Read radius from chunks
		double[] radius = GeffUtils.readAsDoubleArray( reader, "/nodes/props/radius/values", "radius" );
		verifyLength( radius, numNodes, "/nodes/props/radius/values" );

		// TODO: ellipsoid etc

		// Create node objects
		final List< GeffNode > nodes = new ArrayList<>( numNodes );
		for ( int i = 0; i < numNodes; i++ )
		{
			final int id	 = nodeIds[ i ];
			final int t = timepoints != null ? timepoints[ i ] : -1;
			final double x = xCoords != null ? xCoords[ i ] : Double.NaN;
			final double y = yCoords != null ? yCoords[ i ] : Double.NaN;
			final double z = zCoords != null ? zCoords[ i ] : Double.NaN;
			final double[] color = colors != null ? colors.rowAt( i ) : DEFAULT_COLOR;
			final int segmentId = trackIds != null ? trackIds[ i ] : -1;
			final double r = radius != null ? radius[ i ] : Double.NaN;
			final double[] covariance2d = DEFAULT_COVARIANCE_2D;
			final double[] covariance3d = DEFAULT_COVARIANCE_2D;
			final GeffNode node = new GeffNode( id, t, x, y, z, color, segmentId, r, covariance2d, covariance3d );
			nodes.add( node );
		}
		return nodes;
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
                java.util.Arrays.equals( covariance3d, geffNode.covariance3d );
    }

    @Override
    public int hashCode()
    {
        int result = id;
        result = 31 * result + t;
        result = 31 * result + Double.hashCode( x );
        result = 31 * result + Double.hashCode( y );
        result = 31 * result + Double.hashCode( z );
        result = 31 * result + ( color != null ? java.util.Arrays.hashCode( color ) : 0 );
        result = 31 * result + segmentId;
        result = 31 * result + Double.hashCode( radius );
        result = 31 * result + ( covariance2d != null ? java.util.Arrays.hashCode( covariance2d ) : 0 );
        result = 31 * result + ( covariance3d != null ? java.util.Arrays.hashCode( covariance3d ) : 0 );
        return result;
    }
}
