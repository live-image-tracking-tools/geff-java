package org.mastodon.geff.imglib2;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.integer.UnsignedLongType;

import java.util.Arrays;

public class VarLengthDataPlayground {

    public static void main(String[] args) {
        final VarLengthData<UnsignedLongType, ?> v = new VarLengthData<>(new UnsignedLongType());
        final RandomAccess<UnsignedLongType> a = v.randomAccess();

        print(v);

        a.setPosition(v.size(),0);
        v.growBy(4);
        for (int i = 0; i < 4; i++) {
            a.get().set(i + 1);
            a.fwd(0);
        }
        print(v);

        a.setPosition(v.size(),0);
        v.growBy(5);
        for (int i = 0; i < 5; i++) {
            a.get().set(i + 11);
            a.fwd(0);
        }
        print(v);

        a.setPosition(v.size(),0);
        v.growBy(3);
        for (int i = 0; i < 3; i++) {
            a.get().set(i + 21);
            a.fwd(0);
        }
        print(v);
    }

    private static void print(VarLengthData<UnsignedLongType, ?> data) {

        final int size = (int) data.size();
        System.out.println("data.size() = " + size);
        if (size > 0) {
            final long[] values = new long[size];
            Cursor<UnsignedLongType> c = data.cursor();
            int i = 0;
            while (c.hasNext())
                values[i++] = c.next().get();
            System.out.println("  " + Arrays.toString(values));
        }
    }
}
