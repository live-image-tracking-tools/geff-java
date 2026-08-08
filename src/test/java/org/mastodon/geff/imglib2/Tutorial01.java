package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;

public class Tutorial01 {
    public static void main(String[] args) {

        // The central abstraction is GeffProperty<T> (interface).
        //
        // Each GeffProperty maps one node/edge property into a imglib2-like
        // (lazy) container in memory.
        //
        // This is done by reading the Zarr arrays of the property ("values",
        // "missing", "data") through n5-imglib2.
        // This yields each array as a imglib2 RandomAccessibleInterval (lazy:
        // chunks are read and cached only when needed, works transparently for
        // larger-than-memory data). The RandomAccessibleIntervals<T> are
        // appropriately typed, with T the imglib2 Type<T> corresponding to the
        // Zarr dtype.
        //
        // These RandomAccessibleIntervals are then wrapped and presented as a
        // GeffProperty<T>.
        //
        // Let's look at an example. We open the "x" node property of
        final String path = "cross-language-tests/data/basic_2d_original.zarr";
        final N5Reader n5 = new N5ZarrReader(path);

        final String identifier = "x";
        final String valuesPath = "nodes/props/x/values"; // Zarr array for "values"
        final String missingPath = null; // no "missing" array (property is not optional)
        final String dataPath = null; // no "data" array (property is fixed-length)

        final GeffProperty<?> property = IoUtils.loadProperty(n5, identifier, new ElementIndex(), valuesPath, missingPath, dataPath);
        System.out.println("property = " + property);
        // prints:
        //   property = GeffProperty<DoubleType>{identifier="x", scalar}
        //
        // So this particular property is scalar DoubleType, meaning there is
        // one double value for every node.
        //
        // This is not how you typically would open a property. Instead, you
        // would open all node properties at once from PropMetadata.
        // Nevertheless, it's good to know how to do it manually by specifying
        // paths etc...

        // Before we go on: We know that the property generic type in this case
        // is DoubleType, so let's cast to avoid further casting below...
        final GeffProperty<DoubleType> x = (GeffProperty<DoubleType>) property;

        // Let's look at some general information about the GeffProperty:
        System.out.println("x.identifier() = " + x.identifier());
        System.out.println("x.type() = " + x.type().getClass());
        System.out.println("x.isOptional() = " + x.isOptional());
        System.out.println("x.isVarlength() = " + x.isVarlength());
        // prints:
        //   x.identifier() = x
        //   x.type() = class net.imglib2.type.numeric.real.DoubleType
        //   x.isOptional() = false
        //   x.isVarlength() = false
        // These should be self-explanatory...

        // The following more interesting:
        System.out.println("x.numElements() = " + x.numElements());
        System.out.println("x.numDimensions() = " + x.numDimensions());
        // prints:
        //   x.numElements() = 10
        //   x.numDimensions() = 0
        // The "values" array for this property has shape [10] where the first
        // (and only) dimension is the element dimension: There are 10 elements
        // (nodes), and each row corresponds to the property value(s) for one
        // node.
        // The numDimensions() refers to the dimensionality of the property PER
        // ELEMENT, that is, numDimensions()==0 for scalar properties,
        // numDimensions()==1 for vector properties, numDimensions()==2 for
        // matrix properties, and so on...

        // The values() method exposes the properties values FOR THE CURRENT
        // ELEMENT as a RandomAccessibleIntervals<T>. (This is basically a slice
        // into the "values" array). We can set the index of the current element
        // using elementIndex().set(i). The content (and possibly shape) of
        // values() will change accordingly.
        for (int i = 0; i < 3; ++i) {
            x.elementIndex().set(i);
            DoubleType value = x.values().randomAccess().get();
            System.out.println(i + ": value = " + value.get());
        }
        // prints
        //   0: value = 1.0
        //   1: value = 0.9
        //   2: value = 0.8

        // There are some convenience shortcut methods in GeffProperty that
        // avoid going through values() and randomAccess():
        System.out.println("x.getAt().get() = " + x.getAt().get());
        // prints
        //   x.getAt().get() = 0.8
        // This is equivalent to x.values().randomAccess().get().
        //
        // Equivalently, there is getAt(int) for vector properties,
        // getAt(int, int) for matrix properties, etc.

        // There are various decorators to wrap GeffProperty, for example
        // changing the property identifier or type.
        // Here we turn the DoubleType "x" property into FloatType:
        GeffProperty<FloatType> fx = x.convert(FloatType::new);
        // When using the annotation-based constructor parameters, this happens
        // automatically under the hood.
        // Given this parameter:
        //   @FromProperty("pos") float[] pos
        // the framework sees that:
        // * This value must come from a vector property that (may be var-length
        //   or fixed-length, but not a scalar or a matrix).
        // * It must hav identifier "x" (derived from the annotation).
        // * It must not be optional (we expect a float[] for each element).
        // If such a property is found, but is not FloatType, it is
        // automatically converted.
    }
}
