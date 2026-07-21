package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.mastodon.geff.imglib2.Wrappers.PropertySupplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        NodeData(RandomAccessibleInterval<UnsignedLongType> nodeIdData) {

            id = new FixedLengthProperty<>("id", nodeIdData, null, elementIndex);
            nodeProperties.put("id", id);
        }

        long size() {
            return id.numElements();
        }

        long index() {
            return elementIndex.index();
        }

        void index(long index) {
            if (index < 0 || index >= size())
                throw new IndexOutOfBoundsException(index + "(numNodes=" + size() + ")");
            elementIndex.index(index);
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

    public static void main(String[] args) {
//        final String path = "cross-language-tests/data/basic_3d_original.zarr";
//        final String path = "cross-language-tests/data/covariance_original.zarr";
        final String path = "cross-language-tests/data/varlength_original.zarr";

        final List<Vertex> nodes = new ArrayList<>();

        // read
        try (final N5ZarrReader n5 = new N5ZarrReader(path)) {

            printAttributes(n5, "nodes/ids");
            printAttributes(n5, "nodes/props/x/values");
            printAttributes(n5, "nodes/props/t/values");

            final RandomAccessibleInterval<UnsignedLongType> ids = N5Utils.open(n5, "nodes/ids");
            final RandomAccessibleInterval<DoubleType> xs = N5Utils.open(n5, "nodes/props/x/values");
            final RandomAccessibleInterval<DoubleType> ts = N5Utils.open(n5, "nodes/props/t/values");

            final NodeData nodeData = new NodeData(ids);
            nodeData.addProperty("x", xs);
            nodeData.addProperty("t", ts);

            { // var-length ...
                final String valuesDataset = "nodes/props/var_length/values";
                final String missingDataset = "nodes/props/var_length/missing";
                final String dataDataset = "nodes/props/var_length/data";

                printAttributes(n5, valuesDataset);
                printAttributes(n5, missingDataset);
                printAttributes(n5, dataDataset);

                final RandomAccessibleInterval<UnsignedLongType> values = N5Utils.open(n5, valuesDataset);
                final RandomAccessibleInterval<BoolType> missings = Converters.convert2(
                        N5Utils.<UnsignedByteType>open(n5, missingDataset),
                        (u, b) -> b.set(u.get() != 0),
                        BoolType::new);
                final RandomAccessibleInterval<UnsignedLongType> data = N5Utils.open(n5, dataDataset);

                nodeData.addVarLengthProperty("var_length", values, data, missings);
            }

            final GeffProperty<UnsignedLongType> id = nodeData.get("id");
            final GeffProperty<DoubleType> x = nodeData.get("x");

            // convert geff type to type requested by the client ...
            final GeffProperty<DoubleType> tDouble = nodeData.get("t");
            final Converter<DoubleType, IntType> toIntConverter = RealTypeConverters.getConverter(tDouble.type(), new IntType());
            final RandomAccessibleInterval<IntType> t = Converters.convert2(tDouble.values(), toIntConverter, IntType::new);

            // varlength ...
            final RandomAccessibleInterval<UnsignedLongType> var_length = nodeData.<UnsignedLongType>get("var_length").values();

            for (int i = 0; i < nodeData.size(); i++) {
                nodeData.index(i);

                // varlength ...
                final int len = (int) var_length.size();
                final long[] vldata = new long[len];
                final Cursor<UnsignedLongType> c = var_length.cursor();
                for (int j = 0; j < len; j++)
                    vldata[j] = c.next().get();

                nodes.add(new Vertex(id.getAt().get(), x.getAt().get(), t.getAt().get(), vldata));
            }

            print(nodes);

            System.out.println("nodeData = " + nodeData);
        }

        // write
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {

            // allocate space for property data
            final RandomAccessibleInterval<UnsignedLongType> ids = ArrayImgs.unsignedLongs(nodes.size());
            final RandomAccessibleInterval<DoubleType> xs = ArrayImgs.doubles(nodes.size());
            final RandomAccessibleInterval<DoubleType> ts = ArrayImgs.doubles(nodes.size());

            // set up target NodeData
            final NodeData nodeData = new NodeData(ids);
            nodeData.addProperty("x", xs);
            nodeData.addProperty("t", ts);

            // get target properties
            final GeffProperty<UnsignedLongType> id = nodeData.get("id");
            final GeffProperty<DoubleType> x = nodeData.get("x");
            final GeffProperty<DoubleType> t = nodeData.get("t");

            // source handles and properties
            final PropertySupplier<Vertex, UnsignedLongType> _id = Wrappers.wrap("id", Vertex::id);
            final PropertySupplier<Vertex, DoubleType> _x = Wrappers.wrap("x", Vertex::x);
            final PropertySupplier<Vertex, IntType> _t = Wrappers.wrap("t", Vertex::t);

//            final Converter<IntType, DoubleType> toDoubleConverter = RealTypeConverters.getConverter(new IntType(), t.getType());

            // iterate nodes and fill properties
            for (int i = 0; i < nodes.size(); i++) {

                final Vertex vertex = nodes.get(i);

                nodeData.index(i);
                id.set(_id.update(vertex));
                x.set(_x.update(vertex));

                // TODO: this should be done via converted source property
//                ddata[0] = (double) vertex.t();
//                t.set(_t);
            }

            // write populated data
            writeDataset(n5, "nodes/ids2", "<u8", ids);
            writeDataset(n5, "nodes/props/x/values2", "<f8", xs);
            writeDataset(n5, "nodes/props/t/values2", "<f8", ts);
        }
    }

    private static <T extends NativeType<T>> void writeDataset(
            final N5ZarrWriter n5,
            final String dataset,
            final String typestr,
            final RandomAccessibleInterval<T> data) {
        final long[] dimensions = data.dimensionsAsLongArray();
        final int[] blockSize = Util.long2int(dimensions);
//        final ZstandardCompression compression = new ZstandardCompression(0);
        final BloscCompression compression = new BloscCompression("lz4", 5, 1, 0, 0);
        final ZarrDatasetAttributes attributes = new ZarrDatasetAttributes(
                dimensions,
                blockSize,
                new DType(typestr, null),
                compression,
                true,
                "0"
        );
        n5.createDataset(dataset, attributes);
        N5Utils.saveRegion(data, n5, dataset, attributes);
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
