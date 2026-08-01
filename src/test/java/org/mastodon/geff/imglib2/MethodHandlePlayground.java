package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.Construction.NodeConstructor;
import org.mastodon.geff.imglib2.Construction.FromId;
import org.mastodon.geff.imglib2.Construction.FromProperty;
import org.mastodon.geff.imglib2.Construction.GeffBindError;
import org.mastodon.geff.imglib2.Construction.ResolvedConstructorParameter;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.mastodon.geff.imglib2.Construction.ESCAPE_HATCH;
import static org.mastodon.geff.imglib2.ElementType.NODE;


public class MethodHandlePlayground {

    static class MyBuilder {
        @NodeConstructor
        public void addVertex(
                @FromId() long id,
                @FromProperty("my_x") double x,
                @FromProperty("y") double y,
                @FromProperty("z") double z,
                @FromProperty(value = "covariance2d") double[] cov2d,
                @FromProperty("t") long t) {
            System.out.println("addVertex(id=" + id + ", x=" + x + ", y=" + y + ", z=" + z + ", cov2d=" + Arrays.toString(cov2d) + ", t=" + t + ")");
        }
    }

    public static void main(String[] args) throws Throwable {

        final String path = "cross-language-tests/data/covariance_original.zarr";

        try (final N5Reader n5 = new N5ZarrReader(path)) {
            final GeffProperties props = IoUtils.loadProperties(n5, NODE);
//            props.rename("x", "my_x");
//            final MyBuilder nodeBuilder = new MyBuilder();
            final MyAdvancedBuilder nodeBuilder = new MyAdvancedBuilder();
            buildNodes( nodeBuilder, props);
        }
    }

    static class MyAdvancedBuilder {

        @NodeConstructor
        public void addVertex(
                @FromId() long id,
                @FromProperty("x") double x,
                @FromProperty("y") GeffProperty<DoubleType> y, // escape hatch for stuff we have not implemented yet.
                @FromProperty("t") long t, // automatically type-converted
                @FromProperty("covariance2d") double[] cov2d,
                @FromProperty("covariance3d") Optional<double[]> cov3d) { // for properties that could be missing

            System.out.println("addVertex(id=" + id + ", x=" + x + ", y=" + y.getAt().get() + ", t=" + t + ", cov2d=" + Arrays.toString(cov2d) + ", cov32=" + Arrays.toString(cov3d.orElse(null)) + ")");
        }
    }




    // -------------------------------

    public static void buildNodes(final Object target, final GeffProperties properties) throws Throwable {

        final Method method = Construction.getAnnotatedMethod(target.getClass(), NodeConstructor.class);
        final ResolvedConstructorParameter[] params = Construction.resolveConstructorParameters(method);

        final MethodHandles.Lookup lk = MethodHandles.lookup();
        MethodHandle mh = lk.unreflect(method).bindTo(target);

        for (int i = 0; i < params.length; i++) {
            final ResolvedConstructorParameter param = params[i];
            final MethodHandle supplier = propertyHandle(lk, properties, param);
            mh = collectArguments(mh, 0, supplier);
        }

        for (int i = 0; i < properties.numElements(); i++) {
            properties.elementIndex().index(i);
            mh.invokeExact();
        }
    }



    private static MethodHandle propertyHandle(
            final MethodHandles.Lookup lookup,
            final GeffProperties properties,
            final ResolvedConstructorParameter param
    ) throws NoSuchMethodException, IllegalAccessException, GeffBindError {

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
                    // TODO: support boxed Optional<Double> etc
                    throw new UnsupportedOperationException("TODO: support boxed Optional<Double> etc");

                } else if (targetType.numDimensions() == 1) { // vectors
                    final MethodType omt = methodType(Object.class);

                    if (false) {
                    } else if (rawType == byte[].class) {
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

    @FunctionalInterface
    interface ByteSupplier {
        byte getAsByte();
    }

    private static <T extends GenericByteType<T>> ByteSupplier asByteSupplier(final GeffProperty<?> property) {
        final GeffProperty<T> p = Cast.unchecked(property);
        return () -> p.getAt().getByte();
    }

    @FunctionalInterface
    interface ShortSupplier {
        short getAsShort();
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

    @FunctionalInterface
    interface FloatSupplier {
        float getAsFloat();
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


    static void printMethodInfo() throws NoSuchMethodException {
        Method method = MyBuilder.class.getMethod("addVertex", long.class);
        System.out.println("method = " + method);

        final Parameter[] parameters = method.getParameters();
        System.out.println("parameters = " + Arrays.toString(parameters));

        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        System.out.println("parameterAnnotations = " + Arrays.deepToString(parameterAnnotations));

        final Class<?>[] parameterTypes = method.getParameterTypes();
        System.out.println("parameterTypes = " + Arrays.toString(parameterTypes));

        final AnnotatedType[] annotatedParameterTypes = method.getAnnotatedParameterTypes();
        System.out.println("annotatedParameterTypes = " + Arrays.toString(annotatedParameterTypes));
    }

}
