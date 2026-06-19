[![Build Status](https://github.com/live-image-tracking-tools/geff-java/actions/workflows/build.yml/badge.svg)](https://github.com/live-image-tracking-tools/geff-java/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=live-image-tracking-tools_geff-java&metric=coverage)](https://sonarcloud.io/summary/overall?id=live-image-tracking-tools_geff-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=live-image-tracking-tools_geff-java&metric=ncloc)](https://sonarcloud.io/summary/overall?id=live-image-tracking-tools_geff-java)

# GEFF Java

This repository contains the Java implementation of the [geff](https://github.com/live-image-tracking-tools/geff) specification.

The **Graph Exchange File Format (GEFF)** is a standardized format for storing and exchanging graphs, particularly for cell tracking and lineage analysis. This Java implementation provides comprehensive support for reading and writing GEFF data using the Zarr storage format.

## Features

- **GEFF v1 spec compliance** - Reads and writes GEFF v0.2 through v1.x and beyond; version validation uses a semver pattern rather than an allowlist
- **Zarr-based storage** - Efficient chunked array storage for large-scale tracking data
- **Complete data model** - Support for nodes (spatial-temporal features), edges (connections), and metadata
- **Flexible metadata handling** - Axis-based metadata with GeffAxis objects; supports `time`, `space`, and `channel` axis types with any axis name
- **Property metadata** - Full `node_props_metadata` / `edge_props_metadata` support as required by the v1 spec
- **Variable-length properties** - Read and write properties with `varlength: true` (e.g. polygon coordinates per node)
- **Type safety** - Strong typing with comprehensive validation; graceful skip with warning for unsupported types (`str`, `bytes`)
- **Adaptive chunking** - Chunk sizes targeting ~8 MiB per chunk (power-of-two on the first dimension)
- **Builder patterns** - Convenient object construction with builder classes for GeffNode and GeffEdge

## Core Classes

### GeffNode
Represents nodes in tracking graphs with spatial and temporal attributes:
- Time point information (`t` property)
- Spatial coordinates (x, y, z)
- Segment identifiers (dynamic property name via metadata)
- Additional properties: color, radius, covariance2d, covariance3d
- Polygon geometry stored via `polygonX`/`polygonY` builder fields, serialized to `serialized_props/polygon/`
- Variable-length properties accessible via `getVarlengthProperty(name)` / `setVarlengthProperty(name, ...)`
- Builder pattern for convenient object construction
- Chunked Zarr I/O supporting v0.2 through v1.x and beyond

### GeffEdge
Represents connections between nodes in tracking graphs:
- Source and target node references
- Edge properties: score, distance
- Builder pattern for convenient object construction
- Chunked storage for efficient large-scale edge data

### GeffAxis
Represents axis metadata for spatial and temporal dimensions:
- Predefined constants for common axis names (t, x, y, z)
- Type classifications: `time`, `space`, `channel`
- Unit specifications with common constants
- Optional min/max bounds for ROI definition

### GeffMetadata
Handles GEFF metadata with schema validation:
- Version validation via semver pattern (accepts any well-formed version)
- GeffAxis array supporting any axis name and the three axis types
- Node/edge property metadata maps (`nodePropsMetadata`, `edgePropsMetadata`)
- Dynamic tracklet property name from `track_node_props["tracklet"]`
- Graph properties (directed/undirected)

### PropMetadata
Describes a single node or edge property as required by the v1 spec:
- Fields: `identifier`, `dtype`, `varlength`, `unit`, `name`, `description`
- Used to infer data types on write and skip unsupported types on read

### VarlengthProperty
Stores a variable-length property (one array per node with potentially different shapes):
- `getNodeData(int nodeIndex)` – extract the data array for a specific node
- `isMissing(int nodeIndex)` – check the optional missing-value indicator
- Backed by a flattened data array and an offset/shape index

### GeffSerializableVertex
Lightweight geometry class internally used for storing polygon vertex coordinates:
- Simple (x, y) coordinate storage
- Part of the geometry package for efficient polygon handling

### Geff
Main utility class demonstrating library usage and providing examples.

## Requirements

- Java 8 or higher
- Maven 3.6 or higher

## Dependencies

- **n5** - N5/Zarr core data model
- **n5-zarr** - Zarr format reader/writer
- **n5-blosc** - Optional Blosc compression (falls back to raw compression if the native library is absent)
- **imglib2** - Multi-dimensional array and interval utilities
- **slf4j-api** - Logging facade

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
GeffNode.writeToZarr(newNodes, "/path/to/output.zarr/tracks", "1.0.0");

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
GeffEdge.writeToZarr(newEdges, "/path/to/output.zarr/tracks", "1.0.0");

// Access variable-length properties after reading (e.g. per-node polygon)
List<GeffNode> readNodes = GeffNode.readFromZarr("/path/to/data.zarr/tracks");
VarlengthProperty polygon = readNodes.get(0).getVarlengthProperty("polygon");
if (polygon != null) {
    Object nodeData = polygon.getNodeData(0); // double[] or int[] for node 0
}

// Create metadata with axis information
GeffAxis[] axes = {
    new GeffAxis(GeffAxis.NAME_TIME, GeffAxis.TYPE_TIME, GeffAxis.UNIT_SECOND, 0.0, 100.0),
    new GeffAxis(GeffAxis.NAME_SPACE_X, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 1024.0),
    new GeffAxis(GeffAxis.NAME_SPACE_Y, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 1024.0),
    new GeffAxis(GeffAxis.NAME_SPACE_Z, GeffAxis.TYPE_SPACE, GeffAxis.UNIT_MICROMETER, 0.0, 100.0)
};
GeffMetadata metadata = new GeffMetadata("1.0.0", true, axes);
GeffMetadata.writeToZarr(metadata, "/path/to/output.zarr/tracks");
```

## Building

geff-java prefers `BloscCompression` for writing datasets when [c-blosc](https://github.com/Blosc/c-blosc) is available. If Blosc is not installed, it will print a warning and automatically fall back to `RawCompression`.

```bash
mvn clean compile
mvn test
mvn package
```

## Cross-Language Tests

Round-trip tests validate interoperability between geff-java and the Python reference implementation.

**Requirements:**
- [uv](https://docs.astral.sh/uv/) (Python package manager)

**Optional compression support:**
- If [c-blosc](https://github.com/Blosc/c-blosc) is installed, geff-java will use `BloscCompression` for output datasets.
- If Blosc is not available, geff-java prints a warning and automatically falls back to `RawCompression`, so end users can still run the library and the cross-language tests without extra native setup.
- On macOS, Blosc can be installed with `brew install c-blosc`.

**Run tests:**
```bash
mvn package -DskipTests
cd cross-language-tests
uv run run_tests.py
```

The tests create GEFF files with Python, read/write them with Java, and validate the results.

## Data Format

The library follows the Geff specification for biological tracking data:

```
dataset.zarr/
├── .zgroup
├── .zattrs                         # Geff metadata:
│                                   #   version, directed, axes,
│                                   #   node_props_metadata,
│                                   #   edge_props_metadata,
│                                   #   track_node_props
└── tracks/
    ├── .zgroup
    ├── nodes/
    │   ├── ids/                    # Node IDs [N]
    │   ├── props/
    │   │   ├── t/values            # Time points [N]
    │   │   ├── x/values            # X coordinates [N]
    │   │   ├── y/values            # Y coordinates [N]
    │   │   ├── z/values            # Z coordinates [N] (optional)
    │   │   ├── color/values        # RGBA colors [N, 4] (optional)
    │   │   ├── radius/values       # Node radii [N] (optional)
    │   │   ├── <tracklet>/values   # Track IDs [N] (name from track_node_props, optional)
    │   │   ├── covariance2d/values # Flattened 2D covariance [N, 4] (optional)
    │   │   ├── covariance3d/values # Flattened 3D covariance [N, 6] (optional)
    │   │   └── <varlength_prop>/   # Variable-length property (optional)
    │   │       ├── data            # Flattened values [V]
    │   │       ├── values          # Offsets and shapes [N, ndim+1]
    │   │       └── missing         # Missing-value mask [N] (optional)
    │   └── serialized_props/
    │       └── polygon/
    │           ├── slices          # Start/end index per node [N, 2]
    │           └── values          # Vertex XY coordinates [numVertices, 2]
    └── edges/
        ├── ids/                    # Source/target node ID pairs [N, 2]
        └── props/
            ├── distance/values     # Edge distances [N] (optional)
            └── score/values        # Edge scores [N] (optional)
```

## Technical Information

### Maintainer

* [Ko Sugawara](https://github.com/ksugar/)

### Contributors

* [Ko Sugawara](https://github.com/ksugar/)
* [Jean-Yves Tinevez](https://github.com/tinevez)
* [Tobias Pietzsch](https://github.com/tpietzsch)
* [Caroline Malin-Mayor](https://github.com/cmalinmayor)

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
* [N5-universe](https://github.com/saalfeldlab/n5) - N5/Zarr I/O libraries used by this implementation
