package org.litt.geff.bind;

import static java.lang.invoke.MethodType.methodType;
import static org.litt.geff.bind.Construction.ESCAPE_HATCH;
import static org.litt.geff.props.Suppliers.asBooleanArraySupplier;
import static org.litt.geff.props.Suppliers.asByteArraySupplier;
import static org.litt.geff.props.Suppliers.asByteSupplier;
import static org.litt.geff.props.Suppliers.asDoubleArraySupplier;
import static org.litt.geff.props.Suppliers.asDoubleSupplier;
import static org.litt.geff.props.Suppliers.asFloatArraySupplier;
import static org.litt.geff.props.Suppliers.asFloatSupplier;
import static org.litt.geff.props.Suppliers.asIntArraySupplier;
import static org.litt.geff.props.Suppliers.asIntSupplier;
import static org.litt.geff.props.Suppliers.asLongArraySupplier;
import static org.litt.geff.props.Suppliers.asLongSupplier;
import static org.litt.geff.props.Suppliers.asMaybeBooleanArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeBooleanSupplier;
import static org.litt.geff.props.Suppliers.asMaybeByteArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeByteSupplier;
import static org.litt.geff.props.Suppliers.asMaybeDoubleArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeDoubleSupplier;
import static org.litt.geff.props.Suppliers.asMaybeFloatArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeFloatSupplier;
import static org.litt.geff.props.Suppliers.asMaybeIntArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeIntSupplier;
import static org.litt.geff.props.Suppliers.asMaybeLongArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeLongSupplier;
import static org.litt.geff.props.Suppliers.asMaybeShortArraySupplier;
import static org.litt.geff.props.Suppliers.asMaybeShortSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedBooleanSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedByteSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedDoubleSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedFloatSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedIntSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedLongSupplier;
import static org.litt.geff.props.Suppliers.asOptionalBoxedShortSupplier;
import static org.litt.geff.props.Suppliers.asOptionalByteArraySupplier;
import static org.litt.geff.props.Suppliers.asOptionalDoubleArraySupplier;
import static org.litt.geff.props.Suppliers.asOptionalDoubleSupplier;
import static org.litt.geff.props.Suppliers.asOptionalFloatArraySupplier;
import static org.litt.geff.props.Suppliers.asOptionalIntArraySupplier;
import static org.litt.geff.props.Suppliers.asOptionalIntSupplier;
import static org.litt.geff.props.Suppliers.asOptionalLongArraySupplier;
import static org.litt.geff.props.Suppliers.asOptionalLongSupplier;
import static org.litt.geff.props.Suppliers.asOptionalShortArraySupplier;
import static org.litt.geff.props.Suppliers.asOptionalSupplier;
import static org.litt.geff.props.Suppliers.asShortArraySupplier;
import static org.litt.geff.props.Suppliers.asShortSupplier;

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

import org.litt.geff.props.FunctionTypes.ByteSupplier;
import org.litt.geff.props.FunctionTypes.FloatSupplier;
import org.litt.geff.props.FunctionTypes.ShortSupplier;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;
import org.litt.geff.props.GeffPropertyType;
import org.litt.geff.props.Maybe;
import org.litt.geff.props.Maybe.MaybeBoolean;
import org.litt.geff.props.Maybe.MaybeBooleanArray;
import org.litt.geff.props.Maybe.MaybeByte;
import org.litt.geff.props.Maybe.MaybeByteArray;
import org.litt.geff.props.Maybe.MaybeDouble;
import org.litt.geff.props.Maybe.MaybeDoubleArray;
import org.litt.geff.props.Maybe.MaybeFloat;
import org.litt.geff.props.Maybe.MaybeFloatArray;
import org.litt.geff.props.Maybe.MaybeInt;
import org.litt.geff.props.Maybe.MaybeIntArray;
import org.litt.geff.props.Maybe.MaybeLong;
import org.litt.geff.props.Maybe.MaybeLongArray;
import org.litt.geff.props.Maybe.MaybeShort;
import org.litt.geff.props.Maybe.MaybeShortArray;

