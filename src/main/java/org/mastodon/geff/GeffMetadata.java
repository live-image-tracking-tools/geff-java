package org.mastodon.geff;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

/**
 * Represents metadata for a Geff (Graph Exchange Format for Features) dataset.
 * This class handles reading and writing metadata from/to Zarr format.
 * 
 * This is the Java equivalent of the Python GeffMetadata schema from:
 * https://github.com/live-image-tracking-tools/geff/blob/main/src/geff/metadata_schema.py
 */
public class GeffMetadata {

    // Supported GEFF versions
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList("0.0", "0.1", "0.2");
    // Pattern to match major.minor versions, allowing for patch versions (e.g.,
    // 0.1.1 matches 0.1)
    private static final Pattern SUPPORTED_VERSIONS_PATTERN = Pattern
            .compile("(0\\.0(?:\\.\\d+)?|0\\.1(?:\\.\\d+)?|0\\.2(?:\\.\\d+)?)");

    // Metadata attributes - matching the Python schema
    private String geffVersion;
    private boolean directed;
    private double[] roiMin;
    private double[] roiMax;
    private String positionAttr;
    private String[] axisNames;
    private String[] axisUnits;

    /**
     * Default constructor
     */
    public GeffMetadata() {
    }

    /**
     * Constructor with basic parameters
     */
    public GeffMetadata(String geffVersion, boolean directed) {
        setGeffVersion(geffVersion);
        this.directed = directed;
    }

    /**
     * Constructor with all parameters
     */
    public GeffMetadata(String geffVersion, boolean directed, double[] roiMin, double[] roiMax,
            String positionAttr, String[] axisNames, String[] axisUnits) {
        setGeffVersion(geffVersion);
        this.directed = directed;
        setRoiMin(roiMin);
        setRoiMax(roiMax);
        setPositionAttr(positionAttr);
        setAxisNames(axisNames);
        setAxisUnits(axisUnits);
        validate();
    }

    // Getters and Setters
    public String getGeffVersion() {
        return geffVersion;
    }

    public void setGeffVersion(String geffVersion) {
        if (geffVersion != null && !SUPPORTED_VERSIONS_PATTERN.matcher(geffVersion).matches()) {
            throw new IllegalArgumentException(
                    "Unsupported GEFF version: " + geffVersion +
                            ". Supported major.minor versions are: " + SUPPORTED_VERSIONS +
                            " (patch versions like 0.1.1 are also supported)");
        }
        this.geffVersion = geffVersion;
    }

    public boolean isDirected() {
        return directed;
    }

    public void setDirected(boolean directed) {
        this.directed = directed;
    }

    public double[] getRoiMin() {
        return roiMin != null ? roiMin.clone() : null;
    }

    public void setRoiMin(double[] roiMin) {
        this.roiMin = roiMin != null ? roiMin.clone() : null;
    }

    public double[] getRoiMax() {
        return roiMax != null ? roiMax.clone() : null;
    }

    public void setRoiMax(double[] roiMax) {
        this.roiMax = roiMax != null ? roiMax.clone() : null;
    }

    public String getPositionAttr() {
        return positionAttr;
    }

    public void setPositionAttr(String positionAttr) {
        this.positionAttr = positionAttr;
    }

    public String[] getAxisNames() {
        return axisNames != null ? axisNames.clone() : null;
    }

    public void setAxisNames(String[] axisNames) {
        this.axisNames = axisNames != null ? axisNames.clone() : null;
    }

    public String[] getAxisUnits() {
        return axisUnits != null ? axisUnits.clone() : null;
    }

    public void setAxisUnits(String[] axisUnits) {
        this.axisUnits = axisUnits != null ? axisUnits.clone() : null;
    }

