package org.mastodon.geff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bc.zarr.ArrayParams;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

/**
 * Represents an edge in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing edge data from/to Zarr format. An edge
 * connects two nodes in a tracking graph, typically representing temporal
 * connections between objects across time points.
 */
public class GeffEdge implements ZarrEntity
{

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
    public static List< GeffEdge > readFromZarr( String zarrPath ) throws IOException, InvalidRangeException
    {
        return readFromZarr( zarrPath, Geff.VERSION );
    }

    public static List< GeffEdge > readFromZarr( String zarrPath, String geffVersion )
            throws IOException, InvalidRangeException
    {
        return readFromZarrWithChunks( zarrPath, geffVersion );
    }

    /**
     * Alternative method to read edges with different chunk handling
     */
    public static List< GeffEdge > readFromZarrWithChunks( String zarrPath, String geffVersion )
            throws IOException, InvalidRangeException
    {
        List< GeffEdge > edges = new ArrayList<>();

        ZarrGroup edgesGroup = ZarrGroup.open( zarrPath + "/edges" );

        System.out.println(
                "Reading edges from Zarr path: " + zarrPath + " with Geff version: " + geffVersion );

        if ( geffVersion.startsWith( "0.1" ) )
        {

            int[][] edgeIds = ZarrUtils.readChunkedIntMatrix( edgesGroup, "ids", "edge IDs" );

            double[] distances = new double[ 0 ];
            double[] scores = new double[ 0 ];

            if ( edgesGroup.getGroupKeys().contains( "attrs" ) )
            {

                // Read attributes
                ZarrGroup attrsGroup = edgesGroup.openSubGroup( "attrs" );

                // Read distances from chunks
                try
                {
                    distances = ZarrUtils.readChunkedDoubleArray( attrsGroup, "distance/values", "distances" );
                }
                catch ( Exception e )
                {
                    System.out.println( "Warning: Could not read distances: " + e.getMessage() + " skipping..." );
                }

                // Read scores from chunks
                try
                {
                    scores = ZarrUtils.readChunkedDoubleArray( attrsGroup, "score/values", "scores" );
                }
                catch ( Exception e )
                {
                    System.out.println( "Warning: Could not read scores: " + e.getMessage() + " skipping..." );
                }
            }

            // 2D array case: each row is [source, target]
            for ( int i = 0; i < edgeIds.length; i++ )
            {
                if ( edgeIds[ i ].length == 2 )
                {
                    GeffEdge edge = GeffEdge.builder()
                            .setId( i )
                            .setSourceNodeId( edgeIds[ i ][ 0 ] )
                            .setTargetNodeId( edgeIds[ i ][ 1 ] )
                            .setDistance( i < distances.length ? distances[ i ] : DEFAULT_DISTANCE )
                            .setScore( i < scores.length ? scores[ i ] : DEFAULT_SCORE )
                            .build();
                    edges.add( edge );
                }
                else
                {
                    System.err.println( "Unexpected edge format at index " + i + ": " + edgeIds[ i ].length
                            + " elements. Expected 2 (source, target)." );
                }
            }
        }
        else if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) )
        {

            int[][] edgeIds = ZarrUtils.readChunkedIntMatrix( edgesGroup, "ids", "edge IDs" );

            double[] distances = new double[ 0 ];
            double[] scores = new double[ 0 ];

            // Read attributes
            if ( edgesGroup.getGroupKeys().contains( "props" ) )
            {
                ZarrGroup propsGroup = edgesGroup.openSubGroup( "props" );

                // Read distances from chunks
                try
                {
                    distances = ZarrUtils.readChunkedDoubleArray( propsGroup, "distance/values", "distances" );
                }
                catch ( Exception e )
                {
                    System.out.println( "Warning: Could not read distances: " + e.getMessage() + " skipping..." );
                }

                // Read scores from chunks
                try
                {
                    scores = ZarrUtils.readChunkedDoubleArray( propsGroup, "score/values", "scores" );
                }
                catch ( Exception e )
                {
                    System.out.println( "Warning: Could not read scores: " + e.getMessage() + " skipping..." );
                }
            }

            // 2D array case: each row is [source, target]
            for ( int i = 0; i < edgeIds.length; i++ )
            {
                if ( edgeIds[ i ].length == 2 )
                {
                    GeffEdge edge = GeffEdge.builder()
                            .setId( i )
                            .setSourceNodeId( edgeIds[ i ][ 0 ] )
                            .setTargetNodeId( edgeIds[ i ][ 1 ] )
                            .setDistance( i < distances.length ? distances[ i ] : DEFAULT_DISTANCE )
                            .setScore( i < scores.length ? scores[ i ] : DEFAULT_SCORE )
                            .build();
                    edges.add( edge );
                }
                else
                {
                    System.err.println( "Unexpected edge format at index " + i + ": " + edgeIds[ i ].length
                            + " elements. Expected 2 (source, target)." );
                }
            }
        }
        else
        {
            throw new UnsupportedOperationException( "Unsupported Geff version: " + geffVersion );
        }

        return edges;
    }

    /**
     * Write edges to Zarr format with chunked structure
     */
    public static void writeToZarr( List< GeffEdge > edges, String zarrPath ) throws IOException, InvalidRangeException
    {
        writeToZarr( edges, zarrPath, ZarrUtils.DEFAULT_CHUNK_SIZE ); // Default
                                                                      // chunk
                                                                      // size
    }

    public static void writeToZarr( List< GeffEdge > edges, String zarrPath, String geffVersion )
            throws IOException, InvalidRangeException
    {
        writeToZarr( edges, zarrPath, ZarrUtils.DEFAULT_CHUNK_SIZE, geffVersion ); // Default
                                                                                   // chunk
                                                                                   // size
    }

    /**
     * Write edges to Zarr format with specified chunk size
     */
    public static void writeToZarr( List< GeffEdge > edges, String zarrPath, int chunks )
            throws IOException, InvalidRangeException
    {
        writeToZarr( edges, zarrPath, chunks, Geff.VERSION ); // Default Geff
                                                              // version
    }

    public static void writeToZarr( List< GeffEdge > edges, String zarrPath, int chunks, String geffVersion )
            throws IOException, InvalidRangeException
    {
        if ( edges == null )
        { throw new IllegalArgumentException( "Edges list cannot be null or empty" ); }

        if ( geffVersion == null || geffVersion.isEmpty() )
        {
            geffVersion = Geff.VERSION; // Use default version if not specified
        }

        System.out.println(
                "Writing " + edges.size() + " edges to Zarr path: " + zarrPath + " with chunk size: " + chunks );

        if ( geffVersion.startsWith( "0.1" ) )
        {
            // Create attrs subgroup for 0.1 versions

            // Create the main edges group
            ZarrGroup rootGroup = ZarrGroup.create( zarrPath );

            ZarrGroup edgesGroup = rootGroup.createSubGroup( "edges" );

            writeChunkedEdgeIds( edgesGroup, edges, chunks );

            ZarrGroup attrsGroup = edgesGroup.createSubGroup( "attrs" );

            // Write distances
            ZarrUtils.writeChunkedDoubleAttribute( edges, attrsGroup, "distance", chunks, GeffEdge::getDistance );

            // Write scores
            ZarrUtils.writeChunkedDoubleAttribute( edges, attrsGroup, "score", chunks, GeffEdge::getScore );
        }
        else if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) )
        {
            // Create props subgroup for 0.3 version

            // Create the main edges group
            ZarrGroup edgesGroup = ZarrGroup.create( zarrPath );

            writeChunkedEdgeIds( edgesGroup, edges, chunks );

            ZarrGroup propsGroup = edgesGroup.createSubGroup( "props" );

            // Write distances
            ZarrUtils.writeChunkedDoubleAttribute( edges, propsGroup, "distance", chunks, GeffEdge::getDistance );

            // Write scores
            ZarrUtils.writeChunkedDoubleAttribute( edges, propsGroup, "score", chunks, GeffEdge::getScore );
        }
        else
        {
            throw new UnsupportedOperationException( "Unsupported Geff version: " + geffVersion );
        }
    }

    private static void writeChunkedEdgeIds( ZarrGroup edgesGroup, List< GeffEdge > edges, int chunks )
            throws InvalidRangeException, IOException
    {
        // Write edges in chunks
        int totalEdges = edges.size();

        // Create ids subgroup
        ZarrGroup idsGroup = edgesGroup.createSubGroup( "ids" );

        // Create a single ZarrArray for all edges with proper chunking
        ZarrArray edgesArray = idsGroup.createArray( "", new ArrayParams()
                .shape( totalEdges, 2 )
                .chunks( chunks, 2 )
                .dataType( DataType.i4 ) );

        int chunkIndex = 0;
        for ( int startIdx = 0; startIdx < totalEdges; startIdx += chunks )
        {
            int endIdx = Math.min( startIdx + chunks, totalEdges );
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            int[] chunkData = new int[ currentChunkSize * 2 ]; // Flattened
                                                               // pairs for this
                                                               // chunk

            // Fill chunk data array
            for ( int i = 0; i < currentChunkSize; i++ )
            {
                GeffEdge edge = edges.get( startIdx + i );
                chunkData[ i * 2 ] = edge.getSourceNodeId(); // Source node ID
                chunkData[ i * 2 + 1 ] = edge.getTargetNodeId(); // Target node
                                                                 // ID
            }

            // Write chunk at specific offset
            edgesArray.write( chunkData, new int[] { currentChunkSize, 2 }, new int[] { startIdx, 0 } );

            String chunkKey = String.format( "%.1f", ( double ) chunkIndex );
            System.out.println( "- Wrote chunk " + chunkKey + ": " + currentChunkSize + " edges (indices " + startIdx
                    + "-" + ( endIdx - 1 ) + ")" );
            chunkIndex++;
        }

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
        System.out.println( "- " + totalEdges + " edges written in " + chunkIndex + " chunks" );
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
                Double.compare( geffEdge.score, score ) == 0;
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
