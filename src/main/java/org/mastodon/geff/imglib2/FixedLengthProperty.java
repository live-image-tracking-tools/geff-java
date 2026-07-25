package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

class FixedLengthProperty<T extends Type<T>> implements GeffProperty<T> {

    private final String identifier;
    private final boolean isOptional;

    private final long numElements;
    private final ElementIndex elementIndex;

    private final RandomAccess<? extends BooleanType<?>> missingAccess;
    private final PropertyRAI<T> values;

    final RandomAccessibleInterval<T> valuesRAI; // for IO
    final RandomAccessibleInterval<? extends BooleanType<?>> missingRAI; // for IO

    FixedLengthProperty(
            final String identifier,
            final RandomAccessibleInterval<T> propertyValues,
            final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing, // optional
            final ElementIndex sharedElementIndex
    ) {
        this.identifier = identifier;
        this.elementIndex = sharedElementIndex;

        numElements = propertyValues.dimension(propertyValues.numDimensions() - 1);
        final PropertySlice<T> valuesSlice = new PropertySlice<>(propertyValues, elementIndex);
        final RandomAccess<T> valuesAccess = valuesSlice.randomAccess();
        final FinalDimensions valuesDimensions = new FinalDimensions(valuesSlice);
        values = new PropertyRAI<>(valuesDimensions, valuesAccess);

        if (propertyMissing != null) {
            if (propertyMissing.numDimensions() != 1)
                throw new IllegalArgumentException("The \"missing\" array must be 1-dimensional");
            if (propertyMissing.dimension(0) != numElements)
                throw new IllegalArgumentException("The \"missing\" array must contain the same number of rows as the \"values\" array");
            final PropertySlice<? extends BooleanType<?>> missingSlice = new PropertySlice<>(propertyMissing, elementIndex);
            missingAccess = missingSlice.randomAccess();
            isOptional = true;
        } else {
            missingAccess = null;
            isOptional = false;
        }

        // keep these around for serialization ...
        valuesRAI = propertyValues;
        missingRAI = propertyMissing;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public boolean isVarlength() {
        return false;
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public long numElements() {
        return numElements;
    }

    @Override
    public ElementIndex elementIndex() {
        return elementIndex;
    }

    @Override
    public boolean isMissing() {
        return isOptional && missingAccess.get().get();
    }

    @Override
    public RandomAccessibleInterval<T> values() {
        return values;
    }

    @Override
    public void set(final GeffProperty<T> property) {

        if (isOptional) {
            final boolean missing = property.isMissing();
            missingAccess.get().set(missing);
            if(missing)
                return;
        }

        final int n = numDimensions();
        if (n == 0) {
            values().randomAccess().get().set(property.getAt());
        } else if (n == 1) {
            final int w = (int) dimensions().dimension(0);
            for (int x = 0; x < w; x++)
                getAt(x).set(property.getAt(x));
        } else if (n == 2) {
            final int w = (int) dimensions().dimension(0);
            final int h = (int) dimensions().dimension(1);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    getAt(x, y).set(property.getAt(x, y));
        } else {
            final Cursor<T> s = property.values().cursor();
            final Cursor<T> t = values().cursor();
            while (s.hasNext())
                t.next().set(s.next());
        }
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }





    // ------------------------------------------------------------------------
    // TODO: move to IoUtils class?


    // TODO: for testing...
    //        final Compression compression = new RawCompression();
    //        final DType dType = new DType(typestr, null);


    // TODO. If true, appends "2" to end of every dataset written.
    private static boolean DEBUG_WRITING = true;


    // TODO: might add int chunkSize argument later (chunking along elementIndex axis only).
    public void write(
            final N5ZarrWriter n5,
            final ElementType elementType, // TODO: maybe add to GeffProperty?
            final DType optionalDType, // optional, will use default DType corresponding to type()
            final boolean isIdsProperty, // goes into special "/ids" dataset, not "/props/<identifier>/values"
            final Compression compression,
            final String geffGroup) { // geffGroup is optional...

        if (!(type() instanceof NativeType))
            throw new IllegalArgumentException("Only NativeType supported for writing. (" + type().getClass().getSimpleName() + ")");

        final DType dType = optionalDType != null
                ? optionalDType
                : GeffPropertySpec.defaultDType(Cast.unchecked(type()));

        // TODO: This is inherently fragile. We should revisit later, and use N5Path (once that is available).
        final String group = IoUtils.normalizeGroupPath(geffGroup);

        if (isIdsProperty) {
            final String idsGroup = group + elementType.elementGroup() + "/ids";
            writeDataset(n5, idsGroup, dType, compression, Cast.unchecked(valuesRAI));
        } else {
            final String propsGroup = group + elementType.elementGroup() + "/props/" + identifier;
            writeDataset(n5, propsGroup + "/values", dType, compression, Cast.unchecked(valuesRAI));
            if (isOptional) {
                final RandomAccessibleInterval<UnsignedByteType> missing_uint8 = Converters.convert(missingRAI,
                        (b, u) -> u.set(b.get() ? 1 : 0),
                        new UnsignedByteType());
                final DType uint8 = new DType("|b1", null);
                writeDataset(n5, propsGroup + "/missing", uint8, compression, missing_uint8);
            }
        }
    }

    // TODO: Move to Utils class
    static <T extends NativeType<T>> void writeDataset(
            final N5ZarrWriter n5,
            final String dataset,
            final DType dType,
            final Compression compression,
            final RandomAccessibleInterval<T> data) {
        final long[] dimensions = data.dimensionsAsLongArray();
        final int[] blockSize = Util.long2int(dimensions);
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

}
