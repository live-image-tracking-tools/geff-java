package org.litt.geff.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.litt.geff.io.IoUtils.DatasetPaths;
import org.litt.geff.props.ElementIndex;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FixedLengthProperty;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;
import org.litt.geff.props.GeffPropertyType;
import org.litt.geff.props.PropertyAdapter;
import org.litt.geff.props.PropertyAdapters;
import org.litt.geff.props.StringAsVarLengthPropertyAdapter;
import org.litt.geff.props.VarLengthData;
import org.litt.geff.props.VarLengthDataImpl;
import org.litt.geff.props.VarLengthWriteProperty;
import org.litt.geff.props.FunctionTypes.ToBooleanArrayFunction;
import org.litt.geff.props.FunctionTypes.ToBooleanFunction;
import org.litt.geff.props.FunctionTypes.ToByteArrayFunction;
import org.litt.geff.props.FunctionTypes.ToByteFunction;
import org.litt.geff.props.FunctionTypes.ToDoubleArrayFunction;
import org.litt.geff.props.FunctionTypes.ToFloatArrayFunction;
import org.litt.geff.props.FunctionTypes.ToFloatFunction;
import org.litt.geff.props.FunctionTypes.ToIntArrayFunction;
import org.litt.geff.props.FunctionTypes.ToLongArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeBooleanArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeBooleanFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeByteArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeByteFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeDoubleArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeDoubleFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeFloatArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeFloatFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeIntArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeIntFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeLongArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeLongFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeShortArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeShortFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeStringFunction;
import org.litt.geff.props.FunctionTypes.ToShortArrayFunction;
import org.litt.geff.props.FunctionTypes.ToShortFunction;
import org.litt.geff.props.FunctionTypes.ToStringFunction;

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


