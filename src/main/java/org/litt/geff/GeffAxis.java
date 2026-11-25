/*-
 * #%L
 * geff-java
 * %%
 * Copyright (C) 2025 - 2026 Ko Sugawara
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
package org.litt.geff;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an axis in the GEFF format, modeled on the OME-zarr axes
 * convention. An axis has a name, an optional type ({@code space}, {@code
 * time} or {@code channel}), an optional unit, optional bounds, and the
 * OME-zarr scaling fields ({@code scale}, {@code scaled_unit} and {@code
 * offset}).
 * <p>
 * The order of the axes in {@link GeffMetadata#getGeffAxes()} is meaningful:
 * it defines the dimension order of array-valued properties, and is used as a
 * fallback when aligning a graph with image or segmentation data that do not
 * declare axes of their own.
 *
 * @see <a href=
 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
 *      specification</a>
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

	public static final String TYPE_CHANNEL = "channel";

	// Common units
	public static final String UNIT_SECOND = "second";

	public static final String UNIT_MICROMETER = "micrometer";

	public static final String UNIT_PIXEL = "pixel";

	public static final String UNIT_MILLIMETER = "millimeter";

	private String name;

	private String type;

	private String unit;

	private Double min; // Optional - can be null

	private Double max; // Optional - can be null

	/**
	 * The scale of the axis, in OME-zarr convention: a value {@code v} in the
	 * graph is mapped to {@code scale * v + offset} in {@code scaled_unit}
	 * physical units. Optional, can be {@code null}.
	 */
	private Double scale; // Optional - can be null

	/**
	 * The physical unit of {@link #getScale() scale} (e.g. {@code "µm"}).
	 * Optional, can be {@code null}. According to the GEFF specification, if
	 * {@code scaled_unit} is set, a {@code scale} value must also be provided.
	 */
	@SerializedName( "scaled_unit" )
	private String scaledUnit; // Optional - can be null

	/**
	 * The offset of the axis in OME-zarr convention: the amount by which to
	 * offset this axis. Optional, can be {@code null}.
	 */
	private Double offset; // Optional - can be null

	/**
	 * Default constructor
	 */
	public GeffAxis()
	{}

	/**
	 * Constructor with required fields
	 */
	public GeffAxis( final String name, final String type, final String unit )
	{
		this.name = name;
		this.type = type;
		this.unit = unit;
	}

	/**
	 * Constructor with all fields
	 */
	public GeffAxis( final String name, final String type, final String unit, final Double min, final Double max )
	{
		this.name = name;
		this.type = type;
		this.unit = unit;
		this.min = min;
		this.max = max;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType( final String type )
	{
		if ( type != null && !TYPE_TIME.equals( type ) && !TYPE_SPACE.equals( type ) && !TYPE_CHANNEL.equals( type ) )
			throw new IllegalArgumentException( "Axis type must be '"
					+ TYPE_TIME + "', '" + TYPE_SPACE + "', or '" + TYPE_CHANNEL + "', got: " + type );
		this.type = type;
	}

	public String getUnit()
	{
		return unit;
	}

	public void setUnit( final String unit )
	{
		this.unit = unit;
	}

	public Double getMin()
	{
		return min;
	}

	public void setMin( final Double min )
	{
		this.min = min;
		validateBounds();
	}

	public Double getMax()
	{
		return max;
	}

	public void setMax( final Double max )
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
	public void setBounds( final Double min, final Double max )
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
			return max - min;
		return null;
	}

	public Double getScale()
	{
		return scale;
	}

	public void setScale( final Double scale )
	{
		this.scale = scale;
	}

	public String getScaledUnit()
	{
		return scaledUnit;
	}

	public void setScaledUnit( final String scaledUnit )
	{
		this.scaledUnit = scaledUnit;
	}

	public Double getOffset()
	{
		return offset;
	}

	public void setOffset( final Double offset )
	{
		this.offset = offset;
	}

	/**
	 * Validate that min <= max if both are defined
	 */
	private void validateBounds()
	{
		if ( min != null && max != null && min > max )
			throw new IllegalArgumentException( "Axis min (" + min + ") cannot be greater than max (" + max + ")" );
	}

	/**
	 * Validate the axis according to GEFF rules
	 */
	public void validate()
	{
		if ( name == null || name.trim().isEmpty() )
			throw new IllegalArgumentException( "Axis name cannot be null or empty" );

		if ( type == null || type.trim().isEmpty() )
			throw new IllegalArgumentException( "Axis type cannot be null or empty" );

		if ( !TYPE_TIME.equals( type ) && !TYPE_SPACE.equals( type ) && !TYPE_CHANNEL.equals( type ) )
			throw new IllegalArgumentException( "Axis type must be '" + TYPE_TIME
					+ "', '" + TYPE_SPACE + "', or '" + TYPE_CHANNEL + "', got: " + type );

		if ( unit == null || unit.trim().isEmpty() )
			throw new IllegalArgumentException( "Axis unit cannot be null or empty" );

		validateBounds();
	}

	/**
	 * Create a time axis
	 */
	public static GeffAxis createTimeAxis( final String name, final String unit, final Double min, final Double max )
	{
		return new GeffAxis( name, TYPE_TIME, unit, min, max );
	}

	/**
	 * Create a space axis
	 */
	public static GeffAxis createSpaceAxis( final String name, final String unit, final Double min, final Double max )
	{
		return new GeffAxis( name, TYPE_SPACE, unit, min, max );
	}

	/**
	 * Create a time axis without bounds
	 */
	public static GeffAxis createTimeAxis( final String name, final String unit )
	{
		return new GeffAxis( name, TYPE_TIME, unit );
	}

	/**
	 * Create a space axis without bounds
	 */
	public static GeffAxis createSpaceAxis( final String name, final String unit )
	{
		return new GeffAxis( name, TYPE_SPACE, unit );
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append( "GeffAxis{" );
		sb.append( "name='" ).append( name ).append( '\'' );
		sb.append( ", type='" ).append( type ).append( '\'' );
		sb.append( ", unit='" ).append( unit ).append( '\'' );

		if ( min != null )
			sb.append( ", min=" ).append( min );

		if ( max != null )
			sb.append( ", max=" ).append( max );

		if ( scale != null )
			sb.append( ", scale=" ).append( scale );

		if ( scaledUnit != null )
			sb.append( ", scaledUnit='" ).append( scaledUnit ).append( '\'' );

		if ( offset != null )
			sb.append( ", offset=" ).append( offset );

		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals( final Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null || getClass() != obj.getClass() )
			return false;

		final GeffAxis geffAxis = ( GeffAxis ) obj;

		return Objects.equals( name, geffAxis.name ) &&
				Objects.equals( type, geffAxis.type ) &&
				Objects.equals( unit, geffAxis.unit ) &&
				Objects.equals( min, geffAxis.min ) &&
				Objects.equals( max, geffAxis.max ) &&
				Objects.equals( scale, geffAxis.scale ) &&
				Objects.equals( scaledUnit, geffAxis.scaledUnit ) &&
				Objects.equals( offset, geffAxis.offset );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( name, type, unit, min, max, scale, scaledUnit, offset );
	}
}
