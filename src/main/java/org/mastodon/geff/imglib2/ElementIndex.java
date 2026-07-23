package org.mastodon.geff.imglib2;

import java.util.Arrays;

public final class ElementIndex {

    final long[] slicePos = new long[1];

    // TODO rename to get()
    public long index() {
        return slicePos[0];
    }

    // TODO rename to set()
    public void index( final long index ) {
        slicePos[0] = index;
    }

    @Override
    public String toString() {
        return Long.toString(index());
    }
}
