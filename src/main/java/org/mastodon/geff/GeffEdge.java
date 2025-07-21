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

import com.bc.zarr.ArrayParams;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

/**
 * Represents an edge in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing edge data from/to Zarr format.
 * An edge connects two nodes in a tracking graph, typically representing
 * temporal connections between objects across time points.
 */
public class GeffEdge {

    // Edge attributes
    private int sourceNodeId;
    private int targetNodeId;
    private int id; // Edge ID if available

    /**
     * Default constructor
     */
    public GeffEdge() {
    }

    /**
     * Constructor with source and target node IDs
     */
    public GeffEdge(int sourceNodeId, int targetNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.id = -1; // Default to -1 if no specific ID is assigned
    }

    /**
     * Constructor with edge ID, source and target node IDs
     */
    public GeffEdge(int id, int sourceNodeId, int targetNodeId) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(int sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public int getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(int targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    /**
     * Read edges from a Zarr group
     */
    public static List<GeffEdge> readFromZarr(String zarrPath) throws IOException, InvalidRangeException {
        List<GeffEdge> edges = new ArrayList<>();

        ZarrGroup edgesGroup = ZarrGroup.open(zarrPath + "/edges");

        // Read edge IDs - this appears to be a 2D array where each row represents an
        // edge
        // with [source_node_id, target_node_id] format
        ZarrArray idsArray = edgesGroup.openArray("ids");

        // The edges/ids array contains pairs of node IDs representing connections
        Object idsData = idsArray.read();

        if (idsData instanceof int[][]) {
            // 2D array case: each row is [source, target]
            int[][] edgeIds = (int[][]) idsData;
            for (int i = 0; i < edgeIds.length; i++) {
                if (edgeIds[i].length >= 2) {
                    GeffEdge edge = new GeffEdge(i, edgeIds[i][0], edgeIds[i][1]);
                    edges.add(edge);
                }
            }
        } else if (idsData instanceof int[]) {
            // 1D array case: assume pairs of values [source1, target1, source2, target2,
            // ...]
            int[] edgeIds = (int[]) idsData;
            for (int i = 0; i < edgeIds.length - 1; i += 2) {
                GeffEdge edge = new GeffEdge(i / 2, edgeIds[i], edgeIds[i + 1]);
                edges.add(edge);
            }
        } else {
            System.err.println("Warning: Unexpected edge IDs data type: " + idsData.getClass().getName());
        }

        return edges;
    }

    /**
     * Alternative method to read edges with different chunk handling
     */
    public static List<GeffEdge> readFromZarrWithChunks(String zarrPath) throws IOException, InvalidRangeException {
        List<GeffEdge> edges = new ArrayList<>();

        ZarrGroup edgesGroup = ZarrGroup.open(zarrPath + "/edges");
        ZarrArray idsArray = edgesGroup.openArray("ids");

        // Check the array keys to see what chunks are available
        String[] arrayKeys = edgesGroup.getArrayKeys().toArray(new String[0]);
        System.out.println("Available arrays in edges group: " + String.join(", ", arrayKeys));

        // Try to read each chunk separately if the data is chunked
        try {
            // First, try reading the entire array
            Object idsData = idsArray.read();
            return parseEdgeData(idsData);
        } catch (Exception e) {
            System.err.println("Could not read entire edges array, trying chunk-based approach: " + e.getMessage());

            // If that fails, try reading individual chunks
            // This is a fallback approach for cases where the array is stored in chunks
            List<String> chunkKeys = new ArrayList<>();

            // Look for numeric chunk keys (0.0, 1.0, etc.)
            for (String key : arrayKeys) {
                try {
                    Double.parseDouble(key);
                    chunkKeys.add(key);
                } catch (NumberFormatException nfe) {
                    // Not a numeric chunk key, skip
                }
            }

            int edgeId = 0;
            for (String chunkKey : chunkKeys) {
                try {
                    ZarrArray chunkArray = edgesGroup.openArray("ids/" + chunkKey);
                    Object chunkData = chunkArray.read();
                    List<GeffEdge> chunkEdges = parseEdgeData(chunkData);

                    // Assign sequential IDs
                    for (GeffEdge edge : chunkEdges) {
                        edge.setId(edgeId++);
                        edges.add(edge);
                    }
                } catch (Exception chunkException) {
                    System.err.println("Could not read chunk " + chunkKey + ": " + chunkException.getMessage());
                }
            }
        }

        return edges;
    }

    /**
     * Helper method to parse edge data from various formats
     */
    private static List<GeffEdge> parseEdgeData(Object idsData) {
        List<GeffEdge> edges = new ArrayList<>();

        if (idsData instanceof int[][]) {
            // 2D array case: each row is [source, target]
            int[][] edgeIds = (int[][]) idsData;
            for (int i = 0; i < edgeIds.length; i++) {
                if (edgeIds[i].length >= 2) {
                    GeffEdge edge = new GeffEdge(i, edgeIds[i][0], edgeIds[i][1]);
                    edges.add(edge);
                }
            }
        } else if (idsData instanceof int[]) {
            // 1D array case: assume pairs of values [source1, target1, source2, target2,
            // ...]
            int[] edgeIds = (int[]) idsData;
            for (int i = 0; i < edgeIds.length - 1; i += 2) {
                GeffEdge edge = new GeffEdge(i / 2, edgeIds[i], edgeIds[i + 1]);
                edges.add(edge);
            }
        } else {
            System.err.println("Warning: Unexpected edge IDs data type: " + idsData.getClass().getName());
        }

        return edges;
    }

    public static int getChunkSize(String zarrPath) throws IOException, InvalidRangeException {
        try {
            ZarrGroup group = ZarrGroup.open(zarrPath + "/edges");
            return group.openArray("ids").getChunks()[0];
        } catch (IOException e) {
            // If the path doesn't exist, return a default chunk size
            System.out.println("Path doesn't exist, using default chunk size: " + e.getMessage());
            return 1000; // Default chunk size
        }
    }

    /**
     * Write edges to Zarr format with chunked structure
     */
    public static void writeToZarr(List<GeffEdge> edges, String zarrPath) throws IOException, InvalidRangeException {
        writeToZarr(edges, zarrPath, 1000); // Default chunk size
    }

    /**
     * Write edges to Zarr format with specified chunk size
     */
    public static void writeToZarr(List<GeffEdge> edges, String zarrPath, int chunks)
            throws IOException, InvalidRangeException {
        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("Edges list cannot be null or empty");
        }

        System.out.println(
                "Writing " + edges.size() + " edges to Zarr path: " + zarrPath + " with chunk size: " + chunks);

        // Create the main edges group
        ZarrGroup edgesGroup = ZarrGroup.create(zarrPath);

        // Analyze edge data format
        long validEdges = edges.stream().filter(GeffEdge::isValid).count();
        long selfLoops = edges.stream().filter(GeffEdge::isSelfLoop).count();

        System.out.println("Edge analysis:");
        System.out.println("- Valid edges: " + validEdges + "/" + edges.size());
        if (selfLoops > 0) {
            System.out.println("- Self-loops detected: " + selfLoops);
        }
        System.out.println("- Format: Chunked 2D arrays [[source1, target1], [source2, target2], ...]");

        // Write edges in chunks
        int totalEdges = edges.size();

        // Create ids subgroup
        ZarrGroup idsGroup = edgesGroup.createSubGroup("ids");

        // Create a single ZarrArray for all edges with proper chunking
        ZarrArray edgesArray = idsGroup.createArray("", new ArrayParams()
                .shape(totalEdges, 2)
                .chunks(chunks, 2)
                .dataType(DataType.i4));

        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalEdges; startIdx += chunks) {
            int endIdx = Math.min(startIdx + chunks, totalEdges);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            int[] chunkData = new int[currentChunkSize * 2]; // Flattened pairs for this chunk

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                GeffEdge edge = edges.get(startIdx + i);
                chunkData[i * 2] = edge.getSourceNodeId(); // Source node ID
                chunkData[i * 2 + 1] = edge.getTargetNodeId(); // Target node ID
            }

            // Write chunk at specific offset
            edgesArray.write(chunkData, new int[] { currentChunkSize, 2 }, new int[] { startIdx, 0 });

            String chunkKey = String.format("%.1f", (double) chunkIndex);
            System.out.println("- Wrote chunk " + chunkKey + ": " + currentChunkSize + " edges (indices " + startIdx
                    + "-" + (endIdx - 1) + ")");
            chunkIndex++;
        }

