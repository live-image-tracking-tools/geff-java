package org.mastodon.geff.imglib2;

import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.mastodon.geff.imglib2.FunctionTypes.*;
import org.mastodon.geff.imglib2.Maybe.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class PropertyAdapters {

    //-------------------------------------------------------------------------
    //  scalar
    //-------------------------------------------------------------------------

    public static <O> PropertyAdapter<O, UnsignedByteType> wrap(final String identifier, final ToByteFunction<O> supplier) {
        return scalar(identifier, UnsignedByteType::new, (type, obj) -> type.set(supplier.applyAsByte(obj)));
    }

    public static <O> PropertyAdapter<O, UnsignedShortType> wrap(final String identifier, final ToShortFunction<O> supplier) {
        return scalar(identifier, UnsignedShortType::new, (type, obj) -> type.set(supplier.applyAsShort(obj)));
    }

    public static <O> PropertyAdapter<O, UnsignedIntType> wrap(final String identifier, final ToIntFunction<O> supplier) {
        return scalar(identifier, UnsignedIntType::new, (type, obj) -> type.set(supplier.applyAsInt(obj)));
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToLongFunction<O> supplier) {
        return scalar(identifier, UnsignedLongType::new, (type, obj) -> type.set(supplier.applyAsLong(obj)));
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToFloatFunction<O> supplier) {
        return scalar(identifier, FloatType::new, (type, obj) -> type.set(supplier.applyAsFloat(obj)));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToDoubleFunction<O> supplier) {
        return scalar(identifier, DoubleType::new, (type, obj) -> type.set(supplier.applyAsDouble(obj)));
    }

    public static <O> PropertyAdapter<O, BoolType> wrap(final String identifier, final ToBooleanFunction<O> supplier) {
        return scalar(identifier, BoolType::new, (type, obj) -> type.set(supplier.applyAsBoolean(obj)));
    }

    public static <O> PropertyAdapter<O, String> wrap(final String identifier, final ToStringFunction<O> supplier) {
        final ValueRandomAccess<String> ra = new ValueRandomAccess<>("");
        final PropertyRAI<String> values = new PropertyRAI<>(new FinalDimensions(), ra);
        return new DefaultPropertyAdapter<>(identifier, false, false, values, obj -> ra.setValue(supplier.apply(obj)));
    }


    //-------------------------------------------------------------------------
    //  fixed-length vector
    //-------------------------------------------------------------------------

    public static <O> PropertyAdapter<O, UnsignedByteType> wrap(final String identifier, final ToByteArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, UnsignedByteType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedShortType> wrap(final String identifier, final ToShortArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, UnsignedShortType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedIntType> wrap(final String identifier, final ToIntArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, UnsignedIntType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToLongArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, UnsignedLongType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToFloatArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, FloatType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToDoubleArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, DoubleType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, BoolType> wrap(final String identifier, final ToBooleanArrayFunction<O> supplier, final int length) {
        return fixedLengthVector(identifier, BoolType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }


    //-------------------------------------------------------------------------
    //  var-length vector
    //-------------------------------------------------------------------------

    public static <O> PropertyAdapter<O, UnsignedByteType> wrap(final String identifier, final ToByteArrayFunction<O> supplier) {
        return varLengthVector(identifier, UnsignedByteType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedShortType> wrap(final String identifier, final ToShortArrayFunction<O> supplier) {
        return varLengthVector(identifier, UnsignedShortType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedIntType> wrap(final String identifier, final ToIntArrayFunction<O> supplier) {
        return varLengthVector(identifier, UnsignedIntType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToLongArrayFunction<O> supplier) {
        return varLengthVector(identifier, UnsignedLongType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToFloatArrayFunction<O> supplier) {
        return varLengthVector(identifier, FloatType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToDoubleArrayFunction<O> supplier) {
        return varLengthVector(identifier, DoubleType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, BoolType> wrap(final String identifier, final ToBooleanArrayFunction<O> supplier) {
        return varLengthVector(identifier, BoolType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }


    //-------------------------------------------------------------------------
    //  maybe scalar
    //-------------------------------------------------------------------------

    public static <O> PropertyAdapter<O, UnsignedByteType> wrap(final String identifier, final ToMaybeByteFunction<O> supplier) {
        return maybeScalar(identifier, UnsignedByteType::new, (type, missing, obj) -> {
            final MaybeByte maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, UnsignedShortType> wrap(final String identifier, final ToMaybeShortFunction<O> supplier) {
        return maybeScalar(identifier, UnsignedShortType::new, (type, missing, obj) -> {
            final MaybeShort maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, UnsignedIntType> wrap(final String identifier, final ToMaybeIntFunction<O> supplier) {
        return maybeScalar(identifier, UnsignedIntType::new, (type, missing, obj) -> {
            final MaybeInt maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToMaybeLongFunction<O> supplier) {
        return maybeScalar(identifier, UnsignedLongType::new, (type, missing, obj) -> {
            final MaybeLong maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToMaybeFloatFunction<O> supplier) {
        return maybeScalar(identifier, FloatType::new, (type, missing, obj) -> {
            final MaybeFloat maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToMaybeDoubleFunction<O> supplier) {
        return maybeScalar(identifier, DoubleType::new, (type, missing, obj) -> {
            final MaybeDouble maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, BoolType> wrap(final String identifier, final ToMaybeBooleanFunction<O> supplier) {
        return maybeScalar(identifier, BoolType::new, (type, missing, obj) -> {
            final MaybeBoolean maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                type.set(maybe.get());
        });
    }

    public static <O> PropertyAdapter<O, String> wrap(final String identifier, final ToMaybeStringFunction<O> supplier) {
        final ValueRandomAccess<String> ra = new ValueRandomAccess<>("");
        final PropertyRAI<String> values = new PropertyRAI<>(new FinalDimensions(), ra);
        final Missing missing = new Missing();
        return new DefaultPropertyAdapter<>(identifier, true, false, values, missing, obj -> {
            final MaybeString maybe = supplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                ra.setValue(maybe.get());
        });
    }


    //-------------------------------------------------------------------------
    //  maybe fixed-length vector
    //-------------------------------------------------------------------------

    public static <O> PropertyAdapter<O, UnsignedByteType> wrap(final String identifier, final ToMaybeByteArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, UnsignedByteType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedShortType> wrap(final String identifier, final ToMaybeShortArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, UnsignedShortType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedIntType> wrap(final String identifier, final ToMaybeIntArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, UnsignedIntType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToMaybeLongArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, UnsignedLongType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToMaybeFloatArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, FloatType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToMaybeDoubleArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, DoubleType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, BoolType> wrap(final String identifier, final ToMaybeBooleanArrayFunction<O> supplier, final int length) {
        return maybeFixedLengthVector(identifier, BoolType::new, supplier, length, (type, data, i) -> type.set(data[i]));
    }


    //-------------------------------------------------------------------------
    //  maybe var-length vector
    //-------------------------------------------------------------------------

    public static <O> PropertyAdapter<O, UnsignedByteType> wrap(final String identifier, final ToMaybeByteArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, UnsignedByteType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedShortType> wrap(final String identifier, final ToMaybeShortArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, UnsignedShortType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedIntType> wrap(final String identifier, final ToMaybeIntArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, UnsignedIntType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, UnsignedLongType> wrap(final String identifier, final ToMaybeLongArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, UnsignedLongType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, FloatType> wrap(final String identifier, final ToMaybeFloatArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, FloatType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, DoubleType> wrap(final String identifier, final ToMaybeDoubleArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, DoubleType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }

    public static <O> PropertyAdapter<O, BoolType> wrap(final String identifier, final ToMaybeBooleanArrayFunction<O> supplier) {
        return maybeVarLengthVector(identifier, BoolType::new, supplier, data -> data.length, (type, data, i) -> type.set(data[i]));
    }


    //-------------------------------------------------------------------------
    //  internal
    //-------------------------------------------------------------------------

    @FunctionalInterface
    private interface DataUpdate<O, T> {
        void update(T dataHolder, O obj);
    }

    static <O, T> PropertyAdapter<O, T> scalar(final String identifier, final Supplier<T> typeSupplier, final DataUpdate<O, T> update) {
        final T type = typeSupplier.get();
        final RandomAccess<T> ra = new ScalarRandomAccess<>(type);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(), ra);
        return new DefaultPropertyAdapter<>(identifier, false, false, values, obj -> update.update(type, obj));
    }

    @FunctionalInterface
    private interface MaybeDataUpdate<O, T> {
        void update(T dataHolder, Missing missingHolder, O obj);
    }

    static <O, T> PropertyAdapter<O, T> maybeScalar(final String identifier, final Supplier<T> typeSupplier, final MaybeDataUpdate<O, T> update) {
        final T type = typeSupplier.get();
        final RandomAccess<T> ra = new ScalarRandomAccess<>(type);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(), ra);
        final Missing missing = new Missing();
        return new DefaultPropertyAdapter<>(identifier, true, false, values, missing, obj -> update.update(type, missing, obj));
    }

    @FunctionalInterface
    private interface VectorTypeUpdate<P, T> {
        void update(T type, P data, int i);
    }

    // var-length
    static <O, T, P> PropertyAdapter<O, T> varLengthVector(
            final String identifier,
            final Supplier<T> typeSupplier,
            final Function<O, P> dataSupplier,
            final ToIntFunction<P> lengthSupplier,
            final VectorTypeUpdate<P, T> elementUpdate) {
        final T type = typeSupplier.get();
        final VectorRandomAccess<P, T> ra = new VectorRandomAccess<>(type, elementUpdate);
        final long[] length = new long[1];
        final PropertyRAI<T> values = new PropertyRAI<>(FinalDimensions.wrap(length), ra);
        return new DefaultPropertyAdapter<>(identifier, false, true, values, obj -> {
            final P apply = dataSupplier.apply(obj);
            ra.setData(apply);
            length[0] = lengthSupplier.applyAsInt(apply);
        });
    }

    // fixed-length
    static <O, T, P> PropertyAdapter<O, T> fixedLengthVector(
            final String identifier,
            final Supplier<T> typeSupplier,
            final Function<O, P> dataSupplier,
            final int length,
            final VectorTypeUpdate<P, T> elementUpdate) {
        final T type = typeSupplier.get();
        final VectorRandomAccess<P, T> ra = new VectorRandomAccess<>(type, elementUpdate);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(length), ra);
        return new DefaultPropertyAdapter<>(identifier, false, false, values, obj -> ra.setData(dataSupplier.apply(obj)));
    }

    // var-length
    static <O, T, P> PropertyAdapter<O, T> maybeVarLengthVector(
            final String identifier,
            final Supplier<T> typeSupplier,
            final Function<O, ? extends Maybe<P>> dataSupplier,
            final ToIntFunction<P> lengthSupplier,
            final VectorTypeUpdate<P, T> elementUpdate) {
        final T type = typeSupplier.get();
        final VectorRandomAccess<P, T> ra = new VectorRandomAccess<>(type, elementUpdate);
        final long[] length = new long[1];
        final PropertyRAI<T> values = new PropertyRAI<>(FinalDimensions.wrap(length), ra);
        final Missing missing = new Missing();
        return new DefaultPropertyAdapter<>(identifier, true, true, values, missing, obj -> {
            final Maybe<P> maybe = dataSupplier.apply(obj);
            if (missing.setPresent(maybe.isPresent())) {
                ra.setData(maybe.get());
                length[0] = lengthSupplier.applyAsInt(maybe.get());
            }
        });
    }

    // fixed-length
    static <O, T, P> PropertyAdapter<O, T> maybeFixedLengthVector(
            final String identifier,
            final Supplier<T> typeSupplier,
            final Function<O, ? extends Maybe<P>> dataSupplier,
            final int length,
            final VectorTypeUpdate<P, T> elementUpdate) {
        final T type = typeSupplier.get();
        final VectorRandomAccess<P, T> ra = new VectorRandomAccess<>(type, elementUpdate);
        final PropertyRAI<T> values = new PropertyRAI<>(new FinalDimensions(length), ra);
        final Missing missing = new Missing();
        return new DefaultPropertyAdapter<>(identifier, true, false, values, missing, obj -> {
            final Maybe<P> maybe = dataSupplier.apply(obj);
            if (missing.setPresent(maybe.isPresent()))
                ra.setData(maybe.get());
        });
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

    private static class DefaultPropertyAdapter<O, T> implements PropertyAdapter<O, T> {

        private final String identifier;
        private final boolean isOptional;
        private final boolean isVarLength;
        private final RandomAccessibleInterval<T> values;
        private final Missing missing;
        private final Consumer<O> update;

        DefaultPropertyAdapter(
                final String identifier,
                final boolean isOptional,
                final boolean isVarLength,
                final RandomAccessibleInterval<T> values,
                final Consumer<O> update) {
            this(identifier, isOptional, isVarLength, values, new Missing(), update);
        }

        DefaultPropertyAdapter(
                final String identifier,
                final boolean isOptional,
                final boolean isVarLength,
                final RandomAccessibleInterval<T> values,
                final Missing missing,
                final Consumer<O> update) {
            this.identifier = identifier;
            this.isVarLength = isVarLength;
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
            return isVarLength;
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
