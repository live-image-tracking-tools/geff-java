package org.mastodon.geff.imglib2;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.type.Index;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Cast;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class VarLengthData<T extends NativeType<T>, A extends ArrayDataAccess<A>> implements NativeImg<T, A> {

    private static final int CHUNK_POT = 10;
    private static final int CHUNK_SIZE = 1 << CHUNK_POT;
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    private final Supplier<A> createChunk;
    private final List<A> chunks = new ArrayList<>();

    private final T linkedType;
    private long size;

    VarLengthData(final T type) {
        final NativeTypeFactory<T, ? super A> typeFactory = Cast.unchecked(type.getNativeTypeFactory());
        final A access = ArrayDataAccessFactory.get( typeFactory );
        final int elementsPerChunk = (int) type.getEntitiesPerPixel().mulCeil(CHUNK_SIZE);
        createChunk = () -> access.createArray(elementsPerChunk);
        linkedType = typeFactory.createLinkedType(this);
        size = 0;
    }

    @Override
    public long size() {
        return size;
    }

    /**
     * Grow to size {@code s}.
     * Does nothing if current {@link #size()} {@code >= s}.
     */
    public void growTo(final long s) {
        if (s <= size)
            return;
        size = s;
        final int cs = (int) ((size + CHUNK_MASK) >> CHUNK_POT);
        while (cs > chunks.size()) {
            chunks.add(createChunk.get());
        }
    }

    /**
     * Grow by {@code s} elements.
     */
    public void growBy(final long s) {
        growTo(size + s);
    }

    @SuppressWarnings("unchecked")
    @Override
    public A update(Object obj) {
        return chunks.get(((RA) obj).chunk);
    }

    @Override
    public void setLinkedType(T type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T createLinkedType() {
        return linkedType.duplicateTypeOnSameNativeImg();
    }

    @Override
    public ImgFactory<T> factory() {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public Img<T> copy() {
        throw new UnsupportedOperationException("TODO?");
    }

    @Override
    public long min(int d) {
        assert d == 0;
        return 0;
    }

    @Override
    public long max(int d) {
        assert d == 0;
        return size - 1;
    }

    @Override
    public RandomAccess<T> randomAccess() {
        return new RA();
    }

    @Override
    public RandomAccess<T> randomAccess(Interval interval) {
        return randomAccess();
    }

    @Override
    public int numDimensions() {
        return 1;
    }

    private class RA extends Point implements RandomAccess<T> {

        private final T type;
        private final Index typeIndex;
        private int chunk = -1;

        RA() {
            super(1);
            type = linkedType.duplicateTypeOnSameNativeImg();
            typeIndex = type.index();
        }

        @Override
        public T get() {
            final long pos = position[0];
            final int c = (int) (pos >> CHUNK_POT);
            if (chunk != c) {
                chunk = c;
                type.updateContainer(this);
            }
            typeIndex.set(CHUNK_MASK & (int) pos);
            return type;
        }

        @Override
        public T getType() {
            return type;
        }

        @Override
        public RandomAccess<T> copy() {
            final RA copy = new RA();
            copy.setPosition(this);
            return copy;
        }
    }
}
