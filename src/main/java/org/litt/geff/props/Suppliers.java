package org.litt.geff.props;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.litt.geff.props.Maybe.MaybeBoolean;
import org.litt.geff.props.Maybe.MaybeBooleanArray;
import org.litt.geff.props.Maybe.MaybeByte;
import org.litt.geff.props.Maybe.MaybeByteArray;
import org.litt.geff.props.Maybe.MaybeDouble;
import org.litt.geff.props.Maybe.MaybeDoubleArray;
import org.litt.geff.props.Maybe.MaybeFloat;
import org.litt.geff.props.Maybe.MaybeFloatArray;
import org.litt.geff.props.Maybe.MaybeInt;
import org.litt.geff.props.Maybe.MaybeIntArray;
import org.litt.geff.props.Maybe.MaybeLong;
import org.litt.geff.props.Maybe.MaybeLongArray;
import org.litt.geff.props.Maybe.MaybeShort;
import org.litt.geff.props.Maybe.MaybeShortArray;
import org.litt.geff.props.Maybe.MaybeString;

public class Suppliers {

    /*
     * Array extraction helpers.
     *
     * Property values may be n-dimensional (e.g. a polygon entry is a
     * (n_points, 2) array). Suppliers deliver the values as flat primitive
     * arrays by iterating a cursor over the whole RAI. imglib2 cursors walk
     * the last dimension fastest, which reproduces the C-order flattening
     * used by the geff spec (e.g. X,Y-interleaved polygon points).
     */

    private static int flatSize(final RandomAccessibleInterval<?> values) {
        return (int) Intervals.numElements(values);
    }

    @FunctionalInterface
    private interface ElementSetter<T, A> {
        void set(A array, int index, T type);
    }

    private static <T, A> void fill(final RandomAccessibleInterval<T> values, final A array, final ElementSetter<T, A> setter) {
        final Cursor<T> cursor = values.cursor();
        int i = 0;
        while (cursor.hasNext())
            setter.set(array, i++, cursor.next());
    }

    // ------------------------------------------------------------------------
    //
    //   Scalar, Non-Optional
    //
    // ------------------------------------------------------------------------

    // TODO: asBooleanSupplier?

