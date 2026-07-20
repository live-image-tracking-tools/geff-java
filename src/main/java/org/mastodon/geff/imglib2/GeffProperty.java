package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.util.Intervals;

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
        return randomAccess().getType();
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
    Dimensions dimensions();

    // TODO optional DType (from geff file, must map to imglib2 type's primitive)

    // ------------------------------------------------------------------------
    // Access to property values
    // ------------------------------------------------------------------------

    /**
     * Get the number of elements (nodes/edges) for which this property can be
     * queried. This defines the range valid {@link #elementIndex() element
     * indices}.
     */
    long size();

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
     * Get the {@code RandomAccess<T>} through which values of this property can be accessed.
     * <p>
     * Note that currently, implementations of this method always return the
     * same {@code RandomAccess<T>} instance. This is for convenience so that we
     * don't have to pass around pairs of ({@code GeffProperty<T>} and  {@code
     * RandomAccess<T>} all the time.
     * <p>
     * It is possible to {@link RandomAccess#copy copy} the RandomAccess, but note that
     * The copy shares the {@code ElementIndex} of this node. That is, copies
     * can be used for multithreaded access to the property values of the same
     * node/edge but cannot be positioned independently on different nodes.
     */
    RandomAccess<T> randomAccess();

    /**
     * Returns {@code true} if the property is missing for the current node.
     */
    boolean isMissing();

    // ------------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------------

//    /**
//     * Create independent copy of this {@code }GeffProperty}.
//     * <p>
//     * The copy shares the {@code ElementIndex} of this node. That is, copies
//     * can be used for multithreaded access to the property values of the same
//     * node/edge but cannot be positioned independently on different nodes.
//     */
//    GeffProperty<T> copy();
    // --> We can just use randomAccess().copy() ... this will have the same effect
    // TODO copy() with a provided ElementIndex ?

    static String toString(final GeffProperty<?> p) {
        final String optional = p.isOptional() ? ", optional" : "";
        final String dim;
        if ( p.isVarlength() ) {
            dim = ", varlength[" + p.numDimensions() + "]";
        } else if ( p.numDimensions() == 0 ) {
            dim = ", scalar";
        } else {
            dim = ", dimensions=" + Intervals.toString(p.dimensions());
        }
        return "GeffProperty<" + p.type().getClass().getSimpleName() + ">{" +
                "identifier=\"" + p.identifier() + "\"" + dim + optional + '}';
    }
}
