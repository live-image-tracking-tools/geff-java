package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;

/**
 * Describes the runtime type of a {@code GeffProperty}:
 * <ul>
 *     <li>the ImgLib2 type of the property values</li>
 *     <li>whether the property is var-length (or fixed-length)</li>
 *     <li>whether the property is optional (that is, may be undefined for some elements)</li>
 *     <li>the dimensions of the property (per element)</li>
 * </ul>
 * For var-length properties, the dimensions may vary per element.
 * By convention, the dimensions are specified as {@code [0,..., 0]} for var-length properties.
 * Only the number of dimensions is relevant (and is the same for all elements).
 * <p>
 * The {@code GeffPropertyType} determines whether properties can be converted
 * into each other and which field constructor types they may bind to.
 *
 * @param type the Class of property values (e.g. {@code DoubleType.class}, {@code GenericLongType.class}, {@code String.class})
 * @param isVarLength {@code true} if the property is var-length
 * @param isOptional {@code true} if the property is optional (that is, may be undefined for some elements)
 * @param dimensions the dimensions of the property (per element)
 */
// TODO: convert record to class (for Java 8)
public record GeffPropertyType(
        Class<?> type,
        boolean isVarLength,
        boolean isOptional,
        Dimensions dimensions) {

    public int numDimensions() {
        return dimensions.numDimensions();
    }
}
