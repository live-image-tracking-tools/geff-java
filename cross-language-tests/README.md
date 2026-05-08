# Cross-Language Round-Trip Tests

This directory contains tests for verifying interoperability between `geff-java` and the Python `geff` reference implementation.

## How It Works

1. Python creates mock GEFF files using `geff.testing.data` (available from geff package)
2. Java reads and re-writes them via `RoundTripGeff`
3. Python compares original vs round-tripped to verify equivalence

## Prerequisites

### Java
```bash
# Build the Java project (from repo root)
cd ..
mvn package -DskipTests
```

### Python
The script uses [uv](https://github.com/astral-sh/uv) for dependency management.

#### Setup Option 1: Using `uv sync` (recommended)
```bash
cd cross-language-tests
uv sync
```

#### Setup Option 2: Run directly (no environment setup needed)
```bash
cd cross-language-tests
uv run run_tests.py
```

Latest versions:
- `geff` (Python package) - see [live-image-tracking-tools/geff](https://github.com/live-image-tracking-tools/geff)
- `geff-spec` - GEFF metadata specification

## Running the Tests

After setting up with `uv sync`:
```bash
cd cross-language-tests
uv run run_tests.py
```

Or activate the virtual environment and run directly:
```bash
cd cross-language-tests
. .venv/bin/activate  # or .venv\Scripts\activate on Windows
python run_tests.py
```

## Test Data Generation

The `geff.testing.data` module provides several fixture generators:

- `create_simple_2d_geff()` - 2D graphs with (t, x, y) + edge properties
- `create_simple_3d_geff()` - 3D graphs with (t, x, y, z) + edge properties
- `create_simple_temporal_geff()` - Temporal-only graphs (t dimension only)
- `create_empty_geff()` - Empty graphs (no nodes/edges, useful for edge cases)
- `create_mock_geff()` - Advanced: full control over node/edge properties, dtypes, dimensions

See the [geff.testing.data source](https://github.com/live-image-tracking-tools/geff/blob/main/packages/geff/src/geff/testing/data.py) for advanced usage with custom properties, variable-length arrays, and missing values.

## Test Cases

| Test               | Description                              | Status                    |
| ------------------ | ---------------------------------------- | ------------------------- |
| `basic_3d`         | Simple 3D graph (t, x, y, z) with edges  | Check compatibility       |
| `basic_2d`         | Simple 2D graph (t, x, y) with edges     | Check compatibility       |
| `temporal_only`    | Temporal graph (t only, no spatial dims) | Check temporal handling   |
| `empty`            | Empty graph (no nodes/edges)             | Check edge case handling  |
| `varlength_arrays` | Variable-length array properties         | Check if supported/warned |
| `missing_values`   | Properties with missing value arrays     | Check if supported/warned |

## Output

Test data is written to `data/` directory (git-ignored).

Each test creates:
- `<test>_original.zarr` - Created by Python
- `<test>_roundtrip.zarr` - Read by Java, written by Java
