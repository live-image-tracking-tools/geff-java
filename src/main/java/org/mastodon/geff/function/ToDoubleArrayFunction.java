package org.mastodon.geff.function;

/**
 * Represents a function that produces a double-array result. This is the
 * {@code double} array-producing primitive specialization for {@link Function}.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #applyAsDoubleArray(Object)}.
 *
 * @param <T> the type of the input to the function
 *
 * @see Function
 * @since 1.8
 */
@FunctionalInterface
public interface ToDoubleArrayFunction<T> {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    double[] applyAsDoubleArray(T value);
}
