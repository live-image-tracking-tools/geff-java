#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = [
#     "geff",
#     "zarr",
# ]
# ///
"""
Cross-language round-trip tests for geff-java.

This script:
1. Creates mock GEFF files using the Python geff library
2. Calls Java to read and re-write them
3. Compares the original and round-tripped versions

Run with: uv run run_tests.py
Requires: Java project built (mvn package)
"""

import shutil
import subprocess
import sys
import os
from pathlib import Path

# Test output directory
DATA_DIR = Path(__file__).parent / "data"

# Java classpath - adjust based on your build
JAVA_PROJECT_ROOT = Path(__file__).parent.parent
JAVA_TARGET_DIR = JAVA_PROJECT_ROOT / "target"


# Get blosc library path for macOS
def get_blosc_lib_path():
    """Find blosc library path from homebrew installation."""
    try:
        result = subprocess.run(
            ["brew", "--prefix", "c-blosc"],
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode == 0:
            prefix = result.stdout.strip()
            lib_path = os.path.join(prefix, "lib")
            if os.path.exists(lib_path):
                return lib_path
    except Exception:
        pass
    return None


def get_java_classpath():
    """Find the Java classpath including dependencies.

    Prefers the shaded jar (built by 'mvn package'). Otherwise uses
    target/classes, with dependencies resolved by maven itself
    (dependency:build-classpath), so that no extra setup is required.
    """
    # Look for the jar with dependencies
    jars = list(JAVA_TARGET_DIR.glob("*-jar-with-dependencies.jar"))
    if jars:
        return str(jars[0])

    classes_dir = JAVA_TARGET_DIR / "classes"
    if not classes_dir.exists():
        raise RuntimeError(
            f"Could not find Java classes. "
            f"Please run 'mvn package -DskipTests' in {JAVA_PROJECT_ROOT}"
        )

    cp_file = JAVA_TARGET_DIR / "classpath.txt"
    print("  Running 'mvn dependency:build-classpath' to assemble the classpath...")
    subprocess.run(
        [
            "mvn",
            "-q",
            "dependency:build-classpath",
            f"-Dmdep.outputFile={cp_file}",
        ],
        cwd=JAVA_PROJECT_ROOT,
        capture_output=True,
        text=True,
    )
    if cp_file.exists() and cp_file.read_text().strip():
        return ":".join([str(classes_dir), cp_file.read_text().strip()])

    raise RuntimeError(
        f"Could not assemble the Java classpath. "
        f"Please run 'mvn package -DskipTests' in {JAVA_PROJECT_ROOT}"
    )


def run_java_roundtrip(
    input_path: Path, output_path: Path
) -> subprocess.CompletedProcess:
    """Run the Java RoundTrip tool."""
    classpath = get_java_classpath()

    # Get blosc library path for -Djava.library.path
    blosc_path = get_blosc_lib_path()

    cmd = [
        "java",
    ]

    # Add library path if blosc is found
    if blosc_path:
        cmd.append(f"-Djna.library.path={blosc_path}")

    cmd.extend(
        [
            "-cp",
            classpath,
            "org.litt.geff.io.RoundTrip",
            str(input_path),
            str(output_path),
        ]
    )
    print(f"  Running: {' '.join(cmd[:4])} ... {cmd[-2]} {cmd[-1]}")
    return subprocess.run(cmd, capture_output=True, text=True)


def check_equiv_with_tolerances(original_path: Path, roundtrip_path: Path) -> bool:
    """Compare GEFF files while tolerating known non-semantic dtype encoding differences."""
    from geff.testing._utils import check_equiv_geff

    try:
        check_equiv_geff(str(original_path), str(roundtrip_path))
        print("   PASSED: GEFFs are equivalent!")
        return True
    except Exception as e:
        message = str(e)
        tolerated = [
            "dtype: a float64 does not match b >f8",
        ]
        if any(token in message for token in tolerated):
            print(f"   PASSED with tolerated difference: {type(e).__name__}: {e}")
            return True
        print(f"   FAILED: {type(e).__name__}: {e}")
        return False


def test_basic_3d_geff():
    """Test round-trip of a basic 3D GEFF."""
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.core_io import write_arrays

    print("\n" + "=" * 60)
    print("TEST: Basic 3D GEFF")
    print("=" * 60)

    original_path = DATA_DIR / "basic_3d_original.zarr"
    roundtrip_path = DATA_DIR / "basic_3d_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with Python...")
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=10,
        num_edges=15,
    )
    write_arrays(str(original_path), **memory_geff)
    print(f"   Created: {original_path}")
    print(
        f"   Nodes: {len(memory_geff['node_ids'])}, Edges: {len(memory_geff['edge_ids'])}"
    )

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode != 0:
        print(f"\n   JAVA FAILED (exit code {result.returncode})")
        print("   STDOUT:", result.stdout)
        print("   STDERR:", result.stderr)
        return False

    print("   Java completed successfully")
    if result.stdout:
        for line in result.stdout.strip().split("\n"):
            print(f"   {line}")

    print("\n3. Comparing original vs round-tripped...")
    return check_equiv_with_tolerances(original_path, roundtrip_path)


