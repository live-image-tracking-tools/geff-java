package org.mastodon.geff.imglib2;

import com.google.gson.reflect.TypeToken;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.type.numeric.IntegerType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.PropMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeffPropertySpecs {

    private final ElementType elementType;
    private final GeffPropertySpec id;
    private final Map<String, GeffPropertySpec> properties;

    public GeffPropertySpecs(
            final ElementType elementType,
            final GeffPropertySpec idSpec,
            final List<GeffPropertySpec> specs
    ) {
        this.elementType = elementType;
        this.id = idSpec;
        properties = new LinkedHashMap<>();
        for (final GeffPropertySpec spec : specs) {
            final String identifier = spec.identifier();
            if (identifier.equals(idSpec.identifier()) || properties.containsKey(identifier))
                throw new IllegalArgumentException("Duplicate property identifier: " + identifier);
            properties.put(identifier, spec);
        }
    }

    public ElementType elementType() {
        return elementType;
    }

    public GeffPropertySpec id() {
        return id;
    }

    public Map<String, GeffPropertySpec> properties() {
        return properties;
    }

    @Override
    public String toString() {
        final String nl = System.lineSeparator();
        final String cnl = "," + nl;
        final String props = properties.values().stream().map(p ->
                nl + "    " + p.identifier() + "=" + p
        ).collect(Collectors.joining());
        return "GeffPropertySpecs{" + nl +
                "  elementType=" + elementType + cnl +
                "  id=" + id + cnl +
                "  properties={" + props + "}" + nl +
                '}';
    }

    // ------------------------------------------------------------------------
    // TODO: move to IoUtils class?

    public static GeffPropertySpecs load(final N5ZarrReader n5, final ElementType elementType) {
        return load(n5, elementType, null);
    }

    /**
     * @param n5
     * @param elementType
     * @param geffGroup   optional (if geff is not at the root of the container)
     * @return
     */
    public static GeffPropertySpecs load(
            final N5ZarrReader n5,
            final ElementType elementType,
            final String geffGroup) {

        // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
        final String group = normalizeGroupPath(geffGroup);

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

    private static ZarrDatasetAttributes attrs(final N5ZarrReader n5, final String dataset) {
        return (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
    }

    private static GeffPropertySpec loadPropertySpec(
            final N5ZarrReader n5,
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

    // Non-empty geffGroup path should end with trailing slash.
    // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
    // TODO: Move to Utils class
    static String normalizeGroupPath(String geffGroup) {
        final String group;
        if (geffGroup == null || geffGroup.isEmpty()) {
            group = "";
        } else {
            group = geffGroup.endsWith("/") ? geffGroup : geffGroup + "/";
        }
        return group;
    }
}
