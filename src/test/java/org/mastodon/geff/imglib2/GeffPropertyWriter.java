package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.mastodon.geff.imglib2.FunctionTypes.ToBooleanArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToBooleanFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToByteArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToByteFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToDoubleArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToFloatArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToFloatFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToIntArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToLongArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeBooleanArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeBooleanFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeByteArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeByteFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeFloatArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeFloatFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeIntArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeIntFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeLongArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeLongFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeShortArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeShortFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeStringFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToShortArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToShortFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToStringFunction;
import org.mastodon.geff.imglib2.IoUtils.DatasetPaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;


public class GeffPropertyWriter<O> {

    private final ElementType elementType;
    private final Collection<O> objects; //
    private final int numElements;
    private final ElementIndex elementIndex;

    private final List<Consumer<O>> setters = new ArrayList<>();

    private GeffProperty<? extends IntegerType<?>> id = null;
    private DType idDType = null;

    private final List<GeffProperty<?>> props = new ArrayList<>();
    private final List<DType> propsDTypes = new ArrayList<>();


    /**
     * Create a new {@code GeffWriter}.
     *
     * @param objects     the objects from which to extract properties
     *                    (collection of the client's Node or Edge objects)
     * @param elementType which type of element ({@code NODE} or {@code
     *                    EDGE}) the property refers to
     */
    public GeffPropertyWriter(
            final Collection<O> objects,
            final ElementType elementType) {

        this.elementType = elementType;
        this.objects = objects;
        numElements = objects.size();
        elementIndex = new ElementIndex();
    }

    public GeffPropertyWriter<O> id(ToByteFunction<O> supplier)  {return id(supplier, null);}
    public GeffPropertyWriter<O> id(ToShortFunction<O> supplier) {return id(supplier, null);}
    public GeffPropertyWriter<O> id(ToIntFunction<O> supplier)   {return id(supplier, null);}
    public GeffPropertyWriter<O> id(ToLongFunction<O> supplier)  {return id(supplier, null);}

    public GeffPropertyWriter<O> id(ToByteFunction<O> supplier, String typestr)  {return id(PropertyAdapters.wrap("id", supplier), typestr);}
    public GeffPropertyWriter<O> id(ToShortFunction<O> supplier, String typestr) {return id(PropertyAdapters.wrap("id", supplier), typestr);}
    public GeffPropertyWriter<O> id(ToIntFunction<O> supplier, String typestr)   {return id(PropertyAdapters.wrap("id", supplier), typestr);}
    public GeffPropertyWriter<O> id(ToLongFunction<O> supplier, String typestr)  {return id(PropertyAdapters.wrap("id", supplier), typestr);}

    public GeffPropertyWriter<O> id(ToByteArrayFunction<O> supplier, int length)  {return id(supplier, length, null);}
    public GeffPropertyWriter<O> id(ToShortArrayFunction<O> supplier, int length) {return id(supplier, length, null);}
    public GeffPropertyWriter<O> id(ToIntArrayFunction<O> supplier, int length)   {return id(supplier, length, null);}
    public GeffPropertyWriter<O> id(ToLongArrayFunction<O> supplier, int length)  {return id(supplier, length, null);}

    public GeffPropertyWriter<O> id(ToByteArrayFunction<O> supplier, int length, String typestr)  {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}
    public GeffPropertyWriter<O> id(ToShortArrayFunction<O> supplier, int length, String typestr) {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}
    public GeffPropertyWriter<O> id(ToIntArrayFunction<O> supplier, int length, String typestr)   {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}
    public GeffPropertyWriter<O> id(ToLongArrayFunction<O> supplier, int length, String typestr)  {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}

    public GeffPropertyWriter<O> add(String identifier, ToByteFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToShortFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToIntFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToLongFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToFloatFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToDoubleFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToBooleanFunction<O> supplier) {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToStringFunction<O> supplier)  {return add(new StringAsVarLengthPropertyAdapter<O>(PropertyAdapters.wrap(identifier, supplier)));}

