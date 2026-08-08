package org.mastodon.geff.imglib2;

import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.mastodon.geff.imglib2.Maybe.MaybeBoolean;
import org.mastodon.geff.imglib2.Maybe.MaybeBooleanArray;
import org.mastodon.geff.imglib2.Maybe.MaybeByte;
import org.mastodon.geff.imglib2.Maybe.MaybeByteArray;
import org.mastodon.geff.imglib2.Maybe.MaybeDouble;
import org.mastodon.geff.imglib2.Maybe.MaybeDoubleArray;
import org.mastodon.geff.imglib2.Maybe.MaybeFloat;
import org.mastodon.geff.imglib2.Maybe.MaybeFloatArray;
import org.mastodon.geff.imglib2.Maybe.MaybeInt;
import org.mastodon.geff.imglib2.Maybe.MaybeIntArray;
import org.mastodon.geff.imglib2.Maybe.MaybeLong;
import org.mastodon.geff.imglib2.Maybe.MaybeLongArray;
import org.mastodon.geff.imglib2.Maybe.MaybeShort;
import org.mastodon.geff.imglib2.Maybe.MaybeShortArray;
import org.mastodon.geff.imglib2.Maybe.MaybeString;

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



    // ------------------------------------------------------------------------
    //
    //   Vector, Optional
    //
    // ------------------------------------------------------------------------

    // TODO: could reuse one-time allocated primitive array for fixed-length properties

    public static <T extends GenericByteType<T>> Supplier<MaybeByteArray> asMaybeByteArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeByteArray v = new MaybeByteArray();
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

    public static <T extends GenericShortType<T>> Supplier<MaybeShortArray> asMaybeShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeShortArray v = new MaybeShortArray();
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

    public static <T extends GenericIntType<T>> Supplier<MaybeIntArray> asMaybeIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeIntArray v = new MaybeIntArray();
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

    public static <T extends GenericLongType<T>> Supplier<MaybeLongArray> asMaybeLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeLongArray v = new MaybeLongArray();
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

    public static <T extends BooleanType<T>> Supplier<MaybeBooleanArray> asMaybeBooleanArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        final MaybeBooleanArray v = new MaybeBooleanArray();
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

    public static Supplier<MaybeFloatArray> asMaybeFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        final MaybeFloatArray v = new MaybeFloatArray();
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

    public static Supplier<MaybeDoubleArray> asMaybeDoubleArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        final MaybeDoubleArray v = new MaybeDoubleArray();
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
