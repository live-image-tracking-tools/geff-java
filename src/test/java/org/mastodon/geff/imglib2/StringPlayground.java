package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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

            final GeffProperty<String> converted = new VarLengthAsStringProperty(name);

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


        // write
        {
            final List<MyNamedThing> myNamedThings = Arrays.stream(new String[]{"hello", "world", "here", "are", "some", "strings"}).map(MyNamedThing::new).toList();
            final PropertySupplier<MyNamedThing, String> _name = Wrappers.wrap("name", MyNamedThing::getName);
            System.out.println("_name = " + GeffProperty.toString(_name));
            System.out.println();
            final GeffProperty<UnsignedByteType> converted = new StringAsVarLengthProperty(_name);
            System.out.println("converted = " + converted);
            System.out.println();

            for (MyNamedThing obj : myNamedThings) {
                System.out.println("_name.update(obj).getAt() = " + _name.update(obj).getAt());
                print(converted);
            }
        }
    }

    static class MyNamedThing {
        private final String name;
        private MyNamedThing(String name) {this.name = name;}
        public String getName() {return name;}
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

    private static void print(GeffProperty<UnsignedByteType> data) {

        final int size = (int) data.values().size();
        System.out.println("data.size() = " + size);
        if (size > 0) {
            final byte[] values = new byte[size];
            final Cursor<UnsignedByteType> c = data.values().cursor();
            int i = 0;
            while (c.hasNext())
                values[i++] = c.next().getByte();
            System.out.println("  " + Arrays.toString(values));
        }
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





    static class StringAsVarLengthProperty implements GeffProperty<UnsignedByteType> {

        private final GeffProperty<String> parent;

        private String currentValue;
        private byte[] data = null;
        private long[] dataLength = new long[1];
        private final PropertyRAI<UnsignedByteType> values;

        StringAsVarLengthProperty(final GeffProperty<String> parent) {
            assert !parent.isVarlength();
            assert parent.numDimensions() == 0;
            this.parent = parent;
            final Dimensions dimensions = FinalDimensions.wrap(dataLength);
            values = new PropertyRAI<>(() -> {
                updateData();
                return dimensions;
            }, new RA());
        }

        @Override
        public String identifier() {
            return parent.identifier();
        }

        @Override
        public boolean isVarlength() {
            return true;
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
        public RandomAccessibleInterval<UnsignedByteType> values() {
            return values;
        }

        @Override
        public void set(GeffProperty<UnsignedByteType> property) {
            throw new UnsupportedOperationException("StringAsVarLengthProperty is read-only");
        }

        @Override
        public String toString() {
            return GeffProperty.toString(this);
        }

        private void updateData() {
            final String value = parent.getAt();
            if (currentValue != value) {
                currentValue = value;
                data = value.getBytes(StandardCharsets.UTF_8);
                dataLength[0] = data.length;
            }
        }

        private class RA extends Point implements RandomAccess<UnsignedByteType> {

            private final UnsignedByteType type;

            RA() {
                super(1);
                type = new UnsignedByteType();
            }

            @Override
            public UnsignedByteType getType() {
                return type;
            }

            @Override
            public UnsignedByteType get() {
                updateData();
                type.setByte(data[getIntPosition(0)]);
                return type;
            }

            @Override
            public RandomAccess<UnsignedByteType> copy() {
                final RA copy = new RA();
                copy.setPosition(this);
                return copy;
            }
        }
    }
}

