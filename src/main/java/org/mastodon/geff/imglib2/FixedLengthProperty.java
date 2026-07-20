package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;

class FixedLengthProperty<T> implements GeffProperty<T> {

    private final String identifier;
    private final boolean isOptional;

    private final long numElements;
    private final ElementIndex elementIndex;
    private final Dimensions dimensions;

    private final RandomAccess<T> valuesAccess;
    private final RandomAccess<BooleanType<?>> missingAccess;

    FixedLengthProperty(
            final String identifier,
            final RandomAccessibleInterval<T> propertyValues,
            final RandomAccessibleInterval<BooleanType<?>> propertyMissing, // optional
            final ElementIndex sharedElementIndex
    ) {
        this.identifier = identifier;
        this.elementIndex = sharedElementIndex;

        numElements = propertyValues.dimension(propertyValues.numDimensions() - 1);
        final PropertySlice<T> valuesSlice = new PropertySlice<>(propertyValues, elementIndex);
        dimensions = new FinalDimensions(valuesSlice);
        valuesAccess = valuesSlice.randomAccess();

        if (propertyMissing != null) {
            if (propertyMissing.numDimensions() != 1)
                throw new IllegalArgumentException("The \"missing\" array must be 1-dimensional");
            if (propertyMissing.dimension(0) != numElements)
                throw new IllegalArgumentException("The \"missing\" array must contain the same number of rows as the \"values\" array");
            final PropertySlice<BooleanType<?>> missingSlice = new PropertySlice<>(propertyMissing, elementIndex);
            missingAccess = missingSlice.randomAccess();
            isOptional = true;
        } else {
            missingAccess = null;
            isOptional = false;
        }
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public boolean isVarlength() {
        return false;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public Dimensions dimensions() {
        return dimensions;
    }

    @Override
    public long size() {
        return numElements;
    }

    @Override
    public ElementIndex elementIndex() {
        return elementIndex;
    }

    @Override
    public RandomAccess<T> randomAccess() {
        return valuesAccess;
    }

    @Override
    public boolean isMissing() {
        return isOptional && missingAccess.get().get();
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }
}
