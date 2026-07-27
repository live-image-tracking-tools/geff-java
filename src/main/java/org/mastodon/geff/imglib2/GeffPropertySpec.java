package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.type.NativeType;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;

import java.util.EnumMap;


/**
 * Describes a property type.
 *
 * @param identifier the identifier of the property
 * @param elementType whether the
 * @param isVarLength
 * @param isOptional
 * @param dType
 * @param numDimensions
 * @param dimensions
 */
// TODO: convert record to class (for Java 8)
public record GeffPropertySpec(
        String identifier,
        ElementType elementType,
        boolean isVarLength,
        boolean isOptional,
        DType dType,
        int numDimensions,
        Dimensions dimensions) {

    @Override
    public String toString() {
        return "GeffPropertySpec[" +
                "identifier='" + identifier + '\'' +
                ", elementType=" + elementType +
                ", isVarLength=" + isVarLength +
                ", isOptional=" + isOptional +
                ", dType=" + dType +
                ", numDimensions=" + numDimensions +
                ", dimensions=" + (dimensions == null ? "null" : "{" + Intervals.toString(dimensions) + "}") +
                ']';
    }
}
