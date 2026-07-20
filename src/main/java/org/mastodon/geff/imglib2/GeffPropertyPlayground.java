package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.converter.read.ConvertedRandomAccess;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

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
    record Vertex(long id, double x, int t) {
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
            return id.size();
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

        void addProperty(
                final String identifier,
                final RandomAccessibleInterval<?> propertyValues ) {
            addProperty(identifier, propertyValues, null);
        }

        void addProperty(
                final String identifier,
                final RandomAccessibleInterval<?> propertyValues,
                final RandomAccessibleInterval<BooleanType<?>> propertyMissing) {
            final GeffProperty<?> property = new FixedLengthProperty<>(identifier, propertyValues, propertyMissing, elementIndex);
            nodeProperties.put(identifier, property);
        }

        void addVarLengthProperty(
                final String identifier,
                final RandomAccessibleInterval<UnsignedLongType> propertyValues,
                final RandomAccessibleInterval<?> propertyData) {
            addVarLengthProperty(identifier, propertyValues, propertyData, null);
        }

        void addVarLengthProperty(
                final String identifier,
                final RandomAccessibleInterval<UnsignedLongType> propertyValues,
                final RandomAccessibleInterval<?> propertyData,
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


            final Sampler<UnsignedLongType> id = Cast.unchecked(nodeData.get("id").randomAccess());
            final Sampler<DoubleType> x = Cast.unchecked(nodeData.get("x").randomAccess());

            // convert geff type to type requested by the client ...
            final RandomAccess<DoubleType> tDouble = Cast.unchecked(nodeData.get("t").randomAccess());
            final Converter<DoubleType, IntType> toIntConverter = RealTypeConverters.getConverter(tDouble.getType(), new IntType());
            final Sampler<IntType> t = new ConvertedRandomAccess<>(tDouble, toIntConverter, IntType::new);

            for (int i = 0; i < nodeData.size(); i++) {
                nodeData.index(i);
                nodes.add(new Vertex(id.get().get(), x.get().get(), t.get().get()));
            }

            print(nodes);

            System.out.println("nodeData = " + nodeData);
        }

        // write
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {

            final RandomAccessibleInterval<UnsignedLongType> ids = ArrayImgs.unsignedLongs(nodes.size());
            final RandomAccessibleInterval<DoubleType> xs = ArrayImgs.doubles(nodes.size());
            final RandomAccessibleInterval<DoubleType> ts = ArrayImgs.doubles(nodes.size());

            final NodeData nodeData = new NodeData(ids);
            nodeData.addProperty("x", xs);
            nodeData.addProperty("t", ts);

            final Sampler<UnsignedLongType> id = nodeData.<UnsignedLongType>get("id").randomAccess();
            final Sampler<DoubleType> x = nodeData.<DoubleType>get("x").randomAccess();
            final Sampler<DoubleType> t = nodeData.<DoubleType>get("t").randomAccess();

            // convert client provided type to geff type ...
            final Converter<IntType, DoubleType> toDoubleConverter = RealTypeConverters.getConverter(new IntType(), t.getType());
            final IntType tInt = new IntType();

            for (int i = 0; i < nodes.size(); i++) {
                nodeData.index(i);
                id.get().set(nodes.get(i).id());
                x.get().set(nodes.get(i).x());

                // convert client provided type to geff type ...
                tInt.set(nodes.get(i).t());
                toDoubleConverter.convert(tInt, t.get());
            }

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
        final ZarrDatasetAttributes attributes = new ZarrDatasetAttributes(
                dimensions,
                blockSize,
                new DType(typestr, null),
                new ZstandardCompression(0),
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
