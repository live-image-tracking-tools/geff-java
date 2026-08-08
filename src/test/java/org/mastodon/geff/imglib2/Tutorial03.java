package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.Construction.FromId;
import org.mastodon.geff.imglib2.Construction.FromProperty;
import org.mastodon.geff.imglib2.Construction.NodeConstructor;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import static org.mastodon.geff.imglib2.ConstructorPlayground.buildNodes;

public class Tutorial03 {
    public static void main(String[] args) throws Throwable {

        // As mentioned before, you typically won't load individual properties manually.
        // Instead, you can open all node or edge properties in a geff dataset as follows:
        final String path = "cross-language-tests/data/basic_2d_original.zarr";
        final N5Reader n5 = new N5ZarrReader(path);
        GeffProperties nodeProps = IoUtils.loadProperties(n5, ElementType.NODE);

        // This loads the PropMetadata map and then loads one GeffProperty for
        // every listed property (with the canonical paths determined by
        // ElementType).
        System.out.println("nodeProps = " + nodeProps);
        // prints:
        //   nodeProps = GeffProperties{
        //     elementType=NODE,
        //     numElements=10,
        //     elementIndex=0,
        //     id=GeffProperty<UnsignedLongType>{identifier="id", scalar},
        //     properties={
        //       t=GeffProperty<DoubleType>{identifier="t", scalar}
        //       y=GeffProperty<DoubleType>{identifier="y", scalar}
        //       x=GeffProperty<DoubleType>{identifier="x", scalar}}
        //   }

        // Individual properties can be obtained by identifier:
        GeffProperty<DoubleType> x = nodeProps.property("x");
        GeffProperty<DoubleType> y = nodeProps.property("y");
        GeffProperty<DoubleType> t = nodeProps.property("t");

        // All these properties are linked with a shared .elementIndex() so that
        // they can be iterated through elements in lock-step.
        nodeProps.elementIndex().set(5);
        System.out.println("x.elementIndex = " + x.elementIndex().get());
        System.out.println("y.elementIndex = " + y.elementIndex().get());
        System.out.println("t.elementIndex = " + t.elementIndex().get());
        // prints:
        //   x.elementIndex = 5
        //   y.elementIndex = 5
        //   t.elementIndex = 5

        // Using converter and supplier wrappers as before ...
        DoubleSupplier sx = Suppliers.asDoubleSupplier(x);
        DoubleSupplier sy = Suppliers.asDoubleSupplier(y);
        IntSupplier st = Suppliers.asIntSupplier(t.convert(IntType::new));
        //
        // ... we can now easily iterate through all nodes:
        for (int i = 0; i < nodeProps.numElements(); i++) {
            nodeProps.elementIndex().set(i);
            System.out.println(i + ": x=" + sx.getAsDouble() + ", y=" + sy.getAsDouble() + ", t=" + st.getAsInt());
        }
        // prints:
        //   0: x=1.0, y=100.0, t=1
        //   1: x=0.9, y=144.44444444444446, t=1
        //   2: x=0.8, y=188.88888888888889, t=2
        //   3: x=0.7, y=233.33333333333331, t=2
        //   4: x=0.6, y=277.77777777777777, t=3
        //   5: x=0.5, y=322.22222222222223, t=3
        //   6: x=0.3999999999999999, y=366.66666666666663, t=4
        //   7: x=0.29999999999999993, y=411.1111111111111, t=4
        //   8: x=0.19999999999999996, y=455.55555555555554, t=5
        //   9: x=0.1, y=500.0, t=5

        // This is basically exactly what happens for the annotated constructor method binding.
        class Builder {
            @NodeConstructor
            public void addNode(
                    @FromId long id,
                    @FromProperty("x") double x,
                    @FromProperty("y") double y,
                    @FromProperty("t") int t) {
                System.out.println("id=" + id + ": x=" + sx.getAsDouble() + ", y=" + sy.getAsDouble() + ", t=" + st.getAsInt());
            }
        }
        buildNodes(new Builder(), nodeProps);
        // prints:
        //   id=0: x=1.0, y=100.0, t=1
        //   id=1: x=0.9, y=144.44444444444446, t=1
        //   id=2: x=0.8, y=188.88888888888889, t=2
        //   id=3: x=0.7, y=233.33333333333331, t=2
        //   id=4: x=0.6, y=277.77777777777777, t=3
        //   id=5: x=0.5, y=322.22222222222223, t=3
        //   id=6: x=0.3999999999999999, y=366.66666666666663, t=4
        //   id=7: x=0.29999999999999993, y=411.1111111111111, t=4
        //   id=8: x=0.19999999999999996, y=455.55555555555554, t=5
        //   id=9: x=0.1, y=500.0, t=5


        // A few things worth noting:
        //
        // (1)
        // Manually loaded GeffProperty<T> can also be added to a GeffProperties collection.
        final GeffProperty<?> property = IoUtils.loadProperty(n5, "x2", nodeProps.elementIndex(), "nodes/props/x/values", null, null);
        nodeProps.put(property);
        // This could be useful for supporting older geff versions, where
        // PropMetadata is missing and/or array paths are in non-standard
        // locations.
        //
        // (2)
        // You can mix and match low-level properties and constructor magic:
        class Builder2 {

            private final IntSupplier st;

            Builder2(GeffProperties props) {
                st = Suppliers.asIntSupplier(
                        nodeProps.<DoubleType>property("t")
                                .convert(IntType::new));
            }

            @NodeConstructor
            public void addNode(
                    @FromId long id,
                    @FromProperty("x") double x) {
                int t = st.getAsInt();
                System.out.println("id=" + id + ": x=" + sx.getAsDouble() + ", manual t=" + st.getAsInt());
            }
        }
        buildNodes(new Builder2(nodeProps), nodeProps);
        // prints:
        //   id=0: x=1.0, manual t=1
        //   id=1: x=0.9, manual t=1
        //   id=2: x=0.8, manual t=2
        //   id=3: x=0.7, manual t=2
        //   id=4: x=0.6, manual t=3
        //   id=5: x=0.5, manual t=3
        //   id=6: x=0.3999999999999999, manual t=4
        //   id=7: x=0.29999999999999993, manual t=4
        //   id=8: x=0.19999999999999996, manual t=5
        //   id=9: x=0.1, manual t=5
    }
}
