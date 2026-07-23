package org.mastodon.geff.imglib2;

import com.google.gson.reflect.TypeToken;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
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

import static org.mastodon.geff.imglib2.Playground.GeffPropertySpecs.normalizeGroupPath;

public class Playground {


    // ------------------------------------------------------------------------
    //
    //    ElementType
    //
    // ------------------------------------------------------------------------

    enum ElementType {
        NODE("nodes", "geff/node_props_metadata"),
        EDGE("edges", "geff/edge_props_metadata");

        private final String elementGroup;
        private final String propMetadataAttribute;

        ElementType(final String elementGroup, String propMetadataAttribute) {
            this.elementGroup = elementGroup;
            this.propMetadataAttribute = propMetadataAttribute;
        }

        public String elementGroup() {
            return elementGroup;
        }

        public String propMetadataAttribute() {
            return propMetadataAttribute;
        }
    }



    // ------------------------------------------------------------------------
    //
    //    GeffPropertySpecs
    //
    // ------------------------------------------------------------------------

    public static class GeffPropertySpecs {

        private final ElementType elementType;
        private final GeffPropertySpec idSpec;
        private final Map<String, GeffPropertySpec> propertySpecs;

        GeffPropertySpecs(
                final ElementType elementType,
                final GeffPropertySpec id,
                final List<GeffPropertySpec> properties
        ) {
            this.elementType = elementType;
            idSpec = id;
            propertySpecs = new LinkedHashMap<>();
            for (final GeffPropertySpec spec : properties) {
                final String identifier = spec.identifier();
                if (identifier.equals(id.identifier()) || propertySpecs.containsKey(identifier))
                    throw new IllegalArgumentException("Duplicate property identifier: " + identifier);
                propertySpecs.put(identifier, spec);
            }
        }

        public ElementType elementType() {
            return elementType;
        }


        // ------------------------------------------------------------------------


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
            final GeffPropertySpec id = new GeffPropertySpec("id", false, false, idDType, 0, null);
            if (!(N5Utils.type(id.dType().getDataType()) instanceof IntegerType))
                throw new IllegalArgumentException(idValuesDataset + " must be an integer type");

            final List< GeffPropertySpec > properties = new ArrayList<>();
            final Map<String, PropMetadata> propsMetadata = n5.getAttribute(group,
                    elementType.propMetadataAttribute(),
                    new TypeToken<Map<String, PropMetadata>>() {
                    }.getType());
            propsMetadata.forEach((identifier, metadata) -> {
                final String propsGroup = group + elementType.elementGroup() + "/props/" + identifier;
                final GeffPropertySpec spec = loadPropertySpec(n5, propsGroup, metadata);
                properties.add(spec);
            });

            return new GeffPropertySpecs(elementType, id, properties);
        }

        private static ZarrDatasetAttributes attrs(final N5ZarrReader n5, final String dataset) {
            return (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
        }

        private static GeffPropertySpec loadPropertySpec(
                final N5ZarrReader n5,
                final String propsGroup,
                final PropMetadata metadata) {

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

            return new GeffPropertySpec(metadata.getIdentifier(), isVarLength, isOptional, dType, numDimensions, dimensions);
        }

        // Non-empty geffGroup path should end with trailing slash.
        // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
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




    // ------------------------------------------------------------------------
    //
    //    GeffProperties
    //
    // ------------------------------------------------------------------------

    public static class GeffProperties {

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


        public static GeffProperties load(final N5ZarrReader n5, final GeffPropertySpecs specs) {
            return load(n5, specs, null);
        }

        public static GeffProperties load(
                final N5ZarrReader n5,
                final GeffPropertySpecs specs,
                final String geffGroup) {

            // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
            final String group = normalizeGroupPath(geffGroup);

            final ElementType elementType = specs.elementType();

            final String idValuesDataset = group + elementType.elementGroup() + "/ids";
            final ElementIndex index = new ElementIndex();
            final GeffProperty<? extends IntegerType<?>> id = loadProperty(n5, "id", index, idValuesDataset, null);

            final List< GeffProperty<?> > properties = new ArrayList<>();
            specs.propertySpecs.forEach((identifier, spec) -> {
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




    // ------------------------------------------------------------------------
    //
    //    WIP
    //
    // ------------------------------------------------------------------------



    // TODO: Where to store GeffPropertySpec? In GeffProperty or separately
    //  (for example in GeffProperties)? Must every GeffProperty always have a GeffPropertySpec?
    //  - When I load GeffProperty from props_metadata, GeffPropertySpec is
    //    a natural part of the process.
    //  - When I write a GeffProperty, it should have GeffPropertySpec for
    //    DType etc.
    //  - In between, I deal with converted properties, bind to
    //    constructor/deconstructor handles, etc.
    //    For that I don't need GeffPropertySpec. (Or do I?). ImgLib2 type
    //    information should be enough to distinguish signed/unsigned (which
    //    seems a rough edge).



    // ------------------------------------------------------------------------
    //
    //    main
    //
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
//        final String path = "cross-language-tests/data/basic_3d_original.zarr";
//        final String path = "cross-language-tests/data/covariance_original.zarr";
        final String path = "cross-language-tests/data/varlength_original.zarr";

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {
            final GeffProperties nodeProperties = loadGeffProperties(n5, ElementType.NODE);
            System.out.println("nodeProperties = " + nodeProperties);
        }
    }

    private static GeffProperties loadGeffProperties(
            final N5ZarrReader n5,
            final ElementType elementType) {
        return loadGeffProperties(n5, elementType, null);
    }

    /**
     * @param n5
     * @param elementType
     * @param geffGroup   optional (if geff is not at the root of the container)
     * @return
     */
    private static GeffProperties loadGeffProperties(
            final N5ZarrReader n5,
            final ElementType elementType,
            final String geffGroup) {
        final GeffPropertySpecs specs = GeffPropertySpecs.load(n5, elementType);
        System.out.println("specs = " + specs);
        return GeffProperties.load(n5, specs);
    }

    private static ZarrDatasetAttributes attrs(final N5ZarrReader n5, final String dataset) {
        return (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
    }

    private static void printAttributes(final N5ZarrReader n5, final String dataset) {
        final ZarrDatasetAttributes attributes = attrs(n5, dataset);
        System.out.println(dataset + ":");
        System.out.println("  DType=\"" + attributes.getDType() + "\"");
        System.out.println("  dimensions=\"" + Arrays.toString(attributes.getDimensions()) + "\"");
    }
}
