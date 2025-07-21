package org.mastodon.geff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import org.mastodon.geff.function.ToDoubleArrayFunction;
import org.mastodon.geff.function.ToIntArrayFunction;

import com.bc.zarr.ArrayParams;
import com.bc.zarr.DataType;
import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

public class ZarrUtils {

    public static final int DEFAULT_CHUNK_SIZE = 1000; // Default chunk size if not specified

    /**
     * Helper method to read chunked int arrays
     */
    public static int[] readChunkedIntArray(ZarrGroup group, String arrayPath, String description)
            throws IOException, InvalidRangeException {
        if (group.getArrayKeys() == null || group.getArrayKeys().isEmpty()) {
            System.out.println("No arrays found in group for " + description);
            return new int[0]; // Return empty array if no arrays found
        }
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToIntArray(data, description);
        } catch (Exception e) {

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
    public static double[] readChunkedDoubleArray(ZarrGroup group, String arrayPath, String description)
            throws IOException, InvalidRangeException {
        if (group.getArrayKeys() == null || group.getArrayKeys().isEmpty()) {
            System.out.println("No arrays found in group for " + description);
            return new double[0]; // Return empty array if no arrays found
        }
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToDoubleArray(data, description);
        } catch (Exception e) {

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
     * Helper method to read chunked integer matrix
     */
    public static int[][] readChunkedIntMatrix(ZarrGroup group, String arrayPath, String description)
            throws IOException, InvalidRangeException {
        if (group.getArrayKeys() == null || group.getArrayKeys().isEmpty()) {
            System.out.println("No arrays found in group for " + description);
            return new int[0][]; // Return empty matrix if no arrays found
        }
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToIntMatrix(data, description);
        } catch (Exception e) {

            // Try reading individual chunks if whole array reading fails
            List<int[]> allData = new ArrayList<>();

            // Look for numeric chunk keys (0, 1, 2, etc.)
            ZarrGroup arrayGroup = group.openSubGroup(arrayPath);
            String[] chunkKeys = arrayGroup.getArrayKeys().toArray(new String[0]);

            for (String chunkKey : chunkKeys) {
                try {
                    if (chunkKey.matches("\\d+(\\.\\d+)?")) { // numeric chunk key
                        ZarrArray chunkArray = arrayGroup.openArray(chunkKey);
                        Object chunkData = chunkArray.read();
                        int[][] chunkMatrix = convertToIntMatrix(chunkData, description);
                        for (int[] row : chunkMatrix) {
                            allData.add(row);
                        }
                        System.out.println(
                                "Read " + description + " chunk " + chunkKey + " with " + chunkMatrix.length);
                    }
                } catch (Exception chunkException) {
                    System.err
                            .println("Could not read " + description + " chunk " + chunkKey + ": "
                                    + chunkException.getMessage());
                }
            }

            return allData.toArray(new int[0][]);
        }
    }

    /**
     * Helper method to read chunked double matrix
     */
    public static double[][] readChunkedDoubleMatrix(ZarrGroup group, String arrayPath, String description)
            throws IOException, InvalidRangeException {
        if (group.getArrayKeys() == null || group.getArrayKeys().isEmpty()) {
            System.out.println("No arrays found in group for " + description);
            return new double[0][]; // Return empty matrix if no arrays found
        }
        try {
            // First try reading as a whole array
            ZarrArray array = group.openArray(arrayPath);
            Object data = array.read();
            return convertToDoubleMatrix(data, description);
        } catch (Exception e) {

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
                        double[][] chunkMatrix = convertToDoubleMatrix(chunkData, description);
                        for (double[] row : chunkMatrix) {
                            allData.add(row);
                        }
                        System.out.println(
                                "Read " + description + " chunk " + chunkKey + " with " + chunkMatrix.length);
                    }
                } catch (Exception chunkException) {
                    System.err
                            .println("Could not read " + description + " chunk " + chunkKey + ": "
                                    + chunkException.getMessage());
                }
            }

