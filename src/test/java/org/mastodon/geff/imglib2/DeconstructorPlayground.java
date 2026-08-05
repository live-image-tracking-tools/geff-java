package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.imglib2.FunctionTypes.ToDoubleArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToFloatArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleFunction;
import org.mastodon.geff.imglib2.IoUtils.DatasetPaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

public class DeconstructorPlayground {

    record Node(long id, double x, double y, int t) {

        public double[] doublePos() {
            return new double[]{x, y};
        }

        public float[] floatPos() {
            return new float[]{(float) x, (float) y};
        }
    }

    public static void main(String[] args) throws Throwable {

        final List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(0, 0, 9, 0));
        nodes.add(new Node(1, 2, 8, 1));
        nodes.add(new Node(2, 4, 7, 1));
        nodes.add(new Node(3, 5, 6, 2));
        nodes.add(new Node(4, 6, 5, 2));
        nodes.add(new Node(5, 7, 4, 2));
        nodes.add(new Node(6, 8, 3, 2));
        nodes.add(new Node(7, 9, 2, 3));

        for (Node node : nodes) {
            System.out.println(node);
        }



//        final String path = "cross-language-tests/data/basic_3d_original.zarr";
//        final String path = "cross-language-tests/data/covariance_original.zarr";
        final String path = "cross-language-tests/data/deconstructor_playground.zarr";
        try (final N5Writer n5 = new N5ZarrWriter(path)) {

            final GeffWriter<Node> writer = new GeffWriter<>(n5, nodes, ElementType.NODE, null, 0, null);

            writer.add("x", Node::x);
            writer.add("y", (Node node) -> new Maybe.MaybeDouble(node.y()));
            writer.add("doublePos", Node::doublePos);
            writer.add("floatPos", Node::floatPos);

            writer.collectPropertyValues(); // TODO: collectProperties should be folded into write()
            writer.write();
        }
    }


    // ------------------------------------------------------------------------


    static class GeffWriter<O> {

        private final N5Writer n5;
        private final Compression compression;
        private final ElementType elementType;
        private final int chunkSize;
        private final String geffGroup;

        private final Collection<O> objects; //
        private final int numElements;
        private final ElementIndex elementIndex;

        private final List<Consumer<O>> setters = new ArrayList<>();
        private final GeffProperty<? extends IntegerType<?>> id = null;
        private final List<GeffProperty<?>> props = new ArrayList<>();


        /**
         *
         * @param n5
         * @param objects
         * @param elementType which type of element ({@code NODE} or {@code EDGE}) the property refers to
         * @param optionalCompression compression to use (or {@code null} for no compression)
         * @param chunkSize           if {@code >0}, the chunk size in the slowest-moving
         *                            dimension (last dimension in imglib2 convention, first dimension in numpy
         *                            convention)
         * @param geffGroup   path to the geff hierarchy (relative to container root)
         */
        public GeffWriter(
                final N5Writer n5,
                final Collection<O> objects,
                final ElementType elementType,
                final Compression optionalCompression,
                final int chunkSize,
                final String geffGroup) {
            this.n5 = n5;
            this.elementType = elementType;
            this.compression = optionalCompression != null ? optionalCompression : new RawCompression();
            this.chunkSize = chunkSize;
            this.geffGroup = geffGroup;

            this.objects = objects;
            numElements = objects.size();
            elementIndex = new ElementIndex();
        }

        // iterate objects and fill properties
        public void collectPropertyValues() {
            int i = 0;
            for (final O obj : objects) {
                elementIndex.set(i++);
                for (Consumer<O> setter : setters)
                    setter.accept(obj);
            }
        }

        public GeffProperties createGeffProperties() {
            if (id == null)
                throw new IllegalStateException("No id property set");
            return new GeffProperties(elementType, id, props);
        }

        public void add(String identifier, ToDoubleFunction<O> supplier) {
            add(PropertyAdapters.wrap(identifier, supplier));
        }

        public void add(String identifier, ToMaybeDoubleFunction<O> supplier) {
            add(PropertyAdapters.wrap(identifier, supplier));
        }

        public <T> void add(String identifier, ToDoubleArrayFunction<O> supplier) {
            add(PropertyAdapters.wrap(identifier, supplier));
        }

        public void add(String identifier, ToFloatArrayFunction<O> supplier) {
            add(PropertyAdapters.wrap(identifier, supplier));
        }

        private <T extends Type<T>> void add(PropertyAdapter<O, T> propertyAdapter) {

            // Create a property that we can write to.
            // We do this so that we can then write all properties for an
            // element at once, instead of iterating the input elements for
            // every property over and over.
            final GeffPropertyType propertyType = propertyAdapter.propertyType();
            final String identifier = propertyAdapter.identifier();
            final GeffProperty<T> property = WritableProperties.createProperty(identifier, propertyType, numElements, elementIndex);

            System.out.println("propertyAdapter = " + propertyAdapter);
            System.out.println("  writeProperty = " + property);

            setters.add(obj -> property.set(propertyAdapter.adapt(obj)));
            props.add(property);
        }

        public void write() {

            if (id != null)
                IoUtils.writeProperty(n5,Cast.unchecked(id), DatasetPaths.ofId(elementType, geffGroup),
                        null, // TODO: optionalDType: it should be possible to specify this when adding a property
                        compression, chunkSize);
            else
                System.err.println("WARNING: GeffWriter.write: no id property set"); // TODO: exception? log?

            for (GeffProperty<?> prop : props)
                IoUtils.writeProperty(n5, Cast.unchecked(prop), DatasetPaths.ofProperty(prop, elementType, geffGroup),
                        null, // TODO: optionalDType: it should be possible to specify this when adding a property
                        compression, chunkSize);
        }
    }


    static class WriterBuilder {

    }




    static class WritableProperties {

        static <T extends Type<T>> GeffProperty<T> createProperty(final String identifier, final GeffPropertyType propertyType, final long numElements, final ElementIndex sharedElementIndex) {

            if (propertyType.isVarLength()) {
                throw new UnsupportedOperationException("TODO");

            } else {
                final int n = propertyType.numDimensions();
                final long[] dimensions = Arrays.copyOf(propertyType.dimensions().dimensionsAsLongArray(), n + 1);
                dimensions[n] = numElements;

                final RandomAccessibleInterval<T> propertyValues = Cast.unchecked(arrayImg(propertyType.type(), dimensions));
                final RandomAccessibleInterval<BitType> propertyMissing = propertyType.isOptional()
                        ? Cast.unchecked( arrayImg(BitType.class, new long[] {numElements}) )
                        : null;
                return new FixedLengthProperty<>(identifier, propertyValues, propertyMissing, sharedElementIndex);
            }
        }

        private static RandomAccessibleInterval<?> arrayImg(Class<?> type, long[] dimensions) {
            if (type == DoubleType.class) {
                return ArrayImgs.doubles(dimensions);
            } else if (type == FloatType.class) {
                return ArrayImgs.floats(dimensions);
            } else if (type == BitType.class) {
                return ArrayImgs.bits(dimensions);
            }
            throw new UnsupportedOperationException("TODO " + type);
        }

    }
}
