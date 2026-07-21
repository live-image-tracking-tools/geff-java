package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccess;

import java.util.function.Supplier;

class ConvertedProperty<S, T> implements GeffProperty<T> {

    private final GeffProperty<S> parent;
    private final PropertyRAI<T> values;

    ConvertedProperty(final GeffProperty<S> parent, final Converter<S, T> converter, final Supplier<T> typeSupplier) {
        this.parent = parent;

        final RandomAccessibleInterval<S> parentValues = parent.values();
        final RandomAccess<T> randomAccess = new ConvertedRandomAccess<>(parentValues.randomAccess(), converter, typeSupplier);
        values = new PropertyRAI<>(parentValues, randomAccess );
    }

    @Override
    public String identifier() {
        return parent.identifier();
    }

    @Override
    public boolean isVarlength() {
        return parent.isVarlength();
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
    public RandomAccessibleInterval<T> values() {
        return values;
    }

    @Override
    public void set(GeffProperty<T> property) {
        throw new UnsupportedOperationException("ConvertedProperty is read-only");
    }
}
