package org.mastodon.geff.imglib2;

import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import java.nio.charset.StandardCharsets;

class VarLengthAsStringProperty implements GeffProperty<String> {

    private final GeffProperty<UnsignedByteType> parent;
    private final PropertyRAI<String> values;

    VarLengthAsStringProperty(final GeffProperty<UnsignedByteType> parent) {

        assert parent.isVarlength();
        assert parent.numDimensions() == 1;

        this.parent = parent;
        values = new PropertyRAI<>(new FinalDimensions(), new RA());
    }

    @Override
    public String identifier() {
        return parent.identifier();
    }

    @Override
    public boolean isVarlength() {
        return false;
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
    public RandomAccessibleInterval<String> values() {
        return values;
    }

    @Override
    public void set(GeffProperty<String> property) {
        throw new UnsupportedOperationException("VarLengthAsStringProperty is read-only");
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }

    private class RA extends Point implements RandomAccess<String> {

        private final RandomAccess<UnsignedByteType> a;

        RA() {
            super(0);
            a = parent.values().randomAccess().copy();
        }

        @Override
        public String getType() {
            return "";
        }

        @Override
        public String get() {
            final int len = (int) parent.values().dimension(0);
            byte[] data = new byte[len];
            a.setPosition(0, 0);
            for (int j = 0; j < len; j++) {
                data[j] = a.get().getByte();
                a.fwd(0);
            }
            return new String(data, StandardCharsets.UTF_8);
        }

        @Override
        public RandomAccess<String> copy() {
            return new RA();
        }
    }
}
