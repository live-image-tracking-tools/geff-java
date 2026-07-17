package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;
import org.mastodon.geff.imglib2.Slice.SlicePosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Playground {


    // -----------------------------------------------------------------------
    // client-side
    //
    record Vertex(long id, double x) {
    }


    // -----------------------------------------------------------------------
    // geff-java side
    //
    static class NodeData
    {
        private final long numNodes;

        private final List<SlicePosition> slicePositions = new ArrayList<>();
        private final Slice<UnsignedLongType> nodeIdSlice;

        private final HashMap<String, RandomAccessibleInterval<?>> nodeProperties = new HashMap<>();
        private final HashMap<String, RandomAccess<?>> nodeProperyAccesses = new HashMap<>();

        // TODO:                          generic int type here?
        NodeData(RandomAccessibleInterval<UnsignedLongType> nodeIdData) {

            numNodes = nodeIdData.dimension(0);
            nodeIdSlice = Slice.slice(nodeIdData, 0);

            nodeProperties.put("id", nodeIdSlice);
            slicePositions.add(nodeIdSlice.slicePosition());
            nodeProperyAccesses.put("id", nodeIdSlice.randomAccess());
        }

        long size() {
            return numNodes;
        }

        void nodeIndex(long index) {
            if (index < 0 || index >= numNodes)
                throw new IndexOutOfBoundsException(index + "(numNodes=" + numNodes + ")");
            slicePositions.forEach(p -> p.setPosition(index, 0));
        }

        RandomAccess<?> getPropertyAccess(final String property) {
            return nodeProperyAccesses.get(property);
        }

        public void addProperty(final String identifier, final RandomAccessibleInterval<?> data) {

            final int n = data.numDimensions();
            if (data.dimension(n-1) != numNodes)
                throw new IllegalArgumentException("property \"" + identifier + "\": dimensions don't match number of nodes (" + data.dimension(n - 1) + "!=" + numNodes + ")");

            Slice<?> propSlice = Slice.slice(data, n - 1);
            nodeProperties.put(identifier, propSlice);
            slicePositions.add(propSlice.slicePosition());
            nodeProperyAccesses.put(identifier, propSlice.randomAccess());
        }
    }



    // -----------------------------------------------------------------------

    public static void main(String[] args) {
//        final String path = "cross-language-tests/data/basic_3d_original.zarr";
        final String path = "cross-language-tests/data/covariance_original.zarr";

        final List<Vertex> nodes = new ArrayList<>();

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {

            printAttributes(n5, "nodes/ids");
            printAttributes(n5, "nodes/props/x/values");
//            printAttributes(n5, "nodes/props/covariance2d/values");
//            printAttributes(n5, "nodes/props/covariance3d/values");

            final RandomAccessibleInterval<UnsignedLongType> ids = N5Utils.open(n5, "nodes/ids");
            final RandomAccessibleInterval<DoubleType> xs = N5Utils.open(n5, "nodes/props/x/values");

            final NodeData nodeData = new NodeData(ids);
            nodeData.addProperty("x", xs);

            final Sampler<UnsignedLongType> id = Cast.unchecked(nodeData.getPropertyAccess("id"));
            final Sampler<DoubleType> x = Cast.unchecked(nodeData.getPropertyAccess("x"));
            for (int i = 0; i < nodeData.size(); i++) {
                nodeData.nodeIndex(i);
                nodes.add(new Vertex(id.get().get(), x.get().get()));
            }

            print(nodes);
        }

        // write
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {

            final RandomAccessibleInterval<UnsignedLongType> ids = ArrayImgs.unsignedLongs(nodes.size());
            final RandomAccessibleInterval<DoubleType> xs = ArrayImgs.doubles(nodes.size());

            final NodeData nodeData = new NodeData(ids);
            nodeData.addProperty("x", xs);

            final Sampler<UnsignedLongType> id = Cast.unchecked(nodeData.getPropertyAccess("id"));
            final Sampler<DoubleType> x = Cast.unchecked(nodeData.getPropertyAccess("x"));
            for (int i = 0; i < nodes.size(); i++) {
                nodeData.nodeIndex(i);
                id.get().set(nodes.get(i).id());
                x.get().set(nodes.get(i).x());
            }

            writeDataset(n5, "nodes/ids2", "<u8", ids);
            writeDataset(n5, "nodes/props/x/values2", "<f8", xs);
        }
    }

    private static <T extends NativeType<T>> void writeDataset(
            final N5ZarrWriter n5,
            final String dataset,
            final String typestr,
            final RandomAccessibleInterval<T> data) {
        final long[] dimensions = data.dimensionsAsLongArray();
        final int[] blockSize = Util.long2int(dimensions);
        final ZarrDatasetAttributes attributes = new ZarrDatasetAttributes(
                dimensions,
                blockSize,
                new DType(typestr, null),
                new ZstandardCompression(0),
                true,
                "0"
        );
        n5.createDataset(dataset, attributes);
        N5Utils.saveRegion(data, n5, dataset, attributes);
    }

    private static void printAttributes(final N5ZarrReader n5, final String dataset) {
        final ZarrDatasetAttributes attributes = (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
        System.out.println(dataset + ":");
        System.out.println("  DType=\"" + attributes.getDType() + "\"");
        System.out.println("  dimensions=\"" + Arrays.toString(attributes.getDimensions()) + "\"");
    }

    private static void print(final List<Vertex> nodes) {
        nodes.forEach(System.out::println);
    }


    static void slize() {

        final long[] dims = {16, 9};
        final int[] data = new int[(int) Intervals.numElements(dims)];
        Arrays.setAll(data, i -> i);
        final RandomAccessibleInterval<IntType> img = ArrayImgs.ints(data, dims);

        final Slice<IntType> slice = Slice.slice(img, 0);

        System.out.println("slice.dimensionsAsLongArray() = " + Arrays.toString(slice.dimensionsAsLongArray()));
        final RandomAccess<IntType> a = slice.randomAccess();

        for (int i = 0; i < 3; i++) {
            System.out.println("a[ " + i + " ] = " + a.setPositionAndGet(i).get());
        }

        a.setPosition(1, 0);
        for (int i = 0; i < 9; i++) {
            slice.slicePosition().setPosition(i, 0);
            System.out.println(i + ": a[1] = " + a.get());
        }
    }

}