    public GeffPropertyWriter<O> add(String identifier, ToByteFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToShortFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToIntFunction<O> supplier, String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToLongFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToFloatFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToDoubleFunction<O> supplier, String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToBooleanFunction<O> supplier, String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(String identifier, ToMaybeByteFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeShortFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeIntFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeLongFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeFloatFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeDoubleFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeBooleanFunction<O> supplier) {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeStringFunction<O> supplier)  {return add(new StringAsVarLengthPropertyAdapter<O>(PropertyAdapters.wrap(identifier, supplier)));}

    public GeffPropertyWriter<O> add(String identifier, ToMaybeByteFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeShortFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeIntFunction<O> supplier, String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeLongFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeFloatFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeDoubleFunction<O> supplier, String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeBooleanFunction<O> supplier, String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(String identifier, ToByteArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToShortArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToIntArrayFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToLongArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToFloatArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToDoubleArrayFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToBooleanArrayFunction<O> supplier) {return add(identifier, supplier, null);}

    public GeffPropertyWriter<O> add(String identifier, ToByteArrayFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToShortArrayFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToIntArrayFunction<O> supplier, String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToLongArrayFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToFloatArrayFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToDoubleArrayFunction<O> supplier, String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToBooleanArrayFunction<O> supplier, String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(String identifier, ToByteArrayFunction<O> supplier, int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToShortArrayFunction<O> supplier, int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToIntArrayFunction<O> supplier, int length)     {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToLongArrayFunction<O> supplier, int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToFloatArrayFunction<O> supplier, int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToDoubleArrayFunction<O> supplier, int length)  {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToBooleanArrayFunction<O> supplier, int length) {return add(identifier, supplier, length, null);}

    public GeffPropertyWriter<O> add(String identifier, ToByteArrayFunction<O> supplier, int length, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToShortArrayFunction<O> supplier, int length, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToIntArrayFunction<O> supplier, int length, String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToLongArrayFunction<O> supplier, int length, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToFloatArrayFunction<O> supplier, int length, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToDoubleArrayFunction<O> supplier, int length, String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToBooleanArrayFunction<O> supplier, int length, String typestr) {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}

    public GeffPropertyWriter<O> add(String identifier, ToMaybeByteArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeShortArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeIntArrayFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeLongArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeFloatArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeDoubleArrayFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeBooleanArrayFunction<O> supplier) {return add(identifier, supplier, null);}

    public GeffPropertyWriter<O> add(String identifier, ToMaybeByteArrayFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeShortArrayFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeIntArrayFunction<O> supplier, String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeLongArrayFunction<O> supplier, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeFloatArrayFunction<O> supplier, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeDoubleArrayFunction<O> supplier, String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeBooleanArrayFunction<O> supplier, String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(String identifier, ToMaybeByteArrayFunction<O> supplier, int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeShortArrayFunction<O> supplier, int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeIntArrayFunction<O> supplier, int length)     {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeLongArrayFunction<O> supplier, int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeFloatArrayFunction<O> supplier, int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeDoubleArrayFunction<O> supplier, int length)  {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeBooleanArrayFunction<O> supplier, int length) {return add(identifier, supplier, length, null);}

    public GeffPropertyWriter<O> add(String identifier, ToMaybeByteArrayFunction<O> supplier, int length, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeShortArrayFunction<O> supplier, int length, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeIntArrayFunction<O> supplier, int length, String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeLongArrayFunction<O> supplier, int length, String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeFloatArrayFunction<O> supplier, int length, String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeDoubleArrayFunction<O> supplier, int length, String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(String identifier, ToMaybeBooleanArrayFunction<O> supplier, int length, String typestr) {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}

    /**
     * Harvest properties and return as {@code GeffProperties}.
     */
    public GeffProperties createGeffProperties() {

        if (id == null)
            throw new IllegalStateException("No id property set");

        collectPropertyValues();
        return new GeffProperties(elementType, id, props);
    }

    /**
     * Harvest properties and write to geff hierarchy.
     * (Assuming no compression, no chunking, and geff hierarchy at container root.)
     *
     * @param n5 the {@code N5Writer}
     */
    public void write(final N5Writer n5) {
        write(n5, null, 0, null);
    }

    /**
     * Harvest properties and write to geff hierarchy.
     * (Assuming geff hierarchy is at container root.)
     *
     * @param n5                  the {@code N5Writer}
     * @param optionalCompression compression to use (or {@code null} for no compression)
     * @param chunkSize           if {@code >0}, the chunk size in the slowest-moving
     *                            dimension (last dimension in imglib2 convention, first dimension in numpy
     *                            convention)
     */
    public void write(
            final N5Writer n5,
            final Compression optionalCompression,
            final int chunkSize) {
        write(n5, optionalCompression, chunkSize, null);
    }

    /**
     * Harvest properties and write to geff hierarchy.
     *
     * @param n5                  the {@code N5Writer}
     * @param optionalCompression compression to use (or {@code null} for no compression)
     * @param chunkSize           if {@code >0}, the chunk size in the slowest-moving
     *                            dimension (last dimension in imglib2 convention, first dimension in numpy
     *                            convention)
     * @param geffGroup           path to the geff hierarchy (relative to container root)
     */
    public void write(
            final N5Writer n5,
            final Compression optionalCompression,
            final int chunkSize,
            final String geffGroup) {

        if (id == null)
            throw new IllegalStateException("No id property set");

        collectPropertyValues();
        final Compression compression = optionalCompression != null ? optionalCompression : new RawCompression();
        IoUtils.writeProperty(n5, Cast.unchecked(id), DatasetPaths.ofId(elementType, geffGroup), idDType, compression, chunkSize);
        for (int i = 0; i < props.size(); i++) {
            final GeffProperty<?> prop = props.get(i);
            final DType dType = propsDTypes.get(i);
            IoUtils.writeProperty(n5, Cast.unchecked(prop), DatasetPaths.ofProperty(prop, elementType, geffGroup), dType, compression, chunkSize);
        }
    }



    private <T extends Type<T>> GeffPropertyWriter<O> add(final PropertyAdapter<O, T> propertyAdapter) {
        return add(propertyAdapter, null);
    }

    private <T extends Type<T>> GeffPropertyWriter<O> add(final PropertyAdapter<O, T> propertyAdapter, final String typestr) {
        final GeffPropertyType propertyType = propertyAdapter.propertyType();
        final String identifier = propertyAdapter.identifier();
        final GeffProperty<T> property = WritableProperties.createProperty(identifier, propertyType, numElements, elementIndex);

        setters.add(obj -> property.set(propertyAdapter.adapt(obj)));
        props.add(property);
        propsDTypes.add(verifyDType(typestr, property));
        return this;
    }

    private <T extends IntegerType<T>> GeffPropertyWriter<O> id(final PropertyAdapter<O, T> propertyAdapter) {
        return id(propertyAdapter, null);
    }

    private <T extends IntegerType<T>> GeffPropertyWriter<O> id(final PropertyAdapter<O, T> propertyAdapter, final String typestr) {
        final GeffPropertyType propertyType = propertyAdapter.propertyType();
        final String identifier = propertyAdapter.identifier();
        final GeffProperty<T> property = WritableProperties.createProperty(identifier, propertyType, numElements, elementIndex);

        setters.add(obj -> property.set(propertyAdapter.adapt(obj)));
        id = property;
        idDType = verifyDType(typestr, property);
        return this;
    }

    private static DType verifyDType(final String typestr, final GeffProperty<?> prop) {
        if (typestr == null)
            return null;
        final DType dType = new DType(typestr, null);
        if (unsigned(dType.getDataType()) != unsigned(N5Utils.dataType(Cast.unchecked(prop.type())))) {
            throw new IllegalArgumentException("DataType mismatch: requested \"" + typestr + "\" for " + prop.type().getClass().getSimpleName() + " property");
        }
        return dType;
    }

    private static DataType unsigned(final DataType dataType) {
        switch (dataType) {
            case INT8:
                return DataType.UINT8;
            case INT16:
                return DataType.UINT16;
            case INT32:
                return DataType.UINT32;
            case INT64:
                return DataType.UINT64;
            default:
                return dataType;
        }
    }

    // iterate objects and fill properties
    private void collectPropertyValues() {
        int i = 0;
        for (final O obj : objects) {
            elementIndex.set(i++);
            for (Consumer<O> setter : setters)
                setter.accept(obj);
        }
    }

    /**
     * Create {@code GeffProperty} instances backed by {@code ArrayImg}s that
     * act as intermediate {@link ImgBacked} storage. (These can be written to
     * Zarr directly and are used by {@code GeffWriter} to harvest client
     * properties.)
     */
    static class WritableProperties {

        static <T extends Type<T>> GeffProperty<T> createProperty(final String identifier, final GeffPropertyType propertyType, final long numElements, final ElementIndex sharedElementIndex) {
            final int n = propertyType.numDimensions();
            final RandomAccessibleInterval<BitType> propertyMissing = propertyType.isOptional() ? arrayImg(BitType.class, numElements) : null;
            if (propertyType.isVarLength()) {
                final RandomAccessibleInterval<UnsignedLongType> propertyValues = ArrayImgs.unsignedLongs(n + 1, numElements);
                final VarLengthData<T> propertyData = varLengthData(propertyType.type());
                return new VarLengthWriteProperty<>(identifier, propertyValues, propertyData, propertyMissing, sharedElementIndex);
            } else {
                final long[] dimensions = Arrays.copyOf(propertyType.dimensions().dimensionsAsLongArray(), n + 1);
                dimensions[n] = numElements;
                final RandomAccessibleInterval<T> propertyValues = arrayImg(propertyType.type(), dimensions);
                return new FixedLengthProperty<>(identifier, propertyValues, propertyMissing, sharedElementIndex);
            }
        }

        private static <T, N extends NativeType<N>> RandomAccessibleInterval<T> arrayImg(Class<?> type, long... dimensions) {
            return Cast.unchecked(new ArrayImgFactory<>(WritableProperties.<N>instance(type)).create(dimensions));
        }

        private static <T, N extends NativeType<N>> VarLengthData<T> varLengthData(Class<?> type) {
            return Cast.unchecked(new VarLengthDataImpl<>(WritableProperties.<N>instance(type)));
        }

        @SuppressWarnings("unchecked")
        private static <T extends NativeType<T>> T instance(Class<?> type) {
            if (type == UnsignedByteType.class)
                return (T) new UnsignedByteType();
            else if (type == ByteType.class)
                return (T) new ByteType();
            else if (type == UnsignedShortType.class)
                return (T) new UnsignedShortType();
            else if (type == ShortType.class)
                return (T) new ShortType();
            else if (type == UnsignedIntType.class)
                return (T) new UnsignedIntType();
            else if (type == IntType.class)
                return (T) new IntType();
            else if (type == UnsignedLongType.class)
                return (T) new UnsignedLongType();
            else if (type == LongType.class)
                return (T) new LongType();
            else if (type == FloatType.class)
                return (T) new FloatType();
            else if (type == DoubleType.class)
                return (T) new DoubleType();
            else if (type == BitType.class)
                return (T) new BitType();
            throw new UnsupportedOperationException("TODO " + type);
        }
    }
}
