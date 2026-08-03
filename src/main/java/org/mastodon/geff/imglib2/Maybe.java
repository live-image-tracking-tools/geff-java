package org.mastodon.geff.imglib2;

import java.util.NoSuchElementException;

public class Maybe<T> extends AbstractMaybe {
    private T value;

    public Maybe() {
        super(false);
    }

    public Maybe(final T value) {
        super(true);
        this.value = value;
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the value described by this {@code MaybeString}
     * @throws NoSuchElementException if no value is present
     */
    public T get() {
        if (!isPresent()) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    void set(final T value) {
        setPresent(true);
        this.value = value;
    }

    public static final class MaybeByte extends AbstractMaybe {
        private byte value;

        public MaybeByte() {
            super(false);
        }

        public MaybeByte(final byte value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeByte}
         * @throws NoSuchElementException if no value is present
         */
        public byte get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final byte value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeShort extends AbstractMaybe {
        private short value;

        public MaybeShort() {
            super(false);
        }

        public MaybeShort(final short value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeShort}
         * @throws NoSuchElementException if no value is present
         */
        public short get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final short value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeInt extends AbstractMaybe {
        private int value;

        public MaybeInt() {
            super(false);
        }

        public MaybeInt(final int value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeInt}
         * @throws NoSuchElementException if no value is present
         */
        public int get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final int value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeLong extends AbstractMaybe {
        private long value;

        public MaybeLong() {
            super(false);
        }

        public MaybeLong(final long value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeLong}
         * @throws NoSuchElementException if no value is present
         */
        public long get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final long value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeFloat extends AbstractMaybe {
        private float value;

        public MaybeFloat() {
            super(false);
        }

        public MaybeFloat(final float value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeFloat}
         * @throws NoSuchElementException if no value is present
         */
        public float get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final float value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeDouble extends AbstractMaybe {
        private double value;

        public MaybeDouble() {
            super(false);
        }

        public MaybeDouble(final double value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeDouble}
         * @throws NoSuchElementException if no value is present
         */
        public double get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final double value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeBoolean extends AbstractMaybe {
        private boolean value;

        public MaybeBoolean() {
            super(false);
        }

        public MaybeBoolean(final boolean value) {
            super(true);
            this.value = value;
        }

        /**
         * If a value is present, returns the value, otherwise throws
         * {@code NoSuchElementException}.
         *
         * @return the value described by this {@code MaybeBoolean}
         * @throws NoSuchElementException if no value is present
         */
        public boolean get() {
            if (!isPresent()) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }

        void set(final boolean value) {
            setPresent(true);
            this.value = value;
        }
    }

    public static final class MaybeString extends Maybe<String> {
        public MaybeString() {
            super();
        }

        public MaybeString(final String value) {
            super(value);
        }
    }

    public static final class MaybeByteArray extends Maybe<byte[]> {
        public MaybeByteArray() {
            super();
        }

        public MaybeByteArray(final byte[] value) {
            super(value);
        }
    }

    public static final class MaybeShortArray extends Maybe<short[]> {
        public MaybeShortArray() {
            super();
        }

        public MaybeShortArray(final short[] value) {
            super(value);
        }
    }

    public static final class MaybeIntArray extends Maybe<int[]> {
        public MaybeIntArray() {
            super();
        }

        public MaybeIntArray(final int[] value) {
            super(value);
        }
    }

    public static final class MaybeLongArray extends Maybe<long[]> {
        public MaybeLongArray() {
            super();
        }

        public MaybeLongArray(final long[] value) {
            super(value);
        }
    }

    public static final class MaybeFloatArray extends Maybe<float[]> {
        public MaybeFloatArray() {
            super();
        }

        public MaybeFloatArray(final float[] value) {
            super(value);
        }
    }

    public static final class MaybeDoubleArray extends Maybe<double[]> {
        public MaybeDoubleArray() {
            super();
        }

        public MaybeDoubleArray(final double[] value) {
            super(value);
        }
    }

    public static final class MaybeBooleanArray extends Maybe<boolean[]> {
        public MaybeBooleanArray() {
            super();
        }

        public MaybeBooleanArray(final boolean[] value) {
            super(value);
        }
    }
}
