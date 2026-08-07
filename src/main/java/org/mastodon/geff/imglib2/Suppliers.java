package org.mastodon.geff.imglib2;

import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Suppliers {

    // ------------------------------------------------------------------------
    //
    //   Scalar, Non-Optional
    //
    // ------------------------------------------------------------------------

    // TODO: asBooleanSupplier?

    public static <T extends GenericByteType<T>> Construction.ByteSupplier asByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getByte();
    }

    public static <T extends GenericShortType<T>> Construction.ShortSupplier asShortSupplier(final GeffProperty<?> property) {
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

    public static Construction.FloatSupplier asFloatSupplier(final GeffProperty<?> property) {
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
            final int len = (int) p.values().dimension(0);
            final byte[] array = new byte[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getByte();
            return array;
        };
    }

    public static <T extends GenericShortType<T>> Supplier<short[]> asShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final short[] array = new short[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getShort();
            return array;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<int[]> asIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final int[] array = new int[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getInt();
            return array;
        };
    }

    public static <T extends GenericLongType<T>> Supplier<long[]> asLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final long[] array = new long[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getLong();
            return array;
        };
    }

    public static <T extends BooleanType<T>> Supplier<boolean[]> asBooleanArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final boolean[] array = new boolean[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).get();
            return array;
        };
    }

    public static Supplier<float[]> asFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final float[] array = new float[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).get();
            return array;
        };
    }

    public static Supplier<double[]> asDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final double[] array = new double[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).get();
            return array;
        };
    }


    // ------------------------------------------------------------------------
    //
    //   Scalar, Optional
    //
    // ------------------------------------------------------------------------


    public static <T extends GenericByteType<T>> Supplier<Maybe.MaybeByte> asMaybeByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeByte v = new Maybe.MaybeByte();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getByte());
            return v;
        };
    }

    public static <T extends GenericShortType<T>> Supplier<Maybe.MaybeShort> asMaybeShortSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeShort v = new Maybe.MaybeShort();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getShort());
            return v;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<Maybe.MaybeInt> asMaybeIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeInt v = new Maybe.MaybeInt();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getInt());
            return v;
        };
    }

    public static <T extends GenericLongType<T>> Supplier<Maybe.MaybeLong> asMaybeLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeLong v = new Maybe.MaybeLong();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().getLong());
            return v;
        };
    }

    public static <T extends BooleanType<T>> Supplier<Maybe.MaybeBoolean> asMaybeBooleanSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeBoolean v = new Maybe.MaybeBoolean();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().get());
            return v;
        };
    }

    public static Supplier<Maybe.MaybeFloat> asMaybeFloatSupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        final Maybe.MaybeFloat v = new Maybe.MaybeFloat();
        return () -> {
            if (v.setPresent(!p.isMissing()))
                v.set(p.getAt().get());
            return v;
        };
    }

    public static Supplier<Maybe.MaybeDouble> asMaybeDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        final Maybe.MaybeDouble v = new Maybe.MaybeDouble();
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



    // ------------------------------------------------------------------------
    //
    //   Vector, Optional
    //
    // ------------------------------------------------------------------------

    // TODO: could reuse one-time allocated primitive array for fixed-length properties

    public static <T extends GenericByteType<T>> Supplier<Maybe.MaybeByteArray> asMaybeByteArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeByteArray v = new Maybe.MaybeByteArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final byte[] array = new byte[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).getByte();
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends GenericShortType<T>> Supplier<Maybe.MaybeShortArray> asMaybeShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeShortArray v = new Maybe.MaybeShortArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final short[] array = new short[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).getShort();
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends GenericIntType<T>> Supplier<Maybe.MaybeIntArray> asMaybeIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeIntArray v = new Maybe.MaybeIntArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final int[] array = new int[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).getInt();
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends GenericLongType<T>> Supplier<Maybe.MaybeLongArray> asMaybeLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeLongArray v = new Maybe.MaybeLongArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final long[] array = new long[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).getLong();
                v.set(array);
            }
            return v;
        };
    }

    public static <T extends BooleanType<T>> Supplier<Maybe.MaybeBooleanArray> asMaybeBooleanArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final Maybe.MaybeBooleanArray v = new Maybe.MaybeBooleanArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final boolean[] array = new boolean[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).get();
                v.set(array);
            }
            return v;
        };
    }

    public static Supplier<Maybe.MaybeFloatArray> asMaybeFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        final Maybe.MaybeFloatArray v = new Maybe.MaybeFloatArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final float[] array = new float[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).get();
                v.set(array);
            }
            return v;
        };
    }

    public static Supplier<Maybe.MaybeDoubleArray> asMaybeDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        final Maybe.MaybeDoubleArray v = new Maybe.MaybeDoubleArray();
        return () -> {
            if (v.setPresent(!p.isMissing())) {
                final int len = (int) p.values().dimension(0);
                final double[] array = new double[len];
                for (int i = 0; i < len; i++)
                    array[i] = p.getAt(i).get();
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
            final int len = (int) p.values().dimension(0);
            final byte[] array = new byte[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getByte();
            return Optional.of(array);
        };
    }

    public static <T extends GenericShortType<T>> Supplier<Optional<short[]>> asOptionalShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final int len = (int) p.values().dimension(0);
            final short[] array = new short[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getShort();
            return Optional.of(array);
        };
    }

    public static <T extends GenericIntType<T>> Supplier<Optional<int[]>> asOptionalIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final int len = (int) p.values().dimension(0);
            final int[] array = new int[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getInt();
            return Optional.of(array);
        };
    }

    public static <T extends GenericLongType<T>> Supplier<Optional<long[]>> asOptionalLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final int len = (int) p.values().dimension(0);
            final long[] array = new long[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getLong();
            return Optional.of(array);
        };
    }

    public static Supplier<Optional<float[]>> asOptionalFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final int len = (int) p.values().dimension(0);
            final float[] array = new float[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).get();
            return Optional.of(array);
        };
    }

    public static Supplier<Optional<double[]>> asOptionalDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> {
            if (p.isMissing())
                return Optional.empty();
            final int len = (int) p.values().dimension(0);
            final double[] array = new double[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).get();
            return Optional.of(array);
        };
    }

    private Suppliers() {
        // don't instantiate
    }
}