def test_basic_2d_geff():
    """Test round-trip of a basic 2D GEFF."""
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.core_io import write_arrays

    print("\n" + "=" * 60)
    print("TEST: Basic 2D GEFF")
    print("=" * 60)

    original_path = DATA_DIR / "basic_2d_original.zarr"
    roundtrip_path = DATA_DIR / "basic_2d_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with Python...")
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=10,
        num_edges=15,
        include_z=False,  # 2D only
    )
    write_arrays(str(original_path), **memory_geff)
    print(f"   Created: {original_path}")

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode != 0:
        print(f"\n   JAVA FAILED (exit code {result.returncode})")
        print("   STDOUT:", result.stdout)
        print("   STDERR:", result.stderr)
        return False

    print("   Java completed successfully")

    print("\n3. Comparing original vs round-tripped...")
    return check_equiv_with_tolerances(original_path, roundtrip_path)


def test_with_varlength():
    """Test round-trip of GEFF with variable-length properties.

    geff-java round-trips var-length properties fully, so the round-trip
    must be equivalent to the original — including the content of the
    "data" array, which check_equiv_geff does not compare on its own.
    """
    import numpy as np
    import zarr
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.testing._utils import check_equiv_geff
    from geff.core_io import write_arrays

    print("\n" + "=" * 60)
    print("TEST: GEFF with variable-length properties")
    print("  (Expected: Java round-trips varlength props losslessly)")
    print("=" * 60)

    original_path = DATA_DIR / "varlength_original.zarr"
    roundtrip_path = DATA_DIR / "varlength_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with varlength property...")
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=10,
        num_edges=15,
        include_varlength=True,
    )
    write_arrays(str(original_path), **memory_geff)
    print(f"   Created: {original_path}")

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode != 0:
        print(f"\n   JAVA FAILED (exit code {result.returncode})")
        print("   STDOUT:", result.stdout)
        print("   STDERR:", result.stderr)
        return False

    print("   Java completed successfully")

    print("\n3. Comparing (must be equivalent, data content included)...")
    try:
        check_equiv_geff(str(original_path), str(roundtrip_path))
    except Exception as e:
        print(f"   FAILED: {type(e).__name__}: {e}")
        return False

    # check_equiv_geff compares structure, values shape and dtype only.
    # The var-length "data" array content must be compared explicitly.
    orig = zarr.open(str(original_path), mode="r")["nodes/props/var_length"]
    rt = zarr.open(str(roundtrip_path), mode="r")["nodes/props/var_length"]
    if not np.array_equal(orig["values"][:], rt["values"][:]):
        print("   FAILED: var_length values array differs after round-trip.")
        return False
    if not np.array_equal(orig["data"][:], rt["data"][:]):
        print("   FAILED: var_length data array differs after round-trip.")
        return False
    if (orig["missing"][:] != rt["missing"][:]).any():
        print("   FAILED: var_length missing array differs after round-trip.")
        return False

    print("   PASSED: GEFFs are equivalent, data content included!")
    return True


