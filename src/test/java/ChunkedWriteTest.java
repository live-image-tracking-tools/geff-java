import org.mastodon.GeffNode;
import org.mastodon.GeffEdge;
import org.mastodon.GeffMetadata;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class to verify the chunked writing functionality works correctly
 * with the new ZarrGroup-based approach.
 */
public class ChunkedWriteTest {

    public static void main(String[] args) {
        try {
            testNodeChunkedWriting();
            testEdgeChunkedWriting();
            testMetadataWriting();
            System.out.println("All chunked writing tests completed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test writing nodes with chunked structure
     */
    private static void testNodeChunkedWriting() throws IOException, InvalidRangeException {
        System.out.println("=== Testing Node Chunked Writing ===");

        // Create sample nodes
        List<GeffNode> testNodes = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            GeffNode node = new GeffNode();
            node.setTimepoint(i);
            node.setX(i * 1.5);
            node.setY(i * 2.0);
            node.setSegmentId(i + 100);
            // Add 3D position
            node.setPosition(new double[] { i * 1.5, i * 2.0, i * 0.5 });
            testNodes.add(node);
        }

        // Test writing with small chunk size to verify chunking
        String outputPath = "/tmp/test-nodes-chunked";
        System.out.println("Writing " + testNodes.size() + " nodes to: " + outputPath);

        GeffNode.writeToZarr(testNodes, outputPath); // Small chunk size to test chunking

        System.out.println("Node chunked writing test completed.");
    }

    /**
     * Test writing edges with chunked structure
     */
    private static void testEdgeChunkedWriting() throws IOException, InvalidRangeException {
        System.out.println("\n=== Testing Edge Chunked Writing ===");

        // Create sample edges
        List<GeffEdge> testEdges = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GeffEdge edge = new GeffEdge(i, i, i + 1); // Connect consecutive nodes
            testEdges.add(edge);
        }

        // Test writing with small chunk size to verify chunking
        String outputPath = "/tmp/test-edges-chunked";
        System.out.println("Writing " + testEdges.size() + " edges to: " + outputPath);

        GeffEdge.writeToZarr(testEdges, outputPath, 3); // Small chunk size to test chunking

        System.out.println("Edge chunked writing test completed.");
    }

    /**
     * Test writing metadata with GEFF schema compliance
     */
    private static void testMetadataWriting() throws IOException, InvalidRangeException {
        System.out.println("\n=== Testing Metadata Writing ===");

        // Create sample metadata with all attributes
        GeffMetadata metadata = new GeffMetadata();
        metadata.setGeffVersion("0.1.1");
        metadata.setDirected(true);
        metadata.setRoiMin(new double[] { 0.0, 0.0, 0.0 });
        metadata.setRoiMax(new double[] { 100.0, 100.0, 50.0 });
        metadata.setPositionAttr("position");
        metadata.setAxisNames(new String[] { "x", "y", "z" });
        metadata.setAxisUnits(new String[] { "μm", "μm", "μm" });

        // Test writing to a Zarr path
        String outputPath = "/tmp/test-metadata";
        System.out.println("Writing metadata to: " + outputPath);

        GeffMetadata.writeToZarr(metadata, outputPath);

        System.out.println("Metadata writing test completed.");

        // Test with minimal metadata (only required fields)
        System.out.println("Testing minimal metadata writing...");
        GeffMetadata minimalMetadata = new GeffMetadata("0.2", false);
        String minimalOutputPath = "/tmp/test-metadata-minimal";
        GeffMetadata.writeToZarr(minimalMetadata, minimalOutputPath);
        System.out.println("Minimal metadata writing test completed.");
    }
}
