# Cross-Language Round-Trip Tests

This directory contains tests for verifying interoperability between `geff-java` and the Python `geff` reference implementation.

## How It Works

1. Python creates mock GEFF files using `geff.testing.data`
2. Java reads and re-writes them via `RoundTripGeff`
3. Python compares original vs round-tripped using `check_equiv_geff()`

## Prerequisites

### Java
```bash
# Build the Java project (from repo root)
cd ..
mvn package -DskipTests
```

### Python
The script uses [uv](https://github.com/astral-sh/uv) for dependency management.
Dependencies are declared inline in the script (PEP 723).

## Running the Tests

```bash
cd cross-language-tests
uv run run_tests.py
```

## Test Cases

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `basic_3d` | Simple 3D graph (t,x,y,z) | Currently fails (missing metadata support) |
| `basic_2d` | Simple 2D graph (t,x,y) | Currently fails (missing metadata support) |
| `varlength` | Graph with variable-length properties | Expected to fail (Java should warn & skip) |
| `missing` | Graph with missing value arrays | Expected to fail (Java should warn & skip) |
| `string_props` | Graph with string properties | Expected to fail (Java should warn & skip) |

## Output

Test data is written to `data/` directory (git-ignored).

Each test creates:
- `<test>_original.zarr` - Created by Python
- `<test>_roundtrip.zarr` - Read by Java, written by Java
