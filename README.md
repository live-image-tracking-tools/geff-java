[![Build Status](https://github.com/mastodon-sc/geff-java/actions/workflows/build.yml/badge.svg)](https://github.com/mastodon-sc/geff-java/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=mastodon-sc_geff-java&metric=coverage)](https://sonarcloud.io/summary/overall?id=mastodon-sc_geff-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=mastodon-sc_geff-java&metric=ncloc)](https://sonarcloud.io/summary/overall?id=mastodon-sc_geff-java)

# geff Java implementation

This repository contains the Java implementation of the [geff](https://github.com/live-image-tracking-tools/geff) library.

The **Graph Exchange Format for Features (Geff)** is a standardized format for storing and exchanging biological tracking data, particularly for cell tracking and lineage analysis. This Java implementation provides comprehensive support for reading and writing Geff data using the Zarr storage format.

## Features

- **Full Geff specification compliance** - Supports Geff versions 0.0, 0.1, 0.2, and 0.3 (including patch versions, development versions, and metadata like 0.2.2.dev20+g611e7a2.d20250719)
- **Zarr-based storage** - Efficient chunked array storage for large-scale tracking data
- **Complete data model** - Support for nodes (spatial-temporal features), edges (connections), and metadata
- **Flexible metadata handling** - Axis-based metadata with GeffAxis objects for spatial and temporal dimensions
- **Type safety** - Strong typing with comprehensive validation
- **Memory efficient** - Chunked reading and writing for handling large datasets
- **Builder patterns** - Convenient object construction with builder classes for GeffNode and GeffEdge

## Core Classes

### GeffNode
Represents nodes in tracking graphs with spatial and temporal attributes:
- Time point information (`t` property)
- Spatial coordinates (x, y, z)
- Segment identifiers
- Additional properties: color, radius, covariance2d, covariance3d
- Builder pattern for convenient object construction
- Chunked Zarr I/O support for versions 0.1, 0.2, and 0.3

### GeffEdge  
Represents connections between nodes in tracking graphs:
- Source and target node references
- Edge properties: score, distance
- Builder pattern for convenient object construction
- Chunked storage for efficient large-scale edge data
- Support for different Geff version formats

### GeffAxis
Represents axis metadata for spatial and temporal dimensions:
- Predefined constants for common axis names (t, x, y, z)
- Type classifications (time, space)
- Unit specifications with common constants
- Optional min/max bounds for ROI definition

### GeffMetadata
Handles Geff metadata with schema validation:
- Version compatibility checking with pattern matching for development versions
- GeffAxis array for spatial/temporal metadata
- Graph properties (directed/undirected)
- Comprehensive validation with detailed error messages
- Support for multiple Geff version formats (0.1, 0.2, 0.3)

### Geff
Main utility class demonstrating library usage and providing examples.

## Requirements

- Java 8 or higher
- Maven 3.6 or higher

## Dependencies

- **jzarr 0.3.5** - Zarr format support for Java
- **ucar.ma2** - Multi-dimensional array operations

## Usage Example

```java
import org.mastodon.geff.GeffEdge;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffNode;
import org.mastodon.geff.GeffAxis;
import java.util.List;
import java.util.ArrayList;

// Read Geff data from Zarr
List<GeffNode> nodes = GeffNode.readFromZarr("/path/to/data.zarr/tracks/nodes");
List<GeffEdge> edges = GeffEdge.readFromZarr("/path/to/data.zarr/tracks/edges");
GeffMetadata metadata = GeffMetadata.readFromZarr("/path/to/data.zarr");

// Create new Geff data using builder pattern
List<GeffNode> newNodes = new ArrayList<>();
GeffNode node0 = new GeffNode.Builder()
    .id(0)
    .timepoint(0)
    .x(10.5)
    .y(20.3)
    .z(5.0)
    .segmentId(0)
    .color(new double[]{1.0, 0.0, 0.0, 1.0}) // Red color
    .radius(2.5)
    .build();
newNodes.add(node0);

GeffNode node1 = new GeffNode.Builder()
    .id(1)
    .timepoint(1)
    .x(11.5)
    .y(21.3)
    .z(6.0)
    .segmentId(1)
    .build();
newNodes.add(node1);

// Write to Zarr format with version specification
GeffNode.writeToZarr(newNodes, "/path/to/output.zarr/tracks/nodes", "0.3.0");

// Create new edges using builder pattern
List<GeffEdge> newEdges = new ArrayList<>();
GeffEdge edge = new GeffEdge.Builder()
    .setId(0)
    .setSourceNodeId(0)
    .setTargetNodeId(1)
    .setScore(0.95)
    .setDistance(1.4)
    .build();
newEdges.add(edge);

// Write to Zarr format
GeffEdge.writeToZarr(newEdges, "/path/to/output.zarr/tracks/edges", "0.3.0");

// Create metadata with axis information
GeffAxis[] axes = {
    new GeffAxis(GeffAxis.NAME_TIME, GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECONDS, 0.0, 100.0),
    new GeffAxis(GeffAxis.NAME_SPACE_X, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETERS, 0.0, 1024.0),
    new GeffAxis(GeffAxis.NAME_SPACE_Y, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETERS, 0.0, 1024.0),
    new GeffAxis(GeffAxis.NAME_SPACE_Z, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETERS, 0.0, 100.0)
};
GeffMetadata metadata = new GeffMetadata("0.3.0", true, axes);
GeffMetadata.writeToZarr(metadata, "/path/to/output.zarr");
```

## Building

```bash
mvn clean compile
mvn test
mvn package
```

## Data Format

The library follows the Geff specification for biological tracking data:

```
dataset.zarr/
├── .zgroup                 # Zarr group metadata
├── .zattrs                 # Geff metadata (version, spatial info, etc.)
└── tracks/
    ├── .zgroup
    ├── nodes/
    │   ├── .zgroup
    │   ├── attrs/              # For Geff 0.1 format
    │   │   ├── t/              # Time points
    │   │   ├── x/              # X coordinates  
    │   │   ├── y/              # Y coordinates
    │   │   ├── seg_id/         # Segment IDs
    │   │   └── position/       # Multidimensional positions
    │   ├── props/              # For Geff 0.2/0.3 format
    │   │   ├── t/              # Time points
    │   │   ├── x/              # X coordinates
    │   │   ├── y/              # Y coordinates
    │   │   ├── z/              # Z coordinates (optional)
    │   │   ├── color/          # Node colors (optional)
    │   │   ├── radius/         # Node radii (optional)
    │   │   ├── track_id/       # Track identifiers (optional)
    │   │   ├── covariance2d/   # 2D covariance matrices (optional)
    │   │   └── covariance3d/   # 3D covariance matrices (optional)
    │   └── ids/
    │       └── 0               # Node ID chunks
    └── edges/
        ├── .zgroup
        ├── props/              # For Geff 0.2/0.3 format
        │   ├── distance/       # Edge distances (optional)
        │   └── score/          # Edge scores (optional)
        └── ids/
            ├── 0.0             # Edge chunks (source nodes)
            └── 1.0             # Edge chunks (target nodes)
```

## Technical Information

### Maintainer

* [Ko Sugawara](https://github.com/ksugar/)

### Contributors

* [Ko Sugawara](https://github.com/ksugar/) - Project maintainer

### License

* [BSD 2-Clause License](https://opensource.org/license/bsd-2-clause/)

### Contribute Code or Provide Feedback

* You are welcome to submit Pull Requests to this repository. This repository runs code analyses on
  every Pull Request using [SonarCloud](https://sonarcloud.io/dashboard?id=mastodon-sc_geff-java).
* Please read the [general advice](https://github.com/mastodon-sc/) re contributing to Mastodon and its plugins.

### Contribute Documentation

* If you would like to contribute to this documentation, feel free to open a pull request. The documentation is written in Markdown format.

## Acknowledgements

* [Geff Python implementation](https://github.com/live-image-tracking-tools/geff) - Original specification and reference implementation
* [jzarr library](https://github.com/bcdev/jzarr) - Zarr format support for Java
