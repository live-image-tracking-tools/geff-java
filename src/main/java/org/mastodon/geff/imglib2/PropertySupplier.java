package org.mastodon.geff.imglib2;

import net.imglib2.converter.Converter;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;

import java.util.function.Supplier;

public interface PropertySupplier<O, T> extends GeffProperty<T> {
    PropertySupplier<O, T> update(O obj);

    default <U> PropertySupplier<O, U> convert(final Converter<T, U> converter, final Supplier<U> typeSupplier) {
        return new ConvertedPropertySupplier<>(this, converter, typeSupplier);
    }

    default <U> PropertySupplier<O, U> convert(final Supplier<U> typeSupplier) {
        final U u = typeSupplier.get();
        if( u.getClass().equals(type().getClass())) {
            return Cast.unchecked(this);
        } else if( u instanceof RealType && type() instanceof RealType) {
            final Converter<?, ?> converter = RealTypeConverters.getConverter(
                    Cast.unchecked(type()),
                    Cast.unchecked(u));
            return new ConvertedPropertySupplier<>(this, Cast.unchecked(converter), typeSupplier);
        } else {
            throw new IllegalArgumentException("Unsupported types: " +
                    type().getClass().getSimpleName() + ", " +
                    u.getClass().getSimpleName() +
                    " (both must be RealType<?>)");
        }
    }

}
