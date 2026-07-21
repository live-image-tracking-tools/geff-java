package org.mastodon.geff.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;

import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class Wrappers {

    public static <O> PropertySupplier<O, UnsignedLongType> wrap(final String identifier, final ToLongFunction<O> supplier) {
        return scalar(identifier, UnsignedLongType::new, (p, obj) -> p.getAt().set(supplier.applyAsLong(obj)));
    }

    public static <O> PropertySupplier<O, DoubleType> wrap(final String identifier, final ToDoubleFunction<O> supplier) {
        return scalar(identifier, DoubleType::new, (p, obj) -> p.getAt().set(supplier.applyAsDouble(obj)));
    }

    public static <O> PropertySupplier<O, IntType> wrap(final String identifier, final ToIntFunction<O> supplier) {
        return scalar(identifier, IntType::new, (p, obj) -> p.getAt().set(supplier.applyAsInt(obj)));
    }

    public static <O, T> PropertySupplier<O, T> scalar(final String identifier, Supplier<T> typeSupplier, FixedPropertyWrapper.Update<O, T> update) {
        return new FixedPropertyWrapper<>(identifier, false, new ScalarRandomAccessibleInterval<>(typeSupplier), update);
    }

    private static class ScalarRandomAccess<T> extends Point implements RandomAccess<T> {
        private final T type;

        ScalarRandomAccess(T type) {
            super(0);
            this.type = type;
        }

        @Override
        public T get() {
            return type;
        }

        @Override
        public RandomAccess<T> copy() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ScalarRandomAccessibleInterval<T> implements RandomAccessibleInterval<T> {
        private final RandomAccess<T> a;

        private ScalarRandomAccessibleInterval(final Supplier<T> typeSupplier) {
            a = new ScalarRandomAccess<>(typeSupplier.get());
        }

        @Override
        public T getType() {
            return a.getType();
        }

        @Override
        public int numDimensions() {
            return 0;
        }
        @Override
        public long min(int d) {
            return 0;
        }

        @Override
        public long max(int d) {
            return 0;
        }

        @Override
        public RandomAccess<T> randomAccess() {
            return a;
        }

        @Override
        public RandomAccess<T> randomAccess(Interval interval) {
            return a;
        }
    }

    private static class FixedPropertyWrapper<O, T> implements PropertySupplier<O, T> {

        private final String identifier;
        private final boolean isOptional;
        private final RandomAccessibleInterval<T> values;
        private final Update<O, T> update;

        @FunctionalInterface
        interface Update<O, T> {
            void update(FixedPropertyWrapper<O, T> property, O obj);
        }

        public FixedPropertyWrapper(
                final String identifier,
                final boolean isOptional,
                final RandomAccessibleInterval<T> values,
                final Update<O, T> update) {
            this.identifier = identifier;
            this.values = values;
            this.isOptional = isOptional;
            this.update = update;
        }

        @Override
        public PropertySupplier<O, T> update(O obj) {
            update.update(this, obj);
            return this;
        }

        @Override
        public String identifier() {
            return identifier;
        }

        @Override
        public boolean isVarlength() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return isOptional;
        }

        @Override
        public long numElements() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ElementIndex elementIndex() {
            throw new UnsupportedOperationException();
        }

        private boolean isMissing;

        public void setMissing(final boolean missing) {
            isMissing = missing;
        }

        @Override
        public boolean isMissing() {
            return isMissing;
        }

        @Override
        public RandomAccessibleInterval<T> values() {
            return values;
        }

        @Override
        public void set(GeffProperty<T> property) {
            throw new UnsupportedOperationException();
        }

    }

}
