package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;

class RenamedProperty<T> implements GeffProperty<T> {

    private final GeffProperty<T> parent;
    private final String identifier;

    RenamedProperty(final GeffProperty<T> parent, final String identifier) {
        this.parent = parent;
        this.identifier = identifier;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public boolean isVarlength() {
        return parent.isVarlength();
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
    public RandomAccessibleInterval<T> values() {
        return parent.values();
    }

    @Override
    public void set(GeffProperty<T> property) {
        throw new UnsupportedOperationException("ConvertedProperty is read-only");
    }

    @Override
    public String toString() {
        return GeffProperty.toString(this);
    }
}
