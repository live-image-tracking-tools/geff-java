package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.integer.UnsignedLongType;

public interface ImgBacked<T> {

    /**
     * The "values" dataset for fixed-length properties or the "data" dataset
     * for var-length properties.
     * <p>
     * Must be present.
     */
    RandomAccessibleInterval<T> getDataRAI();

    /**
     * The "missing" dataset for optional properties.
     * <p>
     * {@code null} for non-optional properties.
     */
    RandomAccessibleInterval<? extends BooleanType<?>> getMissingRAI();

    /**
     * The "values" dataset for var-length properties.
     * <p>
     * {@code null} for fixed-length properties.
     */
    RandomAccessibleInterval<UnsignedLongType> getIndexRAI();
}
