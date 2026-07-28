package org.mastodon.geff.imglib2;

import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StringPlayground {


    // ------------------------------------------------------------------------
    //
    //    main
    //
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
        final String path = "/Users/pietzsch/Desktop/data/JYT/TrackMate-GEFF-examples/MAX_Merged.geff";

        // read
        try (final N5Reader n5 = new N5ZarrReader(path)) {

            final GeffPropertySpecs nodePropertySpecs = IoUtils.loadPropertySpecs(n5, ElementType.NODE);
            final GeffProperties nodeProperties = IoUtils.loadProperties(n5, nodePropertySpecs);

//            System.out.println("nodePropertySpecs = " + nodePropertySpecs);
//            System.out.println("nodeProperties = " + nodeProperties);

            final GeffPropertySpec nameSpec = nodePropertySpecs.properties().get("name");
            System.out.println("nameSpec = " + nameSpec);
            System.out.println();
            GeffProperty<UnsignedByteType> name = nodeProperties.property("name");
            System.out.println("name = " + name);
            System.out.println();

            final VarLengthAsStringProperty converted = new VarLengthAsStringProperty(name);

            for (int i = 0; i < Math.min(nodeProperties.numElements(), 5); i++) {
                nodeProperties.elementIndex().index(i);

                System.out.println("name.values().dimensionsAsLongArray() = " + Arrays.toString(name.values().dimensionsAsLongArray()));

                final int len = (int) name.values().dimension(0);
                byte[] data = new byte[len];
                for (int j = 0; j < len; j++) {
                    data[j] = name.getAt(j).getByte();
                }
                String str = new String(data, StandardCharsets.UTF_8);
                System.out.println("str = \"" + str +"\"");

                System.out.println("converted.getAt() = \"" + converted.getAt() + "\"");
            }
        }
    }




    private static ZarrDatasetAttributes attrs(final N5Reader n5, final String dataset) {
        return (ZarrDatasetAttributes) n5.getDatasetAttributes(dataset);
    }

    private static void printAttributes(final N5Reader n5, final String dataset) {
        final ZarrDatasetAttributes attributes = attrs(n5, dataset);
        System.out.println(dataset + ":");
        System.out.println("  DType=\"" + attributes.getDType() + "\"");
        System.out.println("  dimensions=\"" + Arrays.toString(attributes.getDimensions()) + "\"");
    }



    static class VarLengthAsStringProperty implements GeffProperty<String> {

        private final GeffProperty<UnsignedByteType> parent;
        private final PropertyRAI<String> values;

        VarLengthAsStringProperty(final GeffProperty<UnsignedByteType> parent) {

            assert parent.isVarlength();
            assert parent.numDimensions() == 1;

            this.parent = parent;
            values = new PropertyRAI<>(new FinalDimensions(), new RA());
        }

        @Override
        public String identifier() {
            return parent.identifier();
        }

        @Override
        public boolean isVarlength() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return parent.isOptional();
        }

        @Override
        public long numElements() {
            return parent.numElements();
        }

        @Override
        public ElementIndex elementIndex() {
            return parent.elementIndex();
        }

        @Override
        public boolean isMissing() {
            return parent.isMissing();
        }

        @Override
        public RandomAccessibleInterval<String> values() {
            return values;
        }

        @Override
        public void set(GeffProperty<String> property) {
            throw new UnsupportedOperationException("VarLengthAsStringProperty is read-only");
        }

        @Override
        public String toString() {
            return GeffProperty.toString(this);
        }

        private class RA extends Point implements RandomAccess<String> {

            private final RandomAccess<UnsignedByteType> a;

            RA() {
                super(0);
                a = parent.values().randomAccess().copy();
            }

            @Override
            public String getType() {
                return "";
            }

            @Override
            public String get() {
                final int len = (int) parent.values().dimension(0);
                byte[] data = new byte[len];
                a.setPosition(0, 0);
                for (int j = 0; j < len; j++) {
                    data[j] = a.get().getByte();
                    a.fwd(0);
                }
                return new String(data, StandardCharsets.UTF_8);
            }

            @Override
            public RandomAccess<String> copy() {
                return new RA();
            }
        }
    }
}
