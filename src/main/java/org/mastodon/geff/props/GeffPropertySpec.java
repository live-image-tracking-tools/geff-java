package org.mastodon.geff.props;

import org.janelia.saalfeldlab.n5.zarr.DType;

import net.imglib2.Dimensions;
import net.imglib2.util.Intervals;

/**
 * Describes a property type.
 *
 * @param identifier
 *            the identifier of the property
 * @param elementType
 *            whether the
 * @param isVarLength
 * @param isOptional
 * @param dType
 * @param dimensions
 */
public record GeffPropertySpec(
		String identifier,
		ElementType elementType,
		boolean isVarLength,
		boolean isOptional,
		DType dType,
		Dimensions dimensions )
{

	@Override
	public String toString()
	{
		return "GeffPropertySpec[" +
				"identifier='" + identifier + '\'' +
				", elementType=" + elementType +
				", isVarLength=" + isVarLength +
				", isOptional=" + isOptional +
				", dType=" + dType +
				", numDimensions=" + numDimensions() +
				", dimensions=" + ( dimensions == null ? "null" : "{" + Intervals.toString( dimensions ) + "}" ) +
				']';
	}

	public int numDimensions()
	{
		return dimensions.numDimensions();
	}
}
