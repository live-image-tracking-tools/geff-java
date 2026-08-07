package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedLongType;

class VarLengthWriteProperty<T extends Type<T>> extends VarLengthProperty<T> {

    private final VarLengthData<T> propertyData;

    VarLengthWriteProperty(
            final String identifier,
            final RandomAccessibleInterval<UnsignedLongType> propertyValues,
            final VarLengthData<T> propertyData,
            final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing, // optional
            final ElementIndex sharedElementIndex
    ) {
        super(identifier, propertyValues, propertyData, propertyMissing, sharedElementIndex);
        this.propertyData = propertyData;
    }

    @Override
    public void set(final GeffProperty<T> property) {
        if (isOptional()) {
            final boolean missing = property.isMissing();
            missingAccess.get().set(missing);
            if(missing)
                return;
        }

        final long offset = propertyData.size();
        valuesAccess.setPositionAndGet(0).set(offset);
        final Dimensions dims = property.dimensions();
        final int n = numDimensions();
        for (int i = 0; i < n; i++) {
            valuesAccess.setPositionAndGet(n - i).set(dims.dimension(i));
        }
        updateDataOffset(true);

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
}
