package org.mastodon.geff.imglib2;

import net.imglib2.Dimensions;
import net.imglib2.type.NativeType;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;

import java.util.EnumMap;

public record GeffPropertySpec(String identifier, boolean isVarLength, boolean isOptional, DType dType, int numDimensions, Dimensions dimensions) {

    public static <T extends NativeType<T>> DType defaultDType(final T type) {
        return new DType(typestrs.get(N5Utils.dataType(type)), null);
    }

    // copied from DType, modified to use little-endian
    private static final EnumMap<DataType, String> typestrs = new EnumMap<>(DataType.class);

    static {
        typestrs.put(DataType.INT8, "|i1");
        typestrs.put(DataType.UINT8, "|u1");
        typestrs.put(DataType.INT16, "<i2");
        typestrs.put(DataType.UINT16, "<u2");
        typestrs.put(DataType.INT32, "<i4");
        typestrs.put(DataType.UINT32, "<u4");
        typestrs.put(DataType.INT64, "<i8");
        typestrs.put(DataType.UINT64, "<u8");
        typestrs.put(DataType.FLOAT32, "<f4");
        typestrs.put(DataType.FLOAT64, "<f8");
        typestrs.put(DataType.STRING, "|O");
        typestrs.put(DataType.OBJECT, "|O");
    }
}
