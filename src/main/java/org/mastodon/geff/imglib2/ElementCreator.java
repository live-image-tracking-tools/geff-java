package org.mastodon.geff.imglib2;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodHandles.collectArguments;
import static org.mastodon.geff.imglib2.ConstructorBinding.propertyHandle;
import static org.mastodon.geff.imglib2.ElementType.NODE;

@FunctionalInterface
public interface ElementCreator {
    void createElement(final long index) throws GeffException;

    /**
     * Make an {@code ElementCreator} that binds the {@code builder} method
     * annotated with {@link Construction.NodeConstructor} or {@link
     * Construction.EdgeConstructor} (depending on the {@link
     * GeffProperties#elementType() element type} of {@code properties})
     * to the given {@code properties}.
     */
    static ElementCreator of(final Object builder, final GeffProperties properties)
    // TODO: revise exceptions... Reflection exceptions should be wrapped into appropriate GeffException
            throws IllegalAccessException, Construction.GeffBindError, NoSuchMethodException {

        final Class<? extends Annotation> annotationClass = properties.elementType() == NODE ? Construction.NodeConstructor.class : Construction.EdgeConstructor.class;
        final Method method = Construction.getAnnotatedMethod(builder.getClass(), annotationClass);
        final Construction.ResolvedConstructorParameter[] params = Construction.resolveConstructorParameters(method);

        final MethodHandles.Lookup lk = MethodHandles.lookup();
        MethodHandle mh = lk.unreflect(method).bindTo(builder);
        for (int i = 0; i < params.length; i++) {
            final Construction.ResolvedConstructorParameter param = params[i];
            final MethodHandle supplier = propertyHandle(lk, properties, param);
            mh = collectArguments(mh, 0, supplier);
        }
        final MethodHandle fmh = mh;
        return i -> {
            try {
                properties.elementIndex().set(i);
                fmh.invokeExact();
            } catch (Throwable e) {
                throw new GeffException(e);
            }
        };
    }
}
