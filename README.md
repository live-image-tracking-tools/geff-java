[![Build Status](https://github.com/live-image-tracking-tools/geff-java/actions/workflows/build.yml/badge.svg)](https://github.com/live-image-tracking-tools/geff-java/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=live-image-tracking-tools_geff-java&metric=coverage)](https://sonarcloud.io/summary/overall?id=live-image-tracking-tools_geff-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=live-image-tracking-tools_geff-java&metric=ncloc)](https://sonarcloud.io/summary/overall?id=live-image-tracking-tools_geff-java)

# GEFF Java

This repository contains the Java implementation of the [geff](https://github.com/live-image-tracking-tools/geff) specification.

The **Graph Exchange File Format (GEFF)** is a standardized format for storing and exchanging graphs, particularly for cell tracking and lineage analysis. This Java implementation provides comprehensive support for reading and writing GEFF data using the Zarr storage format.

## Features

- **GEFF v1 spec compliance** - Reads and writes GEFF v0.2 through v1.x and beyond; version validation uses a semver pattern rather than an allowlist
- **Zarr Format 2** - Reads and writes [Zarr Format 2](https://zarr-specs.readthedocs.io/en/latest/v2/v2.0.html) only; Zarr Format 3 is not supported
- **Complete data model** - Support for nodes (spatial-temporal features), edges (connections), and metadata
- **Flexible metadata handling** - Axis-based metadata with GeffAxis objects; supports `time`, `space`, and `channel` axis types with any axis name
- **Custom axis names** - Node property paths for coordinates (t/x/y/z) are derived from `display_hints` combined with the axis names declared in `axes`, so non-standard names such as `frame`, `cell_x`, `cell_y` work out of the box; standard names are used as fallbacks when neither is declared
- **Display hints** - Full `display_hints` support (`display_horizontal`, `display_vertical`, `display_depth`, `display_time`), validated against the declared axis names
- **Property metadata** - Full `node_props_metadata` / `edge_props_metadata` support as required by the v1 spec
- **Variable-length properties** - Read and write properties with `varlength: true` (e.g. polygon coordinates per node)
- **Type safety** - Strong typing with comprehensive validation; graceful skip with warning for unsupported types (`str`, `bytes`)
- **Adaptive chunking** - Chunk sizes targeting ~8 MiB per chunk (power-of-two on the first dimension)
- **Lazy, imglib2-backed properties** - Each property is exposed as a `GeffProperty<T>` backed by an imglib2 `RandomAccessibleInterval`; chunks are read on demand and cached, so files larger than memory work out of the box
- **Generic property model** - `GeffProperties` gives uniform, type-safe access to arbitrary node and edge properties, without imposing an application-specific data model
- **Annotation-based binding** - Bind your own node and edge classes to a GEFF file with `@NodeConstructor` / `@EdgeConstructor` and `@FromId` / `@FromProperty` annotations; type conversion happens automatically

## Core Classes

### GeffProperty
The central abstraction: one node or edge property, exposed lazily as an imglib2-backed array:
- `T` is the imglib2 type matching the Zarr dtype (e.g. `DoubleType` for `<f8`)
- Scalar, vector, matrix and higher-dimensional properties (see `numDimensions()`)
- Optional properties backed by a `missing` dataset (`isOptional()`)
- Variable-length properties backed by a `data` dataset (`isVarlength()`)
- Decorators to convert types, rename identifiers, or expose var-length `uint8` data as `String`

### GeffProperties
A collection of `GeffProperty` of one element type (nodes or edges), plus the mandatory `id` property:
- Generic read/modify/write access to arbitrary properties, without imposing an application-specific data model
- Used both by `IoUtils` for generic file I/O and by the binding framework as the source for constructor parameters

### IoUtils
Reading and writing property data:
- `loadProperties(n5, elementType)` – read all properties of a GEFF group into a `GeffProperties`
- `writeProperties(n5, properties, compression, chunkSize, geffGroup)` – write a `GeffProperties` back
- `loadProperty` / `writeProperty` for single properties, with explicit paths
- Chunk sizes targeting ~8 MiB per chunk (power-of-two on the first dimension)

### GeffPropertyWriter
Fluent writer that harvests property values from arbitrary application objects (`<O>`):
- Accessor methods take an object and return a property value (possibly a `Maybe` for optional properties, or a `String` encoded as var-length `uint8`)
- Used e.g. by TrackMate's GEFF writer

### Construction / ElementCreator (bind package)
Annotation-based binding of application objects to `GeffProperties`:
- `@NodeConstructor` / `@EdgeConstructor` on a static builder method, with `@FromId` and `@FromProperty("...")` parameters
- `Construction.buildElements(...)` populates your objects from a `GeffProperties`; `ElementCreator` gives step-by-step control
- Parameters may be primitives, primitive arrays, `Maybe*` / `Optional*` wrappers, `String` (from var-length `uint8`), or a `GeffProperty<?>` escape hatch

### GeffAxis
Represents axis metadata for spatial and temporal dimensions:
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
- `display_hints` as a `GeffDisplayHints` object, validated against the declared axis names
- `getAxisNameByType(type)` – returns the name of the first axis matching a given type (e.g. `"time"`)
- `getAxisNamesByType(type)` – returns all axis names matching a given type (e.g. all `"space"` axes in order)
- `getAxisNames()` – returns all axis names, in declaration order
- `getTimeAxisName()` / `getHorizontalAxisName()` / `getVerticalAxisName()` / `getDepthAxisName()` – resolve which axis carries the t/x/y/z coordinate (see below)

### GeffDisplayHints
Represents the optional `display_hints` metadata, i.e. which axis a viewer shows horizontally, vertically, as depth and as time. Since it is optional, `GeffMetadata` resolves each spatial coordinate from the following sources, in order:

1. the matching display hint, which names the axis explicitly;
2. a spatial axis named `x`, `y` or `z` (case-insensitive), or one whose name ends in `_x`, `_y`, `_z` (e.g. `cell_x`);
3. the position in the `axes` list, which the spec defines as image dimension order, i.e. slowest to fastest (`z`, `y`, `x`) and therefore counted from the end;
4. the standard name (`x`, `y`, `z`) for datasets that declare no spatial axes at all.

Depth resolves to `null` for datasets that declare spatial axes but no third one, in which case no z property is read or written.

### PropMetadata
Describes a single node or edge property as required by the v1 spec:
- Fields: `identifier`, `dtype`, `varlength`, `unit`, `name`, `description`
- Used to infer data types on write and skip unsupported types on read

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Dependencies

- **n5** - N5/Zarr core data model
- **n5-zarr** - Zarr format reader/writer
- **n5-blosc** - Optional Blosc compression (falls back to raw compression if the native library is absent)
- **imglib2** - Multi-dimensional array and interval utilities
- **slf4j-api** - Logging facade

## Usage Example

```java
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.litt.geff.GeffMetadata;
import org.litt.geff.io.IoUtils;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;

// Open a GEFF file. The generic API works on any GEFF file, without an
// application-specific data model.
final N5Reader n5 = new N5ZarrReader("input.zarr");

// Metadata: version, axes, display hints, property metadata, ...
final GeffMetadata metadata = GeffMetadata.readFromN5(n5, "/");

// All node properties, read lazily as imglib2-backed GeffProperty.
final GeffProperties nodes = IoUtils.loadProperties(n5, ElementType.NODE);
final GeffProperties edges = IoUtils.loadProperties(n5, ElementType.EDGE);

// Inspect the "x" property: it is a scalar DoubleType property, with one
// value per node.
final GeffProperty<?> x = nodes.property("x");
System.out.println(x); // GeffProperty<DoubleType>{identifier="x", scalar}
for (int i = 0; i < 3; i++) {
    x.elementIndex().set(i);
    System.out.println(i + ": " + x.values().randomAccess().get());
}
// prints:
//   GeffProperty<DoubleType>{identifier="x", scalar}
//   0: 1.0
//   1: 0.9
//   2: 0.8

// Write the properties to a new file...
try (final N5Writer out = new N5ZarrWriter("output.zarr")) {
    IoUtils.writeProperties(out, nodes, null, 0, null);
    IoUtils.writeProperties(out, edges, null, 0, null);
}
// ... and the metadata last.
GeffMetadata.writeToZarr(metadata, "output.zarr");
```

To load GEFF data into your own classes, use the annotation-based binding
framework (see the tutorials in `src/test/java/org/litt/geff/tutorial/` for
full working examples):

```java
import static org.litt.geff.bind.Construction.buildElements;

import org.litt.geff.bind.Construction.FromId;
import org.litt.geff.bind.Construction.FromProperty;
import org.litt.geff.bind.Construction.NodeConstructor;
import org.litt.geff.props.Maybe;

class Builder {
    @NodeConstructor
    public void addNode(
            @FromId final long id,
            @FromProperty("t") final int t,
            @FromProperty("x") final double x,
            @FromProperty("y") final double y,
            @FromProperty("radius") final Maybe.MaybeDouble radius,
            @FromProperty("label") final String label) {
        // ... create and register your node object here.
        // "radius" is optional: Maybe.MaybeDouble tells whether it is present.
        // "label" is a var-length uint8 property, decoded from UTF-8.
    }
}

// Invoke the constructor method for every node in the file.
buildElements(new Builder(), nodes);
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
│                                   #   track_node_props, display_hints
├── nodes/
│   ├── ids/                        # Node IDs [N]
│   └── props/
│       ├── <prop_id>/values        # Property values [N, ...]
│       │   │                       # (first dimension is the element index;
│       │   │                       #  <prop_id> from node_props_metadata;
│       │   │                       #  for var-length properties, offsets and
│       │   │                       #  shapes [N, ndim+1] instead)
│       │   ├── missing             # Missing-value mask [N] (optional)
│       │   └── data                # Flattened values [V] (var-length only)
│       └── ...
└── edges/
    ├── ids/                        # Source/target node ID pairs [N, 2]
    └── props/
        ├── <prop_id>/values        # Edge property values [N, ...]
        └── ...
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
