package org.litt.geff.props;

public enum ElementType {
    NODE("nodes", "geff/node_props_metadata"),
    EDGE("edges", "geff/edge_props_metadata");

    private final String elementGroup;
    private final String propMetadataAttribute;

    ElementType(final String elementGroup, String propMetadataAttribute) {
        this.elementGroup = elementGroup;
        this.propMetadataAttribute = propMetadataAttribute;
    }

    public String elementGroup() {
        return elementGroup;
    }

    public String propMetadataAttribute() {
        return propMetadataAttribute;
    }
}
