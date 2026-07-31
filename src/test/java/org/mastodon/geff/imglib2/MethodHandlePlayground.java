package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.Construction.AttributeParam;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.mastodon.geff.imglib2.ElementType.NODE;


public class MethodHandlePlayground {

    static class MyBuilder {
        public void addVertex(
                @Construction.FromId() long id,
                @Construction.FromProperty("my_x") double x,
                @Construction.FromProperty("y") double y,
                @Construction.FromProperty("z") double z,
                @Construction.FromProperty(value = "covariance2d") double[] cov2d,
                @Construction.FromProperty("t") long t) {
            System.out.println("addVertex(id=" + id + ", x=" + x + ", y=" + y + ", z=" + z + ", cov2d=" + Arrays.toString(cov2d) + ", t=" + t + ")");
        }
    }

    public static void main(String[] args) throws Throwable {

        final String path = "cross-language-tests/data/covariance_original.zarr";

        try (final N5Reader n5 = new N5ZarrReader(path)) {
            final GeffProperties props = IoUtils.loadProperties(n5, NODE);
            props.rename("x", "my_x");
            System.out.println("props = " + props);
            final MyBuilder nodeBuilder = new MyBuilder();
            buildNodes( nodeBuilder, props);
        }
    }

    static class MyAdvancedBuilder {

        public void addVertex(
                @Construction.FromId() long id,
                @Construction.FromProperty("x") double x,
                @Construction.FromProperty("y") GeffProperty<DoubleType> y, // escape hatch for stuff we have not implemented yet.
                @Construction.FromProperty("t") long t, // automatically type-converted
                @Construction.FromProperty("covariance2d") double[] cov2d,
                @Construction.FromProperty("covariance3d") Optional<double[]> cov3d) { // for properties that could be missing

            System.out.println("addVertex(id=" + id + ", x=" + x + ", y=" + y.getAt().get() + ", t=" + t + ", cov2d=" + Arrays.toString(cov2d) + ", cov32=" + Arrays.toString(cov3d.orElse(null)) + ")");
        }
    }




    // -------------------------------


    public static void buildNodes(final Object target, final GeffProperties properties) throws Throwable {

        final Method[] methods = target.getClass().getMethods();
        final Method method = Arrays.stream(methods).filter(m -> m.getName().equals("addVertex")).findFirst().get();
        // TODO should find method by annotation...

        System.out.println("method = " + method);

        final AttributeParam[] attributeParams = getAttributes(method);
        System.out.println("attributeParams = " + Arrays.toString(attributeParams));

        for ( AttributeParam param : attributeParams ) {
            System.out.println("param = " + param);
            System.out.println("propertyType(param) = " + Construction.propertyType(param));
            System.out.println();
        }

        final MethodHandles.Lookup lk = MethodHandles.lookup();
        MethodHandle mh = lk.unreflect(method).bindTo(target);

        for (int i = 0; i < attributeParams.length; i++) {
            final MethodHandle supplier = propertyHandle(lk, properties, attributeParams[i]);
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
            final AttributeParam param
    ) throws NoSuchMethodException, IllegalAccessException {

        final GeffProperty<?> property = param.isId()
                ? properties.id()
                : properties.property(param.identifier());
        final Type rawType = getRawType(param.type());
        if (rawType == long.class) {
            final GeffProperty<?> longProperty = property.type() instanceof GenericLongType ? property : property.convert(LongType::new);
//            checkTypeMatch(property.type().getClass(), UnsignedLongType.class);
            final LongSupplier s = asLongSupplier(Cast.unchecked(longProperty));
            return lookup.findVirtual(LongSupplier.class, "getAsLong", methodType(long.class)).bindTo(s);
        } else if (rawType == double.class) {
            checkTypeMatch(property.type().getClass(), DoubleType.class);
            final DoubleSupplier s = asDoubleSupplier(Cast.unchecked(property));
            return lookup.findVirtual(DoubleSupplier.class, "getAsDouble", methodType(double.class)).bindTo(s);
        } else if (rawType == double[].class) {
            checkTypeMatch(property.type().getClass(), DoubleType.class);
            final Supplier<double[]> s = asDoubleArraySupplier(Cast.unchecked(property));
            return lookup.findVirtual(Supplier.class, "get", methodType(Object.class)).bindTo(s)
                    .asType(methodType(double[].class));
        } else if (rawType == GeffProperty.class) {
            final Supplier<GeffProperty<?>> s = () -> property;
            return lookup.findVirtual(Supplier.class, "get", methodType(Object.class)).bindTo(s)
                    .asType(methodType(GeffProperty.class));
        } else if (rawType == Optional.class) {
            final Type type = ((ParameterizedType) param.type()).getActualTypeArguments()[0];
            if (type == double[].class) {
                checkTypeMatch(property.type().getClass(), DoubleType.class);
                final Supplier<Optional<double[]>> s = asOptionalDoubleArraySupplier(Cast.unchecked(property));
                return lookup.findVirtual(Supplier.class, "get", methodType(Object.class)).bindTo(s)
                        .asType(methodType(Optional.class));

            }

        }

        System.out.println("param = " + param);
        throw new IllegalArgumentException("TODO");
    }

    private static <T extends GenericLongType<T>> LongSupplier asLongSupplier(final GeffProperty<T> property) {
        return () -> property.getAt().getLong();
    }

    private static DoubleSupplier asDoubleSupplier(final GeffProperty<DoubleType> property) {
        return () -> property.getAt().get();
    }

    private static Supplier<double[]> asDoubleArraySupplier(final GeffProperty<DoubleType> property) {
        return () -> {
            final int len = (int) property.values().dimension(0);
            final double[] array = new double[len];
            for (int i = 0; i < len; i++)
                array[i] = property.getAt(i).get();
            return array;
        };
    }

    private static Supplier<Optional<double[]>> asOptionalDoubleArraySupplier(final GeffProperty<DoubleType> property) {
        return () -> {
            if (property.isMissing())
                return Optional.empty();
            final int len = (int) property.values().dimension(0);
            final double[] array = new double[len];
            for (int i = 0; i < len; i++)
                array[i] = property.getAt(i).get();
            return Optional.of(array);
        };
    }

    private static void checkTypeMatch(final Class<?> expected, final Class<?> actual) {
        if (!expected.isAssignableFrom(actual))
            throw new IllegalArgumentException("wrong property type: " + actual.getSimpleName() + " (expected " + expected.getSimpleName() + ")");
    }


    private static Class<?> getRawType(Type t) {
        if (t instanceof Class<?>)
            return (Class<?>)t;
        if (t instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) t).getRawType();
        throw new IllegalArgumentException("TODO");
    }






    private static AttributeParam[] getAttributes(final Method method) {
        final Type[] types = method.getGenericParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        final AttributeParam[] params = new AttributeParam[types.length];
        Arrays.setAll(params, i -> Construction.resolveParameter(types[i], annotations[i]));
        return params;
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
