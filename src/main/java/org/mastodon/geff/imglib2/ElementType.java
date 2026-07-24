package org.mastodon.geff.imglib2;

public enum ElementType {
    NODE("nodes", "geff/node_props_metadata"),
    EDGE("edges", "geff/edge_props_metadata");

    private final String elementGroup;
    private final String propMetadataAttribute;

    ElementType(final String elementGroup, String propMetadataAttribute) {
        this.elementGroup = elementGroup;
        this.propMetadataAttribute = propMetadataAttribute;
    }

    String elementGroup() {
        return elementGroup;
    }

    String propMetadataAttribute() {
        return propMetadataAttribute;
    }
}
