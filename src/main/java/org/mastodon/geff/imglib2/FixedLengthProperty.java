package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.Type;

class FixedLengthProperty<T extends Type<T>> implements GeffProperty<T> {

    private final String identifier;
    private final boolean isOptional;

    private final long numElements;
    private final ElementIndex elementIndex;

    private final RandomAccess<? extends BooleanType<?>> missingAccess;
    private final PropertyRAI<T> values;

    FixedLengthProperty(
            final String identifier,
            final RandomAccessibleInterval<T> propertyValues,
            final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing, // optional
            final ElementIndex sharedElementIndex
    ) {
        this.identifier = identifier;
        this.elementIndex = sharedElementIndex;

        numElements = propertyValues.dimension(propertyValues.numDimensions() - 1);
        final PropertySlice<T> valuesSlice = new PropertySlice<>(propertyValues, elementIndex);
        final RandomAccess<T> valuesAccess = valuesSlice.randomAccess();
        final FinalDimensions valuesDimensions = new FinalDimensions(valuesSlice);
        values = new PropertyRAI<>(valuesDimensions, valuesAccess);

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
    public long numElements() {
        return numElements;
    }

    @Override
    public ElementIndex elementIndex() {
        return elementIndex;
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

        final int n = numDimensions();
        if (n == 0) {
            values().randomAccess().get().set(property.getAt());
        } else if (n == 1) {
            final int w = (int) dimensions().dimension(0);
            for (int x = 0; x < w; x++)
                getAt(x).set(property.getAt(x));
        } else if (n == 2) {
            final int w = (int) dimensions().dimension(0);
            final int h = (int) dimensions().dimension(1);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    getAt(x, y).set(property.getAt(x, y));
        } else {
            final Cursor<T> s = property.values().cursor();
            final Cursor<T> t = values().cursor();
            while (s.hasNext())
                t.next().set(s.next());
        }
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }
}