    /**
     * Validates the metadata according to the GEFF schema rules
     */
    public void validate() {
        // Check spatial metadata consistency if position is provided
        if (positionAttr != null) {
            // Check roi consistency if both are provided
            if (roiMin != null && roiMax != null) {
                if (roiMin.length != roiMax.length) {
                    throw new IllegalArgumentException(
                            "Roi min " + Arrays.toString(roiMin) + " and roi max " + Arrays.toString(roiMax) +
                                    " have different lengths.");
                }

                int ndim = roiMin.length;
                for (int dim = 0; dim < ndim; dim++) {
                    if (roiMin[dim] > roiMax[dim]) {
                        throw new IllegalArgumentException(
                                "Roi min " + Arrays.toString(roiMin) + " is greater than " +
                                        "max " + Arrays.toString(roiMax) + " in dimension " + dim);
                    }
                }

                // Check axis metadata consistency with roi dimensions
                if (axisNames != null && axisNames.length != ndim) {
                    throw new IllegalArgumentException(
                            "Length of axis names (" + axisNames.length + ") does not match number of" +
                                    " dimensions in roi (" + ndim + ")");
                }

                if (axisUnits != null && axisUnits.length != ndim) {
                    throw new IllegalArgumentException(
                            "Length of axis units (" + axisUnits.length + ") does not match number of" +
                                    " dimensions in roi (" + ndim + ")");
                }
            } else if ((roiMin != null && roiMax == null) || (roiMin == null && roiMax != null)) {
                // If only one roi is provided, that's inconsistent
                throw new IllegalArgumentException(
                        "Both roi_min and roi_max must be provided together, or both omitted.");
            }
            // Note: roiMin and roiMax are now optional even when position_attr is specified
        } else {
            // If no position, check that other spatial metadata is not provided
            if (roiMin != null || roiMax != null || axisNames != null || axisUnits != null) {
                throw new IllegalArgumentException(
                        "Spatial metadata (roi_min, roi_max, axis_names or axis_units) provided without" +
                                " position_attr");
            }
        }
    }

    /**
     * Read metadata from a Zarr group
     */
    public static GeffMetadata readFromZarr(String zarrPath) throws IOException, InvalidRangeException {
        ZarrGroup group = ZarrGroup.open(zarrPath);
        return readFromZarr(group);
    }

    /**
     * Read metadata from a Zarr group
     */
    public static GeffMetadata readFromZarr(ZarrGroup group) throws IOException {
        // Check if geff_version exists in zattrs
        if (!group.getAttributes().containsKey("geff_version")) {
            throw new IllegalArgumentException(
                    "No geff_version found in " + group + ". This may indicate the path is incorrect or " +
                            "zarr group name is not specified (e.g. /dataset.zarr/tracks/ instead of " +
                            "/dataset.zarr/).");
        }

        GeffMetadata metadata = new GeffMetadata();

        // Read required fields
        String geffVersion = (String) group.getAttributes().get("geff_version");
        metadata.setGeffVersion(geffVersion);

        Object directedObj = group.getAttributes().get("directed");
        if (directedObj instanceof Boolean) {
            metadata.setDirected((Boolean) directedObj);
        } else if (directedObj instanceof String) {
            metadata.setDirected(Boolean.parseBoolean((String) directedObj));
        }

        // Read optional fields
        Object roiMinObj = group.getAttributes().get("roi_min");
        if (roiMinObj != null) {
            metadata.setRoiMin(convertToDoubleArray(roiMinObj));
        }

        Object roiMaxObj = group.getAttributes().get("roi_max");
        if (roiMaxObj != null) {
            metadata.setRoiMax(convertToDoubleArray(roiMaxObj));
        }

        String positionAttr = (String) group.getAttributes().get("position_attr");
        metadata.setPositionAttr(positionAttr);

        Object axisNamesObj = group.getAttributes().get("axis_names");
        if (axisNamesObj != null) {
            metadata.setAxisNames(convertToStringArray(axisNamesObj));
        }

        Object axisUnitsObj = group.getAttributes().get("axis_units");
        if (axisUnitsObj != null) {
            metadata.setAxisUnits(convertToStringArray(axisUnitsObj));
        } else {
            metadata.setAxisUnits(null); // Ensure it's set to null if not present
        }

        // Validate the loaded metadata
        metadata.validate();

        return metadata;
    }

