package org.mastodon.geff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

public class Geff
{
    // This class serves as a placeholder for the Geff package.
    // It can be used to define package-level constants or utility methods in
    // the
    // future.
    private List< GeffNode > nodes = new ArrayList<>();

    private List< GeffEdge > edges = new ArrayList<>();

    private GeffMetadata metadata;

    public static final String VERSION = "0.3.0"; // Example version constant

    private Geff( List< GeffNode > nodes, List< GeffEdge > edges, GeffMetadata metadata )
    {
        this.nodes = nodes;
        this.edges = edges;
        this.metadata = metadata;
    }

    public static void main( String[] args )
    {
        System.out.println( "Geff library version: " + VERSION );

        String zarrPath = "src/test/resources/mouse-20250719.zarr/tracks";
        String outputZarrPath = "src/test/resources/mouse-20250719_output.zarr/tracks";

        try
        {
            // Demonstrate reading metadata
            System.out.println( "\n=== Reading Metadata ===" );
            GeffMetadata metadata = GeffMetadata.readFromZarr( zarrPath );
            System.out.println( "Metadata loaded:" + metadata );

            // Demonstrate reading nodes
            System.out.println( "\n=== Reading Nodes ===" );
            List< GeffNode > nodes = GeffNode.readFromZarr( zarrPath, metadata.getGeffVersion() );
            System.out.println( "Read " + nodes.size() + " nodes:" );
            for ( int i = 0; i < Math.min( 5, nodes.size() ); i++ )
            {
                System.out.println( "  " + nodes.get( i ) );
            }
            if ( nodes.size() > 5 )
            {
                System.out.println( "  ... and " + ( nodes.size() - 5 ) + " more nodes" );
            }

            // Demonstrate reading edges
            System.out.println( "\n=== Reading Edges ===" );
            List< GeffEdge > edges = GeffEdge.readFromZarr( zarrPath, metadata.getGeffVersion() );
            System.out.println( "Read " + edges.size() + " edges:" );
            for ( int i = 0; i < Math.min( 5, edges.size() ); i++ )
            {
                System.out.println( "  " + edges.get( i ) );
            }
            if ( edges.size() > 5 )
            {
                System.out.println( "  ... and " + ( edges.size() - 5 ) + " more edges" );
            }

            // Try to write nodes (will show what would be written)
            try
            {
                GeffNode.writeToZarr( nodes, outputZarrPath, ZarrUtils.getChunkSize( zarrPath ) );
            }
            catch ( UnsupportedOperationException e )
            {
                System.out.println( "Note: " + e.getMessage() );
            }
            catch ( InvalidRangeException e )
            {
                System.err.println( "InvalidRangeException during node writing: " + e.getMessage() );
            }

            // Try to write edges (will show what would be written)
            try
            {
                GeffEdge.writeToZarr( edges, outputZarrPath, ZarrUtils.getChunkSize( zarrPath ) );
            }
            catch ( UnsupportedOperationException e )
            {
                System.out.println( "Note: " + e.getMessage() );
            }

            // Try to write edges (will show what would be written)
            try
            {
                GeffMetadata.writeToZarr( metadata, outputZarrPath );
            }
            catch ( UnsupportedOperationException e )
            {
                System.out.println( "Note: " + e.getMessage() );
            }

            // Create a Geff object with the loaded data
            Geff geff = new Geff( nodes, edges, metadata );
            System.out.println( "\n=== Geff Object Created ===" );
            System.out.println( "Geff object contains " + geff.getNodes().size() + " nodes and " + geff.getEdges().size()
                    + " edges" );

        }
        catch ( IOException e )
        {
            System.err.println( "IOException occurred: " + e.getMessage() );
            e.printStackTrace();
        }
        catch ( InvalidRangeException e )
        {
            System.err.println( "InvalidRangeException occurred: " + e.getMessage() );
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            System.err.println( "Unexpected exception occurred: " + e.getMessage() );
            e.printStackTrace();
        }

        // Also demonstrate the original Zarr exploration code
        System.out.println( "\n=== Original Zarr Exploration ===" );
        try
        {
            final ZarrGroup zarrTracks = ZarrGroup.open( zarrPath );
            final Iterator< String > groupKeyIter = zarrTracks.getGroupKeys().iterator();
            while ( groupKeyIter.hasNext() )
            {
                String groupKey = groupKeyIter.next();
                System.out.println( "Found group: " + groupKey );
            }
            final Iterator< String > arrayKeyIter = zarrTracks.getArrayKeys().iterator();
            while ( arrayKeyIter.hasNext() )
            {
                String arrayKey = arrayKeyIter.next();
                System.out.println( "Found array: " + arrayKey );
            }
            final Iterator< String > attrKeyIter = zarrTracks.getAttributes().keySet().iterator();
            while ( attrKeyIter.hasNext() )
            {
                String attrKey = attrKeyIter.next();
                System.out.print( "Found attribute: " + attrKey );
                Object attrValue = zarrTracks.getAttributes().get( attrKey );
                System.out.println( "  Value: " + attrValue );
            }
            // Example of opening an array
            System.out.println( "Opening 'nodes/ids' array..." );
            ZarrArray nodesIds = zarrTracks.openArray( "nodes/ids" );
            double[] nodesIdsData = ( double[] ) nodesIds.read();
            System.out.println( "Read nodes/ids data: " + nodesIdsData.length + " elements." );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        catch ( InvalidRangeException e )
        {
            e.printStackTrace();
        }
    }

    // Getter methods for nodes, edges, and metadata
    public List< GeffNode > getNodes()
    {
        return new ArrayList<>( nodes );
    }

    public List< GeffEdge > getEdges()
    {
        return new ArrayList<>( edges );
    }

    public GeffMetadata getMetadata()
    {
        return metadata;
    }
}