        // Log summary
        int uniqueSourceNodes = (int) edges.stream().mapToInt(GeffEdge::getSourceNodeId).distinct().count();
        int uniqueTargetNodes = (int) edges.stream().mapToInt(GeffEdge::getTargetNodeId).distinct().count();

        System.out.println("Successfully wrote edges to Zarr format:");
        System.out.println("- " + totalEdges + " edges written in " + chunkIndex + " chunks");
        System.out.println("- Source nodes: " + uniqueSourceNodes + " unique");
        System.out.println("- Target nodes: " + uniqueTargetNodes + " unique");

        // Sample verification
        if (!edges.isEmpty()) {
            System.out.println("Sample written edge data:");
            for (int i = 0; i < Math.min(3, edges.size()); i++) {
                GeffEdge edge = edges.get(i);
                System.out.println("  [" + edge.getSourceNodeId() + ", " + edge.getTargetNodeId() + "] - " + edge);
            }
        }
    }

    /**
     * Check if this edge is valid (has valid source and target node IDs)
     */
    public boolean isValid() {
        return sourceNodeId >= 0 && targetNodeId >= 0;
    }

    /**
     * Check if this edge represents a self-loop (source == target)
     */
    public boolean isSelfLoop() {
        return sourceNodeId == targetNodeId;
    }

    @Override
    public String toString() {
        return String.format("GeffEdge{id=%d, source=%d, target=%d}",
                id, sourceNodeId, targetNodeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        GeffEdge geffEdge = (GeffEdge) obj;
        return sourceNodeId == geffEdge.sourceNodeId &&
                targetNodeId == geffEdge.targetNodeId &&
                id == geffEdge.id;
    }

    @Override
    public int hashCode() {
        int result = sourceNodeId;
        result = 31 * result + targetNodeId;
        result = 31 * result + id;
        return result;
    }
}
