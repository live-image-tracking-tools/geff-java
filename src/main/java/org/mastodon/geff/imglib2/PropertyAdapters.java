package org.mastodon.geff.imglib2;

import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.mastodon.geff.imglib2.Types.MaybeDouble;
import org.mastodon.geff.imglib2.Types.ToDoubleArrayFunction;
import org.mastodon.geff.imglib2.Types.ToFloatArrayFunction;
import org.mastodon.geff.imglib2.Types.ToMaybeDoubleFunction;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class PropertyAdapters {

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToLongFunction<O> supplier) {
        return scalar(identifier, UnsignedLongType::new, (type, obj) -> type.set(supplier.applyAsLong(obj)));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToDoubleFunction<O> supplier) {
        return scalar(identifier, DoubleType::new, (type, obj) -> type.set(supplier.applyAsDouble(obj)));
    }

    public static <O> PropertyAdapter<O, IntType> wrap(final String identifier, final ToIntFunction<O> supplier) {
        return scalar(identifier, IntType::new, (type, obj) -> type.set(supplier.applyAsInt(obj)));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToDoubleArrayFunction<O> supplier) {
        return vector(identifier, DoubleType::new, supplier::apply, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToFloatArrayFunction<O> supplier) {
        return vector(identifier, FloatType::new, supplier::apply, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, String> wrap(final String identifier, final Function<O, String> supplier) {
        final ValueRandomAccess<String> ra = new ValueRandomAccess<>("");
        final PropertyRAI<String> values = new PropertyRAI<>(new FinalDimensions(), ra);
        return new FixedPropertyWrapper<>(identifier, false, values, obj -> ra.setValue(supplier.apply(obj)));
    }

    static <O, T> PropertyAdapter<O, T> scalar(final String identifier, final Supplier<T> typeSupplier, final DataUpdate<O, T> update) {
        final T type = typeSupplier.get();
        final RandomAccess<T> ra = new ScalarRandomAccess<>(type);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(), ra);
        return new FixedPropertyWrapper<>(identifier, false, values, obj -> update.update(type, obj));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToMaybeDoubleFunction<O> supplier) {
        return maybeScalar(identifier, DoubleType::new, (type, missing, obj) -> {
            final MaybeDouble maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    static <O, T> PropertyAdapter<O, T> maybeScalar(final String identifier, final Supplier<T> typeSupplier, final MaybeDataUpdate<O, T> update) {
        final T type = typeSupplier.get();
        final RandomAccess<T> ra = new ScalarRandomAccess<>(type);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(), ra);
        final Missing missing = new Missing();
        return new FixedPropertyWrapper<>(identifier, true, values, missing, obj -> update.update(type, missing, obj));
    }

    static <O, T, P> PropertyAdapter<O, T> vector(
            final String identifier,
            final Supplier<T> typeSupplier,
            final Function<O, P> dataSupplier,
            final VectorTypeUpdate<P, T> elementUpdate) {
        final T type = typeSupplier.get();
        final VectorRandomAccess<P, T> ra = new VectorRandomAccess<>(type, elementUpdate);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(), ra);
        return new FixedPropertyWrapper<>(identifier, false, values, obj -> ra.setData(dataSupplier.apply(obj)));
    }

    @FunctionalInterface
    public interface DataUpdate<O, T> {
        void update(T dataHolder, O obj);
    }

    @FunctionalInterface
    public interface MaybeDataUpdate<O, T> {
        void update(T dataHolder, Missing missingHolder, O obj);
    }

    @FunctionalInterface
    public interface VectorTypeUpdate<P, T> {
        void update(T type, P data, int i);
    }

    private static final class Missing {
        private boolean missing = false;

        public boolean isMissing() {
            return missing;
        }

        public boolean setMissing(boolean missing) {
            this.missing = missing;
            return missing;
        }

        public boolean setPresent(boolean present) {
            this.missing = !present;
            return present;
        }
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

    private static class VectorRandomAccess<P, T> extends Point implements RandomAccess<T> {
        private final T type;
        private final VectorTypeUpdate<P, T> vectorTypeUpdate;
        private P data;

        VectorRandomAccess(T type, VectorTypeUpdate<P, T> vectorTypeUpdate) {
            super(1);
            this.type = type;
            this.vectorTypeUpdate = vectorTypeUpdate;
        }

        void setData(P data) {
            this.data = data;
        }

        @Override
        public T getType() {
            return type;
        }

        @Override
        public T get() {
            vectorTypeUpdate.update(type, data, (int) position[0]);
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

    private static class FixedPropertyWrapper<O, T> implements PropertyAdapter<O, T> {

        private final String identifier;
        private final boolean isOptional;
        private final RandomAccessibleInterval<T> values;
        private final Missing missing;
        private final Consumer<O> update;

        FixedPropertyWrapper(
                final String identifier,
                final boolean isOptional,
                final RandomAccessibleInterval<T> values,
                final Consumer<O> update) {
            this(identifier, isOptional, values, new Missing(), update);
        }

        FixedPropertyWrapper(
                final String identifier,
                final boolean isOptional,
                final RandomAccessibleInterval<T> values,
                final Missing missing,
                final Consumer<O> update) {
            this.identifier = identifier;
            this.values = values;
            this.missing = missing;
            this.isOptional = isOptional;
            this.update = update;
        }

        @Override
        public PropertyAdapter<O, T> adapt(O obj) {
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

        @Override
        public boolean isMissing() {
            return missing.isMissing();
        }

        @Override
        public RandomAccessibleInterval<T> values() {
            return values;
        }

        @Override
        public void set(GeffProperty<T> property) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return GeffProperty.toString(this);
        }
    }
}
