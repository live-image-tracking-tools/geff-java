package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import org.mastodon.geff.imglib2.Maybe.MaybeBoolean;
import org.mastodon.geff.imglib2.Maybe.MaybeByte;
import org.mastodon.geff.imglib2.Maybe.MaybeDouble;
import org.mastodon.geff.imglib2.Maybe.MaybeFloat;
import org.mastodon.geff.imglib2.Maybe.MaybeInt;
import org.mastodon.geff.imglib2.Maybe.MaybeLong;
import org.mastodon.geff.imglib2.Maybe.MaybeShort;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Supplier;

public class Construction {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NodeConstructor {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EdgeConstructor {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface FromProperty {
        String value();

        /**
         * The expected number of elements for constructor from vector properties.
         * If this is -1, any length will match (including var-length).
         */
        int length() default -1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface FromId {
    }

    @FunctionalInterface
    public interface ByteSupplier {
        byte getAsByte();
    }

    @FunctionalInterface
    public interface ShortSupplier {
        short getAsShort();
    }

    @FunctionalInterface
    public interface FloatSupplier {
        float getAsFloat();
    }



    /**
     * Find the method (there must be exactly one) in {@code targetClass} annotated with the given {@code annotationClass}.
     * @param targetClass
     * @param annotationClass
     * @return
     */
    static Method getAnnotatedMethod(final Class<?> targetClass, final Class<? extends Annotation> annotationClass) {

        Method result = null;
        for (final Method method : targetClass.getMethods()) {
            if (method.getAnnotation(annotationClass) != null) {
                if (result != null )
                    throw new IllegalArgumentException("Multiple methods are annotated with @" + annotationClass.getSimpleName());
                result = method;
            }
        }
        if (result == null )
            throw new IllegalArgumentException("No method annotated with @" + annotationClass.getSimpleName() + " found");
        return result;
    }

    /**
     * Extract {@code ConstructorParameter} parameters of the given {@code method}.
     */
    static ResolvedConstructorParameter[] resolveConstructorParameters(final Method method) {
        final Type[] types = method.getGenericParameterTypes();
        final Annotation[][] annotations = method.getParameterAnnotations();
        final ResolvedConstructorParameter[] params = new ResolvedConstructorParameter[types.length];
        for (int i = 0; i < params.length; i++) {
            final ConstructorParameter param = ConstructorParameter.from(types[i], annotations[i]);
            params[i] = resolveConstructorParameter(param);
        }
        return params;
    }




    /**
     * Information about one parameter of a constructor method (annotated with either {@link FromId} or {@link FromProperty}.
     *
     * @param type the {@link java.lang.reflect.Type} of the method parameter
     * @param identifier the property identifier (or "id" if annotation is {@code FromId})
     * @param isId {@code true} if annotation is {@code FromId}, {@code false} otherwise
     * @param length the length, for fixed-length vector parameters. {@code -1} for scalar or var-length vectors parameters.
     */
    // TODO: Keep this internal and just return ResolvedConstructorParameter[] for target method directly
    // TODO: convert record to class (for Java 8)
    private record ConstructorParameter(Type type, String identifier, boolean isId, int length) {

        public ConstructorParameter withType(final Type type) {
            return new ConstructorParameter(type, identifier, isId, length);
        }

        /**
         * Validate an annotated parameter of a constructor method and collect
         * information into {@code ConstructorParameter}.
         * <p>
         * The {@code annotations} of a constructor parameter must contain exactly
         * one {@code @FromProperty} or {@code @FromId} annotation.
         *
         * @param type the type of the parameter
         * @param annotations the annotations on the parameter
         * @return a new {@code ConstructorParameter} instance
         */
        public static ConstructorParameter from(
                final Type type,
                final Annotation[] annotations) {

            final String error = "Every parameter must have exactly one @FromProperty or @FromId annotation";
            ConstructorParameter result = null;
            for (Annotation a : annotations) {
                if (a instanceof FromProperty) {
                    if (result != null)
                        throw new IllegalStateException(error);
                    result = new ConstructorParameter(type, ((FromProperty) a).value(), false, ((FromProperty) a).length());
                } else if (a instanceof FromId) {
                    if (result != null)
                        throw new IllegalStateException(error);
                    result = new ConstructorParameter(type, "id", true, -1);
                }
            }
            if (result == null) {
                throw new IllegalStateException(error);
            }
            return result;
        }
    }

    /**
     *
     * @param identifier property identifier
     * @param propertyType runtime type of a {@code GeffProperty} that matches this constructor parameter
     * @param rawType the {@code Class} of the parameter, or nested {@code Class<T>} if the parameter is {@code Optional<T>} or {@code Maybe<T>}.
     * @param rawOptionalType the {@code Class} of the parameter (for {@code Optional<T>} this is {@code Optional.class})
     */
    // TODO: convert record to class (for Java 8)
    record ResolvedConstructorParameter(String identifier, boolean isId, GeffPropertyType propertyType, Class<?> rawType, Class<?> rawOptionalType) {

