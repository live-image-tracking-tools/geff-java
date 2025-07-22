package org.mastodon.geff;

import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents an axis in the GEFF format with name, type, unit, and optional
 * bounds. This class handles axis metadata for spatial and temporal dimensions.
 * 
 * Example axis structures: - Time axis: {'name': 't', 'type': "time", 'unit':
 * "seconds", 'min': 0, 'max': 125} - Space axis: {'name': 'x', 'type': "space",
 * 'unit': "micrometers", 'min': 764.42, 'max': 2152.3}
 */
public class GeffAxis
{

    public static final String NAME_TIME = "t";

    public static final String NAME_SPACE_X = "x";

    public static final String NAME_SPACE_Y = "y";

    public static final String NAME_SPACE_Z = "z";

    // Supported axis types
    public static final String TYPE_TIME = "time";

    public static final String TYPE_SPACE = "space";

    // Common units
    public static final String UNIT_SECONDS = "seconds";

    public static final String UNIT_MICROMETERS = "micrometers";

    public static final String UNIT_PIXELS = "pixels";

    public static final String UNIT_MILLIMETERS = "millimeters";

    private String name;

    private String type;

    private String unit;

    private Double min; // Optional - can be null

    private Double max; // Optional - can be null

    /**
     * Default constructor
     */
    public GeffAxis()
    {}

    /**
     * Constructor with required fields
     */
    public GeffAxis( String name, String type, String unit )
    {
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    /**
     * Constructor with all fields
     */
    public GeffAxis( String name, String type, String unit, Double min, Double max )
    {
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    // Getters and Setters
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        if ( type != null && !TYPE_TIME.equals( type ) && !TYPE_SPACE.equals( type ) )
        { throw new IllegalArgumentException(
                "Axis type must be '" + TYPE_TIME + "' or '" + TYPE_SPACE + "', got: " + type ); }
        this.type = type;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit( String unit )
    {
        this.unit = unit;
    }

    public Double getMin()
    {
        return min;
    }

    public void setMin( Double min )
    {
        this.min = min;
        validateBounds();
    }

    public Double getMax()
    {
        return max;
    }

    public void setMax( Double max )
    {
        this.max = max;
        validateBounds();
    }

    /**
     * Set both min and max bounds
     * 
     * @param min
     *            the minimum bound value
     * @param max
     *            the maximum bound value
     */
    public void setBounds( Double min, Double max )
    {
        this.min = min;
        this.max = max;
        validateBounds();
    }

    /**
     * Check if this axis has bounds defined
     */
    public boolean hasBounds()
    {
        return min != null && max != null;
    }

    /**
     * Get the range (max - min) if bounds are defined
     */
    public Double getRange()
    {
        if ( hasBounds() )
        { return max - min; }
        return null;
    }

    /**
     * Validate that min <= max if both are defined
     */
    private void validateBounds()
    {
        if ( min != null && max != null && min > max )
        { throw new IllegalArgumentException( "Axis min (" + min + ") cannot be greater than max (" + max + ")" ); }
    }

    /**
     * Validate the axis according to GEFF rules
     */
    public void validate()
    {
        if ( name == null || name.trim().isEmpty() )
        { throw new IllegalArgumentException( "Axis name cannot be null or empty" ); }

        if ( type == null || type.trim().isEmpty() )
        { throw new IllegalArgumentException( "Axis type cannot be null or empty" ); }

        if ( !TYPE_TIME.equals( type ) && !TYPE_SPACE.equals( type ) )
        { throw new IllegalArgumentException(
                "Axis type must be '" + TYPE_TIME + "' or '" + TYPE_SPACE + "', got: " + type ); }

        if ( unit == null || unit.trim().isEmpty() )
        { throw new IllegalArgumentException( "Axis unit cannot be null or empty" ); }

        validateBounds();
    }

    /**
     * Create a time axis
     */
    public static GeffAxis createTimeAxis( String name, String unit, Double min, Double max )
    {
        return new GeffAxis( name, TYPE_TIME, unit, min, max );
    }

    /**
     * Create a space axis
     */
    public static GeffAxis createSpaceAxis( String name, String unit, Double min, Double max )
    {
        return new GeffAxis( name, TYPE_SPACE, unit, min, max );
    }

    /**
     * Create a time axis without bounds
     */
    public static GeffAxis createTimeAxis( String name, String unit )
    {
        return new GeffAxis( name, TYPE_TIME, unit );
    }

    /**
     * Create a space axis without bounds
     */
    public static GeffAxis createSpaceAxis( String name, String unit )
    {
        return new GeffAxis( name, TYPE_SPACE, unit );
    }

    /**
     * Write this axis to json format for serialization. This is a placeholder
     * method for future implementation.
     */
    public TreeMap< String, Object > toTreeMap()
    {
        TreeMap< String, Object > map = new TreeMap<>();
        map.put( "name", name );
        map.put( "type", type );
        map.put( "unit", unit );
        map.put( "min", min );
        map.put( "max", max );
        return map;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "GeffAxis{" );
        sb.append( "name='" ).append( name ).append( '\'' );
        sb.append( ", type='" ).append( type ).append( '\'' );
        sb.append( ", unit='" ).append( unit ).append( '\'' );
        if ( min != null )
        {
            sb.append( ", min=" ).append( min );
        }
        if ( max != null )
        {
            sb.append( ", max=" ).append( max );
        }
        sb.append( '}' );
        return sb.toString();
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null || getClass() != obj.getClass() )
            return false;

        GeffAxis geffAxis = ( GeffAxis ) obj;

        return Objects.equals( name, geffAxis.name ) &&
                Objects.equals( type, geffAxis.type ) &&
                Objects.equals( unit, geffAxis.unit ) &&
                Objects.equals( min, geffAxis.min ) &&
                Objects.equals( max, geffAxis.max );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( name, type, unit, min, max );
    }
}
