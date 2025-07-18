[![Build Status](https://github.com/mastodon-sc/geff-java/actions/workflows/build.yml/badge.svg)](https://github.com/mastodon-sc/geff-java/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=mastodon-sc_geff-java&metric=coverage)](https://sonarcloud.io/summary/overall?id=mastodon-sc_geff-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=mastodon-sc_geff-java&metric=ncloc)](https://sonarcloud.io/summary/overall?id=mastodon-sc_geff-java)

# geff Java implementation

This repository contains the Java implementation of the [geff](https://github.com/live-image-tracking-tools/geff) library.

The **Graph Exchange Format for Features (Geff)** is a standardized format for storing and exchanging biological tracking data, particularly for cell tracking and lineage analysis. This Java implementation provides comprehensive support for reading and writing Geff data using the Zarr storage format.

## Features

- **Full Geff specification compliance** - Supports Geff versions 0.0, 0.1, and 0.2 (including patch versions like 0.1.1)
- **Zarr-based storage** - Efficient chunked array storage for large-scale tracking data
- **Complete data model** - Support for nodes (spatial-temporal features), edges (connections), and metadata
- **Flexible metadata handling** - Optional ROI parameters, customizable axis names and units
- **Type safety** - Strong typing with comprehensive validation
- **Memory efficient** - Chunked reading and writing for handling large datasets

## Core Classes

### GeffNode
Represents nodes in tracking graphs with spatial and temporal attributes:
- Time point information
- Spatial coordinates (x, y, z)
- Segment identifiers
- Multidimensional position arrays
- Chunked Zarr I/O support

### GeffEdge  
Represents connections between nodes in tracking graphs:
- Source and target node references
- Time point associations
- Chunked storage for efficient large-scale edge data

### GeffMetadata
Handles Geff metadata with schema validation:
- Version compatibility checking
- Spatial metadata (ROI bounds, axis names/units)
- Graph properties (directed/undirected)
- Alphabetically ordered attribute output
- Support for ArrayList data type conversion

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
import org.mastodon.*;
import java.util.List;
import java.util.ArrayList;

// Read Geff data from Zarr
List<GeffNode> nodes = GeffNode.readFromZarr("/path/to/data.zarr/tracks/nodes");
List<GeffEdge> edges = GeffEdge.readFromZarr("/path/to/data.zarr/tracks/edges");
GeffMetadata metadata = GeffMetadata.readFromZarr("/path/to/data.zarr");

// Create new Geff data
List<GeffNode> newNodes = new ArrayList<>();
GeffNode node0 = new GeffNode();
node0.setSegmentId(0);
node0.setTimepoint(0);
node0.setPosition(new double[]{10.5, 20.3, 5.0});
newNodes.add(node0);
GeffNode node1 = new GeffNode();
node1.setSegmentId(1);
node1.setTimepoint(1);
node1.setPosition(new double[]{11.5, 21.3, 6.0});
newNodes.add(node1);

// Write to Zarr format
GeffNode.writeToZarr(newNodes, "/path/to/output.zarr/tracks/nodes");

// Create new edges
List<GeffEdge> newEdges = new ArrayList<>();
GeffEdge edge = new GeffEdge();
edge.setSourceNodeId(0);
edge.setTargetNodeId(1);
// or
// GeffEdge edge = new GeffEdge(0, 1);

// Write to Zarr format
GeffEdge.writeToZarr(newEdges, "/path/to/output.zarr/tracks/edges");

// Create and write metadata
GeffMetadata metadata = new GeffMetadata("0.1.1", true);
metadata.setAxisNames(new String[]{"x", "y", "z"});
metadata.setAxisUnits(new String[]{"μm", "μm", "μm"});
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
    │   ├── attrs/
    │   │   ├── t/              # Time points
    │   │   ├── x/              # X coordinates  
    │   │   ├── y/              # Y coordinates
    │   │   ├── seg_id/         # Segment IDs
    │   │   └── position/       # Multidimensional positions
    │   └── ids/
    │       └── 0               # Node ID chunks
    └── edges/
        ├── .zgroup
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