    /**
     * Write metadata to Zarr format
     */
    public void writeToZarr(ZarrGroup group) throws IOException {
        // Validate before writing
        validate();

        // Create a TreeMap to ensure attributes are ordered alphabetically by key
        java.util.Map<String, Object> attrs = new java.util.TreeMap<>();

        // Write required fields
        attrs.put("geff_version", geffVersion);
        attrs.put("directed", directed);

        // Write optional fields
        if (roiMin != null) {
            attrs.put("roi_min", roiMin);
        }
        if (roiMax != null) {
            attrs.put("roi_max", roiMax);
        }
        if (positionAttr != null) {
            attrs.put("position_attr", positionAttr);
        }
        if (axisNames != null) {
            attrs.put("axis_names", axisNames);
        }
        // Always write axis_units, even if null
        attrs.put("axis_units", axisUnits);

        // Write the attributes to the Zarr group
        group.writeAttributes(attrs);

        System.out.println("Written metadata attributes: " + attrs.keySet());
    }

    /**
     * Write metadata to Zarr format at specified path
     */
    public static void writeToZarr(GeffMetadata metadata, String zarrPath) throws IOException {
        ZarrGroup group = ZarrGroup.create(zarrPath);
        metadata.writeToZarr(group);
    }

    // Helper methods for type conversion
    private static double[] convertToDoubleArray(Object obj) {
        if (obj instanceof double[]) {
            return (double[]) obj;
        } else if (obj instanceof java.util.ArrayList) {
            @SuppressWarnings("unchecked")
            java.util.ArrayList<Object> list = (java.util.ArrayList<Object>) obj;
            double[] result = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Number) {
                    result[i] = ((Number) list.get(i)).doubleValue();
                } else {
                    result[i] = Double.parseDouble(list.get(i).toString());
                }
            }
            return result;
        } else if (obj instanceof Object[]) {
            Object[] arr = (Object[]) obj;
            double[] result = new double[arr.length];
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] instanceof Number) {
                    result[i] = ((Number) arr[i]).doubleValue();
                } else {
                    result[i] = Double.parseDouble(arr[i].toString());
                }
            }
            return result;
        } else if (obj instanceof float[]) {
            float[] floatArray = (float[]) obj;
            double[] result = new double[floatArray.length];
            for (int i = 0; i < floatArray.length; i++) {
                result[i] = floatArray[i];
            }
            return result;
        }
        return null;
    }

    private static String[] convertToStringArray(Object obj) {
        if (obj instanceof String[]) {
            return (String[]) obj;
        } else if (obj instanceof java.util.ArrayList) {
            @SuppressWarnings("unchecked")
            java.util.ArrayList<Object> list = (java.util.ArrayList<Object>) obj;
            String[] result = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i) != null ? list.get(i).toString() : null;
            }
            return result;
        } else if (obj instanceof Object[]) {
            Object[] arr = (Object[]) obj;
            String[] result = new String[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i] != null ? arr[i].toString() : null;
            }
            return result;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format(
                "GeffMetadata{geffVersion='%s', directed=%s, roiMin=%s, roiMax=%s, " +
                        "positionAttr='%s', axisNames=%s, axisUnits=%s}",
                geffVersion, directed, Arrays.toString(roiMin), Arrays.toString(roiMax),
                positionAttr, Arrays.toString(axisNames), Arrays.toString(axisUnits));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        GeffMetadata that = (GeffMetadata) obj;

        if (directed != that.directed)
            return false;
        if (geffVersion != null ? !geffVersion.equals(that.geffVersion) : that.geffVersion != null)
            return false;
        if (!Arrays.equals(roiMin, that.roiMin))
            return false;
        if (!Arrays.equals(roiMax, that.roiMax))
            return false;
        if (positionAttr != null ? !positionAttr.equals(that.positionAttr) : that.positionAttr != null)
            return false;
        if (!Arrays.equals(axisNames, that.axisNames))
            return false;
        return Arrays.equals(axisUnits, that.axisUnits);
    }

    @Override
    public int hashCode() {
        int result = geffVersion != null ? geffVersion.hashCode() : 0;
        result = 31 * result + (directed ? 1 : 0);
        result = 31 * result + Arrays.hashCode(roiMin);
        result = 31 * result + Arrays.hashCode(roiMax);
        result = 31 * result + (positionAttr != null ? positionAttr.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(axisNames);
        result = 31 * result + Arrays.hashCode(axisUnits);
        return result;
    }
}
