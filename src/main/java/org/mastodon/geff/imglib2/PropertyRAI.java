package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;

import java.util.function.Supplier;

class PropertyRAI<T> implements RandomAccessibleInterval<T> {

    final Supplier<Dimensions> dimensions;
    private final RandomAccess<T> randomAccess;
    private final int n;

    PropertyRAI(final Dimensions dimensions, final RandomAccess<T> randomAccess) {
        this(() -> dimensions, randomAccess);
    }

    PropertyRAI(final Supplier<Dimensions> dimensions, final RandomAccess<T> randomAccess) {
        this.dimensions = dimensions;
        this.randomAccess = randomAccess;
        n = dimensions.get().numDimensions();
    }

    @Override
    public T getType() {
        return randomAccess.getType();
    }

    @Override
    public RandomAccess<T> randomAccess() {
        return randomAccess;
    }

    @Override
    public RandomAccess<T> randomAccess(Interval interval) {
        return randomAccess;
    }

    @Override
    public int numDimensions() {
        return n;
    }

    @Override
    public long min(int d) {
        return 0;
    }

    @Override
    public long dimension(int d) {
        return dimensions.get().dimension(d);
    }

    @Override
    public long max(int d) {
        return dimension(d) - 1;
    }
}
