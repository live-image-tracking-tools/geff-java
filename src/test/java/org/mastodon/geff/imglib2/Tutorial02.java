package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.Maybe.MaybeByteArray;
import org.mastodon.geff.imglib2.Maybe.MaybeString;

import java.util.Arrays;
import java.util.function.Supplier;

public class Tutorial02 {
    public static void main(String[] args) {

        // More about GeffProperty<T> ...
        //
        // Let's look at a more interesting example. We open the "name" node property of
        final String path = "/Users/pietzsch/Desktop/data/JYT/TrackMate-GEFF-examples/MAX_Merged.geff";
        final N5Reader n5 = new N5ZarrReader(path);

        final String identifier = "name";
        final String valuesPath = "nodes/props/name/values"; // Zarr array for "values"
        final String missingPath = "nodes/props/name/missing"; // Zarr array for "values"
        final String dataPath = "nodes/props/name/data"; // Zarr array for "data"

        final GeffProperty<?> property = IoUtils.loadProperty(n5, identifier, new ElementIndex(), valuesPath, missingPath, dataPath);
        System.out.println("property = " + property);
        // prints:
        //   property = GeffProperty<UnsignedByteType>{identifier="name", varlength[1], optional}
        //
        // So this particular property is var-length[1] (1-dimensional, that is,
        // vector) UnsignedByteType, meaning there is a (var-length) vector of
        // UnsignedByteType for every node.
        //
        // Also, the property is "optional", to may be "missing" for individual elements.
        // Let's check whether the property is "missing" for the first three elements (nodes):
        for (int i = 0; i < 3; ++i) {
            property.elementIndex().set(i);
            System.out.println(i + ": isMissing = " + property.isMissing());
        }
        // prints:
        //   0: isMissing = false
        //   1: isMissing = false
        //   2: isMissing = false
        //
        // Before we go on: We know that the property generic type in this case
        // is UnsignedByteType, so let's cast to avoid further casting below...
        final GeffProperty<UnsignedByteType> name = (GeffProperty<UnsignedByteType>) property;

        // We can again access the property's values for the current element
        // using values(), but this is already more cumbersome than for the
        // scalar property:
        for (int i = 0; i < 3; ++i) {
            name.elementIndex().set(i);
            int size = (int) name.values().dimension(0);
            final byte[] bytes = new byte[size];
            for (int j = 0; j < size; ++j) {
                bytes[j] = name.values().randomAccess().setPositionAndGet(j).getByte();
            }
            System.out.println(i + ": " + Arrays.toString(bytes));
        }
        // prints:
        //   0: [65, 66, 46, 49]
        //   1: [80, 49, 46, 49]
        //   2: [71, 46, 49]
        //
        // This is made minimally easier by the convenience shortcut getAt(int)
        for (int i = 0; i < 3; ++i) {
            name.elementIndex().set(i);
            int size = (int) name.dimensions().dimension(0);
            final byte[] bytes = new byte[size];
            for (int j = 0; j < size; ++j) {
                bytes[j] = name.getAt(j).getByte();
            }
            System.out.println(i + ": " + Arrays.toString(bytes));
        }
        // ...but still cumbersome.
        //
        // Knowing that this property describes a UnsignedByteType vector, we
        // can wrap is as a Supplier that does the above value-harvesting for
        // us:
        Supplier<MaybeByteArray> vectorSupplier = Suppliers.asMaybeByteArraySupplier(name);
        // Note that the property is "optional", which is why we have to wrap it
        // asMaybeByteArraySupplier instead of
        // asByteArraySupplier (which would give a Supplier<byte[]>).
        // With that:
        for (int i = 0; i < 3; ++i) {
            name.elementIndex().set(i);
            System.out.println(i + ": " + Arrays.toString(vectorSupplier.get().get()));
        }

        // We know that this is actually encoding a UTF8 String.
        // This special-case can also be handled by a Supplier:
        Supplier<MaybeString> stringSupplier = Suppliers.asMaybeStringSupplier(name);
        for (int i = 0; i < 3; ++i) {
            name.elementIndex().set(i);
            System.out.println(i + ": " + stringSupplier.get().get());
        }
        // prints:
        //   0: AB.1
        //   1: P1.1
        //   2: G.1
        //
        // Under the hood, this again uses a decorator to convert the property.
        // This case is also handled automatically for annotation-based constructor parameters.
        // Given this parameter:
        //   @FromProperty("name") Optional<String> name
        // the framework does the appropriate conversion (if applicable).
    }
}
