package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mastodon.geff.imglib2.GeffPropertySpecs.normalizeGroupPath;

public class GeffProperties {

    private final ElementType elementType;
    private final ElementIndex elementIndex;
    private final long numElements;

    private final GeffProperty<? extends IntegerType<?>> id;
    private final Map<String, GeffProperty<?>> properties;

    GeffProperties(
            final ElementType elementType,
            final GeffProperty<? extends IntegerType<?>> id,
            final List<GeffProperty<?>> props) {
        this.elementType = elementType;
        this.id = id;
        elementIndex = id.elementIndex();
        numElements = id.numElements();
        properties = new LinkedHashMap<>();
        for (final GeffProperty<?> prop : props) {
            final String identifier = prop.identifier();
            if (identifier.equals(id.identifier()) || properties.containsKey(identifier))
                throw new IllegalArgumentException("Duplicate property identifier: " + identifier);
            if (prop.numElements() != numElements)
                throw new IllegalArgumentException("Number of elements mismatch: " + identifier);
            properties.put(identifier, prop);
        }
    }

    public ElementType elementType() {
        return elementType;
    }

    public long numElements() {
        return numElements;
    }

    // elementIndex should be shared by all properties
    public ElementIndex elementIndex() {
        return elementIndex;
    }

    @Override
    public String toString() {
        final String nl = System.lineSeparator();
        final String cnl = "," + nl;
        final String props = properties.values().stream().map(p ->
                nl + "    " + p.identifier() + "=" + p
        ).collect(Collectors.joining());
        return "GeffProperties{" + nl +
                "  elementType=" + elementType + cnl +
                "  numElements=" + numElements + cnl +
                "  elementIndex=" + elementIndex + cnl +
                "  id=" + id + cnl +
                "  properties={" + props + "}" + nl +
                '}';
    }

    void put(GeffProperty<?> property) {
        properties.put(property.identifier(), property);
    }


    // ------------------------------------------------------------------------
    // TODO: move to IoUtils class?

    public static GeffProperties load(final N5ZarrReader n5, final GeffPropertySpecs specs) {
        return load(n5, specs, null);
    }

    public static GeffProperties load(
            final N5ZarrReader n5,
            final GeffPropertySpecs specs,
            final String geffGroup) {

        // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
        final String group = GeffPropertySpecs.normalizeGroupPath(geffGroup);

        final ElementType elementType = specs.elementType();

        final String idValuesDataset = group + elementType.elementGroup() + "/ids";
        final ElementIndex index = new ElementIndex();
        final GeffProperty<? extends IntegerType<?>> id = loadProperty(n5, "id", index, idValuesDataset, null);

        final List<GeffProperty<?>> properties = new ArrayList<>();
        specs.properties().forEach((identifier, spec) -> {
            final String propsGroup = group + elementType.elementGroup() + "/props/" + identifier;
            final GeffProperty<?> property;
            if (spec.isVarLength()) {
                property = loadProperty(n5, identifier, index,
                        propsGroup + "/values",
                        spec.isOptional() ? propsGroup + "/missing" : null,
                        propsGroup + "/data");
            } else {
                property = loadProperty(n5, identifier, index,
                        propsGroup + "/values",
                        spec.isOptional() ? propsGroup + "/missing" : null);
            }
            properties.add(property);
        });

        return new GeffProperties(elementType, id, properties);
    }

    // fixed-length
    private static <T extends NativeType<T>> GeffProperty<T> loadProperty(
            final N5ZarrReader n5,
            final String identifier,
            final ElementIndex sharedElementIndex,
            final String valuesPath,
            final String missingPath) {

        final RandomAccessibleInterval<T> values = N5Utils.open(n5, valuesPath);
        final RandomAccessibleInterval<BoolType> missing;
        if (missingPath != null) {
            final CachedCellImg<UnsignedByteType, ?> missing_uint8 = N5Utils.open(n5, missingPath);
            missing = Converters.convert(missing_uint8, (u, b) -> b.set(u.get() != 0), new BoolType());
        } else {
            missing = null;
        }
        return new FixedLengthProperty<>(identifier, values, missing, sharedElementIndex);
    }

    // var-length
    private static <T extends NativeType<T>> GeffProperty<T> loadProperty(
            final N5ZarrReader n5,
            final String identifier,
            final ElementIndex sharedElementIndex,
            final String valuesPath,
            final String missingPath,
            final String dataPath) {

        final RandomAccessibleInterval<UnsignedLongType> values = N5Utils.open(n5, valuesPath);
        final RandomAccessibleInterval<BoolType> missing;
        if (missingPath != null) {
            final CachedCellImg<UnsignedByteType, ?> missing_uint8 = N5Utils.open(n5, missingPath);
            missing = Converters.convert(missing_uint8, (u, b) -> b.set(u.get() != 0), new BoolType());
        } else {
            missing = null;
        }
        final RandomAccessibleInterval<T> data = N5Utils.open(n5, dataPath);
        return new VarLengthProperty<>(identifier, values, data, missing, sharedElementIndex);
    }
}
