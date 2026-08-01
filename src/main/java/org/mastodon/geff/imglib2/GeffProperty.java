package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;

import java.util.function.Supplier;

public interface GeffProperty<T> {

    // ------------------------------------------------------------------------
    // Property metadata
    // ------------------------------------------------------------------------

    String identifier();

    /**
     * Returns {@code true} if this is a varlength property.
     */
    boolean isVarlength();

    /**
     * Returns {@code true} if values of this property may be missing.
     */
    boolean isOptional();

    /**
     * Returns the ImgLib2 type of this property
     */
    default T type() {
        return values().getType();
    }

    /**
     * Get the number of dimensions of this property.
     * (For example {@code 0} for scalar properties).
     */
    default int numDimensions() {
        return dimensions().numDimensions();
    }

    /**
     * Get the dimensions of this property.
     * <p>
     * For {@link #isVarlength() varlength} properties, the dimensions may be different for each node.
     * (However, the {@link #numDimensions()} number of dimensions never changes).
     */
    default Dimensions dimensions() {
        return values();
    }

    // TODO optional DType (from geff file, must map to imglib2 type's primitive)

    // ------------------------------------------------------------------------
    // Access to property values
    // ------------------------------------------------------------------------

    /**
     * Get the number of elements (nodes/edges) for which this property can be
     * queried. This defines the range valid {@link #elementIndex() element
     * indices}.
     */
    long numElements();

    /**
     * Get the {@code ElementIndex}.
     * <p>
     * The {@code ElementIndex} allows to get/set node/edge index.
     * <p>
     * Be Careful! Typically, this is shared among many {@code GeffProperty} in
     * a GeffProperties collection and should not be used directly.
     */
    ElementIndex elementIndex();

    /**
     * Returns {@code true} if the property is missing for the current node.
     */
    boolean isMissing();

    /**
     * Provides the {@code RandomAccess<T>} through which values of this
     * property can be accessed, as well as the {@code Dimensions} of the
     * property.
     * <p>
     * Note that currently, {@code values().randomAccess()} always return the
     * same {@code RandomAccess<T>} instance. It is possible to {@link
     * RandomAccess#copy copy} the RandomAccess, but note that The copy shares
     * the {@code ElementIndex} of this node. That is, copies can be used for
     * multithreaded access to the property values of the same node/edge but
     * cannot be positioned independently on different nodes.
     */
    RandomAccessibleInterval<T> values();

    void set(final GeffProperty<T> property);

    // ------------------------------------------------------------------------
    // shortcuts
    // ------------------------------------------------------------------------

    default T getAt() {
        final RandomAccess<T> a = values().randomAccess();
        return a.get();
    }

    default T getAt(final int p0) {
        final RandomAccess<T> a = values().randomAccess();
        a.setPosition(p0, 0);
        return a.get();
    }

    default T getAt(final int p0, final int p1) {
        final RandomAccess<T> a = values().randomAccess();
        a.setPosition(p0, 0);
        a.setPosition(p1, 1);
        return a.get();
    }

    default T getAt(final int... position) {
        return values().randomAccess().setPositionAndGet(position);
    }

    default <U> GeffProperty<U> convert(final Converter<T, U> converter, final Supplier<U> typeSupplier) {
        return new ConvertedProperty<>(this, converter, typeSupplier);
    }

    default <U> GeffProperty<U> convert(final Supplier<U> typeSupplier) {
        final U u = typeSupplier.get();
        if( u.getClass().equals(type().getClass())) {
            return Cast.unchecked(this);
        } else if( u instanceof RealType && type() instanceof RealType) {
            final Converter<?, ?> converter = RealTypeConverters.getConverter(
                    Cast.unchecked(type()),
                    Cast.unchecked(u));
            return new ConvertedProperty<>(this, Cast.unchecked(converter), typeSupplier);
        } else {
            throw new IllegalArgumentException("Unsupported types: " +
                    type().getClass().getSimpleName() + ", " +
                    u.getClass().getSimpleName() +
                    " (both must be RealType<?>)");
        }
    }

    // TODO: exploratory. probably should be overridden by implementations such that it is not frequently re-created.
    default GeffPropertyType propertyType() {
        final Dimensions dimensions = isVarlength()
                ? FinalDimensions.wrap(new long[numDimensions()])
                : new FinalDimensions(dimensions());
        return new GeffPropertyType(type().getClass(), isVarlength(), isOptional(), dimensions);
    }

    // ------------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------------

    static String toString(final GeffProperty<?> p) {
        final String optional = p.isOptional() ? ", optional" : "";
        final String dim;
        if ( p.isVarlength() ) {
            dim = ", varlength[" + p.numDimensions() + "]";
        } else if ( p.numDimensions() == 0 ) {
            dim = ", scalar";
        } else {
            dim = ", dimensions={" + Intervals.toString(p.dimensions()) + "}";
        }
        return "GeffProperty<" + p.type().getClass().getSimpleName() + ">{" +
                "identifier=\"" + p.identifier() + "\"" + dim + optional + '}';
    }
}
