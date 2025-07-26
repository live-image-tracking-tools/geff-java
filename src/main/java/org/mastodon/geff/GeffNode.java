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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mastodon.geff.geom.GeffSerializableVertex;

import com.bc.zarr.ArrayParams;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

/**
 * Represents a node in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing node data from/to Zarr format.
 */
public class GeffNode implements ZarrEntity
{

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

    private int polygonStartIndex = -1;

    private double[] polygonX;

    private double[] polygonY;

    private static final double[] DEFAULT_COLOR = { 1.0, 1.0, 1.0, 1.0 }; // RGBA

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
     * Get the polygon offset for the serialized vertex array.
     * 
     * @return The polygon offset.
     */
    public int getPolygonStartIndex()
    {
        return polygonStartIndex;
    }

    /**
     * Set the polygon offset for the serialized vertex array.
     * 
     * @param polygonOffset
     *            The polygon offset to set.
     */
    public void setPolygonStartIndex( int polygonOffset )
    {
        this.polygonStartIndex = polygonOffset;
    }

    /**
     * Get the slice information for polygon vertices as an array.
     * 
     * @return An array containing the polygon startIndex and endIndex.
     */
    public int[] getPolygonSliceAsArray()
    {
        if ( polygonX == null || polygonY == null )
        {
            System.err.println( "Warning: Polygon is null, returning empty array." );
            return new int[] { polygonStartIndex, 0 };
        }
        if ( polygonStartIndex < 0 )
            throw new IllegalArgumentException( "Polygon startIndex is invalid: " + polygonStartIndex );
        return new int[] { polygonStartIndex, polygonStartIndex + polygonX.length };
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
     * @return List of GeffNode objects read from the Zarr path.
     */
    public static List< GeffNode > readFromZarr( String zarrPath ) throws IOException, InvalidRangeException
    {
        return readFromZarrWithChunks( zarrPath, Geff.VERSION );
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
    public static List< GeffNode > readFromZarr( String zarrPath, String geffVersion )
            throws IOException, InvalidRangeException
    {
        return readFromZarrWithChunks( zarrPath, geffVersion );
    }

    /**
     * Read nodes from Zarr format with chunked structure. This method handles
     * different Geff versions and reads node attributes accordingly.
     *
     * @param zarrPath
     *            The path to the Zarr directory containing nodes.
     * @param geffVersion
     *            The version of the GEFF format to read.
     * @return List of GeffNode objects read from the Zarr path.
     */
    public static List< GeffNode > readFromZarrWithChunks( String zarrPath, String geffVersion )
            throws IOException, InvalidRangeException
    {
        List< GeffNode > nodes = new ArrayList<>();

        ZarrGroup nodesGroup = ZarrGroup.open( zarrPath + "/nodes" );

        System.out.println(
                "Reading nodes from Zarr path: " + zarrPath + " with Geff version: " + geffVersion );

        if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) )
        {
            // Read node IDs from chunks
            int[] nodeIds = ZarrUtils.readChunkedIntArray( nodesGroup, "ids", "node IDs" );

            // Read attributes
            ZarrGroup attrsGroup = nodesGroup.openSubGroup( "attrs" );

            // Read time points from chunks
            int[] timepoints = ZarrUtils.readChunkedIntArray( attrsGroup, "t/values", "timepoints" );

            // Read X coordinates from chunks
            double[] xCoords = ZarrUtils.readChunkedDoubleArray( attrsGroup, "x/values", "X coordinates" );

            // Read Y coordinates from chunks
            double[] yCoords = ZarrUtils.readChunkedDoubleArray( attrsGroup, "y/values", "Y coordinates" );

            // Read segment IDs from chunks
            int[] segmentIds = new int[ 0 ];
            try
            {
                segmentIds = ZarrUtils.readChunkedIntArray( attrsGroup, "seg_id/values", "segment IDs" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read segment IDs: " + e.getMessage() + " skipping..." );
            }

            // Read positions if available from chunks
            double[][] positions = new double[ 0 ][];
            try
            {
                positions = ZarrUtils.readChunkedDoubleMatrix( attrsGroup, "position/values", "positions" );
            }
            catch ( Exception e )
            {
                // Position array might not exist or be in different format
                System.out.println( "Warning: Could not read position array: " + e.getMessage() );
            }

            // Create node objects
            for ( int i = 0; i < nodeIds.length; i++ )
            {
                GeffNode node = new Builder()
                        .id( nodeIds[ i ] )
                        .timepoint( i < timepoints.length ? timepoints[ i ] : -1 )
                        .x( i < xCoords.length ? xCoords[ i ] : Double.NaN )
                        .y( i < yCoords.length ? yCoords[ i ] : Double.NaN )
                        .z( i < positions.length ? positions[ i ][ 0 ] : Double.NaN )
                        .segmentId( i < segmentIds.length ? segmentIds[ i ] : -1 )
                        .build();

                nodes.add( node );
            }
        }
        else if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) ||
                geffVersion.startsWith( "0.4" ) )
        {
            // Read node IDs from chunks
            int[] nodeIds = ZarrUtils.readChunkedIntArray( nodesGroup, "ids", "node IDs" );

            // Read properties
            ZarrGroup propsGroup = nodesGroup.openSubGroup( "props" );

            // Read time points from chunks
            int[] timepoints = ZarrUtils.readChunkedIntArray( propsGroup, "t/values", "timepoints" );

            // Read X coordinates from chunks
            double[] xCoords = ZarrUtils.readChunkedDoubleArray( propsGroup, "x/values", "X coordinates" );

            // Read Y coordinates from chunks
            double[] yCoords = ZarrUtils.readChunkedDoubleArray( propsGroup, "y/values", "Y coordinates" );

            // Read Z coordinates from chunks
            double[] zCoords = new double[ 0 ];
            try
            {
                zCoords = ZarrUtils.readChunkedDoubleArray( propsGroup, "z/values", "Z coordinates" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read Z coordinates: " + e.getMessage() + " skipping..." );
            }

            // Read color from chunks
            double[][] colors = new double[ 0 ][];
            try
            {
                colors = ZarrUtils.readChunkedDoubleMatrix( propsGroup, "color/values", "color" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read color array: " + e.getMessage() + " skipping..." );
            }

            // Read track IDs from chunks
            int[] trackIds = new int[ 0 ];
            try
            {
                trackIds = ZarrUtils.readChunkedIntArray( propsGroup, "track_id/values", "track IDs" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read track IDs: " + e.getMessage() + " skipping..." );
            }

            // Read radius from chunks
            double[] radii = new double[ 0 ];
            try
            {
                radii = ZarrUtils.readChunkedDoubleArray( propsGroup, "radius/values", "radius" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read radius: " + e.getMessage() + " skipping..." );
            }

            // Read covariance2d from chunks
            double[][] covariance2ds = new double[ 0 ][];
            try
            {
                covariance2ds = ZarrUtils.readChunkedDoubleMatrix( propsGroup, "covariance2d/values",
                        "covariance2d" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read covariance2d: " + e.getMessage() + " skipping..." );
            }

            // Read covariance3d from chunks
            double[][] covariance3ds = new double[ 0 ][];
            try
            {
                covariance3ds = ZarrUtils.readChunkedDoubleMatrix( propsGroup, "covariance3d/values",
                        "covariance3d" );
            }
            catch ( Exception e )
            {
                System.out.println( "Warning: Could not read covariance3d: " + e.getMessage() + " skipping..." );
            }

            // Read polygon from chunks
            double[][] polygonsX = new double[ 0 ][];
            double[][] polygonsY = new double[ 0 ][];
            if ( geffVersion.startsWith( "0.4" ) )
            {
                try
                {
                    int[][] polygonSlices = ZarrUtils.readChunkedIntMatrix( propsGroup, "polygon/slices", "polygon slices" );
                    // expected shape: [numVertices, 2]
                    double[][] polygonValues = ZarrUtils.readChunkedDoubleMatrix( propsGroup, "polygon/values", "polygon values" );
                    polygonsX = new double[ polygonSlices.length ][];
                    polygonsY = new double[ polygonSlices.length ][];
                    for ( int i = 0; i < polygonSlices.length; i++ )
                    {
                        int start = polygonSlices[ i ][ 0 ];
                        int length = polygonSlices[ i ][ 1 ];
                        if ( start >= 0 && start + length <= polygonValues.length )
                        {
                            double[] xPoints = new double[ length ];
                            double[] yPoints = new double[ length ];
                            for ( int j = 0; j < length; j++ )
                            {
                                xPoints[ j ] = polygonValues[ start + j ][ 0 ];
                                yPoints[ j ] = polygonValues[ start + j ][ 1 ];
                            }
                            polygonsX[ i ] = xPoints;
                            polygonsY[ i ] = yPoints;
                        }
                        else
                        {
                            System.out.println( "Warning: Invalid polygon slice at index " + i + ", skipping..." );
                        }
                    }
                }
                catch ( Exception e )
                {
                    System.out.println( "Warning: Could not read polygon: " + e.getMessage() + " skipping..." );
                }
            }

            // Create node objects
            for ( int i = 0; i < nodeIds.length; i++ )
            {
                GeffNode node = new Builder()
                        .id( nodeIds[ i ] )
                        .timepoint( i < timepoints.length ? timepoints[ i ] : -1 )
                        .x( i < xCoords.length ? xCoords[ i ] : Double.NaN )
                        .y( i < yCoords.length ? yCoords[ i ] : Double.NaN )
                        .z( i < zCoords.length ? zCoords[ i ] : Double.NaN )
                        .color( i < colors.length ? colors[ i ] : DEFAULT_COLOR )
                        .segmentId( i < trackIds.length ? trackIds[ i ] : -1 )
                        .radius( i < radii.length ? radii[ i ] : Double.NaN )
                        .covariance2d( i < covariance2ds.length ? covariance2ds[ i ] : DEFAULT_COVARIANCE_2D )
                        .covariance3d( i < covariance3ds.length ? covariance3ds[ i ] : DEFAULT_COVARIANCE_3D )
                        .polygonX( i < polygonsX.length ? polygonsX[ i ] : null )
                        .polygonY( i < polygonsY.length ? polygonsY[ i ] : null )
                        .build();

                nodes.add( node );
            }
        }
        else
        {
            throw new IOException( "Unsupported Geff version: " + geffVersion );
        }

        return nodes;
    }

    /**
     * Write nodes to Zarr format with chunked structure
     */
    public static void writeToZarr( List< GeffNode > nodes, String zarrPath ) throws IOException, InvalidRangeException
    {
        writeToZarr( nodes, zarrPath, ZarrUtils.DEFAULT_CHUNK_SIZE );
    }

    public static void writeToZarr( List< GeffNode > nodes, String zarrPath, String geffVersion )
            throws IOException, InvalidRangeException
    {
        if ( geffVersion == null || geffVersion.isEmpty() )
        {
            geffVersion = Geff.VERSION; // Use default version if not specified
        }
        writeToZarr( nodes, zarrPath, ZarrUtils.DEFAULT_CHUNK_SIZE, geffVersion );
    }

    /**
     * Write nodes to Zarr format with specified chunk size
     */
    public static void writeToZarr( List< GeffNode > nodes, String zarrPath, int chunkSize )
            throws IOException, InvalidRangeException
    {
        writeToZarr( nodes, zarrPath, chunkSize, Geff.VERSION );
    }

    public static void writeToZarr( List< GeffNode > nodes, String zarrPath, int chunkSize, String geffVersion )
            throws IOException, InvalidRangeException
    {
        if ( nodes == null )
        { throw new IllegalArgumentException( "Nodes list cannot be null or empty" ); }

        if ( geffVersion == null || geffVersion.isEmpty() )
        {
            geffVersion = Geff.VERSION; // Use default version if not specified
        }

        System.out.println(
                "Writing " + nodes.size() + " nodes to Zarr path: " + zarrPath + " with chunk size: " + chunkSize
                        + " to Geff version: " + geffVersion );

        if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) )
        {
            // Create the main nodes group
            ZarrGroup rootGroup = ZarrGroup.create( zarrPath );

            // Create the main nodes group
            ZarrGroup nodesGroup = rootGroup.createSubGroup( "nodes" );

            // Create attrs subgroup for chunked storage
            ZarrGroup attrsGroup = nodesGroup.createSubGroup( "attrs" );

            // Check if any nodes have 3D positions
            boolean hasPositions = nodes.stream()
                    .anyMatch( node -> node.getPosition() != null && node.getPosition().length >= 3 );

            System.out.println( "Node analysis:" );
            System.out.println( "- Has 3D positions: " + hasPositions );
            System.out.println( "- Format: Chunked arrays with separate values subgroups" );

            // Write node IDs in chunks
            writeChunkedNodeIds( nodes, nodesGroup, chunkSize );

            // Write timepoints in chunks
            ZarrUtils.writeChunkedIntAttribute( nodes, attrsGroup, "t", chunkSize, GeffNode::getT );

            // Write X coordinates in chunks
            ZarrUtils.writeChunkedDoubleAttribute( nodes, attrsGroup, "x", chunkSize, GeffNode::getX );

            // Write Y coordinates in chunks
            ZarrUtils.writeChunkedDoubleAttribute( nodes, attrsGroup, "y", chunkSize, GeffNode::getY );

            // Write segment IDs in chunks
            ZarrUtils.writeChunkedIntAttribute( nodes, attrsGroup, "seg_id", chunkSize, GeffNode::getSegmentId );

            // Write positions if available in chunks
            if ( hasPositions )
            {
                ZarrUtils.writeChunkedDoubleMatrix( nodes, attrsGroup, "position", chunkSize, GeffNode::getPosition, 3 );
            }
        }
        else if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) || geffVersion.startsWith( "0.4" ) )
        {
            // Create the main nodes group
            ZarrGroup rootGroup = ZarrGroup.create( zarrPath );

            // Create the main nodes group
            ZarrGroup nodesGroup = rootGroup.createSubGroup( "nodes" );

            // Create props subgroup for chunked storage
            ZarrGroup propsGroup = nodesGroup.createSubGroup( "props" );

            // Write node IDs in chunks
            writeChunkedNodeIds( nodes, nodesGroup, chunkSize );

            // Write timepoints in chunks
            ZarrUtils.writeChunkedIntAttribute( nodes, propsGroup, "t/values", chunkSize, GeffNode::getT );

            // Write X coordinates in chunks
            ZarrUtils.writeChunkedDoubleAttribute( nodes, propsGroup, "x/values", chunkSize, GeffNode::getX );

            // Write Y coordinates in chunks
            ZarrUtils.writeChunkedDoubleAttribute( nodes, propsGroup, "y/values", chunkSize, GeffNode::getY );

            // Write Z coordinates in chunks
            ZarrUtils.writeChunkedDoubleAttribute( nodes, propsGroup, "z/values", chunkSize, GeffNode::getZ );

            // Write color in chunks
            ZarrUtils.writeChunkedDoubleMatrix( nodes, propsGroup, "color/values", chunkSize, GeffNode::getColor, 4 );

            // Write segment IDs in chunks
            ZarrUtils.writeChunkedIntAttribute( nodes, propsGroup, "track_id/values", chunkSize, GeffNode::getSegmentId );

            // Write radius and covariance attributes if available
            ZarrUtils.writeChunkedDoubleAttribute( nodes, propsGroup, "radius/values", chunkSize, GeffNode::getRadius );

            // Write covariance2d in chunks
            ZarrUtils.writeChunkedDoubleMatrix( nodes, propsGroup, "covariance2d/values", chunkSize, GeffNode::getCovariance2d,
                    4 );

            // Write covariance3d in chunks
            ZarrUtils.writeChunkedDoubleMatrix( nodes, propsGroup, "covariance3d/values", chunkSize, GeffNode::getCovariance3d,
                    6 );

            if ( geffVersion.startsWith( "0.4" ) )
            {
                // Write polygon slices and values if available
                List< GeffSerializableVertex > geffVertices = new ArrayList<>();
                int polygonOffset = 0;
                for ( GeffNode node : nodes )
                {
                    if ( node.polygonX == null || node.polygonY == null )
                        throw new IllegalArgumentException( "Polygon coordinates cannot be null" );
                    if ( node.getPolygonX().length != node.getPolygonY().length )
                        throw new IllegalArgumentException( "Polygon X and Y coordinates must have the same length" );
                    node.setPolygonStartIndex( polygonOffset );
                    for ( int i = 0; i < node.getPolygonX().length; i++ )
                    {
                        geffVertices.add( new GeffSerializableVertex( node.getPolygonX()[ i ],
                                node.getPolygonY()[ i ] ) );
                    }
                    polygonOffset += node.getPolygonX().length;
                }
                ZarrUtils.writeChunkedIntMatrix( nodes, propsGroup, "polygon/slices", chunkSize, GeffNode::getPolygonSliceAsArray, 2 );
                ZarrUtils.writeChunkedDoubleMatrix( geffVertices, propsGroup, "polygon/values", chunkSize, GeffSerializableVertex::getCoordinates, 2 );
            }

        }

        System.out.println( "Successfully wrote nodes to Zarr format with chunked structure" );
    }

