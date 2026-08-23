package org.mastodon.geff.imglib2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.imglib2.IoUtils.DatasetPaths;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.BooleanType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;

public class GeffPropertyPlayground {


    // -----------------------------------------------------------------------
    // client-side
    //
    record Vertex(long id, double x, int t, long[] vl) {
        @Override
        public String toString() {
            return "Vertex[id=" + id + ", x=" + x + ", t=" + t + ", vl=" + Arrays.toString(vl) + "]";
        }
    }


    // -----------------------------------------------------------------------
    // geff-java side
    //
    static class NodeData {

        private final ElementIndex elementIndex = new ElementIndex();

        private final FixedLengthProperty<UnsignedLongType> id;

        private final Map<String, GeffProperty<?>> nodeProperties = new LinkedHashMap<>();

        // TODO:                          generic int type here?
        NodeData(final RandomAccessibleInterval<UnsignedLongType> nodeIdData) {

            id = new FixedLengthProperty<>("id", nodeIdData, null, elementIndex);
            nodeProperties.put("id", id);
        }

        long size() {
            return id.numElements();
        }

        long index() {
            return elementIndex.get();
        }

        void index(final long index) {
            if (index < 0 || index >= size())
                throw new IndexOutOfBoundsException(index + "(numNodes=" + size() + ")");
            elementIndex.set(index);
        }

        <T> GeffProperty<T> get(final String property) {
            return Cast.unchecked(nodeProperties.get(property));
        }

        <T extends Type<T>> void addProperty(
                final String identifier,
                final RandomAccessibleInterval<T> propertyValues ) {
            addProperty(identifier, propertyValues, null);
        }

        <T extends Type<T>> void addProperty(
                final String identifier,
                final RandomAccessibleInterval<T> propertyValues,
                final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing) {
            final GeffProperty<?> property = new FixedLengthProperty<>(identifier, propertyValues, propertyMissing, elementIndex);
            nodeProperties.put(identifier, property);
        }

        <T extends Type<T>> void addVarLengthProperty(
                final String identifier,
                final RandomAccessibleInterval<UnsignedLongType> propertyValues,
                final RandomAccessibleInterval<T> propertyData) {
            addVarLengthProperty(identifier, propertyValues, propertyData, null);
        }

        <T extends Type<T>> void addVarLengthProperty(
                final String identifier,
                final RandomAccessibleInterval<UnsignedLongType> propertyValues,
                final RandomAccessibleInterval<T> propertyData,
                final RandomAccessibleInterval<? extends BooleanType<?>> propertyMissing) {
            final GeffProperty<?> property = new VarLengthProperty<>(identifier, propertyValues, propertyData, propertyMissing, elementIndex);
            nodeProperties.put(identifier, property);
        }

        @Override
        public String toString() {

            final String props = nodeProperties.entrySet().stream()
                    .map(p -> System.lineSeparator() + "  " + p)
                    .collect(Collectors.joining());

            return "NodeData{" +
                    "size=" + size() +
                    ", index=" + index() +
                    ", properties=" + props +
                    '}';
        }
    }


    // -----------------------------------------------------------------------

	public static void main( final String[] args ) throws GeffException
	{
//        final String path = "cross-language-tests/data/basic_3d_original.zarr";
//        final String path = "cross-language-tests/data/covariance_original.zarr";
        final String path = "cross-language-tests/data/varlength_original.zarr";

        final List<Vertex> nodes = new ArrayList<>();

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {

            final GeffPropertySpecs specs = IoUtils.loadPropertySpecs(n5, ElementType.NODE);
            final GeffProperties nodeData = IoUtils.loadProperties(n5, specs);

            final GeffProperty<UnsignedLongType> id = nodeData.id();
            final GeffProperty<DoubleType> x = nodeData.property("x");
            final GeffProperty<IntType> t = nodeData.property("t").convert(IntType::new);
            final GeffProperty<UnsignedLongType> var_length = nodeData.property("var_length");

            for (int i = 0; i < nodeData.numElements(); i++) {
                nodeData.elementIndex().set(i);

                // varlength ...
                final int len = (int) var_length.values().size();
                final long[] vldata = new long[len];
                final Cursor<UnsignedLongType> c = var_length.values().cursor();
                for (int j = 0; j < len; j++)
                    vldata[j] = c.next().get();

                nodes.add(new Vertex(id.getAt().get(), x.getAt().get(), t.getAt().get(), vldata));
            }

            print(nodes);

            System.out.println("specs = " + specs);
            System.out.println("nodeData = " + nodeData);
        }

        // write
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {

            // allocate space for property data

            // set up target NodeData
            final NodeData nodeData = new NodeData(ArrayImgs.unsignedLongs(nodes.size()));
            nodeData.addProperty("x", ArrayImgs.doubles(nodes.size()));
            nodeData.addProperty("t", ArrayImgs.doubles(nodes.size()));

            // get target properties
            final GeffProperty<UnsignedLongType> id = nodeData.get("id");
            final GeffProperty<DoubleType> x = nodeData.get("x");
            final GeffProperty<DoubleType> t = nodeData.get("t");

            // source handles and properties
            final PropertyAdapter<Vertex, UnsignedLongType> _id = PropertyAdapters.wrap("id", Vertex::id);
            final PropertyAdapter<Vertex, DoubleType> _x = PropertyAdapters.wrap("x", Vertex::x);
            final PropertyAdapter<Vertex, DoubleType> _t = PropertyAdapters.wrap("t", Vertex::t).convert(DoubleType::new);

//            final Converter<IntType, DoubleType> toDoubleConverter = RealTypeConverters.getConverter(new IntType(), t.getType());

            // iterate nodes and fill properties
            for (int i = 0; i < nodes.size(); i++) {

                final Vertex vertex = nodes.get(i);
                nodeData.index(i);

                id.set(_id.adapt(vertex));
                x.set(_x.adapt(vertex));
                t.set(_t.adapt(vertex));
            }

            final BloscCompression compression = new BloscCompression("lz4", 5, 1, 0, 0);
            IoUtils.writeProperty(n5, id, DatasetPaths.ofId(ElementType.NODE), null, compression, 0);
            IoUtils.writeProperty(n5, x, DatasetPaths.ofProperty(x, ElementType.NODE), null, compression, 0);
            IoUtils.writeProperty(n5, t, DatasetPaths.ofProperty(t, ElementType.NODE), null, compression, 0);
        }
    }

    private static void printAttributes(final N5ZarrReader n5, final String dataset) {
        final ZarrDatasetAttributes attributes = (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
        System.out.println(dataset + ":");
        System.out.println("  DType=\"" + attributes.getDType() + "\"");
        System.out.println("  dimensions=\"" + Arrays.toString(attributes.getDimensions()) + "\"");
    }

    private static void print(final List<Vertex> nodes) {
        nodes.forEach(System.out::println);
    }
}