            return allData.toArray(new double[0][]);
        }
    }

    public static int getChunkSize(String zarrPath) throws IOException, InvalidRangeException {
        try {
            ZarrGroup group = ZarrGroup.open(zarrPath + "/nodes");
            return group.openArray("ids").getChunks()[0];
        } catch (IOException e) {
            // If the path doesn't exist, return a default chunk size
            System.out.println("Path doesn't exist, using default chunk size: " + e.getMessage());
            return DEFAULT_CHUNK_SIZE; // Default chunk size
        }
    }

    /**
     * Helper method to write chunked int attributes
     */
    public static <T extends ZarrEntity> void writeChunkedIntAttribute(List<T> nodes, ZarrGroup attrsGroup,
            String attrName,
            int chunkSize, ToIntFunction<T> extractor)
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
    public static <T extends ZarrEntity> void writeChunkedDoubleAttribute(List<T> nodes, ZarrGroup attrsGroup,
            String attrName,
            int chunkSize, java.util.function.ToDoubleFunction<T> extractor)
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
     * Helper method to write chunked integer matrices
     */
    public static <T extends ZarrEntity> void writeChunkedIntMatrix(List<T> nodes, ZarrGroup attrsGroup,
            String attrName,
            int chunkSize, ToIntArrayFunction<T> extractor, int numColumns)
            throws IOException, InvalidRangeException {
        int totalNodes = nodes.size();

        // Create the attribute subgroup
        ZarrGroup attrGroup = attrsGroup.createSubGroup(attrName);
        ZarrGroup valuesGroup = attrGroup.createSubGroup("values");

        // Create a single ZarrArray for all data with proper chunking
        ZarrArray array2d = valuesGroup.createArray("", new ArrayParams()
                .shape(totalNodes, numColumns)
                .chunks(new int[] { chunkSize, numColumns })
                .dataType(DataType.f4));

        // Write data in chunks
        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize) {
            int endIdx = Math.min(startIdx + chunkSize, totalNodes);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            int[] chunkData = new int[currentChunkSize * numColumns];

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                T node = nodes.get(startIdx + i);
                int[] values = extractor.applyAsIntArray(node);
                if (values != null && values.length == numColumns) {
                    for (int j = 0; j < numColumns; j++) {
                        chunkData[i * numColumns + j] = values[j];
                    }
                } else {
                    for (int j = 0; j < numColumns; j++) {
                        chunkData[i * numColumns + j] = 0; // Default to zero if not set
                    }
                }
            }

            // Write chunk at specific offset
            array2d.write(chunkData, new int[] { currentChunkSize, numColumns },
                    new int[] { startIdx, 0 });

            System.out.println("- Wrote " + attrName + " chunk " + chunkIndex + ": " + currentChunkSize + " values");
            chunkIndex++;
        }
    }

    /**
     * Helper method to write chunked double matrices
     */
    public static <T extends ZarrEntity> void writeChunkedDoubleMatrix(List<T> nodes, ZarrGroup attrsGroup,
            String attrName,
            int chunkSize, ToDoubleArrayFunction<T> extractor, int numColumns)
            throws IOException, InvalidRangeException {
        int totalNodes = nodes.size();

        // Create the attribute subgroup
        ZarrGroup attrGroup = attrsGroup.createSubGroup(attrName);
        ZarrGroup valuesGroup = attrGroup.createSubGroup("values");

        // Create a single ZarrArray for all data with proper chunking
        ZarrArray array2d = valuesGroup.createArray("", new ArrayParams()
                .shape(totalNodes, numColumns)
                .chunks(new int[] { chunkSize, numColumns })
                .dataType(DataType.f4));

        // Write data in chunks
        int chunkIndex = 0;
        for (int startIdx = 0; startIdx < totalNodes; startIdx += chunkSize) {
            int endIdx = Math.min(startIdx + chunkSize, totalNodes);
            int currentChunkSize = endIdx - startIdx;

            // Prepare chunk data array
            double[] chunkData = new double[currentChunkSize * numColumns];

            // Fill chunk data array
            for (int i = 0; i < currentChunkSize; i++) {
                T node = nodes.get(startIdx + i);
                double[] values = extractor.applyAsDoubleArray(node);
                if (values != null && values.length == numColumns) {
                    for (int j = 0; j < numColumns; j++) {
                        chunkData[i * numColumns + j] = values[j];
                    }
                } else {
                    for (int j = 0; j < numColumns; j++) {
                        chunkData[i * numColumns + j] = 0.0; // Default to zero if not set
                    }
                }
            }

            // Write chunk at specific offset
            array2d.write(chunkData, new int[] { currentChunkSize, numColumns },
                    new int[] { startIdx, 0 });

            System.out.println("- Wrote " + attrName + " chunk " + chunkIndex + ": " + currentChunkSize + " values");
            chunkIndex++;
        }
    }

    // Helper methods for type conversion
    public static int[] convertToIntArray(Object data, String fieldName) {
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

    public static double[] convertToDoubleArray(Object data, String fieldName) {
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

    public static int[][] convertToIntMatrix(Object data, String description) {
        if (data instanceof int[][]) {
            return (int[][]) data;
        } else if (data instanceof long[][]) {
            long[][] longMatrix = (long[][]) data;
            int[][] intMatrix = new int[longMatrix.length][];
            for (int i = 0; i < longMatrix.length; i++) {
                intMatrix[i] = new int[longMatrix[i].length];
                for (int j = 0; j < longMatrix[i].length; j++) {
                    intMatrix[i][j] = (int) longMatrix[i][j];
                }
            }
            return intMatrix;
        } else if (data instanceof double[]) {
            // Single row matrix
            double[] singleRow = (double[]) data;
            int[][] intMatrix = new int[singleRow.length / 2][2];
            for (int i = 0; i < singleRow.length; i += 2) {
                intMatrix[i / 2][0] = (int) singleRow[i];
                intMatrix[i / 2][1] = (int) singleRow[i + 1];
            }
            return intMatrix;
        } else if (data instanceof float[]) {
            // Single row matrix from float array
            float[] floatArray = (float[]) data;
            int[][] intMatrix = new int[floatArray.length / 2][2];
            for (int i = 0; i < floatArray.length; i += 2) {
                intMatrix[i / 2][0] = (int) floatArray[i];
                intMatrix[i / 2][1] = (int) floatArray[i + 1];
            }
            return intMatrix;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported data type for position matrix: " +
                            (data != null ? data.getClass().getName() : "null") + " for " + description);
        }
    }

    public static double[][] convertToDoubleMatrix(Object data, String description) {
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
                            (data != null ? data.getClass().getName() : "null") + " for " + description);
        }
    }
}
