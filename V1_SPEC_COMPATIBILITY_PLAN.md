# GEFF v1 Spec Compatibility Plan

This document summarizes the compatibility analysis between `geff-java` and the GEFF v1 specification (Python reference implementation).
This was created by Claude and Caroline together - as such, the actions proposed for each identified issue is Caroline's recommendation;
however, the identification of the issues relied heavily on Claude, as Claude is better at Java than Caroline. 

This team will attempt to address the concerns as time allows, but welcomes help from actual Java co

## Action Summary

| #   | Issue                                                 | Action          | Priority |
| --- | ----------------------------------------------------- | --------------- | -------- |
| 1   | Missing `node_props_metadata` / `edge_props_metadata` | **FIX**         | Critical |
| 2   | Axis missing `scale`/`scaled_unit`/`offset`           | Document        | Low      |
| 3   | Hardcoded axis names (`t`,`x`,`y`,`z` only)           | **FIX**         | Critical |
| 4   | Missing `channel` axis type                           | **FIX**         | Low      |
| 5   | Covariance format mismatch                            | Document        | Low      |
| 6   | Hardcoded `track_id` path                             | **FIX**         | Medium   |
| 7   | Variable-length properties                            | **FIX**         | High     |
| 8   | String properties                                     | **Warn & skip** | Medium   |
| 9   | `missing` arrays                                      | **Warn & skip** | Medium   |

**Fixes required**: #1, #3, #4, #6, #7
**Graceful handling**: #8, #9
**Document only**: #2, #5

---

## Incompatibilities

### 1. Missing Required Metadata Fields (CRITICAL)

**Problem**: Java does not read or write `node_props_metadata` and `edge_props_metadata`, which are **required** fields in the v1 spec. Files written by Java are invalid per spec.

**Locations**:
- `GeffMetadata.java:70-75`
- Spec: `_schema.py:166-179`

**Plan**:
1. Add `PropMetadata` class to Java with fields: `identifier`, `dtype`, `varlength`, `unit`, `name`, `description`
2. Add `Map<String, PropMetadata>` fields to `GeffMetadata` for node and edge props
3. **Writing**: Infer dtype from actual array data types (e.g., `double[]` → "float64", `int[]` → "int32")
4. **Reading**: Parse and expose metadata via getters
5. Update `writeToN5()` to serialize these as `geff/node_props_metadata` and `geff/edge_props_metadata`

---

### 2. Axis: Missing `scale`, `scaled_unit`, `offset` Fields (MEDIUM)

**Problem**: Java's `GeffAxis` only has `name`, `type`, `unit`, `min`, `max`. The spec also supports `scale`, `scaled_unit`, and `offset` for coordinate transformations.

**Locations**:
- `GeffAxis.java:67-75`
- Spec: `_axis.py:51-63`

**Plan**: Document as known shortcoming (no fix needed now)
- GSON silently ignores unknown fields, so Java will not error when reading files with these fields
- Round-trip through Java will lose `scale`/`scaled_unit`/`offset` data
- Future enhancement: Add these fields to `GeffAxis` if coordinate transformation support is needed

---

### 3. Axis: Hardcoded Names Validation (CRITICAL)

**Problem**: Java only allows axis names `t`, `x`, `y`, `z` and **throws an error** for any other name. The spec allows any string name (must match a node property).

**Location**: `GeffMetadata.java:176-181`

**Plan**:
1. Remove the validation check in `GeffMetadata.validate()` that restricts axis names to `t`, `x`, `y`, `z`
2. Keep `GeffAxis.NAME_TIME`, `NAME_SPACE_X`, etc. as convenience constants, but don't enforce them
3. Axis name validation should only ensure the name is non-empty (which `GeffAxis.validate()` already does)

---

### 4. Axis: Missing `channel` Type (LOW)

**Problem**: Java only supports axis types `time` and `space`. The spec also supports `channel`. Java **throws an error** if an axis has `type: "channel"`.

**Locations**:
- `GeffAxis.java:53-56`
- `GeffMetadata.java:182-185`

**Plan**:
1. Add `public static final String TYPE_CHANNEL = "channel"` to `GeffAxis`
2. Update validation in `GeffAxis.setType()` and `GeffMetadata.validate()` to accept `channel`

---

### 5. Covariance Storage Format (MEDIUM)

**Problem**: Java uses flattened arrays (4 elements for 2D, 6 elements for 3D upper triangular). The spec uses full 2x2 or 3x3 matrices.

**Locations**: `GeffNode.java:76-79, 88-90`

**Note**: Java has a bug - it reads covariance data but ignores it (lines 737-738 use defaults instead of the read `covariance2ds`/`covariance3ds`).

**Plan**: Document as known shortcoming (no fix needed now)
- Java reading Python: No error, but covariance data is silently ignored (already broken)
- Python reading Java: Validation errors on shape, but validation is optional; core graph data still readable
- Future enhancement: Fix Java to use full matrices and actually use the read data

