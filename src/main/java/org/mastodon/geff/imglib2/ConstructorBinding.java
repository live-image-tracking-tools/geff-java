package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.mastodon.geff.imglib2.Construction.ByteSupplier;
import org.mastodon.geff.imglib2.Construction.FloatSupplier;
import org.mastodon.geff.imglib2.Construction.ShortSupplier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.lang.invoke.MethodType.methodType;
import static org.mastodon.geff.imglib2.Construction.ESCAPE_HATCH;

class ConstructorBinding {

    /**
     * Create a {@code T() MethodHandle} where {@code T} is the type of the
     * provided constructor-method {@code param}. The returned method-handle can
     * be used as a filter to bind that constructor parameter in the
     * constructor-method handle.
     *
     * @param lookup the {@code MethodHandles.Lookup} to use
     * @param properties all properties, the property identified by {@code param} will be extracted and converted to the required type if possible
     * @param param describes one parameter of the constructor method
     * @return a method handle of type {@code T()}
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws Construction.GeffBindError
     */
    static MethodHandle propertyHandle(
            final MethodHandles.Lookup lookup,
            final GeffProperties properties,
            final Construction.ResolvedConstructorParameter param
    ) throws NoSuchMethodException, IllegalAccessException, Construction.GeffBindError {

        final GeffPropertyType targetType = param.propertyType();
        final GeffProperty<?> sourceProperty = param.isId()
                ? properties.id()
                : properties.property(param.identifier());
        final GeffProperty<?> property = Construction.convertToMatch(sourceProperty, targetType);

        if (targetType == ESCAPE_HATCH) {
            final Supplier<GeffProperty<?>> s = () -> property;
            return lookup.findVirtual(Supplier.class, "get", methodType(Object.class)).bindTo(s)
                    .asType(methodType(GeffProperty.class));
        }

        if (targetType.isOptional()) {
            final Class<?> rawOptionalType = param.rawOptionalType();
            final MethodType mt = methodType(rawOptionalType);

            if (rawOptionalType == OptionalInt.class) {
                return lookup
                        .findVirtual(OptionalInt.class, "getAsInt", mt)
                        .bindTo(asOptionalIntSupplier(property));
            } else if (rawOptionalType == OptionalLong.class) {
                return lookup
                        .findVirtual(OptionalLong.class, "getAsLong", mt)
                        .bindTo(asOptionalLongSupplier(property));
            } else if (rawOptionalType == OptionalDouble.class) {
                return lookup
                        .findVirtual(OptionalDouble.class, "getAsDouble", mt)
                        .bindTo(asOptionalDoubleSupplier(property));
            } else if (rawOptionalType == Optional.class) {
                final Class<?> rawType = param.rawType();
                if( targetType.numDimensions() == 0 ) { // scalars
                    final MethodType omt = methodType(Object.class);

                    if (rawType == String.class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalSupplier(property))
                                .asType(mt);
                    } else {
                        // TODO: support boxed Optional<Double> etc
                        throw new UnsupportedOperationException("TODO: support boxed Optional<Double> etc");
                    }

                } else if (targetType.numDimensions() == 1) { // vectors
                    final MethodType omt = methodType(Object.class);

                    if (rawType == byte[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalByteArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == short[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalShortArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == int[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalIntArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == long[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalLongArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == float[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalFloatArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == double[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asOptionalDoubleArraySupplier(property))
                                .asType(mt);
                    }
                }
            } else {
                // TODO: maybe add OptionalByte, etc?
                throw new UnsupportedOperationException("TODO: maybe add OptionalByte, etc?");
            }

        } else { // !isOptional
            final Class<?> rawType = param.rawType();
            final MethodType mt = methodType(rawType);

            if (targetType.numDimensions() == 0) { // scalars
                if (rawType == byte.class) {
                    return lookup
                            .findVirtual(ByteSupplier.class, "getAsByte", mt)
                            .bindTo(asByteSupplier(property));
                } else if (rawType == short.class) {
                    return lookup
                            .findVirtual(ShortSupplier.class, "getAsShort", mt)
                            .bindTo(asShortSupplier(property));
                } else if (rawType == int.class) {
                    return lookup
                            .findVirtual(IntSupplier.class, "getAsInt", mt)
                            .bindTo(asIntSupplier(property));
                } else if (rawType == long.class) {
                    return lookup
                            .findVirtual(LongSupplier.class, "getAsLong", mt)
                            .bindTo(asLongSupplier(property));
                } else if (rawType == float.class) {
                    return lookup
                            .findVirtual(FloatSupplier.class, "getAsFloat", mt)
                            .bindTo(asFloatSupplier(property));
                } else if (rawType == double.class) {
                    return lookup
                            .findVirtual(DoubleSupplier.class, "getAsDouble", mt)
                            .bindTo(asDoubleSupplier(property));
                }

            } else if (targetType.numDimensions() == 1) { // vectors
                final MethodType omt = methodType(Object.class);
                if (rawType == byte[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asByteArraySupplier(property))
                            .asType(mt);
                } else if (rawType == short[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asShortArraySupplier(property))
                            .asType(mt);
                } else if (rawType == int[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asIntArraySupplier(property))
                            .asType(mt);
                } else if (rawType == long[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asLongArraySupplier(property))
                            .asType(mt);
                } else if (rawType == float[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asFloatArraySupplier(property))
                            .asType(mt);
                } else if (rawType == double[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asDoubleArraySupplier(property))
                            .asType(mt);
                }
            }
        }

        throw new IllegalArgumentException("TODO? " + param);
    }


    // ------------------------------------------------------------------------
    //
    //   Scalar, Non-Optional
    //
    // ------------------------------------------------------------------------

    // TODO: asBooleanSupplier?

    private static <T extends GenericByteType<T>> ByteSupplier asByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getByte();
    }

    private static <T extends GenericShortType<T>> ShortSupplier asShortSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getShort();
    }

    private static <T extends GenericIntType<T>> IntSupplier asIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getInt();
    }

    private static <T extends GenericLongType<T>> LongSupplier asLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getLong();
    }

