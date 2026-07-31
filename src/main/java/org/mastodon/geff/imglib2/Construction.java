package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

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


    // TODO: convert record to class (for Java 8)
    record AttributeParam(Type type, String identifier, boolean isId, int length) {
    }

    static AttributeParam resolveParameter(
            final Type parameterType,
            final Annotation[] parameterAnnotations) {

        final String error = "Every parameter must have exactly one @FromProperty or @FromId annotation";
        AttributeParam result = null;
        for (Annotation a : parameterAnnotations) {
            if (a instanceof FromProperty) {
                if (result != null)
                    throw new IllegalStateException(error);
                result = new AttributeParam(parameterType, ((FromProperty) a).value(), false, ((FromProperty) a).length());
            }
            else if (a instanceof FromId) {
                if (result != null)
                    throw new IllegalStateException(error);
                result = new AttributeParam(parameterType, "id", true, -1);
            }
        }
        if (result == null) {
            throw new IllegalStateException(error);
        }
        return result;
    }







    static GeffProperty<?> convertToMatch(final GeffProperty<?> property, final GeffPropertyType targetType) {

        final GeffPropertyType sourceType = property.propertyType();


        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE
        // TODO CONTINUE HERE


        // TODO:
        //    [ ] create GeffConvertError extends GeffBindError extends GeffException extends Exception
        //        should contain description what went wrong, so that client can log it to the user later
        //    [ ] implement convertToMatch()
        //        [ ] check isOptional
        //        [ ] check numDimensions
        //        [ ] check isVarLength
        //        [ ] check dimensions
        //        [ ] convert property type
        //    [ ] use convertToMatch() in MethodHandlePlayground




    }


    static GeffPropertyType propertyType(final AttributeParam param) {

        final Type rawType = getRawType(param.type());
        if (rawType == boolean.class
                || rawType == byte.class
                || rawType == short.class
                || rawType == int.class
                || rawType == long.class
                || rawType == float.class
                || rawType == double.class) {
            final Class<?> type = primitiveToImgLibType((Class<?>) rawType);
            final boolean varLength = false;
            final boolean optional = false;
            final Dimensions dimensions = new FinalDimensions(new long[0]);
            return new GeffPropertyType(type, varLength, optional, dimensions);
        } else if (rawType == byte[].class
                || rawType == short[].class
                || rawType == int[].class
                || rawType == long[].class
                || rawType == float[].class
                || rawType == double[].class) {
            final Class<?> type = ((Class<?>) rawType).getComponentType();
            // NB: is no expected length is specified in the annotation a
            // var-length property will also be accepted. (Or fixed-length,
            // which can always be treated as var-length.)
            boolean varLength = param.length() < 0;
            final boolean optional = false;
            final Dimensions dimensions = new FinalDimensions(new long[]{varLength ? 0 : param.length()});
            return new GeffPropertyType(type, varLength, optional, dimensions);
        } else if (rawType == Optional.class) {

            // TODO
//            Optional
//            OptionalLong
//            OptionalInt
//            OptionalDouble

//            TODO: [ ] add OptionalFloat (or find impl (Guava?)
//            TODO: [ ] add OptionalByte (or find impl (Guava?)
//            TODO: [ ] add OptionalShort (or find impl (Guava?)
//            TODO: [ ] add OptionalBoolean (or find impl (Guava?)

            throw new UnsupportedOperationException();
        }

        // TODO
        throw new UnsupportedOperationException();
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
        throw new IllegalArgumentException();
    }


}
