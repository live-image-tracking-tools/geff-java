package org.mastodon.geff.imglib2;

import com.google.gson.reflect.TypeToken;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.PropMetadata;

import java.util.Arrays;
import java.util.Map;

public class Playground {

    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        final String path = "cross-language-tests/data/basic_3d_original.zarr";
//        final String path = "cross-language-tests/data/covariance_original.zarr";
//        final String path = "cross-language-tests/data/varlength_original.zarr";

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {

            final Map<String, PropMetadata> nodePropsMetadata = n5.getAttribute("", "geff/node_props_metadata",
                    new TypeToken<Map<String, PropMetadata>>() {}.getType());

            nodePropsMetadata.forEach((k, v) -> {
                System.out.println(k + ": " + v);
//                System.out.println();

                final String identifier = v.getIdentifier();
                final boolean isVarLength = v.getVarlength();

                final ZarrDatasetAttributes attrValues = attrs(n5, valuesArray(identifier));
                final ZarrDatasetAttributes attrMissing = attrs(n5, missingArray(identifier));
                final ZarrDatasetAttributes attrData = isVarLength? attrs(n5, dataArray(identifier)) : null;

                final DType dType = isVarLength ? attrData.getDType() : attrValues.getDType();
                final boolean isOptional = attrMissing != null;

                final int numDimensions;
                final Dimensions dimensions;
                if (isVarLength) {
                    final long[] valuesDim = attrValues.getDimensions();
                    numDimensions = (int) (valuesDim[0] - 1);
                    dimensions = null;
                } else {
                    final long[] valuesDim = attrValues.getDimensions();
                    final long[] propertyDim = Arrays.copyOf(valuesDim,valuesDim.length - 1);
                    numDimensions = propertyDim.length;
                    dimensions = FinalDimensions.wrap(propertyDim);
                }


//                System.out.println("identifier = " + identifier);
//                System.out.println("isVarLength = " + isVarLength);
//                System.out.println("isOptional = " + isOptional);
                final String imglib2_type_name = N5Utils.type(dType.getDataType()).getClass().getSimpleName();
                System.out.println("dType = " + dType + " (imglib2 " + imglib2_type_name + ")");
//                System.out.println("numDimensions = " + numDimensions);
//                System.out.println("dimensions = " + (dimensions == null ? "null" : "[" + dimensions + "]"));

                final GeffPropertySpec spec = new GeffPropertySpec(identifier, isVarLength, isOptional, dType, numDimensions, dimensions);
                System.out.println("spec = " + spec);
                System.out.println();
            });

            System.out.println("id:");
            final ZarrDatasetAttributes attrIds = attrs(n5, "nodes/ids");
            final DType dType = attrIds.getDType();
            final String imglib2_type_name = N5Utils.type(dType.getDataType()).getClass().getSimpleName();
            System.out.println("dType = " + dType + " (imglib2 " + imglib2_type_name + ")");
            final GeffPropertySpec spec = new GeffPropertySpec("id", false, false, dType, 0, null);
            System.out.println("spec = " + spec);
        }
    }

    private static String valuesArray(final String identifier) {
        return "nodes/props/" + identifier + "/values";
    }

    private static String missingArray(final String identifier) {
        return "nodes/props/" + identifier + "/missing";
    }

    private static String dataArray(final String identifier) {
        return "nodes/props/" + identifier + "/data";
    }

    private static ZarrDatasetAttributes attrs(final N5ZarrReader n5, final String dataset) {
        return (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
    }

    private static void printAttributes(final N5ZarrReader n5, final String dataset) {
        final ZarrDatasetAttributes attributes = attrs(n5, dataset);
        System.out.println(dataset + ":");
        System.out.println("  DType=\"" + attributes.getDType() + "\"");
        System.out.println("  dimensions=\"" + Arrays.toString(attributes.getDimensions()) + "\"");
    }
}