class ConstructorBinding
{

	/**
	 * Create a {@code T() MethodHandle} where {@code T} is the type of the
	 * provided constructor-method {@code param}. The returned method-handle can
	 * be used as a filter to bind that constructor parameter in the
	 * constructor-method handle.
	 *
	 * @param lookup
	 *            the {@code MethodHandles.Lookup} to use
	 * @param properties
	 *            all properties, the property identified by {@code param} will
	 *            be extracted and converted to the required type if possible
	 * @param param
	 *            describes one parameter of the constructor method
	 * @return a method handle of type {@code T()}
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws GeffBindError
	 */
	static MethodHandle propertyHandle(
			final MethodHandles.Lookup lookup,
			final GeffProperties properties,
			final Construction.ResolvedConstructorParameter param ) throws NoSuchMethodException, IllegalAccessException, GeffBindError
	{

		final GeffPropertyType targetType = param.propertyType();
		final GeffProperty< ? > sourceProperty = param.isId()
				? properties.id()
				: properties.property( param.identifier() );
		final GeffProperty< ? > property = Construction.convertToMatch( sourceProperty, targetType );

		if ( targetType == ESCAPE_HATCH )
		{
			final Supplier< GeffProperty< ? > > s = () -> property;
			return lookup.findVirtual( Supplier.class, "get", methodType( Object.class ) ).bindTo( s )
					.asType( methodType( GeffProperty.class ) );
		}

		if ( targetType.isOptional() )
		{
			final Class< ? > rawOptionalType = param.rawOptionalType();
			final MethodType mt = methodType( rawOptionalType );
			final MethodType omt = methodType( Object.class );

			// scalars (Optional)
			if ( rawOptionalType == OptionalInt.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asOptionalIntSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == OptionalLong.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asOptionalLongSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == OptionalDouble.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asOptionalDoubleSupplier( property ) )
						.asType( mt );

				// scalars (Maybel)
			}
			else if ( rawOptionalType == MaybeByte.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeByteSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeShort.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeShortSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeInt.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeIntSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeLong.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeLongSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeFloat.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeFloatSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeDouble.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeDoubleSupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeBoolean.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeBooleanSupplier( property ) )
						.asType( mt );

				// vectors (Maybe)
			}
			else if ( rawOptionalType == MaybeByteArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeByteArraySupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeShortArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeShortArraySupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeIntArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeIntArraySupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeLongArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeLongArraySupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeFloatArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeFloatArraySupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeDoubleArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeDoubleArraySupplier( property ) )
						.asType( mt );
			}
			else if ( rawOptionalType == MaybeBooleanArray.class )
			{
				return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asMaybeBooleanArraySupplier( property ) )
						.asType( mt );
			}
			else if ( Maybe.class.isAssignableFrom( rawOptionalType ) )
			{
				final Class< ? > rawType = param.rawType();
				if ( targetType.numDimensions() == 1 )
				{ // vectors
					if ( rawType == byte[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asMaybeByteArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == short[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asMaybeShortArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == int[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asMaybeIntArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == long[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asMaybeLongArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == float[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asMaybeFloatArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == double[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asMaybeDoubleArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == boolean[].class )
					{ return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asMaybeBooleanArraySupplier( property ) )
							.asType( mt ); }
				}

			}
			else if ( rawOptionalType == Optional.class )
			{
				final Class< ? > rawType = param.rawType();
				if ( targetType.numDimensions() == 0 )
				{ // scalars

					if ( rawType == String.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Byte.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalBoxedByteSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Short.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalBoxedShortSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Integer.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalBoxedIntSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Long.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalBoxedLongSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Float.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalBoxedFloatSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Double.class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalBoxedDoubleSupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == Boolean.class )
					{ return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asOptionalBoxedBooleanSupplier( property ) )
							.asType( mt ); }
					// fall through to the error below

				}
				else if ( targetType.numDimensions() == 1 )
				{ // vectors
					if ( rawType == byte[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalByteArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == short[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalShortArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == int[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalIntArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == long[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalLongArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == float[].class )
					{
						return lookup
								.findVirtual( Supplier.class, "get", omt )
								.bindTo( asOptionalFloatArraySupplier( property ) )
								.asType( mt );
					}
					else if ( rawType == double[].class )
					{ return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asOptionalDoubleArraySupplier( property ) )
							.asType( mt ); }
				}
			}
			else
			{
				throw new GeffBindError(
						"Cannot bind constructor parameter \"" + param.identifier() + "\": the optional type "
								+ rawOptionalType.getName() + " is not supported. Supported types are OptionalInt, OptionalLong, "
								+ "OptionalDouble, Optional<Byte/Short/Integer/Long/Float/Double/Boolean/String>, the Maybe* types, "
								+ "and their array variants." );
			}

		}
		else
		{ // !isOptional
			final Class< ? > rawType = param.rawType();
			final MethodType mt = methodType( rawType );

			if ( targetType.numDimensions() == 0 )
			{ // scalars
				if ( rawType == byte.class )
				{
					return lookup
							.findVirtual( ByteSupplier.class, "getAsByte", mt )
							.bindTo( asByteSupplier( property ) );
				}
				else if ( rawType == short.class )
				{
					return lookup
							.findVirtual( ShortSupplier.class, "getAsShort", mt )
							.bindTo( asShortSupplier( property ) );
				}
				else if ( rawType == int.class )
				{
					return lookup
							.findVirtual( IntSupplier.class, "getAsInt", mt )
							.bindTo( asIntSupplier( property ) );
				}
				else if ( rawType == long.class )
				{
					return lookup
							.findVirtual( LongSupplier.class, "getAsLong", mt )
							.bindTo( asLongSupplier( property ) );
				}
				else if ( rawType == float.class )
				{
					return lookup
							.findVirtual( FloatSupplier.class, "getAsFloat", mt )
							.bindTo( asFloatSupplier( property ) );
				}
				else if ( rawType == double.class )
				{ return lookup
						.findVirtual( DoubleSupplier.class, "getAsDouble", mt )
						.bindTo( asDoubleSupplier( property ) ); }

			}
			else if ( targetType.numDimensions() == 1 )
			{ // vectors
				final MethodType omt = methodType( Object.class );
				if ( rawType == byte[].class )
				{
					return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asByteArraySupplier( property ) )
							.asType( mt );
				}
				else if ( rawType == short[].class )
				{
					return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asShortArraySupplier( property ) )
							.asType( mt );
				}
				else if ( rawType == int[].class )
				{
					return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asIntArraySupplier( property ) )
							.asType( mt );
				}
				else if ( rawType == long[].class )
				{
					return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asLongArraySupplier( property ) )
							.asType( mt );
				}
				else if ( rawType == float[].class )
				{
					return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asFloatArraySupplier( property ) )
							.asType( mt );
				}
				else if ( rawType == double[].class )
				{
					return lookup
							.findVirtual( Supplier.class, "get", omt )
							.bindTo( asDoubleArraySupplier( property ) )
							.asType( mt );
				}
				else if ( rawType == boolean[].class )
				{ return lookup
						.findVirtual( Supplier.class, "get", omt )
						.bindTo( asBooleanArraySupplier( property ) )
						.asType( mt ); }
			}
		}

		throw new GeffBindError(
				"Cannot bind constructor parameter \"" + param.identifier() + "\" of type " + param.rawType().getName()
						+ " to the property " + param.propertyType()
						+ ". Supported types are the primitive scalars byte, short, int, long, float, double; "
						+ "the arrays byte[], short[], int[], long[], float[], double[], boolean[]; "
						+ "String; the Optional and Maybe variants of the above; and GeffProperty<?> as an escape hatch." );
	}
}