    public static <T extends GenericByteType<T>> FunctionTypes.ByteSupplier asByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getByte();
    }

    public static <T extends GenericShortType<T>> FunctionTypes.ShortSupplier asShortSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getShort();
    }

    public static <T extends GenericIntType<T>> IntSupplier asIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getInt();
    }

    public static <T extends GenericLongType<T>> LongSupplier asLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getLong();
    }

    public static FunctionTypes.FloatSupplier asFloatSupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> p.getAt().get();
    }

    public static DoubleSupplier asDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> p.getAt().get();
    }



    // ------------------------------------------------------------------------
    //
    //   Vector, Non-Optional
    //
    // ------------------------------------------------------------------------

    public static <T extends GenericByteType<T>> Supplier<byte[]> asByteArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<T> values = p.values();
            final byte[] array = new byte[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getByte());
            return array;
        };
    }

    public static <T extends GenericShortType<T>> Supplier<short[]> asShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<T> values = p.values();
            final short[] array = new short[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getShort());
            return array;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<int[]> asIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<T> values = p.values();
            final int[] array = new int[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getInt());
            return array;
        };
    }

    public static <T extends GenericLongType<T>> Supplier<long[]> asLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<T> values = p.values();
            final long[] array = new long[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getLong());
            return array;
        };
    }

    public static <T extends BooleanType<T>> Supplier<boolean[]> asBooleanArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<T> values = p.values();
            final boolean[] array = new boolean[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.get());
            return array;
        };
    }

    public static Supplier<float[]> asFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<FloatType> values = p.values();
            final float[] array = new float[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.get());
            return array;
        };
    }

    public static Supplier<double[]> asDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> {
            final RandomAccessibleInterval<DoubleType> values = p.values();
            final double[] array = new double[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.get());
            return array;
        };
    }


    // ------------------------------------------------------------------------
    //
    //   Scalar, Optional
    //
    // ------------------------------------------------------------------------


    public static <T extends GenericByteType<T>> Supplier<MaybeByte> asMaybeByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeByte v = new MaybeByte();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getByte());
            return v;
        };
    }

    public static <T extends GenericShortType<T>> Supplier<MaybeShort> asMaybeShortSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeShort v = new MaybeShort();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getShort());
            return v;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<MaybeInt> asMaybeIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeInt v = new MaybeInt();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getInt());
            return v;
        };
    }

    public static <T extends GenericLongType<T>> Supplier<MaybeLong> asMaybeLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeLong v = new MaybeLong();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getLong());
            return v;
        };
    }

    public static <T extends BooleanType<T>> Supplier<MaybeBoolean> asMaybeBooleanSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeBoolean v = new MaybeBoolean();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().get());
            return v;
        };
    }

    public static Supplier<MaybeFloat> asMaybeFloatSupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        final MaybeFloat v = new MaybeFloat();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().get());
            return v;
        };
    }

    public static Supplier<MaybeDouble> asMaybeDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        final MaybeDouble v = new MaybeDouble();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().get());
            return v;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<OptionalInt> asOptionalIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? OptionalInt.empty() : OptionalInt.of(p.getAt().getInt());
    }

    public static <T extends GenericLongType<T>> Supplier<OptionalLong> asOptionalLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? OptionalLong.empty() : OptionalLong.of(p.getAt().getLong());
    }

    public static Supplier<OptionalDouble> asOptionalDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> p.isMissing() ? OptionalDouble.empty() : OptionalDouble.of(p.getAt().get());
    }

    public static <T> Supplier<Optional<T>> asOptionalSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt());
    }

    // Boxed Optional scalar suppliers, for constructor parameters declared as
    // Optional<Byte>, Optional<Short>, etc. (as opposed to OptionalInt, ...).

    public static <T extends GenericByteType<T>> Supplier<Optional<Byte>> asOptionalBoxedByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().getByte());
    }

    public static <T extends GenericShortType<T>> Supplier<Optional<Short>> asOptionalBoxedShortSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().getShort());
    }

    public static <T extends GenericIntType<T>> Supplier<Optional<Integer>> asOptionalBoxedIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().getInt());
    }

    public static <T extends GenericLongType<T>> Supplier<Optional<Long>> asOptionalBoxedLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().getLong());
    }

    public static <T extends BooleanType<T>> Supplier<Optional<Boolean>> asOptionalBoxedBooleanSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().get());
    }

    public static Supplier<Optional<Float>> asOptionalBoxedFloatSupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().get());
    }

    public static Supplier<Optional<Double>> asOptionalBoxedDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt().get());
    }



    // ------------------------------------------------------------------------
    //
    //   Vector, Optional
    //
    // ------------------------------------------------------------------------

    // NB: The Maybe*Array suppliers below reuse a single Maybe instance and a
    // single scratch array across calls, like the scalar Maybe suppliers
    // above. Callers must not retain the returned Maybe (or the array it
    // wraps) across calls; copy it if needed.

    public static <T extends GenericByteType<T>> Supplier<MaybeByteArray> asMaybeByteArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeByteArray v = new MaybeByteArray();
        final byte[][] scratch = new byte[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<T> values = p.values();
                final int len = flatSize(values);
                byte[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new byte[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.getByte());
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends GenericShortType<T>> Supplier<MaybeShortArray> asMaybeShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeShortArray v = new MaybeShortArray();
        final short[][] scratch = new short[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<T> values = p.values();
                final int len = flatSize(values);
                short[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new short[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.getShort());
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<MaybeIntArray> asMaybeIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeIntArray v = new MaybeIntArray();
        final int[][] scratch = new int[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<T> values = p.values();
                final int len = flatSize(values);
                int[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new int[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.getInt());
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends GenericLongType<T>> Supplier<MaybeLongArray> asMaybeLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeLongArray v = new MaybeLongArray();
        final long[][] scratch = new long[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<T> values = p.values();
                final int len = flatSize(values);
                long[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new long[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.getLong());
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends BooleanType<T>> Supplier<MaybeBooleanArray> asMaybeBooleanArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeBooleanArray v = new MaybeBooleanArray();
        final boolean[][] scratch = new boolean[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<T> values = p.values();
                final int len = flatSize(values);
                boolean[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new boolean[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.get());
                v.set(array);
            }
            return v;
        };
    }

    public static Supplier<MaybeFloatArray> asMaybeFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        final MaybeFloatArray v = new MaybeFloatArray();
        final float[][] scratch = new float[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<FloatType> values = p.values();
                final int len = flatSize(values);
                float[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new float[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.get());
                v.set(array);
            }
            return v;
        };
    }

    public static Supplier<MaybeDoubleArray> asMaybeDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        final MaybeDoubleArray v = new MaybeDoubleArray();
        final double[][] scratch = new double[1][];
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final RandomAccessibleInterval<DoubleType> values = p.values();
                final int len = flatSize(values);
                double[] array = scratch[0];
                if (array == null || array.length != len) {
                    array = new double[len];
                    scratch[0] = array;
                }
                fill(values, array, (a, i, t) -> a[i] = t.get());
                v.set(array);
            }
            return v;
        };
    }

    // TODO: asOptionalBooleanArraySupplier?

    public static <T extends GenericByteType<T>> Supplier<Optional<byte[]>> asOptionalByteArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final RandomAccessibleInterval<T> values = p.values();
            final byte[] array = new byte[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getByte());
            return Optional.of(array);
        };
    }

    public static <T extends GenericShortType<T>> Supplier<Optional<short[]>> asOptionalShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final RandomAccessibleInterval<T> values = p.values();
            final short[] array = new short[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getShort());
            return Optional.of(array);
        };
    }

    public static <T extends GenericIntType<T>> Supplier<Optional<int[]>> asOptionalIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final RandomAccessibleInterval<T> values = p.values();
            final int[] array = new int[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getInt());
            return Optional.of(array);
        };
    }

    public static <T extends GenericLongType<T>> Supplier<Optional<long[]>> asOptionalLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final RandomAccessibleInterval<T> values = p.values();
            final long[] array = new long[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.getLong());
            return Optional.of(array);
        };
    }

    public static Supplier<Optional<float[]>> asOptionalFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final RandomAccessibleInterval<FloatType> values = p.values();
            final float[] array = new float[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.get());
            return Optional.of(array);
        };
    }

    public static Supplier<Optional<double[]>> asOptionalDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final RandomAccessibleInterval<DoubleType> values = p.values();
            final double[] array = new double[flatSize(values)];
            fill(values, array, (a, i, t) -> a[i] = t.get());
            return Optional.of(array);
        };
    }



    // ------------------------------------------------------------------------
    //
    //   Special cases
    //
    // ------------------------------------------------------------------------

    public static Supplier<String> asStringSupplier(final GeffProperty<?> property) {
        if (property.isOptional() || property.numDimensions() != 1 || !(property.type() instanceof UnsignedByteType))
            throw new IllegalArgumentException(property.toString());
        final GeffProperty<String> p = new VarLengthAsStringProperty(Cast.unchecked(property));
        return p::getAt;
    }

    public static Supplier<MaybeString> asMaybeStringSupplier(final GeffProperty<?> property) {
        if (property.numDimensions() != 1 || !(property.type() instanceof UnsignedByteType))
            throw new IllegalArgumentException(property.toString());
        final GeffProperty<String> p = new VarLengthAsStringProperty(Cast.unchecked(property));
        final MaybeString v = new MaybeString();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt());
            return v;
        };
    }

    // TODO: Supplier<Optional<String>>


    private Suppliers() {
        // don't instantiate
    }
}
