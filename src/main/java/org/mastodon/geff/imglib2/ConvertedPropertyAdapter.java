package org.mastodon.geff.imglib2;

import net.imglib2.converter.Converter;

import java.util.function.Supplier;

class ConvertedPropertyAdapter<O, S, T> extends ConvertedProperty<S, T> implements PropertyAdapter<O, T> {

    private final PropertyAdapter<O, S> parent;

    ConvertedPropertyAdapter(final PropertyAdapter<O, S> parent, final Converter<S, T> converter, final Supplier<T> typeSupplier) {
        super(parent, converter, typeSupplier);
        this.parent = parent;
    }

    @Override
    public PropertyAdapter<O, T> adapt(O obj) {
        parent.adapt(obj);
        return this;
    }
}
