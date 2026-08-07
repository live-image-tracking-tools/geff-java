package org.mastodon.geff.imglib2;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.imglib2.Maybe.MaybeDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static org.mastodon.geff.imglib2.ElementType.NODE;

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

            GeffProperties props =
            new GeffPropertyWriter<>(nodes, NODE)
                    .id(Node::id, "<u8")
                    .add("x", Node::x)
                    .add("y", (Node node) -> new MaybeDouble(node.y()))
                    .add("doublePos", Node::doublePos, 2) // fixed-length
                    .add("floatPos", Node::floatPos) // var-length
                    .add("name", Node::toString) // converted to var-length uint8
                    .createGeffProperties();
//                    .write(n5);

            System.out.println("props = " + props);

            final LongSupplier id = Suppliers.asLongSupplier(props.id());
            final DoubleSupplier x = Suppliers.asDoubleSupplier(props.property("x"));
            final Supplier<String> name = Suppliers.asStringSupplier(props.property("name"));
            for (int i = 0; i < props.numElements(); i++) {
                props.elementIndex().set(i);
                System.out.println("id=" + id.getAsLong() +
                ", x=" + x.getAsDouble() +
                ", name=" + name.get());
            }
        }
    }

}

