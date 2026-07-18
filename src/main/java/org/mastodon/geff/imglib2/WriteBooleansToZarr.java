package org.mastodon.geff.imglib2;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.DType;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.janelia.saalfeldlab.n5.zarr.ZarrDatasetAttributes;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

public class WriteBooleansToZarr {

    public static void main(String[] args) {

        final String path = "cross-language-tests/data/covariance_original.zarr";
        try (final N5ZarrWriter n5 = new N5ZarrWriter(path)) {
            final String dataset = "nodes/props/t/missing2";
            final String typestr = "|b1";
            final long[] dimensions = {10};
            final int[] blockSize = Util.long2int(dimensions);
            final ZarrDatasetAttributes attributes = new ZarrDatasetAttributes(
                    dimensions,
                    blockSize,
                    new DType(typestr, null),
                    new ZstandardCompression(0),
                    true,
                    "0"
            );
            n5.createDataset(dataset, attributes );
            System.out.println("attributes.getDataType() = " + attributes.getDataType());

            final byte[] array = new byte[(int) dimensions[0]];
            for (int i = 0; i < dimensions[0]; i++) {
                array[i] = (byte) i;
            }
            RandomAccessibleInterval<UnsignedByteType> data = ArrayImgs.unsignedBytes(array, dimensions);
            N5Utils.saveRegion(data, n5, dataset, attributes);
        }
    }
}
