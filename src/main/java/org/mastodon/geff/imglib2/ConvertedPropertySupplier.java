package org.mastodon.geff.imglib2;

import net.imglib2.converter.Converter;

import java.util.function.Supplier;

class ConvertedPropertySupplier<O, S, T> extends ConvertedProperty<S, T> implements PropertySupplier<O, T> {

    private final PropertySupplier<O, S> parent;

    ConvertedPropertySupplier(final PropertySupplier<O, S> parent, final Converter<S, T> converter, final Supplier<T> typeSupplier) {
        super(parent, converter, typeSupplier);
        this.parent = parent;
    }

    @Override
    public PropertySupplier<O, T> update(O obj) {
        parent.update(obj);
        return this;
    }
}