    /**
     * Helper method to write chunked node IDs
     */
    private static void writeChunkedNodeIds( List< GeffNode > nodes, ZarrGroup parentGroup, int chunkSize )
            throws IOException, InvalidRangeException
    {

        int totalNodes = nodes.size();

        // Create the ids subgroup
        ZarrGroup idsGroup = parentGroup.createSubGroup( "ids" );

        // Create a single ZarrArray for all IDs with proper chunking
        ZarrArray idsArray = idsGroup.createArray( "", new ArrayParams()
                .shape( totalNodes )
                .chunks( chunkSize )
                .dataType( DataType.i4 ) );

        // Write data in chunks
        int chunkIndex = 0;
        for ( int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize )
        {
            int endIdx = Math.min( startIdx + chunkSize, totalNodes );
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            int[] chunkData = new int[ currentChunkSize ];

            // Fill chunk data array
            for ( int i = 0; i < currentChunkSize; i++ )
            {
                chunkData[ i ] = nodes.get( startIdx + i ).getId();
            }

            // Write chunk at specific offset
            idsArray.write( chunkData, new int[] { currentChunkSize }, new int[] { startIdx } );

            System.out.println( "- Wrote node IDs chunk " + chunkIndex + ": " + currentChunkSize + " nodes (indices "
                    + startIdx + "-" + ( endIdx - 1 ) + ")" );
            chunkIndex++;
        }
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
        result = 31 * result + ( color != null ? java.util.Arrays.hashCode( color ) : 0 );
        result = 31 * result + segmentId;
        result = 31 * result + Double.hashCode( radius );
        result = 31 * result + ( covariance2d != null ? java.util.Arrays.hashCode( covariance2d ) : 0 );
        result = 31 * result + ( covariance3d != null ? java.util.Arrays.hashCode( covariance3d ) : 0 );
        return result;
    }
}