        public ResolvedConstructorParameter(String identifier, boolean isId, GeffPropertyType propertyType, Class<?> rawType) {
            this(identifier, isId, propertyType, rawType, null);
        }

        public ResolvedConstructorParameter withOptionalType(Class<?> rawOptionalType) {
            return new ResolvedConstructorParameter(identifier, isId, propertyType.withOptional(true), rawType, rawOptionalType);
        }
    }



    private static ResolvedConstructorParameter resolveConstructorParameter(final ConstructorParameter param) {

        final String identifier = param.identifier();
        final boolean isId = param.isId();
        final Class<?> rawType = getRawType(param.type());

        if (rawType == boolean.class
                || rawType == byte.class
                || rawType == short.class
                || rawType == int.class
                || rawType == long.class
                || rawType == float.class
                || rawType == double.class) {
            final Class<?> type = primitiveToImgLibType(rawType);
            final Dimensions dimensions = new FinalDimensions(new long[0]);
            final GeffPropertyType propertyType = new GeffPropertyType(type, false, false, dimensions);
            return new ResolvedConstructorParameter(identifier, isId, propertyType, rawType);

        } else if (rawType == boolean[].class
                || rawType == byte[].class
                || rawType == short[].class
                || rawType == int[].class
                || rawType == long[].class
                || rawType == float[].class
                || rawType == double[].class) {
            final Class<?> type = primitiveToImgLibType(rawType.getComponentType());
            // NB: if no expected length is specified in the annotation a
            // var-length property will also be accepted. (Or fixed-length,
            // which can always be treated as var-length.)
            boolean varLength = param.length() < 0;
            final Dimensions dimensions = new FinalDimensions(new long[]{varLength ? 0 : param.length()});
            final GeffPropertyType propertyType = new GeffPropertyType(type, varLength, false, dimensions);
            return new ResolvedConstructorParameter(identifier, isId, propertyType, rawType);

        } else if (rawType == MaybeByte.class
                || rawType == MaybeShort.class
                || rawType == MaybeInt.class
                || rawType == MaybeLong.class
                || rawType == MaybeFloat.class
                || rawType == MaybeDouble.class
                || rawType == MaybeBoolean.class
                || rawType == OptionalDouble.class
                || rawType == OptionalLong.class
                || rawType == OptionalInt.class) {
            final Class<?> type = maybeToImgLibType(rawType);
            final Dimensions dimensions = new FinalDimensions(new long[0]);
            final GeffPropertyType propertyType = new GeffPropertyType(type, false, true, dimensions);
            return new ResolvedConstructorParameter(identifier, isId, propertyType, rawType, rawType);

        } else if (rawType == Optional.class || rawType == Maybe.class) {
            final Type inner = ((ParameterizedType) param.type()).getActualTypeArguments()[0];
            final ResolvedConstructorParameter resolved = resolveConstructorParameter(param.withType(inner));
            return resolved.withOptionalType(rawType);

        } else if (Maybe.class.isAssignableFrom(rawType)) {
            final Type inner = getMaybeTypeArgument(param.type());
            final ResolvedConstructorParameter resolved = resolveConstructorParameter(param.withType(inner));
            return resolved.withOptionalType(rawType);

        } else if (rawType == String.class) {
            final Dimensions dimensions = new FinalDimensions(new long[0]);
            final GeffPropertyType propertyType = new GeffPropertyType(rawType, false, false, dimensions);
            return new ResolvedConstructorParameter(identifier, isId, propertyType, rawType, rawType);

        } else if (rawType == GeffProperty.class) {
            // escape hatch for stuff we have not implemented yet.
            return new ResolvedConstructorParameter(identifier, isId, ESCAPE_HATCH, rawType);
        }
        throw new IllegalArgumentException("TODO? " + param);
    }

    private static Class<?> getRawType(Type t) {
        if (t instanceof Class<?>)
            return (Class<?>)t;
        if (t instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) t).getRawType();
        throw new IllegalArgumentException("TODO? " + t);
    }

    private static Class<?> primitiveToImgLibType(Class<?> primitive) {
        if (primitive == boolean.class) {
            return BooleanType.class;
        } else if (primitive == byte.class) {
            return GenericByteType.class;
        } else if (primitive == short.class) {
            return GenericShortType.class;
        } else if (primitive == int.class) {
            return GenericIntType.class;
        } else if (primitive == long.class) {
            return GenericLongType.class;
        } else if (primitive == float.class) {
            return FloatType.class;
        } else if (primitive == double.class) {
            return DoubleType.class;
        }
        throw new IllegalArgumentException("TODO? " + primitive);
    }