    private static FloatSupplier asFloatSupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> p.getAt().get();
    }

    private static DoubleSupplier asDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> p.getAt().get();
    }



    // ------------------------------------------------------------------------
    //
    //   Vector, Non-Optional
    //
    // ------------------------------------------------------------------------

    // TODO: asBooleanArraySupplier?

    private static <T extends GenericByteType<T>> Supplier<byte[]> asByteArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final byte[] array = new byte[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getByte();
            return array;
        };
    }

    private static <T extends GenericShortType<T>> Supplier<short[]> asShortArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final short[] array = new short[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getShort();
            return array;
        };
    }

    private static <T extends GenericIntType<T>> Supplier<int[]> asIntArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final int[] array = new int[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getInt();
            return array;
        };
    }

    private static <T extends GenericLongType<T>> Supplier<long[]> asLongArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final long[] array = new long[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).getLong();
            return array;
        };
    }

    private static Supplier<float[]> asFloatArraySupplier(final GeffProperty<?> property) {
        final GeffProperty<FloatType> p = Cast.unchecked(property);
        return () -> {
            final int len = (int) p.values().dimension(0);
            final float[] array = new float[len];
            for (int i = 0; i < len; i++)
                array[i] = p.getAt(i).get();
            return array;
        };
    }

    private static Supplier<double[]> asDoubleArraySupplier(final GeffProperty<?> property) {
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

    private static <T extends GenericIntType<T>> Supplier<OptionalInt> asOptionalIntSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? OptionalInt.empty() : OptionalInt.of(p.getAt().getInt());
    }

    private static <T extends GenericLongType<T>> Supplier<OptionalLong> asOptionalLongSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? OptionalLong.empty() : OptionalLong.of(p.getAt().getLong());
    }

    private static Supplier<OptionalDouble> asOptionalDoubleSupplier(final GeffProperty<?> property) {
        final GeffProperty<DoubleType> p = Cast.unchecked(property);
        return () -> p.isMissing() ? OptionalDouble.empty() : OptionalDouble.of(p.getAt().get());
    }

    // TODO: OptionalBoolean ???
    // TODO: OptionalByte ???
    // TODO: OptionalShort ???
    // TODO: OptionalFloat ???

    private static <T> Supplier<Optional<T>> asOptionalSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.isMissing() ? Optional.empty() : Optional.of(p.getAt());
    }



    // ------------------------------------------------------------------------
    //
    //   Vector, Optional
    //
    // ------------------------------------------------------------------------

    // TODO: asOptionalBooleanArraySupplier?

    private static <T extends GenericByteType<T>> Supplier<Optional<byte[]>> asOptionalByteArraySupplier(final GeffProperty<?> property) {
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

    private static <T extends GenericShortType<T>> Supplier<Optional<short[]>> asOptionalShortArraySupplier(final GeffProperty<?> property) {
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

    private static <T extends GenericIntType<T>> Supplier<Optional<int[]>> asOptionalIntArraySupplier(final GeffProperty<?> property) {
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

    private static <T extends GenericLongType<T>> Supplier<Optional<long[]>> asOptionalLongArraySupplier(final GeffProperty<?> property) {
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

    private static Supplier<Optional<float[]>> asOptionalFloatArraySupplier(final GeffProperty<?> property) {
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

    private static Supplier<Optional<double[]>> asOptionalDoubleArraySupplier(final GeffProperty<?> property) {
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
}
