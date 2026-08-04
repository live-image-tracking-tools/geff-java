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
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.PropMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reading/writing from/to Zarr
 */
public class IoUtils {

    // TODO. If true, appends "2" to end of every dataset written.
    private static boolean DEBUG_WRITING = true;



    // ------------------------------------------------------------------------
    //   READ
    // ------------------------------------------------------------------------

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

        final String group = normalizeGroupPath(geffGroup);
        final String idValuesDataset = group + elementType.elementGroup() + "/ids";
        final ZarrDatasetAttributes attrIds = attrs(n5, idValuesDataset);
        final DType idDType = attrIds.getDType();
        final GeffPropertySpec id = new GeffPropertySpec("id", elementType, false, false, idDType, new FinalDimensions());
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

        final Dimensions dimensions;
        if (isVarLength) {
            final long[] valuesDim = attrValues.getDimensions();
            final int numDimensions = (int) (valuesDim[0] - 1);
            dimensions = FinalDimensions.wrap(new long[numDimensions]);
        } else {
            final long[] valuesDim = attrValues.getDimensions();
            final long[] propertyDim = Arrays.copyOf(valuesDim, valuesDim.length - 1);
            dimensions = FinalDimensions.wrap(propertyDim);
        }

        return new GeffPropertySpec(metadata.getIdentifier(), elementType, isVarLength, isOptional, dType, dimensions);
    }



    /**
     * Read all {@link GeffProperties} for edges or nodes of a geff hierarchy at
     * the root of {@code n5}.
     *
     * @param n5    the {@code N5Reader}
     * @param elementType for which type of element ({@code NODE} or {@code EDGE}) to read the properties
     * @return the {@code GeffProperties} for the given {@code ElementType}
     */
    public static GeffProperties loadProperties(final N5Reader n5, final ElementType elementType) {
        return loadProperties(n5, elementType, null);
    }

    /**
     * Read all {@link GeffProperties} for edges or nodes of a geff hierarchy.
     *
     * @param n5 the {@code N5Reader}
     * @param elementType for which type of element ({@code NODE} or {@code EDGE}) to read the properties
     * @param geffGroup path to the geff hierarchy (relative to container root)
     * @return the {@code GeffProperties} for the given {@code ElementType}
     */
    public static GeffProperties loadProperties(
            final N5Reader n5,
            final ElementType elementType,
            final String geffGroup) {
        return loadProperties(n5, loadPropertySpecs(n5, elementType, geffGroup), geffGroup);
    }

    /**
     * Read all {@link GeffProperties} for edges or nodes of a geff hierarchy at
     * the root of {@code n5}.
     *
     * @param n5    the {@code N5Reader}
     * @param specs specs for all properties to read
     * @return the {@code GeffProperties} for the given specs
     */
    public static GeffProperties loadProperties(final N5Reader n5, final GeffPropertySpecs specs) {
        return loadProperties(n5, specs, null);
    }

    /**
     * Read all {@link GeffProperties} for edges or nodes of a geff hierarchy.
     *
     * @param n5 the {@code N5Reader}
     * @param specs specs for all properties to read
     * @param geffGroup path to the geff hierarchy (relative to container root)
     * @return the {@code GeffProperties} for the given specs
     */
    public static GeffProperties loadProperties(
            final N5Reader n5,
            final GeffPropertySpecs specs,
            final String geffGroup) {

        final String group = normalizeGroupPath(geffGroup);
        final ElementType elementType = specs.elementType();
        final String idValuesDataset = group + elementType.elementGroup() + "/ids";
        final ElementIndex index = new ElementIndex();
        final GeffProperty<? extends IntegerType<?>> id = loadFixedLengthProperty(n5, "id", index, idValuesDataset, null);

        final List<GeffProperty<?>> properties = new ArrayList<>();
        specs.properties().forEach((identifier, spec) -> {
            final String propsGroup = group + elementType.elementGroup() + "/props/" + identifier;
            final GeffProperty<?> property;
            if (spec.isVarLength()) {
                property = loadVarLengthProperty(n5, identifier, index,
                        propsGroup + "/values",
                        spec.isOptional() ? propsGroup + "/missing" : null,
                        propsGroup + "/data");
            } else {
                property = loadFixedLengthProperty(n5, identifier, index,
                        propsGroup + "/values",
                        spec.isOptional() ? propsGroup + "/missing" : null);
            }
            properties.add(property);
        });

        return new GeffProperties(elementType, id, properties);
    }

    /**
     * Create a fixed-length {@code GeffProperty} tied to the datasets at {@code
     * valuesPath} and (optionally) {@code missingPath}.
     * <p>
     * The last dimension of the values dataset is the element (node/edge)
     * index, the remaining dimensions are property dimensions. That is, a
     * scalar property has a 1D values dataset, a vector property has a 2D
     * values dataset, and so on.
     *
     * @param n5 the {@code N5Reader}
     * @param identifier the identifier of the property (e.g. "x")
     * @param sharedElementIndex the shared {@code ElementIndex} of the property, specifying for which element the property values are currently exposed.
     * @param valuesPath path to the dataset containing the property values (relative to the container root, e.g. "nodes/props/x/values")
     * @param missingPath path to the dataset containing the properties missing information (for optional properties, or {@code null} if property values must be present for all elements
     * @return a {@code GeffProperty}
     * @param <T> the imglib2 type of the property values
     */
    public static <T extends NativeType<T>> GeffProperty<T> loadFixedLengthProperty(
            final N5Reader n5,
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

    /**
     * Create a var-length {@code GeffProperty} tied to the datasets at {@code
     * valuesPath}, (optionally) {@code missingPath}, and {@code dataPath}.
     * <p>
     * The last dimension of the values dataset is the element (node/edge)
     * index, the first dimensions specifies the offset and shape of the
     * property for any given element (the length the first dimension {@code -1}
     * is the number property dimensions}.
     *
     * @param n5 the {@code N5Reader}
     * @param identifier the identifier of the property (e.g. "var_length")
     * @param sharedElementIndex the shared {@code ElementIndex} of the property, specifying for which element the property values are currently exposed.
     * @param valuesPath path to the dataset containing the property offset and shape values (relative to the container root, e.g. "nodes/props/var_length/values")
     * @param missingPath path to the dataset containing the properties missing information (for optional properties, or {@code null} if property values must be present for all elements
     * @param dataPath path to the dataset containing the property data (what offset and shape point to)
     * @return a {@code GeffProperty}
     * @param <T> the imglib2 type of the property values
     */
    public static <T extends NativeType<T>> GeffProperty<T> loadVarLengthProperty(
            final N5Reader n5,
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



    // ------------------------------------------------------------------------
    //   WRITE
    // ------------------------------------------------------------------------

    /**
     * Write a {@code ImgBacked} {@code GeffProperty} into a geff hierarchy.
     * <p>
     * This will write the {@code "values"} dataset, the {@code "missing"}
     * dataset if the property is {@link GeffProperty#isOptional() optional},
     * and the {@code "data"} dataset if the property is {@link
     * GeffProperty#isVarlength() var-length}.
     * <p>
     * The {@code optionalDType} allows to specify the endianness for the
     * "values" dataset (or the "data" dataset for var-length properties). It
     * must correspond to the imglib2 type {@code T} of the property. If ({@code
     * optionalDType==null}), the default DType for {@code T} is used.
     * <p>
     * The "values" dataset for var-length properties is always written as
     * {@code "<u8"}. The "missing" dataset is always written as {@code "|b1"}.
     *
     * @param n5                  the {@code N5Writer}
     * @param property            the property to write
     * @param valuesPath          path to the dataset containing the property data for
     *                            fixed-length properties, or the offset and shape values for var-length
     *                            properties (relative to the container root, e.g.
     *                            "nodes/props/var_length/values")
     * @param missingPath         path to the dataset containing the properties missing
     *                            information (for optional properties, or {@code null} if property values
     *                            must be present for all elements
     * @param dataPath            path to the dataset containing the property data for
     *                            var-length (what offset and shape point to), or {@code null} for
     *                            fixed-length properties
     * @param optionalDType       the exact {@link DType} (corresponding to {@code T}) to write, or {@code null}
     * @param optionalCompression compression to use (or {@code null} for no compression)
     * @param chunkSize           if {@code >0}, the chunk size in the slowest-moving
     *                            dimension (last dimension in imglib2 convention, first dimension in numpy
     *                            convention)
     * @param <T>                 the imglib2 type of the property values
     */
    public static <T extends NativeType<T>> void writeProperty(
            final N5Writer n5,
            final GeffProperty<T> property,
            final String valuesPath,
            final String missingPath,
            final String dataPath,
            final DType optionalDType, // optional, will use default DType corresponding to type()
            final Compression optionalCompression,
            final int chunkSize) {

        if (valuesPath == null)
            throw new NullPointerException("valuesPath is null");

        // Check that property.isOptional() iff missingPath != null
        if (property.isOptional() && missingPath == null)
            throw new IllegalArgumentException("Property to be written is optional, but no path for the \"missing\" dataset was given");
        if (!property.isOptional() && missingPath != null)
            throw new IllegalArgumentException("Property to be written is not optional, but a path for the \"missing\" dataset was given");

        // Check that property.isVarLength() iff dataPath != null
        if (property.isVarlength() && dataPath == null)
            throw new IllegalArgumentException("Property to be written is var-length, but no path for the \"data\" dataset was given");
        if (!property.isVarlength() && dataPath != null)
            throw new IllegalArgumentException("Property to be written is not var-length, but a path for the \"data\" dataset was given");

        // Check that property.type() can be written through N5
        final T type = property.type();
        if (!(type instanceof NativeType))
            throw new IllegalArgumentException("Only NativeType supported for writing. (" + type.getClass().getSimpleName() + ")");

        // Check that the property is instanceof either FixedLengthProperty or
        // VarLengthProperty so that we can extract the underlying RandomAccessibleIntervals
        if (!(property instanceof ImgBacked))
            throw new IllegalArgumentException("Only ImgBacked properties supported for writing.");

        final ImgBacked<T> imgBacked = Cast.unchecked(property);

        final DType uint8 = new DType("|b1", null);
        final DType uint64 = new DType("<u8", null);
        final DType dType = optionalDType != null
                ? optionalDType
                : defaultDType(Cast.unchecked(type));
        final Compression compression = optionalCompression != null
                ? optionalCompression
                : new RawCompression();

        if (property.isVarlength()) {
            writeDataset(n5, valuesPath, uint64, compression, chunkSize, imgBacked.getIndexRAI());
            writeDataset(n5, dataPath, dType, compression, chunkSize, imgBacked.getDataRAI());
        } else {
            writeDataset(n5, valuesPath, dType, compression, chunkSize, imgBacked.getDataRAI());
        }

        if (property.isOptional()) {
            final RandomAccessibleInterval<UnsignedByteType> missing_uint8 = Converters.convert(imgBacked.getMissingRAI(),
                    (b, u) -> u.set(b.get() ? 1 : 0),
                    new UnsignedByteType());
            writeDataset(n5, missingPath, uint8, compression, chunkSize, missing_uint8);
        }
    }

    /**
     * Write a {@code ImgBacked} {@code GeffProperty} into a geff hierarchy.
     * <p>
     * This will write the {@code "values"} dataset, the {@code "missing"}
     * dataset if the property is {@link GeffProperty#isOptional() optional},
     * and the {@code "data"} dataset if the property is {@link
     * GeffProperty#isVarlength() var-length}.
     * <p>
     * The {@code optionalDType} allows to specify the endianness for the
     * "values" dataset (or the "data" dataset for var-length properties). It
     * must correspond to the imglib2 type {@code T} of the property. If ({@code
     * optionalDType==null}), the default DType for {@code T} is used.
     * <p>
     * The "values" dataset for var-length properties is always written as
     * {@code "<u8"}. The "missing" dataset is always written as {@code "|b1"}.
     *
     * @param n5                  the {@code N5Writer}
     * @param property            the property to write
     * @param paths               path to the "values", "missing", and "data" datasets (See {@link #writeProperty(N5Writer, GeffProperty, String, String, String, DType, Compression, int)})
     * @param optionalDType       the exact {@link DType} (corresponding to {@code T}) to write, or {@code null}
     * @param optionalCompression compression to use (or {@code null} for no compression)
     * @param chunkSize           if {@code >0}, the chunk size in the slowest-moving
     *                            dimension (last dimension in imglib2 convention, first dimension in numpy
     *                            convention)
     * @param <T>                 the imglib2 type of the property values
     */
    public static <T extends NativeType<T>> void writeProperty(
            final N5Writer n5,
            final GeffProperty<T> property,
            final DatasetPaths paths,
            final DType optionalDType, // optional, will use default DType corresponding to type() if null
            final Compression optionalCompression, // optional, will use RawCompression if null
            final int chunkSize) {
        writeProperty(n5, property, paths.valuesPath(), paths.missingPath(), paths.dataPath(), optionalDType, optionalCompression, chunkSize);
    }

    // TODO: convert record to class (for Java 8)
    public record DatasetPaths(String valuesPath, String missingPath, String dataPath) {

        /**
         * Default paths for the given property (relative to container root):
         * <ul>
         * <li>{@code "[<geffGroup>/]<elementType>/props/<identifier>/values"},</li>
         * <li>{@code "[<geffGroup>/]<elementType>/props/<identifier>/missing"} (if property is optional), and</li>
         * <li>{@code "[<geffGroup>/]<elementType>/props/<identifier>/data"} (if property is var-length).</li>
         * </ul>
         *
         * @param property    the property to write
         * @param elementType which type of element ({@code NODE} or {@code EDGE}) the property refers to
         * @param geffGroup   path to the geff hierarchy (relative to container root)
         */
        static DatasetPaths ofProperty(
                final GeffProperty<?> property,
                final ElementType elementType,
                final String geffGroup) {
            final String group = normalizeGroupPath(geffGroup);
            final String propsGroup = group + elementType.elementGroup() + "/props/" + property.identifier();
            return new DatasetPaths(propsGroup + "/values",
                    property.isOptional() ? propsGroup + "/missing" : null,
                    property.isVarlength() ? propsGroup + "/data" : null);
        }

        /**
         * Default paths for the given property (relative to container root):
         * <ul>
         * <li>{@code "<elementType>/props/<identifier>/values"},</li>
         * <li>{@code "<elementType>/props/<identifier>/missing"} (if property is optional), and</li>
         * <li>{@code "<elementType>/props/<identifier>/data"} (if property is var-length).</li>
         * </ul>
         *
         * @param property    the property to write
         * @param elementType which type of element ({@code NODE} or {@code EDGE}) the property refers to
         */
        static DatasetPaths ofProperty(
                final GeffProperty<?> property,
                final ElementType elementType) {
            return ofProperty(property, elementType, null);
        }

        /**
         * Default paths for the id property (relative to container root):
         * <ul>
         * <li>{@code "[<geffGroup>/]<elementType>/ids/values"},</li>
         * <li>{@code null},</li>
         * <li>{@code null}.</li>
         * </ul>
         *
         * @param elementType which type of element ({@code NODE} or {@code EDGE}) the property refers to
         * @param geffGroup   path to the geff hierarchy (relative to container root)
         */
        static DatasetPaths ofId(
                final ElementType elementType,
                final String geffGroup) {
            final String group = normalizeGroupPath(geffGroup);
            final String idsGroup = group + elementType.elementGroup() + "/ids";
            return new DatasetPaths(idsGroup, null, null);
        }

        /**
         * Default paths for the id property (relative to container root):
         * <ul>
         * <li>{@code "<elementType>/ids/values"},</li>
         * <li>{@code null},</li>
         * <li>{@code null}.</li>
         * </ul>
         *
         * @param elementType which type of element ({@code NODE} or {@code EDGE}) the property refers to
         */
        static DatasetPaths ofId(final ElementType elementType) {
            return ofId(elementType, null);
        }
    }


    // ------------------------------------------------------------------------
    //   UTILITIES
    // ------------------------------------------------------------------------

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

    static <T extends NativeType<T>> void writeDataset(
            final N5Writer n5,
            final String dataset,
            final DType dType,
            final Compression compression,
            final int chunkSize, // optional (if <= 0, write everything into a single block)
            final RandomAccessibleInterval<T> data) {
        final long[] dimensions = data.dimensionsAsLongArray();
        final int[] blockSize = Util.long2int(dimensions);
        if (chunkSize > 0)
            blockSize[blockSize.length - 1] = chunkSize;
        final ZarrDatasetAttributes attributes = new ZarrDatasetAttributes(
                dimensions,
                blockSize,
                dType,
                compression,
                true,
                "0"
        );
        final String dataset_ = DEBUG_WRITING ? dataset + "2" : dataset;// TODO ...
        n5.createDataset(dataset_, attributes);
        N5Utils.saveRegion(data, n5, dataset_, attributes);
    }

    /**
     * Get the default Zarr {@code DType} to use for writing datasets of the
     * given ImgLib2 {@code type}.
     * <p>
     * For compatibility with python geff the default Zarr DTypes are
     * little-endian (in contrast to n5-zarr which uses big-endian by default).
     *
     * @param type ImgLib2 type
     * @return corresponding default DType
     */
    public static <T extends NativeType<T>> DType defaultDType(final T type) {
        return new DType(typestrs.get(N5Utils.dataType(type)), null);
    }

    // copied from DType, modified to use little-endian
    private static final EnumMap<DataType, String> typestrs = new EnumMap<>(DataType.class);

    static {
        typestrs.put(DataType.INT8, "|i1");
        typestrs.put(DataType.UINT8, "|u1");
        typestrs.put(DataType.INT16, "<i2");
        typestrs.put(DataType.UINT16, "<u2");
        typestrs.put(DataType.INT32, "<i4");
        typestrs.put(DataType.UINT32, "<u4");
        typestrs.put(DataType.INT64, "<i8");
        typestrs.put(DataType.UINT64, "<u8");
        typestrs.put(DataType.FLOAT32, "<f4");
        typestrs.put(DataType.FLOAT64, "<f8");
        typestrs.put(DataType.STRING, "|O");
        typestrs.put(DataType.OBJECT, "|O");
    }
}
