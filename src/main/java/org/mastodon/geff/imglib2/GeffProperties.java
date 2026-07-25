package org.mastodon.geff.imglib2;

import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Cast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeffProperties {

    private final ElementType elementType;
    private final ElementIndex elementIndex;
    private final long numElements;

    private final GeffProperty<? extends IntegerType<?>> id;
    private final Map<String, GeffProperty<?>> properties;

    GeffProperties(
            final ElementType elementType,
            final GeffProperty<? extends IntegerType<?>> id,
            final List<GeffProperty<?>> props) {
        this.elementType = elementType;
        this.id = id;
        elementIndex = id.elementIndex();
        numElements = id.numElements();
        properties = new LinkedHashMap<>();
        for (final GeffProperty<?> prop : props) {
            final String identifier = prop.identifier();
            if (identifier.equals(id.identifier()) || properties.containsKey(identifier))
                throw new IllegalArgumentException("Duplicate property identifier: " + identifier);
            if (prop.numElements() != numElements)
                throw new IllegalArgumentException("Number of elements mismatch: " + identifier);
            properties.put(identifier, prop);
        }
    }

    public ElementType elementType() {
        return elementType;
    }

    public long numElements() {
        return numElements;
    }

    // elementIndex should be shared by all properties
    public ElementIndex elementIndex() {
        return elementIndex;
    }

    public <T extends IntegerType<T>> GeffProperty<T> id() {
        return Cast.unchecked(id);
    }

    public Map<String, GeffProperty<?>> properties() {
        return properties;
    }

    @Override
    public String toString() {
        final String nl = System.lineSeparator();
        final String cnl = "," + nl;
        final String props = properties.values().stream().map(p ->
                nl + "    " + p.identifier() + "=" + p
        ).collect(Collectors.joining());
        return "GeffProperties{" + nl +
                "  elementType=" + elementType + cnl +
                "  numElements=" + numElements + cnl +
                "  elementIndex=" + elementIndex + cnl +
                "  id=" + id + cnl +
                "  properties={" + props + "}" + nl +
                '}';
    }


    // TODO remove?
    <T> GeffProperty<T> property(final String identifier) {
        return Cast.unchecked(properties.get(identifier));
    }

    // TODO remove?
    void put(GeffProperty<?> property) {
        properties.put(property.identifier(), property);
    }
}
