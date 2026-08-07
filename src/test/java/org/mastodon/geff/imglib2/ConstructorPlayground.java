package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.real.DoubleType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.Construction.FromId;
import org.mastodon.geff.imglib2.Construction.FromProperty;
import org.mastodon.geff.imglib2.Construction.NodeConstructor;
import org.mastodon.geff.imglib2.Maybe.MaybeDouble;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.mastodon.geff.imglib2.ElementType.NODE;


public class ConstructorPlayground {

    static class MyBuilder {
        @NodeConstructor
        public void addVertex(
                @FromId() long id,
                @FromProperty("my_x") double x,
                @FromProperty("y") double y,
                @FromProperty("z") double z,
                @FromProperty(value = "covariance2d") double[] cov2d,
                @FromProperty("t") long t) {
            System.out.println("addVertex(id=" + id + ", x=" + x + ", y=" + y + ", z=" + z + ", cov2d=" + Arrays.toString(cov2d) + ", t=" + t + ")");
        }
    }

    static class MyBuilderWithStrings {
        @NodeConstructor
        public void createNode(
                @FromId() int id,
                @FromProperty("POSITION_X") double x,
                @FromProperty("POSITION_Y") MaybeDouble y,
                @FromProperty("name") Optional<String> name
        ) {
            System.out.println("id = " + id + ", x = " + x + ", y = " + y.get() + ", name = " + name);
        }
    }

    public static void main(String[] args) throws Throwable {

        final String path = "/Users/pietzsch/Desktop/data/JYT/TrackMate-GEFF-examples/MAX_Merged.geff";
//        final String path = "cross-language-tests/data/covariance_original.zarr";

        try (final N5Reader n5 = new N5ZarrReader(path)) {
            final GeffProperties props = IoUtils.loadProperties(n5, NODE);
            final MyBuilderWithStrings nodeBuilder = new MyBuilderWithStrings();
            buildNodes( nodeBuilder, props);
        }
    }

    static class MyAdvancedBuilder {

        @NodeConstructor
        public void addVertex(
                @FromId() long id,
                @FromProperty("x") double x,
                @FromProperty("y") GeffProperty<DoubleType> y, // escape hatch for stuff we have not implemented yet.
                @FromProperty("t") long t, // automatically type-converted
                @FromProperty("covariance2d") double[] cov2d,
                @FromProperty("covariance3d") Optional<double[]> cov3d) { // for properties that could be missing

            System.out.println("addVertex(id=" + id + ", x=" + x + ", y=" + y.getAt().get() + ", t=" + t + ", cov2d=" + Arrays.toString(cov2d) + ", cov32=" + Arrays.toString(cov3d.orElse(null)) + ")");
        }
    }

    // -------------------------------

    public static void buildNodes(final Object target, final GeffProperties properties) throws Throwable {

        final ElementCreator c = ElementCreator.of(target, properties);
        for (int i = 0; i < properties.numElements(); i++) {
            c.createElement(i);
        }
    }

}