    private static Class<?> maybeToImgLibType(Class<?> maybe) {
        if (maybe == MaybeBoolean.class) {
            return BooleanType.class;
        } else if (maybe == MaybeByte.class) {
            return GenericByteType.class;
        } else if (maybe == MaybeShort.class) {
            return GenericShortType.class;
        } else if (maybe == MaybeInt.class || maybe == OptionalInt.class) {
            return GenericIntType.class;
        } else if (maybe == MaybeLong.class || maybe == OptionalLong.class) {
            return GenericLongType.class;
        } else if (maybe == MaybeFloat.class) {
            return FloatType.class;
        } else if (maybe == MaybeDouble.class || maybe == OptionalDouble.class) {
            return DoubleType.class;
        }
        throw new IllegalArgumentException("TODO? " + maybe);
    }

    private static Type getMaybeTypeArgument(final Type maybe) {
        Type type = maybe;
        while (type instanceof Class) {
            type = ((Class<?>) type).getGenericSuperclass();
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType() == Maybe.class) {
                final Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class) {
                    return arg;
                }
            }
        }
        throw new IllegalArgumentException("TODO? " + maybe);
    }

    // We allow to take GeffProperty<?> as a constructor type for a field.
    // This will just match anything, so we bypass conversion checks.
    // This is intended to allow clients to adapt to weird edge cases that break our standard type matching.
    // Ideally, these edge cases should be incorporated into geff-java, but we don't want to block clients while we work on that ...
    static final GeffPropertyType ESCAPE_HATCH = new GeffPropertyType(null, false, false, new FinalDimensions(new long[0]));

    static GeffProperty<?> convertToMatch(
            final GeffProperty<?> sourceProperty,
            final GeffPropertyType targetType)
            throws GeffBindError {

        System.out.println("Construction.convertToMatch");
        System.out.println("  sourceProperty = " + sourceProperty);
        System.out.println("  targetType     = " + targetType);

        final GeffPropertyType sourceType = sourceProperty.propertyType();

        // special cases ...
        if (targetType == ESCAPE_HATCH) {
            return sourceProperty;
        } else if (isVarLengthUInt8Vector(sourceType) && isString(targetType)) {
            return new VarLengthAsStringProperty(Cast.unchecked(sourceProperty));
        }

        if (sourceType.isOptional() && !targetType.isOptional()) {
            // If the target property is expected to always be present but the
            // source property may be missing, then the source property does not
            // match.
            throw new GeffConvertError(sourceType, targetType, "Source type is optional but target type is not.");
        }

        if (sourceType.numDimensions() != targetType.numDimensions()) {
            throw new GeffConvertError(sourceType, targetType, "Dimensionality mismatch.");
        }

        if (!targetType.isVarLength()) {
            if (sourceType.isVarLength()) {
                // If the target property is expected to be fixed-length but the
                // source property is var-length, then the source property does
                // not match.
                throw new GeffConvertError(sourceType, targetType, "Var-length property cannot be converted to fixed-length target type.");
            } else if (!Intervals.equalDimensions(sourceType.dimensions(), targetType.dimensions())) {
                // If both source and target are fixed-length but with different
                // dimensions, then the source property does not match.
                throw new GeffConvertError(sourceType, targetType, "Source and target dimensions don't match.");
            }
        }

        if (targetType.type().isAssignableFrom(sourceType.type())) {
            return sourceProperty;
        } else {
            // NB: we have to convert to a concrete type (e.g., UnsignedLongType instead of GenericLongType).
            return sourceProperty.convert(convertTargetSupplier(targetType.type()));

            // TODO: warn if type conversion loses precision or range?
        }
    }

    private static boolean isString(GeffPropertyType propertyType) {
        return propertyType.numDimensions() == 0 && propertyType.type() == String.class;
    }

    // TODO: maybe doesn't even need to be var-length?
    private static boolean isVarLengthUInt8Vector(final GeffPropertyType propertyType) {
        return propertyType.isVarLength() && propertyType.numDimensions() == 1 && propertyType.type() == UnsignedByteType.class;
    }

    private static Supplier<?> convertTargetSupplier(Class<?> targetType) {
        if (targetType == BooleanType.class) {
            return BoolType::new;
        } else if (targetType == GenericByteType.class) {
            return UnsignedByteType::new;
        } else if (targetType == GenericShortType.class) {
            return UnsignedShortType::new;
        } else if (targetType == GenericIntType.class) {
            return UnsignedIntType::new;
        } else if (targetType == GenericLongType.class) {
            return UnsignedLongType::new;
        } else if (targetType == FloatType.class) {
            return FloatType::new;
        } else if (targetType == DoubleType.class) {
            return DoubleType::new;
        }
        throw new IllegalArgumentException(targetType.toString());
    }





    // ------ exceptions. TODO revise --------

    // TODO: move to separate file
    public static class GeffBindError extends GeffException {

        public GeffBindError(String message) {
            super(message);
        }

        public GeffBindError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // TODO: move to separate file
    public static class GeffConvertError extends GeffBindError {

        public GeffConvertError(GeffPropertyType sourceType, GeffPropertyType targetType, String explanation) {
            super("Cannot convert " + sourceType + " to " + targetType + ": " + explanation);
        }
    }

}
