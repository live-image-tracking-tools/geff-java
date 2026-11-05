package org.litt.geff.props;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedLongType;

public class VarLengthWriteProperty<T extends Type<T>> extends VarLengthProperty<T> {

    private final VarLengthData<T> propertyData;

	/*
	 * Reusable cursors for set(). They are only valid for the same
	 * RandomAccessibleInterval they were created for; set() may be called with
	 * varying source properties, in which case the source cursor is re-created.
	 * The target values() RAI is stable, so its cursor is reused throughout the
	 * lifetime of this property.
	 */

    private Cursor<T> sourceCursor;

    private Cursor<T> targetCursor;

    private Object sourceCursorSource;

    public VarLengthWriteProperty(
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

        final int size = (int) property.values().size();
        propertyData.growBy(size);
        if (targetCursor == null)
            targetCursor = values().cursor();
        else
            targetCursor.reset();
        final RandomAccessibleInterval<T> source = property.values();
        if (sourceCursor == null || sourceCursorSource != source) {
            sourceCursor = source.cursor();
            sourceCursorSource = source;
        } else {
            sourceCursor.reset();
        }
        while (sourceCursor.hasNext())
            targetCursor.next().set(sourceCursor.next());
    }
}
