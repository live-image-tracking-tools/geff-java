package org.mastodon;

import java.io.IOException;
import java.util.Iterator;

import com.bc.zarr.ZarrArray;
import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

public class Geff {
    // This class serves as a placeholder for the Geff package.
    // It can be used to define package-level constants or utility methods in the
    // future.

    public static final String VERSION = "0.1.0"; // Example version constant

    private Geff() {
        // Private constructor to prevent instantiation
    }

    public static void main(String[] args) {
        System.out.println("Geff package version: " + VERSION);
        System.out.println("This is a placeholder for the Geff package.");
        try {
            final ZarrGroup zarrTracks = ZarrGroup.open("src/test/resources/Fluo-N2DL-HeLa-01.zarr/tracks");
            final Iterator<String> groupKeyIter = zarrTracks.getGroupKeys().iterator();
            while (groupKeyIter.hasNext()) {
                String groupKey = groupKeyIter.next();
                System.out.println("Found group: " + groupKey);
            }
            final Iterator<String> arrayKeyIter = zarrTracks.getArrayKeys().iterator();
            while (arrayKeyIter.hasNext()) {
                String arrayKey = arrayKeyIter.next();
                System.out.println("Found array: " + arrayKey);
            }
            final Iterator<String> attrKeyIter = zarrTracks.getAttributes().keySet().iterator();
            while (attrKeyIter.hasNext()) {
                String attrKey = attrKeyIter.next();
                System.out.print("Found attribute: " + attrKey);
                Object attrValue = zarrTracks.getAttributes().get(attrKey);
                System.out.println("  Value: " + attrValue);
            }
            // Example of opening an array
            System.out.println("Opening 'edges/ids' array...");
            ZarrArray edgesIds = zarrTracks.openArray("edges/ids");
            int[] edgesIdsData = (int[]) edgesIds.read();
            System.out.println("Read edges/ids data: " + edgesIdsData.length + " elements.");
            // for (int id : edgesIdsData) {
            // System.out.println("Edge ID: " + id);
            // }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidRangeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
