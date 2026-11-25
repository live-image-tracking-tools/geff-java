package org.litt.geff.bind;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.litt.geff.GeffException;
import org.litt.geff.props.GeffProperties;

import static java.lang.invoke.MethodHandles.collectArguments;
import static org.litt.geff.bind.ConstructorBinding.propertyHandle;
import static org.litt.geff.props.ElementType.NODE;

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
    // NB: reflection exceptions should be wrapped into GeffBindError once
    // clients no longer rely on the throws clause (see TrackMate's catch clause).
            throws IllegalAccessException, GeffBindError, NoSuchMethodException {

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
