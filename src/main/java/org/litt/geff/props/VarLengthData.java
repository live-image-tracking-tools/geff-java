package org.litt.geff.props;

import net.imglib2.RandomAccessibleInterval;

/**
 * A growable 1D Img, for storing var-length data when writing.
 * @param <T>
 */
public interface VarLengthData<T> extends RandomAccessibleInterval<T> {

    /**
     * Grow to size {@code s}.
     * Does nothing if current {@link #size()} {@code >= s}.
     */
    void growTo(final long s);

    /**
     * Grow by {@code s} elements.
     */
    default void growBy(final long s) {
        growTo(size() + s);
    }
}