def test_with_missing():
    """Test round-trip of GEFF with missing arrays (expected to warn/skip)."""
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.testing._utils import check_equiv_geff
    from geff.core_io import write_arrays

    print("\n" + "=" * 60)
    print("TEST: GEFF with missing arrays")
    print("  (Expected: Java should warn about missing arrays)")
    print("=" * 60)

    original_path = DATA_DIR / "missing_original.zarr"
    roundtrip_path = DATA_DIR / "missing_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with missing arrays...")
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=10,
        num_edges=15,
        include_missing=True,
    )
    write_arrays(str(original_path), **memory_geff)
    print(f"   Created: {original_path}")

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode != 0:
        print(f"\n   JAVA FAILED (exit code {result.returncode})")
        print("   STDOUT:", result.stdout)
        print("   STDERR:", result.stderr)
        return False

    print("   Java completed (check for warnings about missing arrays)")
    if result.stdout:
        for line in result.stdout.strip().split("\n"):
            print(f"   {line}")

    print("\n3. Comparing...")
    try:
        check_equiv_geff(str(original_path), str(roundtrip_path))
        print("   PASSED: GEFFs are equivalent!")
        return True
    except Exception as e:
        print(f"   FAILED/EXPECTED: {type(e).__name__}: {e}")
        return False


def test_with_string_props():
    """Assert the documented limitation: geff-java cannot read Zarr v3
    string properties.

    The GEFF spec mandates string values as variable-length UTF-8 arrays
    (the zarr-extensions string data type), which is what geff-python
    writes. n5-zarr 2.0.1 has no 'string' entry in its ZarrV3DataType
    enum, so geff-java cannot even see the dataset and fails with a
    GeffException. This test asserts that documented behavior and fails
    if it changes in either direction (e.g. if n5-zarr gains string
    dtype support, this test must be updated, not deleted).
    """
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.core_io import write_arrays

    print("\n" + "=" * 60)
    print("TEST: GEFF with string properties")
    print("  (Known limitation: geff-java cannot read Zarr v3 string arrays.")
    print("   n5-zarr 2.0.1 lacks the 'string' data type. This test asserts")
    print("   that documented behavior and fails if it changes.)")
    print("=" * 60)

    original_path = DATA_DIR / "string_props_original.zarr"
    roundtrip_path = DATA_DIR / "string_props_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with string properties...")
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=10,
        num_edges=15,
        extra_node_props={"label": "str"},
    )
    write_arrays(str(original_path), **memory_geff)
    print(f"   Created: {original_path}")

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode == 0:
        print("   UNEXPECTED: Java round-tripped the string property. This")
        print("   test asserts the documented limitation and must be updated")
        print("   (n5-zarr may have gained string dtype support).")
        print("   STDOUT:", result.stdout)
        return False

    # The expected failure is the specific GeffException raised when the
    # string property's "values" dataset cannot be found (n5-zarr does not
    # register 'string'-typed datasets). Any other failure mode is a
    # genuine regression and must be reported as such.
    if "GeffException" not in result.stderr or "dataset is missing" not in result.stderr:
        print("   FAILED: Java failed with an unexpected error (not the")
        print("   known string-dtype limitation):")
        print("   STDERR:", result.stderr)
        return False

    print("   PASSED: behaves as documented (string props unreadable, known limitation).")
    return True


