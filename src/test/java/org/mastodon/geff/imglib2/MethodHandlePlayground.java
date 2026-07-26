package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.mastodon.geff.imglib2.ElementType.NODE;


public class MethodHandlePlayground {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface FromProperty {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface FromId {
    }

    static class Builder {

        public void addVertex(
                @FromId() final long id,
                @FromProperty("x") final double x,
                @FromProperty("y") final GeffProperty<DoubleType> y, // escape hatch for stuff we have not implemented yet. NOT TYPE-SAFE!
                @FromProperty("covariance3d") final double[] cov) {
            System.out.println("Client.addVertex");
            System.out.println("  id = " + id);
            System.out.println("  x = " + x);
            System.out.println("  y = " + y.getAt().get());
            System.out.println("  cov = " + Arrays.toString(cov));
        }
    }







    public static void main(String[] args) throws Throwable {

        final Builder target = new Builder();




        final GeffProperties properties = load();
        System.out.println("properties = " + properties);




        final Method[] methods = target.getClass().getMethods();
        final Method method = Arrays.stream(methods).filter(m -> m.getName().equals("addVertex")).findFirst().get();
        // TODO should find method by annotation...

        System.out.println("method = " + method);

        final AttributeParam[] attributeParams = getAttributes(method);
        System.out.println("attributeParams = " + Arrays.toString(attributeParams));






        final MethodHandles.Lookup lk = MethodHandles.lookup();
        MethodHandle mh = lk.unreflect(method).bindTo(target);

        for(int i = 0; i < attributeParams.length; i++) {
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

        if (param.type() == long.class) {
            checkTypeMatch(property.type().getClass(), UnsignedLongType.class);
            final LongSupplier s = asLongSupplier(Cast.unchecked(property));
            return lookup.findVirtual(LongSupplier.class, "getAsLong", methodType(long.class)).bindTo(s);
        } else if (param.type() == double.class) {
            checkTypeMatch(property.type().getClass(), DoubleType.class);
            final DoubleSupplier s = asDoubleSupplier(Cast.unchecked(property));
            return lookup.findVirtual(DoubleSupplier.class, "getAsDouble", methodType(double.class)).bindTo(s);
        } else if (param.type() == double[].class) {
            checkTypeMatch(property.type().getClass(), DoubleType.class);
            final Supplier<double[]> s = asDoubleArraySupplier(Cast.unchecked(property));
            return lookup.findVirtual(Supplier.class, "get", methodType(Object.class)).bindTo(s)
                    .asType(methodType(param.type()));
        } else if (param.type() == GeffProperty.class) {
            final Supplier<GeffProperty> s = () -> property;
            return lookup.findVirtual(Supplier.class, "get", methodType(Object.class)).bindTo(s)
                    .asType(methodType(param.type()));
        }

        System.out.println("lookup = " + lookup + ", properties = " + properties + ", param = " + param);
        throw new IllegalArgumentException("TODO");
    }

    private static LongSupplier asLongSupplier(final GeffProperty<UnsignedLongType> property) {
        return () -> property.getAt().get();
    }

    private static DoubleSupplier asDoubleSupplier(final GeffProperty<DoubleType> property) {
        return () -> property.getAt().get();
    }

    private static Supplier<double[]> asDoubleArraySupplier(final GeffProperty<DoubleType> property) {
        return () -> {
            final int len = (int) property.values().dimension(0);
            final double[] array = new double[len];
            for (int i = 0; i < len; i++) {
                array[i] = property.getAt(i).get();
            }
            return array;
        };
    }

    private static void checkTypeMatch(final Class<?> expected, final Class<?> actual) {
        if (!expected.isAssignableFrom(actual))
            throw new IllegalArgumentException("wrong property type: " + actual.getSimpleName() + " (expected " + expected.getSimpleName() + ")");
    }




    record AttributeParam(Class<?> type, String identifier, boolean isId) {}

    private static AttributeParam[] getAttributes(final Method method) {
        final Class<?>[] types = method.getParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        final AttributeParam[] params = new AttributeParam[types.length];
        Arrays.setAll(params, i -> resolveParameter(types[i], annotations[i]));
        return params;
    }

    private static AttributeParam resolveParameter(
            final Class<?> parameterType,
            final Annotation[] parameterAnnotations) {

        final String error = "Every parameter must have exactly one @FromProperty or @FromId annotation";
        AttributeParam result = null;
        for (Annotation a : parameterAnnotations) {
            if (a instanceof FromProperty) {
                if (result != null)
                    throw new IllegalStateException(error);
                result = new AttributeParam(parameterType, ((FromProperty) a).value(), false);
            }
            else if (a instanceof FromId) {
                if (result != null)
                    throw new IllegalStateException(error);
                result = new AttributeParam(parameterType, "id", true);
            }
        }
        if (result == null) {
            throw new IllegalStateException(error);
        }
        return result;
    }



    private static GeffProperties load() {
        final String path = "cross-language-tests/data/covariance_original.zarr";
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {
            final GeffPropertySpecs specs = IoUtils.loadPropertySpecs(n5, NODE);
            return IoUtils.loadProperties(n5, specs);
        }
    }



    static void printMethodInfo() throws NoSuchMethodException {
        Method method = Builder.class.getMethod("addVertex", long.class);
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
