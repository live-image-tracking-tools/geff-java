package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.imglib2.Slice.SlicePosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VarLengthPlayground {


    // -----------------------------------------------------------------------
    // client-side
    //
    record Vertex(long id, double x, int t) {
    }


    // -----------------------------------------------------------------------
    // geff-java side
    //
    static class VarLengthData< T >
    {
        private final long numNodes;
        private final int numDimensions;

        private final List<SlicePosition> slicePositions = new ArrayList<>();

        private final Slice<UnsignedLongType> valuesSlice;
        private final Slice<BoolType> missingSlice;

        private final RandomAccess<UnsignedLongType> valuesAccess;
        private final RandomAccess<BoolType> missingAccess;

        private final RandomAccessibleInterval<T> data;

        private final long[] dataOffset;
        private final long[] dataDimensions;
        private final RA<T> ra;

        VarLengthData(
                final RandomAccessibleInterval<UnsignedLongType> values,
                final RandomAccessibleInterval<BoolType> missing,
                final RandomAccessibleInterval<T> data
        ) {
            // TODO: verify values.numDimensions() == 2
            // TODO: verify missing.numDimensions() == 1

            numNodes = values.dimension(1);
            numDimensions = (int) values.dimension(0) - 1;

            // TODO: verify missing.dimension(0) == numNodes

            valuesSlice = Slice.slice(values, 1);
            missingSlice = Slice.slice(missing, 0);

            slicePositions.add(valuesSlice.slicePosition());
            slicePositions.add(missingSlice.slicePosition());

            valuesAccess = valuesSlice.randomAccess();
            missingAccess = missingSlice.randomAccess();

            this.data = data;
            dataOffset = new long[1];
            dataDimensions = new long[numDimensions];
            ra = new RA<>(data.randomAccess(), dataOffset, dataDimensions);
        }

        long size() {
            return numNodes;
        }

        int numDimensions() {
            return numDimensions;
        }

        void nodeIndex(long index) {
            if (index < 0 || index >= numNodes)
                throw new IndexOutOfBoundsException(index + "(numNodes=" + numNodes + ")");
            slicePositions.forEach(p -> p.setPosition(index, 0));

            dataOffset[0] = valuesAccess.setPositionAndGet(0).get();
            for (int i = 1; i < numDimensions + 1; i++) {
                dataDimensions[numDimensions - i] = valuesAccess.setPositionAndGet(i).get();
            }
        }

        Dimensions dimensions() {
            return new FinalDimensions(ra.dataDimensions);
        }

        RandomAccess<T> randomAccess() {
            return ra;
        }

        void print() {

            final boolean isMissing = missingAccess.get().get();

            System.out.println("  missing = " + isMissing);
            System.out.println("  offset = " + ra.dataOffset[0]);
            System.out.println("  shape = " + Arrays.toString(ra.dataDimensions));
        }

        private static class RA< T > extends Point implements RandomAccess< T >
        {
            private final RandomAccess< T > dataAccess;

            private final long[] dataOffset;

            private final long[] dataDimensions;

            RA(final RandomAccess< T > dataAccess, long[] dataOffset, long[] dataDimensions)
            {
                super(dataDimensions.length);
                this.dataAccess = dataAccess;
                this.dataOffset = dataOffset;
                this.dataDimensions = dataDimensions;
            }

            @Override
            public T get()
            {
                final long index = dataOffset[0] + IntervalIndexer.positionToIndex(position, dataDimensions);
                return dataAccess.setPositionAndGet( index );
            }

            @Override
            public RandomAccess< T > copy()
            {
                return new RA<>( dataAccess.copy(), dataOffset, dataDimensions );
            }
        }

    }




    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        final String path = "cross-language-tests/data/varlength_original.zarr";

        final List<Vertex> nodes = new ArrayList<>();

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {


            final String valuesDataset = "nodes/props/var_length/values";
            final String missingDataset = "nodes/props/var_length/missing";
            final String dataDataset = "nodes/props/var_length/data";

            printAttributes(n5, valuesDataset);
            printAttributes(n5, missingDataset);
            printAttributes(n5, dataDataset);

            final RandomAccessibleInterval<UnsignedLongType> values = N5Utils.open(n5, valuesDataset);
            final RandomAccessibleInterval<BoolType> missings = Converters.convert2(
                    N5Utils.<UnsignedByteType>open(n5, missingDataset),
                    (u, b) -> b.set(u.get() != 0),
                    BoolType::new);
            final RandomAccessibleInterval<UnsignedLongType> data = N5Utils.open(n5, dataDataset);

            VarLengthData<UnsignedLongType> varlength = new VarLengthData<>(values, missings, data);
            System.out.println("varlength.size() = " + varlength.size());
            System.out.println("varlength.numDimensions() = " + varlength.numDimensions());

            for (int i = 0; i < varlength.size(); i++) {
                System.out.println("node " + i + ":");
                varlength.nodeIndex(i);
                varlength.print();

                final RandomAccess<UnsignedLongType> ra = varlength.randomAccess();
                RandomAccessibleInterval<Localizable> positions = Intervals.positions(new FinalInterval(varlength.dimensions()));
                positions.forEach(r -> {
                    System.out.println(r + ": " + ra.setPositionAndGet(r));
                });
            }
        }
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

}
