package org.mastodon.geff.imglib2;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;

// TODO: revise, refactor, rename ...
public class Types {

    public static abstract class AbstractMaybe {

        private boolean present;

        AbstractMaybe(final boolean present) {
            this.present = present;
        }

        /**
         * If a value is present, returns {@code true}, otherwise {@code false}.
         *
         * @return {@code true} if a value is present, otherwise {@code false}
         */
        public boolean isPresent() {
            return present;
        }

        /**
         * If a value is not present, returns {@code true}, otherwise
         * {@code false}.
         *
         * @return  {@code true} if a value is not present, otherwise {@code false}
         * @since   11
         */
        public boolean isMissing() {
            return !present;
        }

        public void setPresent(final boolean present) {
            this.present = present;
        }
    }

    public static final class MaybeDouble  extends AbstractMaybe {

        private double value;

        public MaybeDouble() {
            super(false);
            this.value = 0;
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



    @FunctionalInterface
    public interface ToMaybeDoubleFunction<O> {
        MaybeDouble apply(O o);
    }

    @FunctionalInterface
    public interface ToDoubleArrayFunction<O> {
        double[] apply(O o);
    }

    @FunctionalInterface
    public interface ToFloatArrayFunction<O> {
        float[] apply(O o);
    }

}
