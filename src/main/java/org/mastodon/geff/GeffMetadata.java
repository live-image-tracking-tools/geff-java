/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 Ko Sugawara
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.mastodon.geff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.bc.zarr.ZarrGroup;

import ucar.ma2.InvalidRangeException;

/**
 * Represents metadata for a Geff (Graph Exchange Format for Features) dataset.
 * This class handles reading and writing metadata from/to Zarr format.
 *
 * This is the Java equivalent of the Python GeffMetadata schema from:
 * https://github.com/live-image-tracking-tools/geff/blob/main/src/geff/metadata_schema.py
 */
public class GeffMetadata
{

    // Supported GEFF versions
    public static final List< String > SUPPORTED_VERSIONS = Arrays.asList( "0.2", "0.3" );

    // Pattern to match major.minor versions, allowing for patch versions and
    // development versions
    // Examples: 0.1.1, 0.2.2.dev20+g611e7a2.d20250719, 0.2.0-alpha.1, etc.
    private static final Pattern SUPPORTED_VERSIONS_PATTERN = Pattern
            .compile( "(0\\.0|0\\.1|0\\.2|0\\.3)(?:\\.\\d+)?(?:\\.[a-zA-Z0-9]+(?:\\d+)?)?(?:[+\\-][a-zA-Z0-9\\.]+)*" );

    // Metadata attributes - matching the Python schema
    private String geffVersion;

    private boolean directed;

    private GeffAxis[] geffAxes;

    /**
     * Default constructor
     */
    public GeffMetadata()
    {}

    /**
     * Constructor with basic parameters
     */
    public GeffMetadata( String geffVersion, boolean directed )
    {
        setGeffVersion( geffVersion );
        this.directed = directed;
    }

    /**
     * Constructor with all parameters
     */
    public GeffMetadata( String geffVersion, boolean directed, GeffAxis[] geffAxes )
    {
        setGeffVersion( geffVersion );
        this.directed = directed;
        setGeffAxes( geffAxes );
    }

    // Getters and Setters
    public String getGeffVersion()
    {
        return geffVersion;
    }

    public void setGeffVersion( String geffVersion )
    {
        if ( geffVersion != null && !SUPPORTED_VERSIONS_PATTERN.matcher( geffVersion ).matches() )
        { throw new IllegalArgumentException(
                "Unsupported Geff version: " + geffVersion +
                        ". Supported major.minor versions are: " + SUPPORTED_VERSIONS +
                        " (patch versions, development versions, and metadata are also supported, " +
                        "e.g., 0.1.1, 0.2.2.dev20+g611e7a2.d20250719)" ); }
        this.geffVersion = geffVersion;
    }

    public boolean isDirected()
    {
        return directed;
    }

    public void setDirected( boolean directed )
    {
        this.directed = directed;
    }

    public GeffAxis[] getGeffAxes()
    {
        return geffAxes;
    }

    public void setGeffAxes( GeffAxis[] geffAxes )
    {
        this.geffAxes = geffAxes != null ? geffAxes.clone() : null;
        validate();
    }

    /**
     * Validates the metadata according to the GEFF schema rules
     */
    public void validate()
    {
        // Check spatial metadata consistency if position is provided
        if ( geffAxes != null )
        {
            for ( GeffAxis axis : geffAxes )
            {
                if ( !Arrays.asList( GeffAxis.NAME_TIME, GeffAxis.NAME_SPACE_X, GeffAxis.NAME_SPACE_Y,
                        GeffAxis.NAME_SPACE_Z ).contains( axis.getName() ) )
                { throw new IllegalArgumentException(
                        "Invalid axis name: " + axis.getName() + ". Supported names are: " +
                                GeffAxis.NAME_TIME + ", " + GeffAxis.NAME_SPACE_X + ", " +
                                GeffAxis.NAME_SPACE_Y + ", " + GeffAxis.NAME_SPACE_Z ); }
                if ( !Arrays.asList( GeffAxis.TYPE_TIME, GeffAxis.TYPE_SPACE ).contains( axis.getType() ) )
                { throw new IllegalArgumentException(
                        "Invalid axis type: " + axis.getType() + ". Supported types are: " +
                                GeffAxis.TYPE_TIME + ", " + GeffAxis.TYPE_SPACE ); }
                if ( axis.getMin() > axis.getMax() )
                { throw new IllegalArgumentException(
                        "Roi min " + axis.getMin() + " is greater than " +
                                "max " + axis.getMax() + " in dimension " + axis.getName() ); }
            }
        }
    }

