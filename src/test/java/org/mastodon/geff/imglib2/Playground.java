package org.mastodon.geff.imglib2;

import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import java.util.Arrays;

public class Playground {


    // ------------------------------------------------------------------------
    //
    //    main
    //
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
//        final String path = "cross-language-tests/data/basic_3d_original.zarr";
//        final String path = "cross-language-tests/data/covariance_original.zarr";
        final String path = "cross-language-tests/data/varlength_original.zarr";

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {

            final GeffPropertySpecs nodePropertySpecs = IoUtils.loadPropertySpecs(n5, ElementType.NODE);
            final GeffProperties nodeProperties = IoUtils.loadProperties(n5, nodePropertySpecs);

            final GeffPropertySpecs edgePropertySpecs = IoUtils.loadPropertySpecs(n5, ElementType.EDGE);
            final GeffProperties edgeProperties = IoUtils.loadProperties(n5, edgePropertySpecs);

            System.out.println("nodePropertySpecs = " + nodePropertySpecs);
            System.out.println();
            System.out.println("nodeProperties = " + nodeProperties);
            System.out.println();
            System.out.println("nodePropertySpecs = " + edgePropertySpecs);
            System.out.println();
            System.out.println("edgeProperties = " + edgeProperties);
        }
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
