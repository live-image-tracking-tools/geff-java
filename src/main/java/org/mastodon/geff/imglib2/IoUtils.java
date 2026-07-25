package org.mastodon.geff.imglib2;

import com.google.gson.reflect.TypeToken;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.type.numeric.IntegerType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.PropMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Reading/writing from/to Zarr
 */
public class IoUtils {











    /**
     * Read all {@link GeffPropertySpecs} for edges or nodes of a geff hierarchy
     * at the root of {@code n5}.
     *
     * @param n5 the {@code N5Reader}
     * @param elementType for which type of element ({@code NODE} or {@code EDGE}) to read the specs
     * @return the {@code GeffPropertySpecs} for the given {@code ElementType}
     */
    public static GeffPropertySpecs loadPropertySpecs(final N5Reader n5, final ElementType elementType) {
        return loadPropertySpecs(n5, elementType, null);
    }

    /**
     * Read all {@link GeffPropertySpecs} for edges or nodes of a geff hierarchy.
     *
     * @param n5 the {@code N5Reader}
     * @param elementType for which type of element ({@code NODE} or {@code EDGE}) to read the specs
     * @param geffGroup path to the geff hierarchy (relative to container root)
     * @return the {@code GeffPropertySpecs} for the given {@code ElementType}
     */
    public static GeffPropertySpecs loadPropertySpecs(
            final N5Reader n5,
            final ElementType elementType,
            final String geffGroup) {

        // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
        final String group = IoUtils.normalizeGroupPath(geffGroup);

        final String idValuesDataset = group + elementType.elementGroup() + "/ids";
        final ZarrDatasetAttributes attrIds = attrs(n5, idValuesDataset);
        final DType idDType = attrIds.getDType();
        final GeffPropertySpec id = new GeffPropertySpec("id", elementType, false, false, idDType, 0, new FinalDimensions());
        if (!(N5Utils.type(id.dType().getDataType()) instanceof IntegerType))
            throw new IllegalArgumentException(idValuesDataset + " must be an integer type");

        final List<GeffPropertySpec> properties = new ArrayList<>();
        final Map<String, PropMetadata> propsMetadata = n5.getAttribute(group,
                elementType.propMetadataAttribute(),
                new TypeToken<Map<String, PropMetadata>>() {
                }.getType());
        propsMetadata.values().forEach( metadata -> {
            final GeffPropertySpec spec = loadPropertySpec(n5, group, elementType, metadata);
            properties.add(spec);
        });

        return new GeffPropertySpecs(elementType, id, properties);
    }

    /**
     * Read the {@code GeffPropertySpec} for the property with the given {@code metadata}.
     *
     * @param n5 the {@code N5Reader}
     * @param group path to the geff hierarchy (relative to container root, already {@link #normalizeGroupPath normalized})
     * @param elementType which type of element ({@code NODE} or {@code EDGE}) the property refers to
     * @param metadata the property metadata (identifier, etc.)
     * @return the {@code GeffPropertySpec} for the given property
     */
    private static GeffPropertySpec loadPropertySpec(
            final N5Reader n5,
            final String group, // must be either "" or have a trailing slash
            final ElementType elementType,
            final PropMetadata metadata) {

        final String propsGroup = group + elementType.elementGroup() + "/props/" + metadata.getIdentifier();
        final boolean isVarLength = metadata.getVarlength();

        final ZarrDatasetAttributes attrValues = attrs(n5, propsGroup + "/values");
        final ZarrDatasetAttributes attrMissing = attrs(n5, propsGroup + "/missing");
        final ZarrDatasetAttributes attrData = isVarLength ? attrs(n5, propsGroup + "/data") : null;

        final DType dType = isVarLength ? attrData.getDType() : attrValues.getDType();
        final boolean isOptional = attrMissing != null;

        final int numDimensions;
        final Dimensions dimensions;
        if (isVarLength) {
            final long[] valuesDim = attrValues.getDimensions();
            numDimensions = (int) (valuesDim[0] - 1);
            dimensions = null;
        } else {
            final long[] valuesDim = attrValues.getDimensions();
            final long[] propertyDim = Arrays.copyOf(valuesDim, valuesDim.length - 1);
            numDimensions = propertyDim.length;
            dimensions = FinalDimensions.wrap(propertyDim);
        }

        return new GeffPropertySpec(metadata.getIdentifier(), elementType, isVarLength, isOptional, dType, numDimensions, dimensions);
    }

    /**
     * Normalize {@code geffGroup} path such that it can be prepended to a
     * dataset path within the geff.
     * <p>
     * The {@code geffGroup} may be null or {@code ""} (meaning the geff
     * hierarchy is located in the container root. In this case, {@code ""} is
     * returned.
     * <p>
     * Otherwise (the geff hierarchy is nested in a subgroup), {@code geffGroup}
     * should end in a trailing slash. If there is no trailing slash, we append
     * one.
     */
    // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
    // TODO: make private (?)
    static String normalizeGroupPath(String geffGroup) {
        final String group;
        if (geffGroup == null || geffGroup.isEmpty()) {
            group = "";
        } else {
            group = geffGroup.endsWith("/") ? geffGroup : geffGroup + "/";
        }
        return group;
    }

    /**
     * Get the {@link ZarrDatasetAttributes} of the given {@code Dataset}.
     * (Assumes, that the given {@code n5} reads from a Zarr dataset.)
     */
    // TODO: make private (?)
    static ZarrDatasetAttributes attrs(final N5Reader n5, final String dataset) {
        return (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
    }
}
