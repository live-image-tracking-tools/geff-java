package org.mastodon.geff.imglib2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GeffPropertySpecs {

    private final ElementType elementType;
    private final GeffPropertySpec id;
    private final Map<String, GeffPropertySpec> properties;

    public GeffPropertySpecs(
            final ElementType elementType,
            final GeffPropertySpec idSpec,
            final List<GeffPropertySpec> specs
    ) {
        this.elementType = elementType;
        this.id = idSpec;
        properties = new LinkedHashMap<>();
        for (final GeffPropertySpec spec : specs) {
            final String identifier = spec.identifier();
            if (identifier.equals(idSpec.identifier()) || properties.containsKey(identifier))
                throw new IllegalArgumentException("Duplicate property identifier: " + identifier);
            properties.put(identifier, spec);
        }
    }

    public ElementType elementType() {
        return elementType;
    }

    public GeffPropertySpec id() {
        return id;
    }

    public Map<String, GeffPropertySpec> properties() {
        return properties;
    }

    @Override
    public String toString() {
        final String nl = System.lineSeparator();
        final String cnl = "," + nl;
        final String props = properties.values().stream().map(p ->
                nl + "    " + p.identifier() + "=" + p
        ).collect(Collectors.joining());
        return "GeffPropertySpecs{" + nl +
                "  elementType=" + elementType + cnl +
                "  id=" + id + cnl +
                "  properties={" + props + "}" + nl +
                '}';
    }
}
