package org.mastodon.geff.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;

final class PropertySlice<T> implements RandomAccessibleInterval<T> {

    private final RandomAccessibleInterval<T> delegate;
    private final ElementIndex elementIndex;
    private final int n;

    PropertySlice(final RandomAccessibleInterval<T> parent, final ElementIndex elementIndex) {
        n = parent.numDimensions() - 1;
        delegate = parent;
        this.elementIndex = elementIndex;
    }

    private PropertySlice(final PropertySlice<T> slice) {
        delegate = slice.delegate;
        elementIndex = new ElementIndex();
        elementIndex.set(slice.elementIndex.get());
        n = slice.n;
    }

    public PropertySlice<T> copy() {
        return new PropertySlice<>(this);
    }

    @Override
    public int numDimensions() {
        return n;
    }

    @Override
    public RandomAccess<T> randomAccess() {
        return new RA<>(delegate.randomAccess(), elementIndex.slicePos);
    }

    @Override
    public RandomAccess<T> randomAccess(final Interval interval) {
        final Interval i = Intervals.addDimension(interval, 0, delegate.max(n));
        return new RA<>(delegate.randomAccess(i), elementIndex.slicePos);
    }

    @Override
    public long min(final int d) {
        return delegate.min(d);
    }

    @Override
    public long max(final int d) {
        return delegate.max(d);
    }

    private static class RA<T> extends Point implements RandomAccess<T> {
        private final RandomAccess<T> delegateAccess;

        private final long[] delegatePos;

        private final long[] slicePos;

        RA(final RandomAccess<T> delegateAccess, final long[] slicePos) {
            super(delegateAccess.numDimensions() - 1);
            this.delegateAccess = delegateAccess;
            this.delegatePos = new long[delegateAccess.numDimensions()];
            this.slicePos = slicePos;
        }

        @Override
        public T get() {
            System.arraycopy(position, 0, delegatePos, 0, position.length);
            delegatePos[position.length] = slicePos[0];
            return delegateAccess.setPositionAndGet(delegatePos);
        }

        @Override
        public RandomAccess<T> copy() {
            return new RA<>(delegateAccess.copy(), slicePos);
        }
    }
}
