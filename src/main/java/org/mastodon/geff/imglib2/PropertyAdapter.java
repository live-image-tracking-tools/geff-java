package org.mastodon.geff.imglib2;

import net.imglib2.converter.Converter;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;

import java.util.function.Supplier;

/**
 * Exposes information about objects of type {@code O} as a {@code GeffProperty}.
 * <p>
 * The {@link #adapt(Object)} method sets the instance of {@code O} whose information should be reflected.
 * <p>
 * An example of this would be a {@code Vertex} class with a {@code double X}
 * coordinate. The X coordinate could be exposed as a scalar {@code
 * GeffProperty<DoubleType>} using a {@code PropertyAdapter}.
 *
 * @param <O> type of object from which property values should be extracted
 * @param <T> value type of the resulting property
 */
public interface PropertyAdapter<O, T> extends GeffProperty<T> {

    /**
     * Sets the instance of {@code O} whose information should be reflected via this {@code PropertyAdapter}.
     *
     * @param obj the instance to proxy
     * @return {@code this}
     */
    PropertyAdapter<O, T> adapt(O obj);

    default <U> PropertyAdapter<O, U> convert(final Converter<T, U> converter, final Supplier<U> typeSupplier) {
        return new ConvertedPropertyAdapter<>(this, converter, typeSupplier);
    }

    default <U> PropertyAdapter<O, U> convert(final Supplier<U> typeSupplier) {
        final U u = typeSupplier.get();
        if( u.getClass().equals(type().getClass())) {
            return Cast.unchecked(this);
        } else if( u instanceof RealType && type() instanceof RealType) {
            final Converter<?, ?> converter = RealTypeConverters.getConverter(
                    Cast.unchecked(type()),
                    Cast.unchecked(u));
            return new ConvertedPropertyAdapter<>(this, Cast.unchecked(converter), typeSupplier);
        } else {
            throw new IllegalArgumentException("Unsupported types: " +
                    type().getClass().getSimpleName() + ", " +
                    u.getClass().getSimpleName() +
                    " (both must be RealType<?>)");
        }
    }

}
