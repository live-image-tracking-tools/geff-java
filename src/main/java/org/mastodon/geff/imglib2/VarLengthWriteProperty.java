package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.util.IntervalIndexer;

class VarLengthWriteProperty<T extends NativeType<T>> implements GeffProperty<T> {

    private final String identifier;
    private final boolean isOptional;

    private final long numElements;
    private final ElementIndex elementIndex;

    private final RandomAccess<UnsignedLongType> valuesAccess;
    private final RandomAccess<? extends BooleanType<?>> missingAccess;
    private final VarLengthData<T, ?> propertyData;

    // dataOffset[0] is the elementIndex for which dataOffset and dataDimensions are currently configured
    // dataOffset[1] is the current offset into the data array
    private final long[] dataOffset;
    private final long[] dataDimensions;
    private final PropertyRAI<T> values;

    VarLengthWriteProperty(
            final String identifier,
            final RandomAccessibleInterval<UnsignedLongType> propertyValues,
            final VarLengthData<T, ?> propertyData,
            final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing, // optional
            final ElementIndex sharedElementIndex
    ) {
        this.identifier = identifier;
        this.propertyData = propertyData;
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

    private void updateDataOffset() {
        final long index = elementIndex.index();
        final long previousIndex = dataOffset[0];
        if (previousIndex != index) {
            dataOffset[0] = index;
            dataOffset[1] = valuesAccess.setPositionAndGet(0).get();
            final int n = dataDimensions.length;
            for (int i = 1; i < n + 1; i++) {
                dataDimensions[n - i] = valuesAccess.setPositionAndGet(i).get();
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
        if (isOptional) {
            final boolean missing = property.isMissing();
            missingAccess.get().set(missing);
            if(missing)
                return;
        }

        final long index = elementIndex.index();
        final long offset = propertyData.size();
        dataOffset[0] = index;
        dataOffset[1] = offset;
        final int n = dataDimensions.length;
        final Dimensions dims = property.dimensions();
        for (int i = 1; i < n + 1; i++) {
            final long dim = dims.dimension(i);
            dataDimensions[n - i] = dim;
            valuesAccess.setPositionAndGet(i).set(dim);
        }

        // TODO: reuse Cursor instances:
        //   Override cursor() in values() implementations.
        //   PropertyRAI
        //   Wrappers.ScalarRandomAccessibleInterval
        //
        final int size = (int) property.values().size();
        propertyData.growBy(size);
        final Cursor<T> s = property.values().cursor();
        final Cursor<T> t = values().cursor();
        while (s.hasNext())
            t.next().set(s.next());
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
}
