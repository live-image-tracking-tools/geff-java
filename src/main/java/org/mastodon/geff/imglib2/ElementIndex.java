package org.mastodon.geff.imglib2;

public final class ElementIndex {

    final long[] slicePos = new long[1];

    public long get() {
        return slicePos[0];
    }

    public void set(final long index ) {
        slicePos[0] = index;
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }
}
