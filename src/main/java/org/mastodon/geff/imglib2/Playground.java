package org.mastodon.geff.imglib2;

import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.RawCompression;
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
    record Vertex(long id) {
    }


    // -----------------------------------------------------------------------
    // geff-java side
    //
    static class NodeData
    {
        private final long numNodes;

        private final List<SlicePosition> slicePositions = new ArrayList<>();
        private final Slice<UnsignedLongType> nodeIdSlice;

        private final RandomAccess<UnsignedLongType> id;
        private final HashMap<String, RandomAccess<?>> nodeProperyAccesses = new HashMap<>();

        // TODO:                          generic int type here?
        NodeData(RandomAccessibleInterval<UnsignedLongType> nodeIdData) {

            numNodes = nodeIdData.dimension(0);
            nodeIdSlice = Slice.slice(nodeIdData, 0);

            slicePositions.add(nodeIdSlice.slicePosition());

            id = nodeIdSlice.randomAccess();
            nodeProperyAccesses.put("id", id);
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

    }



    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        final String path = "cross-language-tests/data/basic_3d_original.zarr";

        final List<Vertex> nodes = new ArrayList<>();

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {

            final String dataset = "nodes/ids";
            final ZarrDatasetAttributes attributes = (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
            System.out.println("attributes.getDType() = " + attributes.getDType());

            final RandomAccessibleInterval<UnsignedLongType> ids = N5Utils.open(n5, dataset);
            final NodeData nodeData = new NodeData(ids);

            final Sampler<UnsignedLongType> id = Cast.unchecked(nodeData.getPropertyAccess("id"));
            for (int i = 0; i < nodeData.size(); i++) {
                nodeData.nodeIndex(i);
                nodes.add(new Vertex(id.get().get()));
            }

            print(nodes);
        }

        // write
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {

            final String dataset = "nodes/ids2";
            final ZarrDatasetAttributes attributes = new ZarrDatasetAttributes(
                    new long[]{nodes.size()},
                    new int[]{nodes.size()},
                    new DType("<u8", null),
                    new ZstandardCompression(0),
                    true,
                    "0"
            );

            final RandomAccessibleInterval<UnsignedLongType> ids = ArrayImgs.unsignedLongs(nodes.size());
            final NodeData nodeData = new NodeData(ids);

            final Sampler<UnsignedLongType> id = Cast.unchecked(nodeData.getPropertyAccess("id"));
            for (int i = 0; i < nodes.size(); i++) {
                nodeData.nodeIndex(i);
                id.get().set(nodes.get(i).id());
            }

            n5.createDataset(dataset, attributes);
            N5Utils.saveRegion(ids,n5,dataset, attributes);
        }

    }

    private static void print(List<Vertex> nodes) {
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
