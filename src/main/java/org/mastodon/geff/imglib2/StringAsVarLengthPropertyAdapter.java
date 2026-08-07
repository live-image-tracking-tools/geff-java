package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import java.nio.charset.StandardCharsets;

class StringAsVarLengthPropertyAdapter<O> implements PropertyAdapter<O, UnsignedByteType> {

    private final PropertyAdapter<O, String> parent;

    private String currentValue;
    private byte[] data = null;
    private long[] dataLength = new long[1];
    private final PropertyRAI<UnsignedByteType> values;

    StringAsVarLengthPropertyAdapter(final PropertyAdapter<O, String> parent) {
        assert !parent.isVarlength();
        assert parent.numDimensions() == 0;
        this.parent = parent;
        final Dimensions dimensions = FinalDimensions.wrap(dataLength);
        values = new PropertyRAI<>(() -> {
            updateData();
            return dimensions;
        }, new RA());
    }

    @Override
    public PropertyAdapter<O, UnsignedByteType> adapt(O obj) {
        parent.adapt(obj);
        return this;
    }

    @Override
    public String identifier() {
        return parent.identifier();
    }

    @Override
    public boolean isVarlength() {
        return true;
    }

    @Override
    public boolean isOptional() {
        return parent.isOptional();
    }

    @Override
    public long numElements() {
        return parent.numElements();
    }

    @Override
    public ElementIndex elementIndex() {
        return parent.elementIndex();
    }

    @Override
    public boolean isMissing() {
        return parent.isMissing();
    }

    @Override
    public RandomAccessibleInterval<UnsignedByteType> values() {
        return values;
    }

    @Override
    public void set(GeffProperty<UnsignedByteType> property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }

    private void updateData() {
        final String value = parent.getAt();
        if (currentValue != value) {
            currentValue = value;
            data = value.getBytes(StandardCharsets.UTF_8);
            dataLength[0] = data.length;
        }
    }

    private class RA extends Point implements RandomAccess<UnsignedByteType> {

        private final UnsignedByteType type;

        RA() {
            super(1);
            type = new UnsignedByteType();
        }

        @Override
        public UnsignedByteType getType() {
            return type;
        }

        @Override
        public UnsignedByteType get() {
            updateData();
            type.setByte(data[getIntPosition(0)]);
            return type;
        }

        @Override
        public RandomAccess<UnsignedByteType> copy() {
            final RA copy = new RA();
            copy.setPosition(this);
            return copy;
        }
    }
}
