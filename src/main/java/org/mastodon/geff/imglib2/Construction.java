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
import net.imglib2.util.Intervals;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Supplier;

public class Construction {

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


    /**
     *
     * @param type
     * @param identifier
     * @param isId
     * @param length
     */
    // TODO: convert record to class (for Java 8)
    record ConstructorParameter(Type type, String identifier, boolean isId, int length) {

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
     * @param propertyType
     * @param rawType
     * @param rawOptionalType
     */
    // TODO: convert record to class (for Java 8)
    record ResolvedConstructorParameter(GeffPropertyType propertyType, Class<?> rawType, Class<?> rawOptionalType) {

        public ResolvedConstructorParameter(GeffPropertyType propertyType, Class<?> rawType) {
            this(propertyType, rawType, null);
        }

        public ResolvedConstructorParameter withOptionalType(Class<?> rawOptionalType) {
            return new ResolvedConstructorParameter(propertyType.withOptional(true), rawType, rawOptionalType);
        }
    }



    static ResolvedConstructorParameter resolveConstructorParameter(final ConstructorParameter param) {

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
            return new ResolvedConstructorParameter(propertyType, rawType);

        } else if (rawType == byte[].class
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
            return new ResolvedConstructorParameter(propertyType, rawType);

        } else if (rawType == Optional.class) {
            final Type inner = ((ParameterizedType) param.type()).getActualTypeArguments()[0];
            final ResolvedConstructorParameter resolved = resolveConstructorParameter(param.withType(inner));
            return resolved.withOptionalType(rawType);

        } else if (rawType == OptionalDouble.class
                || rawType == OptionalLong.class
                || rawType == OptionalInt.class) {
            final Class<?> type = primitiveToImgLibType(rawType);
            final Dimensions dimensions = new FinalDimensions(new long[0]);
            final GeffPropertyType propertyType = new GeffPropertyType(type, false, true, dimensions);
            return new ResolvedConstructorParameter(propertyType, rawType, rawType);

        } else if (rawType == GeffProperty.class) {
            // escape hatch for stuff we have not implemented yet.
            return new ResolvedConstructorParameter(ESCAPE_HATCH, rawType);
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
        } else if (primitive == int.class || primitive == OptionalInt.class) {
            return GenericIntType.class;
        } else if (primitive == long.class || primitive == OptionalLong.class) {
            return GenericLongType.class;
        } else if (primitive == float.class) {
            return FloatType.class;
        } else if (primitive == double.class || primitive == OptionalDouble.class) {
            return DoubleType.class;
        }
        throw new IllegalArgumentException("TODO? " + primitive);
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

        if (targetType == ESCAPE_HATCH) {
            return sourceProperty;
        }

        final GeffPropertyType sourceType = sourceProperty.propertyType();

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

    public static class GeffException extends Exception {

        public GeffException(String message) {
            super(message);
        }

        public GeffException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GeffBindError extends GeffException {

        public GeffBindError(String message) {
            super(message);
        }

        public GeffBindError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GeffConvertError extends GeffBindError {

        public GeffConvertError(GeffPropertyType sourceType, GeffPropertyType targetType, String explanation) {
            super("Cannot convert " + sourceType + " to " + targetType + ": " + explanation);
        }
    }

}
