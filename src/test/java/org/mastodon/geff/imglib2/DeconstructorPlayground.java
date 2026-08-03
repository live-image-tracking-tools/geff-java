package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.mastodon.geff.imglib2.FunctionTypes.ToDoubleArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToFloatArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleFunction;

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

        final GeffWriter<Node> writer = new GeffWriter<>(nodes, ElementType.NODE);
        writer.add("x", Node::x);
        writer.add("y", (Node node) -> new Maybe.MaybeDouble(node.y()));
        writer.add("doublePos", Node::doublePos);
        writer.add("floatPos", Node::floatPos);

        writer.collectPropertyValues();
    }


    // ------------------------------------------------------------------------


    static class GeffWriter<O> {

        private final Collection<O> objects; //
        private final int numElements;
        private final ElementType elementType;

        private final ElementIndex elementIndex;

        private final List<Consumer<O>> setters = new ArrayList<>();

        private final GeffProperty<? extends IntegerType<?>> id = null;
        private final List<GeffProperty<?>> props = new ArrayList<>();


        public GeffWriter(final Collection<O> objects, final ElementType elementType) {
            this.objects = objects;
            numElements = objects.size();
            this.elementType = elementType;
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
