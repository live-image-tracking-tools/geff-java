package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.util.Cast;
import net.imglib2.util.IntervalIndexer;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import static org.mastodon.geff.imglib2.FixedLengthProperty.writeDataset;

class VarLengthProperty<T extends Type<T>> implements GeffProperty<T> {

    private final String identifier;
    private final boolean isOptional;

    private final long numElements;
    private final ElementIndex elementIndex;

    protected final RandomAccess<UnsignedLongType> valuesAccess;
    protected final RandomAccess<? extends BooleanType<?>> missingAccess;

    // dataOffset[0] is the elementIndex for which dataOffset and dataDimensions are currently configured
    // dataOffset[1] is the current offset into the data array
    protected final long[] dataOffset;
    private final long[] dataDimensions;
    private final PropertyRAI<T> values;

    final RandomAccessibleInterval<UnsignedLongType> valuesRAI; // for IO
    final RandomAccessibleInterval<? extends BooleanType<?>> missingRAI; // for IO
    final RandomAccessibleInterval<T> dataRAI; // for IO

    VarLengthProperty(
            final String identifier,
            final RandomAccessibleInterval<UnsignedLongType> propertyValues,
            final RandomAccessibleInterval<T> propertyData,
            final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing, // optional
            final ElementIndex sharedElementIndex
    ) {
        this.identifier = identifier;
        this.elementIndex = sharedElementIndex;

        numElements = propertyValues.dimension(propertyValues.numDimensions() - 1);
        valuesAccess = new PropertySlice<>(propertyValues, elementIndex).randomAccess();

        if (propertyMissing != null) {
            if (propertyMissing.numDimensions() != 1)
                throw new IllegalArgumentException("The \"missing\" array must be 1-dimensional");
            if (propertyMissing.dimension(0) != numElements)
                throw new IllegalArgumentException("The \"missing\" array must contain the same number of rows as the \"values\" array");
            final PropertySlice<? extends BooleanType<?>> missingSlice = new PropertySlice<>(propertyMissing, elementIndex);
            missingAccess = missingSlice.randomAccess();
            isOptional = true;
        } else {
            missingAccess = null;
            isOptional = false;
        }

        dataOffset = new long[]{-1, -1};
        final int numDimensions = (int) propertyValues.dimension(0) - 1;
        dataDimensions = new long[numDimensions];
        final Dimensions dimensions = FinalDimensions.wrap(dataDimensions);
        final RandomAccess<T> randomAccess = new RA(propertyData.randomAccess());
        values = new PropertyRAI<>(() -> {
            updateDataOffset();
            return dimensions;
        }, randomAccess);

        // keep these around for serialization ...
        valuesRAI = propertyValues;
        missingRAI = propertyMissing;
        dataRAI = propertyData;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public boolean isVarlength() {
        return true;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public long numElements() {
        return numElements;
    }

    @Override
    public ElementIndex elementIndex() {
        return elementIndex;
    }

    protected void updateDataOffset() {
        final long index = elementIndex.index();
        final long previousIndex = dataOffset[0];
        if (previousIndex != index) {
            dataOffset[0] = index;
            dataOffset[1] = valuesAccess.setPositionAndGet(0).get();
            final int n = dataDimensions.length;
            for (int i = 0; i < n; i++) {
                dataDimensions[n - 1 - i] = valuesAccess.setPositionAndGet(i + 1).get();
            }
        }
    }

    @Override
    public boolean isMissing() {
        return isOptional && missingAccess.get().get();
    }

    @Override
    public RandomAccessibleInterval<T> values() {
        return values;
    }

    @Override
    public void set(final GeffProperty<T> property) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }

    private class RA extends Point implements RandomAccess<T> {
        private final RandomAccess<T> dataAccess;

        RA(final RandomAccess<T> dataAccess) {
            super(dataDimensions.length);
            this.dataAccess = dataAccess;
        }

        @Override
        public T get() {
            updateDataOffset();
            final long index = dataOffset[1] + IntervalIndexer.positionToIndex(position, dataDimensions);
            return dataAccess.setPositionAndGet(index);
        }

        @Override
        public RA copy() {
            return new RA(dataAccess.copy());
        }
    }





    // ------------------------------------------------------------------------
    // TODO: move to IoUtils class?

    // TODO: might add int chunkSize argument later (chunking along elementIndex axis only).
    public void write(
            final N5ZarrWriter n5,
            final ElementType elementType, // TODO: maybe add to GeffProperty?
            final DType optionalDType, // optional, will use default DType corresponding to type()
            final Compression compression,
            final String geffGroup) { // geffGroup is optional...

        if (!(type() instanceof NativeType))
            throw new IllegalArgumentException("Only NativeType supported for writing. (" + type().getClass().getSimpleName() + ")");

        final DType dType = optionalDType != null
                ? optionalDType
                : GeffPropertySpec.defaultDType(Cast.unchecked(type()));

        // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
        final String group = GeffPropertySpecs.normalizeGroupPath(geffGroup);

        final String propsGroup = group + elementType.elementGroup() + "/props/" + identifier;
        final DType uint64 = new DType("<u8", null);
        writeDataset(n5, propsGroup + "/values", uint64, compression, Cast.unchecked(valuesRAI));
        writeDataset(n5, propsGroup + "/data", dType, compression, Cast.unchecked(dataRAI));
        if (isOptional) {
            final RandomAccessibleInterval<UnsignedByteType> missing_uint8 = Converters.convert(missingRAI,
                    (b, u) -> u.set(b.get() ? 1 : 0),
                    new UnsignedByteType());
            final DType uint8 = new DType("|b1", null);
            writeDataset(n5, propsGroup + "/missing", uint8, compression, missing_uint8);
        }
    }
}