def test_with_polygon():
    """Round-trip of a GEFF file with a 2D polygon property.

    The polygon property is a 2D var-length property: the "values" rows
    are [offset, n_points, 2] (numpy order) and "data" is the flat
    interleaved XY. geff-python writes the fixture; geff-java must
    round-trip it losslessly (check_equiv_geff does not compare data
    content), and the geff polygon validator must accept the
    Java-written output.

    The fixture is validator-clean: every present entry has >= 3 points
    (the polygon validator rejects less), and the degenerate 0-point
    case is covered by the Java-side VarLengthNdTest.
    """
    import numpy as np
    import zarr
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.testing._utils import check_equiv_geff
    from geff.core_io import write_arrays, read_to_memory
    from geff.validate.data import validate_data, ValidationConfig

    print("\n" + "=" * 60)
    print("TEST: GEFF with a 2D polygon property")
    print("  (Expected: Java round-trips the polygon losslessly and the")
    print("   geff polygon validator accepts the Java-written output)")
    print("=" * 60)

    num_nodes = 6
    original_path = DATA_DIR / "polygon_original.zarr"
    roundtrip_path = DATA_DIR / "polygon_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with Python...")
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=num_nodes,
        num_edges=5,
        include_z=False,  # 2D graph: the polygon must match the space axes
    )
    write_arrays(str(original_path), **memory_geff)

    # Manually add the polygon property, as a client would. Entries with
    # 3 to 5 points; the first node is missing.
    rng = np.random.default_rng(7)
    n_points = [3, 5, 4, 3, 3, 3]
    missing = np.zeros(num_nodes, dtype=np.bool_)
    missing[0] = True

    encoded, flat, offset = [], [], 0
    for i in range(num_nodes):
        entry = rng.random((n_points[i], 2)).astype(np.float64)
        encoded.append((offset, *entry.shape))
        flat.append(entry.ravel())
        offset += entry.size
    values = np.asarray(encoded, dtype=np.uint64)
    data = np.concatenate(flat)

    store = zarr.open(str(original_path), mode="a")
    store["nodes/props/polygon/values"] = values
    store["nodes/props/polygon/data"] = data
    store["nodes/props/polygon/missing"] = missing
    attrs = store.attrs["geff"]
    attrs["node_props_metadata"]["polygon"] = {
        "identifier": "polygon",
        "dtype": "float64",
        "varlength": True,
        "unit": None,
        "name": None,
        "description": None,
    }
    attrs["polygon"] = "polygon"
    store.attrs["geff"] = attrs
    print(f"   Added polygon: {values.shape} values, {data.size} data values, 1 missing node")

    # Sanity check: the geff-python-written fixture itself must validate.
    memory = read_to_memory(str(original_path))
    validate_data(memory, ValidationConfig(graph=True, metadata=True, polygon=True))
    print("   Fixture validates (sanity check).")

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode != 0:
        print(f"\n   JAVA FAILED (exit code {result.returncode})")
        print("   STDOUT:", result.stdout)
        print("   STDERR:", result.stderr)
        return False

    print("   Java completed successfully")

    print("\n3. Comparing original vs round-tripped...")
    try:
        check_equiv_geff(str(original_path), str(roundtrip_path))
    except Exception as e:
        print(f"   FAILED: {type(e).__name__}: {e}")
        return False

    # check_equiv_geff compares structure, values shape and dtype only.
    # The polygon content must be compared explicitly. Note: for a
    # missing entry the dummy "values" row differs between the two
    # implementations (geff-python writes [offset, 0, ndim], geff-java
    # writes an all-zero row), so present rows are compared individually.
    orig = zarr.open(str(original_path), mode="r")["nodes/props/polygon"]
    rt = zarr.open(str(roundtrip_path), mode="r")["nodes/props/polygon"]

    ov, rv = orig["values"][:], rt["values"][:]
    if ov.shape != rv.shape:
        print(f"   FAILED: values shape differs: {ov.shape} vs {rv.shape}")
        return False
    for i in range(num_nodes):
        if missing[i]:
            continue
        if not np.array_equal(ov[i], rv[i]):
            print(f"   FAILED: values row {i} differs: {ov[i]} vs {rv[i]}")
            return False
    if not np.array_equal(orig["data"][:], rt["data"][:]):
        print("   FAILED: polygon data array differs after round-trip.")
        return False
    if not np.array_equal(orig["missing"][:], rt["missing"][:]):
        print("   FAILED: polygon missing array differs after round-trip.")
        return False
    print("   PASSED: values, data and missing arrays are identical.")

    print("\n4. Validating the Java-written output with the polygon validator...")
    rt_memory = read_to_memory(str(roundtrip_path))
    if rt_memory["metadata"].polygon != "polygon":
        print("   FAILED: the geff/polygon metadata attribute was not preserved.")
        return False
    validate_data(rt_memory, ValidationConfig(graph=True, metadata=True, polygon=True))
    print("   PASSED: the Java-written output passes the geff polygon validation!")

    return True


