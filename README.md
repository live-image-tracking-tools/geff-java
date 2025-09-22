[![Build Status](https://github.com/live-image-tracking-tools/geff-java/actions/workflows/build.yml/badge.svg)](https://github.com/live-image-tracking-tools/geff-java/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=live-image-tracking-tools_geff-java&metric=coverage)](https://sonarcloud.io/summary/overall?id=live-image-tracking-tools_geff-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=live-image-tracking-tools_geff-java&metric=ncloc)](https://sonarcloud.io/summary/overall?id=live-image-tracking-tools_geff-java)

# GEFF Java

This repository contains the Java implementation of the [geff](https://github.com/live-image-tracking-tools/geff) specification.

The **Graph Exchange File Format (GEFF)** is a standardized format for storing and exchanging graphs, particularly for cell tracking and lineage analysis. This Java implementation provides comprehensive support for reading and writing GEFF data using the Zarr storage format.

## Features

- **Full GEFF specification compliance** - Supports Geff versions 0.0, 0.1, 0.2, and 0.3 (including patch versions, development versions, and metadata like 0.2.2.dev20+g611e7a2.d20250719)
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
- Polygon geometry: separate polygonX and polygonY coordinate arrays with polygon offset for serialization
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

### GeffSerializableVertex
Lightweight geometry class internally used for storing polygon vertex coordinates:
- Simple (x, y) coordinate storage
- Part of the geometry package for efficient polygon handling

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
List<GeffNode> nodes = GeffNode.readFromZarr("/path/to/data.zarr/tracks");
List<GeffEdge> edges = GeffEdge.readFromZarr("/path/to/data.zarr/tracks");
GeffMetadata metadata = GeffMetadata.readFromZarr("/path/to/data.zarr/tracks");

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
    .covariance2d(new double[]{1.0, 0.2, 0.2, 1.5}) // 2x2 covariance matrix flattened
    .polygonX(new double[]{1.0, 2.0, 3.0, 4.0}) // Polygon X coordinates
    .polygonY(new double[]{5.0, 6.0, 7.0, 8.0}) // Polygon Y coordinates
    .build();
newNodes.add(node0);

GeffNode node1 = new GeffNode.Builder()
    .id(1)
    .timepoint(1)
    .x(11.5)
    .y(21.3)
    .z(6.0)
    .segmentId(1)
    .covariance2d(new double[]{0.8, 0.1, 0.1, 1.2}) // Different covariance
    .polygonX(new double[]{-1.0, -2.0, -3.0, -4.0}) // Different polygon X coordinates
    .polygonY(new double[]{-5.0, -6.0, -7.0, -8.0}) // Different polygon Y coordinates
    .build();
newNodes.add(node1);

// Write to Zarr format with version specification
GeffNode.writeToZarr(newNodes, "/path/to/output.zarr/tracks", "0.4.0");

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
GeffEdge.writeToZarr(newEdges, "/path/to/output.zarr/tracks", "0.4.0");

// Create metadata with axis information
GeffAxis[] axes = {
    new GeffAxis(GeffAxis.NAME_TIME, GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND, 0.0, 100.0),
    new GeffAxis(GeffAxis.NAME_SPACE_X, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 1024.0),
    new GeffAxis(GeffAxis.NAME_SPACE_Y, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 1024.0),
    new GeffAxis(GeffAxis.NAME_SPACE_Z, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0)
};
GeffMetadata metadata = new GeffMetadata("0.4.0", true, axes);
GeffMetadata.writeToZarr(metadata, "/path/to/output.zarr/tracks");
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
    │   ├── props/              # For Geff 0.2/0.3 format
    │   │   ├── t/              # Time points [N]
    │   │   ├── x/              # X coordinates [N]
    │   │   ├── y/              # Y coordinates [N]
    │   │   ├── z/              # Z coordinates [N] (optional)
    │   │   ├── color/          # Node colors [N] (optional)
    │   │   ├── radius/         # Node radii [N] (optional)
    │   │   ├── track_id/       # Track identifiers [N] (optional)
    │   │   ├── covariance2d/   # 2D covariance matrices for ellipse serialized in 1D [N, 4] (optional)
    │   │   ├── covariance3d/   # 3D covariance matrices for ellipsoid serialized in 1D [N, 6] (optional)
    │   │   └── polygon/        # Polygon coordinates (optional)
    │   │       ├── slices/     # Polygon slices with startIndex and endIndex [N, 2] (optional)
    │   │       └── values/     # XY coordinates of vertices in polygons [numVertices, 2] (optional)
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
  every Pull Request using [SonarCloud](https://sonarcloud.io/dashboard?id=live-image-tracking-tools_geff-java).
* Please read the [general advice](https://github.com/live-image-tracking-tools/) for contributing to Live Image Tracking Tools and its projects.

### Contribute Documentation

* If you would like to contribute to this documentation, feel free to open a pull request. The documentation is written in Markdown format.

## Acknowledgements

* [Geff Python implementation](https://github.com/live-image-tracking-tools/geff) - Original specification and reference implementation
* [jzarr library](https://github.com/bcdev/jzarr) - Zarr format support for Java