---

### 6. Property Paths: `track_id` vs Dynamic (MEDIUM)

**Problem**: Java hardcodes the path `/nodes/props/track_id/values`. The spec uses a dynamic property name from `track_node_props["tracklet"]` in metadata.

**Location**: `GeffNode.java:667`

**Plan**:
1. Add `track_node_props` field to `GeffMetadata` (map with keys "lineage" and/or "tracklet")
2. Read/write this field from `geff/track_node_props` in zarr attributes
3. In `GeffNode.readFromN5()`, look up the tracklet property name from metadata instead of hardcoding `track_id`
4. Fall back to `track_id` if `track_node_props` is not present (backward compatibility)

---

## Missing Features

### Future Enhancements (no action needed now)

1. **`sphere` metadata field** - pointer to radius property name
2. **`ellipsoid` metadata field** - pointer to covariance property name
3. **`related_objects`** - links to segmentation/image data
4. **`display_hints`** - viewer display preferences
5. **`extra`** - free-form metadata dict

### Graceful Handling Needed (warn and skip, don't error)

#### 7. Variable-length Properties

**Problem**: Properties with `varlength: true` in PropMetadata use offset/length encoding. Java currently has no support for these properties.

**Encoding Format** (from spec):
- For each node, the property can have a different shape/length
- A `data` array contains all flattened values concatenated (shape: `(V,)` where V is total number of elements across all nodes)
- A `values` array contains offset and shape information (shape: `(N, ndim+1)` where N is number of nodes, ndim is dimensionality)
  - First column: offset into the data array for that node's data
  - Remaining columns: shape of that node's data (e.g., for 2D polygon: `[offset, rows, cols]`)

**Examples from Spec**:
- Polygon property: `polygon/data`, `polygon/values` (shape: N x 3 for 2D coords, containing offset, height, width)

**Plan**:
1. When reading a property, check `varlength` in PropMetadata
2. If `varlength: true`:
   - Read both the `data` array and `values` array from zarr
   - For each node index, extract the offset and shape from `values[i]`
   - Use these to slice the appropriate section from `data` array
   - Reconstruct the variable-length array for that node
   - Handle optional `missing` array to skip nodes with missing values
3. For Java representation:
   - Store as `List<Object>` or `List<byte[]>` depending on dtype (for flexibility)
   - Or use a wrapper class `VarlengthProperty` containing `Object[] data` indexed by node position
4. When **writing** varlength properties:
   - Flatten all node data into a single `data` array
   - Record offset and shape for each node in `values` array
   - Write to zarr with proper dtype encoding
5. **Initially**: Add support for **reading** varlength properties with graceful error handling
6. **Future**: If writing varlength properties is needed, implement full encoding logic
7. Add unit tests using the polygon property from test data

#### 8. String Properties

**Problem**: Properties with `dtype: "str"`. Java only handles numeric types.

**Plan**: When reading, check `dtype` in PropMetadata; if "str" or "bytes", log warning and skip property.

#### 9. `missing` Arrays

**Problem**: Optional `/nodes/props/{name}/missing` boolean array indicating null values. Java has no support for sparse/missing data.

**Plan**: When reading properties, check if `missing` array exists; if so, log warning that missing values are not supported and read values array only (treating all as present).

---

### 10. Version Checking: Allowlist vs Pattern (LOW)

**Problem**: Java uses an explicit allowlist of supported versions (`0.2`, `0.3`, `0.4`, `1.0`, `1.1`). The Python spec uses a regex pattern that accepts **any** semver-formatted version:

```python
VERSION_PATTERN = r"^\d+\.\d+(?:\.\d+)?(?:\.dev\d+)?(?:\+[a-zA-Z0-9]+)?"
```

This means Python will accept future versions like `2.0`, `1.5`, etc. as long as they match the semver pattern. Pre-v1 versions (like `0.1-pre-v1`) are rejected because they don't match the pattern format.

**Locations**:
- `GeffMetadata.java:62, 67, 120-124`
- Spec: `_schema.py:23`

**Plan**:
1. Change Java from explicit version allowlist to pattern-based validation
2. Accept any version matching semver pattern: `^\d+\.\d+(?:\.\d+)?(?:\.dev\d+)?(?:\+[a-zA-Z0-9.]+)?$`
3. This implicitly rejects pre-v1 style versions without explicit checking
4. Remove `SUPPORTED_VERSIONS` list and update validation logic accordingly

---

## Next Steps

1. **Interoperability Testing**: Create cross-language tests
   - Python writes GEFF → Java reads
   - Java writes GEFF → Python reads & validates
   - Include testing of varlength properties (e.g., polygon data)
2. **Implement Fixes**: Address issues #1, #3, #4, #6, #7 in priority order
3. **Add Graceful Handling**: Implement warn-and-skip for #8, #9
