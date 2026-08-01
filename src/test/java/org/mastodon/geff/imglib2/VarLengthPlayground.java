package org.mastodon.geff.imglib2;

import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import java.util.Arrays;

public class VarLengthPlayground {

    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        final String path = "cross-language-tests/data/varlength_original.zarr";

        // read and populate VarLengthWriteProperty
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {


            final String valuesDataset = "nodes/props/var_length/values";
            final String missingDataset = "nodes/props/var_length/missing";
            final String dataDataset = "nodes/props/var_length/data";

            printAttributes(n5, valuesDataset);
            printAttributes(n5, missingDataset);
            printAttributes(n5, dataDataset);

            final ElementIndex readIndex = new ElementIndex();
            final GeffProperty<UnsignedLongType> readProperty;
            {
                final RandomAccessibleInterval<UnsignedLongType> values = N5Utils.open(n5, valuesDataset);
                final RandomAccessibleInterval<BoolType> missings = Converters.convert2(
                        N5Utils.<UnsignedByteType>open(n5, missingDataset),
                        (u, b) -> b.set(u.get() != 0),
                        BoolType::new);
                final RandomAccessibleInterval<UnsignedLongType> data = N5Utils.open(n5, dataDataset);
                readProperty = new VarLengthProperty<>("var_length", values, data, missings, readIndex);
            }

            final long numDimensions = readProperty.numDimensions();
            final long numElements = readProperty.numElements();

            final ElementIndex writeIndex = new ElementIndex();
            final RandomAccessibleInterval<UnsignedLongType> writeValues = ArrayImgs.unsignedLongs(numDimensions + 1, numElements);
            final RandomAccessibleInterval<? extends BooleanType<?>> writeMissings = ArrayImgs.booleans(numElements);
            final VarLengthData<UnsignedLongType> writeData = new VarLengthDataImpl<>(new UnsignedLongType());
            final GeffProperty<UnsignedLongType> writeProperty = new VarLengthWriteProperty<>("var_length", writeValues, writeData, writeMissings, writeIndex);

            for (int i = 0; i < numElements; i++) {
                readIndex.set(i);
                System.out.println("node " + i + ":");
                System.out.println("  missing = " + readProperty.isMissing());
                System.out.println("  dimensions = " + Arrays.toString(readProperty.dimensions().dimensionsAsLongArray()));


                writeIndex.set(i);
                writeProperty.set(readProperty);

                System.out.println("  (w)missing = " + writeProperty.isMissing());
                System.out.println("  (w)dimensions = " + Arrays.toString(writeProperty.dimensions().dimensionsAsLongArray()));

                {
                    final RandomAccess<UnsignedLongType> ra = readProperty.values().randomAccess();
                    RandomAccessibleInterval<Localizable> positions = Intervals.positions(new FinalInterval(readProperty.dimensions()));
                    positions.forEach(r -> {
                        System.out.println("R" + r + ": " + ra.setPositionAndGet(r));
                    });
                }
                {
                    final RandomAccess<UnsignedLongType> ra = writeProperty.values().randomAccess();
                    RandomAccessibleInterval<Localizable> positions = Intervals.positions(new FinalInterval(writeProperty.dimensions()));
                    positions.forEach(r -> {
                        System.out.println("W" + r + ": " + ra.setPositionAndGet(r));
                    });
                }
            }

            final BloscCompression compression = new BloscCompression("lz4", 5, 1, 0, 0);
            IoUtils.writeProperty(n5, (VarLengthProperty<?>) writeProperty, ElementType.NODE, null, compression, null);
        }
    }

    private static void printAttributes(final N5Reader n5, final String dataset) {
        final ZarrDatasetAttributes attributes = (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
        System.out.println(dataset + ":");
        System.out.println("  DType=\"" + attributes.getDType() + "\"");
        System.out.println("  dimensions=\"" + Arrays.toString(attributes.getDimensions()) + "\"");
    }


}
