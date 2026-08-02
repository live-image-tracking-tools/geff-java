package org.mastodon.geff.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class PropertyAdapters {

    @FunctionalInterface
    public interface TypeUpdate<O, T> {
        void update(T type, O obj);
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToLongFunction<O> supplier) {
        return scalar(identifier, UnsignedLongType::new, (type, obj) -> type.set(supplier.applyAsLong(obj)));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToDoubleFunction<O> supplier) {
        return scalar(identifier, DoubleType::new, (type, obj) -> type.set(supplier.applyAsDouble(obj)));
    }

    public static <O> PropertyAdapter<O, IntType> wrap(final String identifier, final ToIntFunction<O> supplier) {
        return scalar(identifier, IntType::new, (type, obj) -> type.set(supplier.applyAsInt(obj)));
    }

    public static <O> PropertyAdapter<O, String> wrap(final String identifier, final Function<O, String> supplier) {
        final ValueRandomAccess<String> a = new ValueRandomAccess<>("");
        final ScalarRandomAccessibleInterval<String> values = new ScalarRandomAccessibleInterval<>(a);
        return new FixedPropertyWrapper<>(identifier, false, values, obj -> a.setValue(supplier.apply(obj)));
    }

    static <O, T> PropertyAdapter<O, T> scalar(final String identifier, final Supplier<T> typeSupplier, final TypeUpdate<O, T> update) {
        final RandomAccess<T> ra = new ScalarRandomAccess<>(typeSupplier.get());
        final ScalarRandomAccessibleInterval<T> values = new ScalarRandomAccessibleInterval<>(ra);
        return new FixedPropertyWrapper<>(identifier, false, values, obj -> update.update(ra.get(), obj));
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

    private static class ValueRandomAccess<T> extends Point implements RandomAccess<T> {
        private final T type;
        private T value;

        ValueRandomAccess(T type) {
            super(0);
            this.type = type;
        }

        @Override
        public T getType() {
            return type;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public RandomAccess<T> copy() {
            throw new UnsupportedOperationException();
        }

        void setValue(T value) {
            this.value = value;
        }
    }

    private static class ScalarRandomAccessibleInterval<T> implements RandomAccessibleInterval<T> {
        private final RandomAccess<T> a;

        ScalarRandomAccessibleInterval(final RandomAccess<T> randomAccess) {
            a = randomAccess;
        }

        ScalarRandomAccessibleInterval(final Supplier<T> typeSupplier) {
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

    private static class FixedPropertyWrapper<O, T> implements PropertyAdapter<O, T> {

        private final String identifier;
        private final boolean isOptional;
        private final RandomAccessibleInterval<T> values;
        private final Consumer<O> update;

        FixedPropertyWrapper(
                final String identifier,
                final boolean isOptional,
                final RandomAccessibleInterval<T> values,
                final Consumer<O> update) {
            this.identifier = identifier;
            this.values = values;
            this.isOptional = isOptional;
            this.update = update;
        }

        @Override
        public PropertyAdapter<O, T> update(O obj) {
            update.accept(obj);
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
