package org.mastodon.geff.imglib2;

import org.mastodon.geff.imglib2.Construction.ByteSupplier;
import org.mastodon.geff.imglib2.Construction.FloatSupplier;
import org.mastodon.geff.imglib2.Construction.ShortSupplier;
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
import static org.mastodon.geff.imglib2.Suppliers.asBooleanArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asByteArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asByteSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asDoubleArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asDoubleSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asFloatArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asFloatSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asIntArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asIntSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asLongArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asLongSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeBooleanArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeBooleanSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeByteArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeByteSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeDoubleArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeDoubleSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeFloatArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeFloatSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeIntArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeIntSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeLongArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeLongSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeShortArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asMaybeShortSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalByteArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalDoubleArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalDoubleSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalFloatArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalIntArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalIntSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalLongArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalLongSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalShortArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asOptionalSupplier;
import static org.mastodon.geff.imglib2.Suppliers.asShortArraySupplier;
import static org.mastodon.geff.imglib2.Suppliers.asShortSupplier;

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
            final MethodType omt = methodType(Object.class);

            // scalars (Optional)
            if (rawOptionalType == OptionalInt.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asOptionalIntSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == OptionalLong.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asOptionalLongSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == OptionalDouble.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asOptionalDoubleSupplier(property))
                        .asType(mt);

            // scalars (Maybel)
            } else if (rawOptionalType == MaybeByte.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeByteSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeShort.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeShortSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeInt.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeIntSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeLong.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeLongSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeFloat.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeFloatSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeDouble.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeDoubleSupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeBoolean.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeBooleanSupplier(property))
                        .asType(mt);

            // vectors (Maybe)
            } else if (rawOptionalType == MaybeByteArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeByteArraySupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeShortArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeShortArraySupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeIntArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeIntArraySupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeLongArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeLongArraySupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeFloatArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeFloatArraySupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeDoubleArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeDoubleArraySupplier(property))
                        .asType(mt);
            } else if (rawOptionalType == MaybeBooleanArray.class) {
                return lookup
                        .findVirtual(Supplier.class, "get", omt)
                        .bindTo(asMaybeBooleanArraySupplier(property))
                        .asType(mt);
            } else if (Maybe.class.isAssignableFrom(rawOptionalType)) {
                final Class<?> rawType = param.rawType();
                if (targetType.numDimensions() == 1) { // vectors
                    if (rawType == byte[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeByteArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == short[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeShortArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == int[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeIntArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == long[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeLongArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == float[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeFloatArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == double[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeDoubleArraySupplier(property))
                                .asType(mt);
                    } else if (rawType == boolean[].class) {
                        return lookup
                                .findVirtual(Supplier.class, "get", omt)
                                .bindTo(asMaybeBooleanArraySupplier(property))
                                .asType(mt);
                    }
                }

            } else if (rawOptionalType == Optional.class) {
                final Class<?> rawType = param.rawType();
                if( targetType.numDimensions() == 0 ) { // scalars

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
                } else if (rawType == boolean[].class) {
                    return lookup
                            .findVirtual(Supplier.class, "get", omt)
                            .bindTo(asBooleanArraySupplier(property))
                            .asType(mt);
                }
            }
        }

        throw new IllegalArgumentException("TODO? " + param);
    }
}