    /**
     * Read metadata from a Zarr group
     */
    public static GeffMetadata readFromZarr( String zarrPath ) throws IOException, InvalidRangeException
    {
        ZarrGroup group = ZarrGroup.open( zarrPath );
        return readFromZarr( group );
    }

    /**
     * Read metadata from a Zarr group
     */
    public static GeffMetadata readFromZarr( ZarrGroup group ) throws IOException
    {
        // Check if geff_version exists in zattrs
        String geffVersion = null;
        Map< ?, ? > attrs = null;
        if ( group.getAttributes().containsKey( "geff" ) )
        {
            System.out.println( "Found geff entry in " + group );
            Object geffRootObj = group.getAttributes().get( "geff" );
            if ( geffRootObj instanceof Map )
            {
                try
                {
                    // Check if geff_version exists in the geff entry
                    if ( ( ( Map< ?, ? > ) geffRootObj ).containsKey( "geff_version" ) )
                    {
                        System.out.println(
                                "Found geff_version in geff entry: " + ( ( Map< ?, ? > ) geffRootObj ).get( "geff_version" ) );
                        geffVersion = ( String ) ( ( Map< ?, ? > ) geffRootObj ).get( "geff_version" );
                        attrs = ( Map< ?, ? > ) geffRootObj;
                    }
                    else
                    {
                        System.out.println( "No geff_version found in geff entry." );
                    }
                }
                catch ( ClassCastException e )
                {
                    System.err.println( "Invalid geff entry format: " + e.getMessage() );
                }
            }
        }
		if ( geffVersion == null )
		{
			throw new IllegalArgumentException(
					"No geff_version found in " + group + ". This may indicate the path is incorrect or " +
							"zarr group name is not specified (e.g. /dataset.zarr/tracks/ instead of " +
							"/dataset.zarr/)." );
		}

        GeffMetadata metadata = new GeffMetadata();

        // Read required fields

        metadata.setGeffVersion( geffVersion );

        if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) )
        {
            // For 0.2 and 0.3, we expect a different structure
            metadata.setDirected( ( Boolean ) attrs.get( "directed" ) );

            // Read axes
            List< GeffAxis > axes = new ArrayList<>();
            if ( attrs.containsKey( "axes" ) )
            {
                Object axesObj = attrs.get( "axes" );
                if ( axesObj instanceof List )
                {
                    for ( Object axisObj : ( List< ? > ) axesObj )
                    {
                        if ( axisObj instanceof Map )
                        {
                            Map< ?, ? > axisMap = ( Map< ?, ? > ) axisObj;
                            String name = ( String ) axisMap.get( "name" );
                            String type = ( String ) axisMap.get( "type" );
                            String unit = ( String ) axisMap.get( "unit" );
                            Double min = ( Double ) axisMap.get( "min" );
                            Double max = ( Double ) axisMap.get( "max" );
                            axes.add( new GeffAxis( name, type, unit, min, max ) );
                        }
                    }
                }
                else
                {
                    throw new IllegalArgumentException( "Invalid axes format: " + axesObj );
                }
            }
            metadata.setGeffAxes( axes.toArray( new GeffAxis[ 0 ] ) );
        }

        // Validate the loaded metadata
        metadata.validate();

        return metadata;
    }

    /**
     * Write metadata to Zarr format at specified path
     */
    public static void writeToZarr( GeffMetadata metadata, String zarrPath ) throws IOException
    {
        ZarrGroup group = ZarrGroup.create( zarrPath );
        metadata.writeToZarr( group );
    }

    /**
     * Write metadata to Zarr format
     */
    public void writeToZarr( ZarrGroup group ) throws IOException
    {
        // Validate before writing
        validate();

		if ( geffVersion == null )
		{
			throw new IllegalArgumentException( "Geff version must be set before writing metadata." );
		}

        if ( geffVersion.startsWith( "0.2" ) || geffVersion.startsWith( "0.3" ) )
        {
            java.util.Map< String, Object > rootAttrs = new java.util.TreeMap<>();
            java.util.Map< String, Object > attrs = new java.util.TreeMap<>();
            // Write required fields
            attrs.put( "directed", directed );
            attrs.put( "geff_version", geffVersion );
            ArrayList< Map< String, Object > > axisMaps = new ArrayList<>();
            for ( GeffAxis axis : geffAxes )
            {
                if ( axis.getName() == null || axis.getType() == null )
                { throw new IllegalArgumentException(
                        "Axis name and type must be set for all axes in version 0.2 and 0.3." ); }
                Map< String, Object > axisMap = new java.util.TreeMap<>();
                axisMap.put( "name", axis.getName() );
                axisMap.put( "type", axis.getType() );
                axisMap.put( "unit", axis.getUnit() );
                if ( axis.getMin() != null )
                {
                    axisMap.put( "min", axis.getMin() );
                }
                if ( axis.getMax() != null )
                {
                    axisMap.put( "max", axis.getMax() );
                }
                axisMaps.add( axisMap );
            }
            attrs.put( "axes", axisMaps );
            rootAttrs.put( "geff", attrs );
            // Write the attributes to the Zarr group
            group.writeAttributes( rootAttrs );
            System.out.println( "Written metadata attributes: " + rootAttrs.keySet() );
        }

    }

    // Helper methods for type conversion
    private static double[] convertToDoubleArray( Object obj )
    {
        if ( obj instanceof double[] )
        {
            return ( double[] ) obj;
        }
        else if ( obj instanceof java.util.ArrayList )
        {
            @SuppressWarnings( "unchecked" )
            java.util.ArrayList< Object > list = ( java.util.ArrayList< Object > ) obj;
            double[] result = new double[ list.size() ];
            for ( int i = 0; i < list.size(); i++ )
            {
                if ( list.get( i ) instanceof Number )
                {
                    result[ i ] = ( ( Number ) list.get( i ) ).doubleValue();
                }
                else
                {
                    result[ i ] = Double.parseDouble( list.get( i ).toString() );
                }
            }
            return result;
        }
        else if ( obj instanceof Object[] )
        {
            Object[] arr = ( Object[] ) obj;
            double[] result = new double[ arr.length ];
            for ( int i = 0; i < arr.length; i++ )
            {
                if ( arr[ i ] instanceof Number )
                {
                    result[ i ] = ( ( Number ) arr[ i ] ).doubleValue();
                }
                else
                {
                    result[ i ] = Double.parseDouble( arr[ i ].toString() );
                }
            }
            return result;
        }
        else if ( obj instanceof float[] )
        {
            float[] floatArray = ( float[] ) obj;
            double[] result = new double[ floatArray.length ];
            for ( int i = 0; i < floatArray.length; i++ )
            {
                result[ i ] = floatArray[ i ];
            }
            return result;
        }
        return null;
    }

    private static String[] convertToStringArray( Object obj )
    {
        if ( obj instanceof String[] )
        {
            return ( String[] ) obj;
        }
        else if ( obj instanceof java.util.ArrayList )
        {
            @SuppressWarnings( "unchecked" )
            java.util.ArrayList< Object > list = ( java.util.ArrayList< Object > ) obj;
            String[] result = new String[ list.size() ];
            for ( int i = 0; i < list.size(); i++ )
            {
                result[ i ] = list.get( i ) != null ? list.get( i ).toString() : null;
            }
            return result;
        }
        else if ( obj instanceof Object[] )
        {
            Object[] arr = ( Object[] ) obj;
            String[] result = new String[ arr.length ];
            for ( int i = 0; i < arr.length; i++ )
            {
                result[ i ] = arr[ i ] != null ? arr[ i ].toString() : null;
            }
            return result;
        }
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(
                "GeffMetadata{geffVersion='%s', directed=%s, geffAxes=%s}",
                geffVersion, directed, Arrays.toString( geffAxes ) );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null || getClass() != obj.getClass() )
            return false;

        GeffMetadata that = ( GeffMetadata ) obj;

        if ( directed != that.directed )
            return false;
        if ( geffVersion != null ? !geffVersion.equals( that.geffVersion ) : that.geffVersion != null )
            return false;
        for ( int i = 0; i < geffAxes.length; i++ )
        {
            if ( !geffAxes[ i ].equals( that.geffAxes[ i ] ) )
            { return false; }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = geffVersion != null ? geffVersion.hashCode() : 0;
        result = 31 * result + ( directed ? 1 : 0 );
        result = 31 * result + Arrays.hashCode( geffAxes );
        return result;
    }
}
