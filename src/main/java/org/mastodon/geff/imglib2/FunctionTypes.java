package org.mastodon.geff.imglib2;

import org.mastodon.geff.imglib2.Maybe.MaybeBoolean;
import org.mastodon.geff.imglib2.Maybe.MaybeBooleanArray;
import org.mastodon.geff.imglib2.Maybe.MaybeByte;
import org.mastodon.geff.imglib2.Maybe.MaybeByteArray;
import org.mastodon.geff.imglib2.Maybe.MaybeDouble;
import org.mastodon.geff.imglib2.Maybe.MaybeDoubleArray;
import org.mastodon.geff.imglib2.Maybe.MaybeFloat;
import org.mastodon.geff.imglib2.Maybe.MaybeFloatArray;
import org.mastodon.geff.imglib2.Maybe.MaybeInt;
import org.mastodon.geff.imglib2.Maybe.MaybeIntArray;
import org.mastodon.geff.imglib2.Maybe.MaybeLong;
import org.mastodon.geff.imglib2.Maybe.MaybeLongArray;
import org.mastodon.geff.imglib2.Maybe.MaybeShort;
import org.mastodon.geff.imglib2.Maybe.MaybeShortArray;
import org.mastodon.geff.imglib2.Maybe.MaybeString;

import java.util.function.Function;

// TODO: revise, refactor, rename ...
public class FunctionTypes {

    @FunctionalInterface public interface ToByteFunction<T> {
        byte applyAsByte(T value);
    }

    @FunctionalInterface public interface ToShortFunction<T> {
        short applyAsShort(T value);
    }

    @FunctionalInterface public interface ToFloatFunction<T> {
        float applyAsFloat(T value);
    }

    @FunctionalInterface public interface ToBooleanFunction<T> {
        boolean applyAsBoolean(T value);
    }

    @FunctionalInterface public interface ToMaybeByteFunction<T> extends Function<T, MaybeByte> {}
    @FunctionalInterface public interface ToMaybeShortFunction<T> extends Function<T, MaybeShort> {}
    @FunctionalInterface public interface ToMaybeIntFunction<T> extends Function<T, MaybeInt> {}
    @FunctionalInterface public interface ToMaybeLongFunction<T> extends Function<T, MaybeLong> {}
    @FunctionalInterface public interface ToMaybeFloatFunction<T> extends Function<T, MaybeFloat> {}
    @FunctionalInterface public interface ToMaybeDoubleFunction<T> extends Function<T, MaybeDouble> {}
    @FunctionalInterface public interface ToMaybeBooleanFunction<T> extends Function<T, MaybeBoolean> {}

    @FunctionalInterface public interface ToByteArrayFunction<T> extends Function<T, byte[]> {}
    @FunctionalInterface public interface ToShortArrayFunction<T> extends Function<T, short[]> {}
    @FunctionalInterface public interface ToIntArrayFunction<T> extends Function<T, int[]> {}
    @FunctionalInterface public interface ToLongArrayFunction<T> extends Function<T, long[]> {}
    @FunctionalInterface public interface ToFloatArrayFunction<T> extends Function<T, float[]> {}
    @FunctionalInterface public interface ToDoubleArrayFunction<T> extends Function<T, double[]> {}
    @FunctionalInterface public interface ToBooleanArrayFunction<T> extends Function<T, boolean[]> {}

    @FunctionalInterface public interface ToMaybeByteArrayFunction<T> extends Function<T, MaybeByteArray> {}
    @FunctionalInterface public interface ToMaybeShortArrayFunction<T> extends Function<T, MaybeShortArray> {}
    @FunctionalInterface public interface ToMaybeIntArrayFunction<T> extends Function<T, MaybeIntArray> {}
    @FunctionalInterface public interface ToMaybeLongArrayFunction<T> extends Function<T, MaybeLongArray> {}
    @FunctionalInterface public interface ToMaybeFloatArrayFunction<T> extends Function<T, MaybeFloatArray> {}
    @FunctionalInterface public interface ToMaybeDoubleArrayFunction<T> extends Function<T, MaybeDoubleArray> {}
    @FunctionalInterface public interface ToMaybeBooleanArrayFunction<T> extends Function<T, MaybeBooleanArray> {}

    @FunctionalInterface public interface ToStringFunction<T> extends Function<T, String> {}
    @FunctionalInterface public interface ToMaybeStringFunction<T> extends Function<T, MaybeString> {}

    private FunctionTypes() {
        // don't instantiate
    }
}