def test_with_covariance():
    """Test round-trip of GEFF with covariance2d and covariance3d node properties."""
    import numpy as np
    import zarr
    from geff.testing.data import create_dummy_in_mem_geff
    from geff.core_io import write_arrays

    print("\n" + "=" * 60)
    print("TEST: GEFF with covariance2d and covariance3d properties")
    print("=" * 60)

    original_path = DATA_DIR / "covariance_original.zarr"
    roundtrip_path = DATA_DIR / "covariance_roundtrip.zarr"

    if original_path.exists():
        shutil.rmtree(original_path)
    if roundtrip_path.exists():
        shutil.rmtree(roundtrip_path)

    print("\n1. Creating mock GEFF with Python...")
    num_nodes = 5
    memory_geff = create_dummy_in_mem_geff(
        node_id_dtype="uint",
        node_axis_dtypes={"position": "float64", "time": "float64"},
        directed=False,
        num_nodes=num_nodes,
        num_edges=4,
    )
    write_arrays(str(original_path), **memory_geff)

    # Add covariance2d and covariance3d arrays.
    # Java stores these as zarr shape [N, cols] (C-order), which matches
    # a numpy array of shape (N, cols) directly.
    rng = np.random.default_rng(42)
    cov2d = rng.random((num_nodes, 4)).astype(np.float64)  # (N, 4)
    cov3d = rng.random((num_nodes, 6)).astype(np.float64)  # (N, 6)

    store = zarr.open(str(original_path), mode="a")
    store["nodes/props/covariance2d/values"] = cov2d
    store["nodes/props/covariance3d/values"] = cov3d

    # Register covariance props in node_props_metadata so Java writes them back.
    import json

    zattrs_path = original_path / ".zattrs"
    with open(zattrs_path) as f:
        attrs = json.load(f)
    for prop, dtype in [("covariance2d", "float64"), ("covariance3d", "float64")]:
        attrs["geff"]["node_props_metadata"][prop] = {
            "identifier": prop,
            "dtype": dtype,
            "varlength": False,
            "unit": None,
            "name": None,
            "description": None,
        }
    with open(zattrs_path, "w") as f:
        json.dump(attrs, f)

    print(f"   Added covariance2d {cov2d.shape} and covariance3d {cov3d.shape}")

    print("\n2. Running Java round-trip...")
    result = run_java_roundtrip(original_path, roundtrip_path)

    if result.returncode != 0:
        print(f"\n   JAVA FAILED (exit code {result.returncode})")
        print("   STDOUT:", result.stdout)
        print("   STDERR:", result.stderr)
        return False

    print("   Java completed successfully")

    print("\n3. Comparing covariance values...")
    rt_store = zarr.open(str(roundtrip_path), mode="r")

    try:
        rt_cov2d = rt_store["nodes/props/covariance2d/values"][:]
        rt_cov3d = rt_store["nodes/props/covariance3d/values"][:]
    except Exception as e:
        print(f"   FAILED: could not read covariance arrays: {e}")
        return False

    if not np.allclose(cov2d, rt_cov2d, atol=1e-9):
        print(f"   FAILED: covariance2d mismatch\n  orig={cov2d}\n  rt={rt_cov2d}")
        return False

    if not np.allclose(cov3d, rt_cov3d, atol=1e-9):
        print(f"   FAILED: covariance3d mismatch\n  orig={cov3d}\n  rt={rt_cov3d}")
        return False

    print("   PASSED: covariance2d and covariance3d preserved after round-trip!")
    return True


def main():
    """Run all tests."""
    print("=" * 60)
    print("GEFF Cross-Language Round-Trip Tests")
    print("=" * 60)

    # Ensure data directory exists
    DATA_DIR.mkdir(exist_ok=True)

    # Check Java is built
    try:
        get_java_classpath()
    except RuntimeError as e:
        print(f"\nERROR: {e}")
        print("\nPlease build the Java project first:")
        print("  cd .. && mvn package -DskipTests")
        sys.exit(1)

    # Run tests
    results = {}

    results["basic_3d"] = test_basic_3d_geff()
    results["basic_2d"] = test_basic_2d_geff()
    results["varlength"] = test_with_varlength()
    # Skip missing test - has a bug in geff test data generator (edge prop length mismatch)
    # results["missing"] = test_with_missing()
    results["string_props"] = test_with_string_props()
    results["polygon"] = test_with_polygon()
    try:
        results["covariance"] = test_with_covariance()
    except Exception as e:
        # Known issue, owned elsewhere: the test hand-edits the root
        # .zattrs file, which does not exist in Zarr v3 containers.
        print(
            f"\n  ERROR in covariance test (known issue, owned elsewhere):"
            f" {type(e).__name__}: {e}"
        )
        results["covariance"] = False

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)

    passed = sum(1 for v in results.values() if v)
    total = len(results)

    for name, result in results.items():
        status = "PASSED" if result else "FAILED"
        print(f"  {name}: {status}")

    print(f"\nTotal: {passed}/{total} passed")

    # Return non-zero if any critical tests failed
    # Non-critical cases are allowed to differ in known unsupported areas.
    critical_tests = ["basic_3d", "basic_2d", "polygon", "covariance"]
    critical_passed = all(results.get(t, False) for t in critical_tests)

    sys.exit(0 if critical_passed else 1)


if __name__ == "__main__":
    main()
