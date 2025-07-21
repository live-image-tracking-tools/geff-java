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
 * Represents a node in the Geff (Graph Exchange Format for Features) format.
 * This class handles reading and writing node data from/to Zarr format.
 */
public class GeffNode {

    // Node attributes
    private int id;
    private int timepoint;
    private double x;
    private double y;
    private int segmentId;
    private double[] position; // 3D position array if available

    /**
     * Default constructor
     */
    public GeffNode() {
    }

    /**
     * Constructor with basic node parameters
     */
    public GeffNode(int id, int timepoint, double x, double y, int segmentId) {
        this.id = id;
        this.timepoint = timepoint;
        this.x = x;
        this.y = y;
        this.segmentId = segmentId;
        this.position = new double[] { x, y, 0.0 }; // Default Z to 0
    }

    /**
     * Constructor with 3D position
     */
    public GeffNode(int id, int timepoint, double x, double y, double z, int segmentId) {
        this.id = id;
        this.timepoint = timepoint;
        this.x = x;
        this.y = y;
        this.segmentId = segmentId;
        this.position = new double[] { x, y, z };
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(int timepoint) {
        this.timepoint = timepoint;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        updatePosition();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        updatePosition();
    }

    public double getZ() {
        return position != null && position.length > 2 ? position[2] : 0.0;
    }

    public void setZ(double z) {
        if (position == null) {
            position = new double[] { x, y, z };
        } else if (position.length >= 3) {
            position[2] = z;
        } else {
            // Extend array to include Z
            double[] newPosition = new double[3];
            System.arraycopy(position, 0, newPosition, 0, position.length);
            newPosition[2] = z;
            position = newPosition;
        }
    }

    public int getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(int segmentId) {
        this.segmentId = segmentId;
    }

    public double[] getPosition() {
        return position != null ? position.clone() : new double[] { x, y, 0.0 };
    }

    public void setPosition(double[] position) {
        this.position = position != null ? position.clone() : null;
        if (position != null && position.length >= 2) {
            this.x = position[0];
            this.y = position[1];
        }
    }

    private void updatePosition() {
        if (position != null && position.length >= 2) {
            position[0] = x;
            position[1] = y;
        }
    }

    /**
     * Read nodes from Zarr format with chunked structure
     */
    public static List<GeffNode> readFromZarr(String zarrPath) throws IOException, InvalidRangeException {
        return readFromZarrWithChunks(zarrPath);
    }

    /**
     * Read nodes from Zarr format with chunk handling
     */
    public static List<GeffNode> readFromZarrWithChunks(String zarrPath) throws IOException, InvalidRangeException {
        List<GeffNode> nodes = new ArrayList<>();

        ZarrGroup nodesGroup = ZarrGroup.open(zarrPath + "/nodes");

        // Read node IDs from chunks
        int[] nodeIds = readChunkedIntArray(nodesGroup, "ids", "node IDs");

        // Read attributes
        ZarrGroup attrsGroup = nodesGroup.openSubGroup("attrs");

        // Read time points from chunks
        int[] timepoints = readChunkedIntArray(attrsGroup, "t/values", "timepoints");

        // Read X coordinates from chunks
        double[] xCoords = readChunkedDoubleArray(attrsGroup, "x/values", "X coordinates");

        // Read Y coordinates from chunks
        double[] yCoords = readChunkedDoubleArray(attrsGroup, "y/values", "Y coordinates");

        // Read segment IDs from chunks
        int[] segmentIds = readChunkedIntArray(attrsGroup, "seg_id/values", "segment IDs");

        // Read positions if available from chunks
        double[][] positions = null;
        try {
            positions = readChunkedDoubleMatrix(attrsGroup, "position/values");
        } catch (Exception e) {
            // Position array might not exist or be in different format
            System.out.println("Warning: Could not read position array: " + e.getMessage());
        }

        // Create node objects
        for (int i = 0; i < nodeIds.length; i++) {
            GeffNode node = new GeffNode();
            node.setId(nodeIds[i]);

            if (i < timepoints.length)
                node.setTimepoint(timepoints[i]);
            if (i < xCoords.length)
                node.setX(xCoords[i]);
            if (i < yCoords.length)
                node.setY(yCoords[i]);
            if (i < segmentIds.length)
                node.setSegmentId(segmentIds[i]);

            if (positions != null && i < positions.length) {
                node.setPosition(positions[i]);
            }

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * Helper method to read chunked int arrays
     */
    private static int[] readChunkedIntArray(ZarrGroup group, String arrayPath, String description)
            throws IOException, InvalidRangeException {
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToIntArray(data, description);
        } catch (Exception e) {
            System.out.println("Attempting chunked reading for " + description + ": " + e.getMessage());

            // Try reading individual chunks if whole array reading fails
            List<Integer> allData = new ArrayList<>();

            // Look for numeric chunk keys (0, 1, 2, etc.)
            ZarrGroup arrayGroup = group.openSubGroup(arrayPath);
            String[] chunkKeys = arrayGroup.getArrayKeys().toArray(new String[0]);

            for (String chunkKey : chunkKeys) {
                try {
                    if (chunkKey.matches("\\d+(\\.\\d+)?")) { // numeric chunk key
                        ZarrArray chunkArray = arrayGroup.openArray(chunkKey);
                        Object chunkData = chunkArray.read();
                        int[] chunkValues = convertToIntArray(chunkData, description + " chunk " + chunkKey);
                        for (int value : chunkValues) {
                            allData.add(value);
                        }
                        System.out
                                .println("Read chunk " + chunkKey + " with " + chunkValues.length + " " + description);
                    }
                } catch (Exception chunkException) {
                    System.err.println("Could not read chunk " + chunkKey + " for " + description + ": "
                            + chunkException.getMessage());
                }
            }

            return allData.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /**
     * Helper method to read chunked double arrays
     */
    private static double[] readChunkedDoubleArray(ZarrGroup group, String arrayPath, String description)
            throws IOException, InvalidRangeException {
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToDoubleArray(data, description);
        } catch (Exception e) {
            System.out.println("Attempting chunked reading for " + description + ": " + e.getMessage());

            // Try reading individual chunks if whole array reading fails
            List<Double> allData = new ArrayList<>();

            // Look for numeric chunk keys (0, 1, 2, etc.)
            ZarrGroup arrayGroup = group.openSubGroup(arrayPath);
            String[] chunkKeys = arrayGroup.getArrayKeys().toArray(new String[0]);

            for (String chunkKey : chunkKeys) {
                try {
                    if (chunkKey.matches("\\d+(\\.\\d+)?")) { // numeric chunk key
                        ZarrArray chunkArray = arrayGroup.openArray(chunkKey);
                        Object chunkData = chunkArray.read();
                        double[] chunkValues = convertToDoubleArray(chunkData, description + " chunk " + chunkKey);
                        for (double value : chunkValues) {
                            allData.add(value);
                        }
                        System.out
                                .println("Read chunk " + chunkKey + " with " + chunkValues.length + " " + description);
                    }
                } catch (Exception chunkException) {
                    System.err.println("Could not read chunk " + chunkKey + " for " + description + ": "
                            + chunkException.getMessage());
                }
            }

            return allData.stream().mapToDouble(Double::doubleValue).toArray();
        }
    }

    /**
     * Helper method to read chunked double matrix
     */
    private static double[][] readChunkedDoubleMatrix(ZarrGroup group, String arrayPath)
            throws IOException, InvalidRangeException {
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToDoubleMatrix(data);
        } catch (Exception e) {
            System.out.println("Attempting chunked reading for position matrix: " + e.getMessage());

            // Try reading individual chunks if whole array reading fails
            List<double[]> allData = new ArrayList<>();

            // Look for numeric chunk keys (0, 1, 2, etc.)
            ZarrGroup arrayGroup = group.openSubGroup(arrayPath);
            String[] chunkKeys = arrayGroup.getArrayKeys().toArray(new String[0]);

            for (String chunkKey : chunkKeys) {
                try {
                    if (chunkKey.matches("\\d+(\\.\\d+)?")) { // numeric chunk key
                        ZarrArray chunkArray = arrayGroup.openArray(chunkKey);
                        Object chunkData = chunkArray.read();
                        double[][] chunkMatrix = convertToDoubleMatrix(chunkData);
                        for (double[] row : chunkMatrix) {
                            allData.add(row);
                        }
                        System.out.println(
                                "Read position chunk " + chunkKey + " with " + chunkMatrix.length + " positions");
                    }
                } catch (Exception chunkException) {
                    System.err
                            .println("Could not read position chunk " + chunkKey + ": " + chunkException.getMessage());
                }
            }

            return allData.toArray(new double[0][]);
        }
    }

    public static int[] getChunkSize(String zarrPath) throws IOException, InvalidRangeException {
        try {
            ZarrGroup group = ZarrGroup.open(zarrPath + "/nodes");
            return group.openArray("ids").getChunks();
        } catch (IOException e) {
            // If the path doesn't exist, return a default chunk size
            System.out.println("Path doesn't exist, using default chunk size: " + e.getMessage());
            return new int[] { 1000 }; // Default chunk size
        }
    }

    /**
     * Write nodes to Zarr format with chunked structure
     */
    public static void writeToZarr(List<GeffNode> nodes, String zarrPath) throws IOException, InvalidRangeException {
        writeToZarr(nodes, zarrPath, 1000);
    }

    /**
     * Write nodes to Zarr format with specified chunk size
     */
    public static void writeToZarr(List<GeffNode> nodes, String zarrPath, int... chunkSize)
            throws IOException, InvalidRangeException {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes list cannot be null or empty");
        }

        System.out.println(
                "Writing " + nodes.size() + " nodes to Zarr path: " + zarrPath + " with chunk size: " + chunkSize);

        // Create the main nodes group
        ZarrGroup nodesGroup = ZarrGroup.create(zarrPath);

        // Create attrs subgroup for chunked storage
        ZarrGroup attrsGroup = nodesGroup.createSubGroup("attrs");

        // Check if any nodes have 3D positions
        boolean hasPositions = nodes.stream()
                .anyMatch(node -> node.getPosition() != null && node.getPosition().length >= 3);

        System.out.println("Node analysis:");
        System.out.println("- Has 3D positions: " + hasPositions);
        System.out.println("- Format: Chunked arrays with separate values subgroups");

        // Write node IDs in chunks
        writeChunkedNodeIds(nodes, nodesGroup, chunkSize[0]);

        // Write timepoints in chunks
        writeChunkedIntAttribute(nodes, attrsGroup, "t", chunkSize[0], GeffNode::getTimepoint);

        // Write X coordinates in chunks
        writeChunkedDoubleAttribute(nodes, attrsGroup, "x", chunkSize[0], GeffNode::getX);

        // Write Y coordinates in chunks
        writeChunkedDoubleAttribute(nodes, attrsGroup, "y", chunkSize[0], GeffNode::getY);

        // Write segment IDs in chunks
        writeChunkedIntAttribute(nodes, attrsGroup, "seg_id", chunkSize[0], GeffNode::getSegmentId);

        // Write positions if available in chunks
        if (hasPositions) {
            writeChunkedPositions(nodes, attrsGroup, "position", chunkSize);
        }

        System.out.println("Successfully wrote nodes to Zarr format with chunked structure");
    }

    /**
     * Helper method to write chunked node IDs
     */
    private static void writeChunkedNodeIds(List<GeffNode> nodes, ZarrGroup parentGroup, int chunkSize)
            throws IOException, InvalidRangeException {

        int totalNodes = nodes.size();

        // Create the ids subgroup
        ZarrGroup idsGroup = parentGroup.createSubGroup("ids");

        // Create a single ZarrArray for all IDs with proper chunking
        ZarrArray idsArray = idsGroup.createArray("", new ArrayParams()
                .shape(totalNodes)
                .chunks(chunkSize)
                .dataType(DataType.i4));

        // Write data in chunks
        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize) {
            int endIdx = Math.min(startIdx + chunkSize, totalNodes);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            int[] chunkData = new int[currentChunkSize];

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                chunkData[i] = nodes.get(startIdx + i).getId();
            }

            // Write chunk at specific offset
            idsArray.write(chunkData, new int[] { currentChunkSize }, new int[] { startIdx });

            System.out.println("- Wrote node IDs chunk " + chunkIndex + ": " + currentChunkSize + " nodes (indices "
                    + startIdx + "-" + (endIdx - 1) + ")");
            chunkIndex++;
        }
    }

    /**
     * Helper method to write chunked int attributes
     */
    private static void writeChunkedIntAttribute(List<GeffNode> nodes, ZarrGroup attrsGroup, String attrName,
            int chunkSize, java.util.function.ToIntFunction<GeffNode> extractor)
            throws IOException, InvalidRangeException {

        int totalNodes = nodes.size();

        // Create the attribute subgroup
        ZarrGroup attrGroup = attrsGroup.createSubGroup(attrName);
        ZarrGroup valuesGroup = attrGroup.createSubGroup("values");

        // Create a single ZarrArray for all values with proper chunking
        ZarrArray valuesArray = valuesGroup.createArray("", new ArrayParams()
                .shape(totalNodes)
                .chunks(chunkSize)
                .dataType(DataType.i4));

        // Write data in chunks
        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize) {
            int endIdx = Math.min(startIdx + chunkSize, totalNodes);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            int[] chunkData = new int[currentChunkSize];

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                chunkData[i] = extractor.applyAsInt(nodes.get(startIdx + i));
            }

            // Write chunk at specific offset
            valuesArray.write(chunkData, new int[] { currentChunkSize }, new int[] { startIdx });

            System.out.println("- Wrote " + attrName + " chunk " + chunkIndex + ": " + currentChunkSize + " values");
            chunkIndex++;
        }
    }

    /**
     * Helper method to write chunked double attributes
     */
    private static void writeChunkedDoubleAttribute(List<GeffNode> nodes, ZarrGroup attrsGroup, String attrName,
            int chunkSize, java.util.function.ToDoubleFunction<GeffNode> extractor)
            throws IOException, InvalidRangeException {

        int totalNodes = nodes.size();

        // Create the attribute subgroup
        ZarrGroup attrGroup = attrsGroup.createSubGroup(attrName);
        ZarrGroup valuesGroup = attrGroup.createSubGroup("values");

        // Create a single ZarrArray for all values with proper chunking
        ZarrArray valuesArray = valuesGroup.createArray("", new ArrayParams()
                .shape(totalNodes)
                .chunks(chunkSize)
                .dataType(DataType.f8));

        // Write data in chunks
        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize) {
            int endIdx = Math.min(startIdx + chunkSize, totalNodes);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            double[] chunkData = new double[currentChunkSize];

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                chunkData[i] = extractor.applyAsDouble(nodes.get(startIdx + i));
            }

            // Write chunk at specific offset
            valuesArray.write(chunkData, new int[] { currentChunkSize }, new int[] { startIdx });

            System.out.println("- Wrote " + attrName + " chunk " + chunkIndex + ": " + currentChunkSize + " values");
            chunkIndex++;
        }
    }

    /**
     * Helper method to write chunked position matrices
     */
    private static void writeChunkedPositions(List<GeffNode> nodes, ZarrGroup attrsGroup, String attrName,
            int[] chunkSize) throws IOException, InvalidRangeException {

        int totalNodes = nodes.size();
        int nodeChunkSize = chunkSize[0]; // Use first dimension for node chunking

        // Create the attribute subgroup
        ZarrGroup attrGroup = attrsGroup.createSubGroup(attrName);
        ZarrGroup valuesGroup = attrGroup.createSubGroup("values");

        // Create a single ZarrArray for all positions with proper chunking
        ZarrArray positionsArray = valuesGroup.createArray("", new ArrayParams()
                .shape(totalNodes, 3)
                .chunks(chunkSize.length >= 2 ? chunkSize : new int[] { nodeChunkSize, 3 })
                .dataType(DataType.f8));

        // Write data in chunks
        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalNodes; startIdx += nodeChunkSize) {
            int endIdx = Math.min(startIdx + nodeChunkSize, totalNodes);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array - flattened 3D positions
            double[] chunkData = new double[currentChunkSize * 3];

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                GeffNode node = nodes.get(startIdx + i);
                double[] nodePos = node.getPosition();
                if (nodePos != null && nodePos.length >= 3) {
                    chunkData[i * 3] = nodePos[0]; // X
                    chunkData[i * 3 + 1] = nodePos[1]; // Y
                    chunkData[i * 3 + 2] = nodePos[2]; // Z
                } else {
                    // Create 3D position from x, y coordinates with z=0
                    chunkData[i * 3] = node.getX();
                    chunkData[i * 3 + 1] = node.getY();
                    chunkData[i * 3 + 2] = node.getZ();
                }
            }

            // Write chunk at specific offset
            positionsArray.write(chunkData, new int[] { currentChunkSize, 3 }, new int[] { startIdx, 0 });

            System.out.println("- Wrote " + attrName + " chunk " + chunkIndex + ": " + currentChunkSize + " positions");
            chunkIndex++;
        }
    }

    // Helper methods for type conversion
    private static int[] convertToIntArray(Object data, String fieldName) {
        if (data instanceof int[]) {
            return (int[]) data;
        } else if (data instanceof long[]) {
            long[] longArray = (long[]) data;
            int[] intArray = new int[longArray.length];
            for (int i = 0; i < longArray.length; i++) {
                intArray[i] = (int) longArray[i];
            }
            return intArray;
        } else if (data instanceof double[]) {
            double[] doubleArray = (double[]) data;
            int[] intArray = new int[doubleArray.length];
            for (int i = 0; i < doubleArray.length; i++) {
                intArray[i] = (int) doubleArray[i];
            }
            return intArray;
        } else if (data instanceof float[]) {
            float[] floatArray = (float[]) data;
            int[] intArray = new int[floatArray.length];
            for (int i = 0; i < floatArray.length; i++) {
                intArray[i] = (int) floatArray[i];
            }
            return intArray;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported data type for " + fieldName + ": " +
                            (data != null ? data.getClass().getName() : "null"));
        }
    }

    private static double[] convertToDoubleArray(Object data, String fieldName) {
        if (data instanceof double[]) {
            return (double[]) data;
        } else if (data instanceof float[]) {
            float[] floatArray = (float[]) data;
            double[] doubleArray = new double[floatArray.length];
            for (int i = 0; i < floatArray.length; i++) {
                doubleArray[i] = floatArray[i];
            }
            return doubleArray;
        } else if (data instanceof int[]) {
            int[] intArray = (int[]) data;
            double[] doubleArray = new double[intArray.length];
            for (int i = 0; i < intArray.length; i++) {
                doubleArray[i] = intArray[i];
            }
            return doubleArray;
        } else if (data instanceof long[]) {
            long[] longArray = (long[]) data;
            double[] doubleArray = new double[longArray.length];
            for (int i = 0; i < longArray.length; i++) {
                doubleArray[i] = longArray[i];
            }
            return doubleArray;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported data type for " + fieldName + ": " +
                            (data != null ? data.getClass().getName() : "null"));
        }
    }

    private static double[][] convertToDoubleMatrix(Object data) {
        if (data instanceof double[][]) {
            return (double[][]) data;
        } else if (data instanceof float[][]) {
            float[][] floatMatrix = (float[][]) data;
            double[][] doubleMatrix = new double[floatMatrix.length][];
            for (int i = 0; i < floatMatrix.length; i++) {
                doubleMatrix[i] = new double[floatMatrix[i].length];
                for (int j = 0; j < floatMatrix[i].length; j++) {
                    doubleMatrix[i][j] = floatMatrix[i][j];
                }
            }
            return doubleMatrix;
        } else if (data instanceof double[]) {
            // Single row matrix
            double[] singleRow = (double[]) data;
            return new double[][] { singleRow };
        } else if (data instanceof float[]) {
            // Single row matrix from float array
            float[] floatArray = (float[]) data;
            double[] doubleRow = new double[floatArray.length];
            for (int i = 0; i < floatArray.length; i++) {
                doubleRow[i] = floatArray[i];
            }
            return new double[][] { doubleRow };
        } else {
            throw new IllegalArgumentException(
                    "Unsupported data type for position matrix: " +
                            (data != null ? data.getClass().getName() : "null"));
        }
    }

    @Override
    public String toString() {
        return String.format("GeffNode{id=%d, t=%d, x=%.2f, y=%.2f, z=%.2f, segId=%d}",
                id, timepoint, x, y, getZ(), segmentId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        GeffNode geffNode = (GeffNode) obj;
        return id == geffNode.id &&
                timepoint == geffNode.timepoint &&
                Double.compare(geffNode.x, x) == 0 &&
                Double.compare(geffNode.y, y) == 0 &&
                segmentId == geffNode.segmentId;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + timepoint;
        result = 31 * result + Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + segmentId;
        return result;
    }
}