/**
 * Builder for harvesting properties from a collection of client objects and
 * writing them to a geff hierarchy.
 * <p>
 * The client's node or edge class is described by registering one accessor
 * method per geff property: {@code id(...)} for the (mandatory) element ids,
 * and {@code add(identifier, ...)} for everything else. The terminal operations
 * {@link #write(N5Writer)} and {@link #createGeffProperties()} then make a
 * single pass over the collection and harvest property values into intermediate
 * storage. {@link #write(N5Writer)} then writes these to Zarr, {@link
 * #createGeffProperties()} returns them as {@link GeffProperties}.
 * <pre>{@code
 * record Node(long id, double x, double y, int t) {
 *     double[] doublePos() { return new double[] { x, y }; }
 *     float[] floatPos()   { return new float[] { (float) x, (float) y }; }
 * }
 *
 * List<Node> nodes = ...;
 * try (N5Writer n5 = new N5ZarrWriter(path)) {
 *     new GeffPropertyWriter<>(nodes, NODE)
 *             .id(Node::id, "<u8")
 *             .add("x", Node::x)                            // scalar
 *             .add("y", node -> new MaybeDouble(node.y()))  // scalar, optional
 *             .add("doublePos", Node::doublePos, 2)         // fixed-length (2)
 *             .add("floatPos", Node::floatPos)              // var-length
 *             .add("name", Node::toString)                  // converted to var-length uint8
 *             .write(n5);
 * }
 * }</pre>
 *
 * <h2>Choosing the overload</h2>
 *
 * The kind of property that is written is determined by the accessor's
 * signature, not by an explicit descriptor:
 * <ul>
 * <li>a scalar accessor ({@code Node::x}, that is, {@link ToDoubleFunction} and
 *     the corresponding scalar interfaces in
 *     {@link org.litt.geff.props.FunctionTypes}) yields a
 *     scalar property;</li>
 * <li>an array accessor with an explicit {@code length} argument yields a
 *     fixed-length vector property of that length;</li>
 * <li>an array accessor <em>without</em> {@code length} yields a var-length
 *     property, whose rows may differ in length per element;</li>
 * <li>an accessor returning one of the {@link org.litt.geff.props.Maybe} types
 *     yields an optional property, which additionally writes a {@code missing}
 *     dataset;</li>
 * <li>an accessor returning {@code String} is encoded as UTF-8 and stored as a
 *     var-length {@code uint8} property.</li>
 * </ul>
 * The Java return type also picks the imglib2 type of the column, and with it
 * the default Zarr dtype ({@code double} to {@code <f8}, {@code long} to
 * {@code <u8}, and so on). The optional trailing {@code typestr} argument
 * overrides that dtype. It must agree with the column's data type up to
 * signedness &mdash; {@code "<i8"} for a {@code long} accessor is accepted,
 * {@code "<i4"} is not &mdash; and is rejected with an
 * {@link IllegalArgumentException} otherwise.
 *
 * <h2>Notes</h2>
 *
 * <ul>
 * <li>An id property is mandatory. Both terminal operations throw
 *     {@link IllegalStateException} if {@code id(...)} was never called.</li>
 * <li>Values are harvested into in-memory {@code ArrayImg}s before they are
 *     written, so the property data of all elements has to fit into RAM.</li>
 * <li>Only property datasets are written (see {@link DatasetPaths}:
 *     {@code <elementType>/props/<identifier>/values}, plus {@code missing} and
 *     {@code data} where applicable). Geff metadata (version, axes,
 *     {@code props_metadata}) is not written by this class.</li>
 * </ul>
 *
 * @param <O> type of the client's node or edge objects
 */
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
     * Create a new {@code GeffPropertyWriter}.
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

    public GeffPropertyWriter<O> id(final ToByteFunction<O> supplier)  {return id(supplier, null);}
    public GeffPropertyWriter<O> id(final ToShortFunction<O> supplier) {return id(supplier, null);}
    public GeffPropertyWriter<O> id(final ToIntFunction<O> supplier)   {return id(supplier, null);}
    public GeffPropertyWriter<O> id(final ToLongFunction<O> supplier)  {return id(supplier, null);}

    public GeffPropertyWriter<O> id(final ToByteFunction<O> supplier, final String typestr)  {return id(PropertyAdapters.wrap("id", supplier), typestr);}
    public GeffPropertyWriter<O> id(final ToShortFunction<O> supplier, final String typestr) {return id(PropertyAdapters.wrap("id", supplier), typestr);}
    public GeffPropertyWriter<O> id(final ToIntFunction<O> supplier, final String typestr)   {return id(PropertyAdapters.wrap("id", supplier), typestr);}
    public GeffPropertyWriter<O> id(final ToLongFunction<O> supplier, final String typestr)  {return id(PropertyAdapters.wrap("id", supplier), typestr);}

    public GeffPropertyWriter<O> id(final ToByteArrayFunction<O> supplier, final int length)  {return id(supplier, length, null);}
    public GeffPropertyWriter<O> id(final ToShortArrayFunction<O> supplier, final int length) {return id(supplier, length, null);}
    public GeffPropertyWriter<O> id(final ToIntArrayFunction<O> supplier, final int length)   {return id(supplier, length, null);}
    public GeffPropertyWriter<O> id(final ToLongArrayFunction<O> supplier, final int length)  {return id(supplier, length, null);}

    public GeffPropertyWriter<O> id(final ToByteArrayFunction<O> supplier, final int length, final String typestr)  {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}
    public GeffPropertyWriter<O> id(final ToShortArrayFunction<O> supplier, final int length, final String typestr) {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}
    public GeffPropertyWriter<O> id(final ToIntArrayFunction<O> supplier, final int length, final String typestr)   {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}
    public GeffPropertyWriter<O> id(final ToLongArrayFunction<O> supplier, final int length, final String typestr)  {return id(PropertyAdapters.wrap("id", supplier, length), typestr);}

    public GeffPropertyWriter<O> add(final String identifier, final ToByteFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToShortFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToIntFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToLongFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToFloatFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToDoubleFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToBooleanFunction<O> supplier) {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToStringFunction<O> supplier)  {return add(new StringAsVarLengthPropertyAdapter<O>(PropertyAdapters.wrap(identifier, supplier)));}

    public GeffPropertyWriter<O> add(final String identifier, final ToByteFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToShortFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToIntFunction<O> supplier, final String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToLongFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToFloatFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToDoubleFunction<O> supplier, final String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToBooleanFunction<O> supplier, final String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeByteFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeShortFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeIntFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeLongFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeFloatFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeDoubleFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeBooleanFunction<O> supplier) {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeStringFunction<O> supplier)  {return add(new StringAsVarLengthPropertyAdapter<O>(PropertyAdapters.wrap(identifier, supplier)));}

    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeByteFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeShortFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeIntFunction<O> supplier, final String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeLongFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeFloatFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeDoubleFunction<O> supplier, final String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeBooleanFunction<O> supplier, final String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(final String identifier, final ToByteArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToShortArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToIntArrayFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToLongArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToFloatArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToDoubleArrayFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToBooleanArrayFunction<O> supplier) {return add(identifier, supplier, null);}

    public GeffPropertyWriter<O> add(final String identifier, final ToByteArrayFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToShortArrayFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToIntArrayFunction<O> supplier, final String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToLongArrayFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToFloatArrayFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToDoubleArrayFunction<O> supplier, final String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToBooleanArrayFunction<O> supplier, final String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(final String identifier, final ToByteArrayFunction<O> supplier, final int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToShortArrayFunction<O> supplier, final int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToIntArrayFunction<O> supplier, final int length)     {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToLongArrayFunction<O> supplier, final int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToFloatArrayFunction<O> supplier, final int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToDoubleArrayFunction<O> supplier, final int length)  {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToBooleanArrayFunction<O> supplier, final int length) {return add(identifier, supplier, length, null);}

    public GeffPropertyWriter<O> add(final String identifier, final ToByteArrayFunction<O> supplier, final int length, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToShortArrayFunction<O> supplier, final int length, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToIntArrayFunction<O> supplier, final int length, final String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToLongArrayFunction<O> supplier, final int length, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToFloatArrayFunction<O> supplier, final int length, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToDoubleArrayFunction<O> supplier, final int length, final String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToBooleanArrayFunction<O> supplier, final int length, final String typestr) {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}

    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeByteArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeShortArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeIntArrayFunction<O> supplier)     {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeLongArrayFunction<O> supplier)    {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeFloatArrayFunction<O> supplier)   {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeDoubleArrayFunction<O> supplier)  {return add(identifier, supplier, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeBooleanArrayFunction<O> supplier) {return add(identifier, supplier, null);}

    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeByteArrayFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeShortArrayFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeIntArrayFunction<O> supplier, final String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeLongArrayFunction<O> supplier, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeFloatArrayFunction<O> supplier, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeDoubleArrayFunction<O> supplier, final String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeBooleanArrayFunction<O> supplier, final String typestr) {return add(PropertyAdapters.wrap(identifier, supplier), typestr);}

    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeByteArrayFunction<O> supplier, final int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeShortArrayFunction<O> supplier, final int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeIntArrayFunction<O> supplier, final int length)     {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeLongArrayFunction<O> supplier, final int length)    {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeFloatArrayFunction<O> supplier, final int length)   {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeDoubleArrayFunction<O> supplier, final int length)  {return add(identifier, supplier, length, null);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeBooleanArrayFunction<O> supplier, final int length) {return add(identifier, supplier, length, null);}

    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeByteArrayFunction<O> supplier, final int length, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeShortArrayFunction<O> supplier, final int length, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeIntArrayFunction<O> supplier, final int length, final String typestr)     {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeLongArrayFunction<O> supplier, final int length, final String typestr)    {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeFloatArrayFunction<O> supplier, final int length, final String typestr)   {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeDoubleArrayFunction<O> supplier, final int length, final String typestr)  {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}
    public GeffPropertyWriter<O> add(final String identifier, final ToMaybeBooleanArrayFunction<O> supplier, final int length, final String typestr) {return add(PropertyAdapters.wrap(identifier, supplier, length), typestr);}

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
            for (final Consumer<O> setter : setters)
                setter.accept(obj);
        }
    }

    /**
     * Create {@code GeffProperty} instances backed by {@code ArrayImg}s that
     * act as intermediate {@link ImgBacked} storage. (These can be written to
     * Zarr directly and are used by {@code GeffPropertyWriter} to harvest
     * client properties.)
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

        private static <T, N extends NativeType<N>> RandomAccessibleInterval<T> arrayImg(final Class<?> type, final long... dimensions) {
            return Cast.unchecked(new ArrayImgFactory<>(WritableProperties.<N>instance(type)).create(dimensions));
        }

        private static <T, N extends NativeType<N>> VarLengthData<T> varLengthData(final Class<?> type) {
            return Cast.unchecked(new VarLengthDataImpl<>(WritableProperties.<N>instance(type)));
        }

        @SuppressWarnings("unchecked")
        private static <T extends NativeType<T>> T instance(final Class<?> type) {
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
            throw new UnsupportedOperationException(
                    "Unsupported property type " + type.getName()
                            + ". Supported types are ByteType, UnsignedByteType, ShortType, UnsignedShortType, "
                            + "IntType, UnsignedIntType, LongType, UnsignedLongType, FloatType, DoubleType and BitType. "
                            + "Convert the property to one of these types before writing, e.g. with GeffProperty.convert().");
        }
    }
}
