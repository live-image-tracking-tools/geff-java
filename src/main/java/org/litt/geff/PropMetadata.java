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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents metadata for a property in the GEFF format. This class corresponds
 * to the PropMetadata schema in the GEFF v1 specification.
 * <p>
 * Reading and writing are performed on the raw property metadata map: known
 * keys are parsed into the typed fields of this class, and unknown keys are
 * preserved as-is (see {@link #getUnknownAttributes()}) so that files written
 * by future versions of the specification round-trip losslessly. Use
 * {@link #fromMap(Map, String)} and {@link #toMap()} for this; serializing this
 * class directly with Gson would drop the unknown keys.
 */
public class PropMetadata
{
	private String identifier;

	private String dtype;

	private Boolean varlength;

	private String unit;

	private String name;

	private String description;

	/**
	 * The attributes of the property metadata map that this class does not know
	 * about (i.e. attributes introduced by a version of the specification newer
	 * than this library). Never {@code null}; may be modified through
	 * {@link #getUnknownAttributes()}.
	 */
	private final Map< String, Object > unknownAttributes = new LinkedHashMap<>();

	/**
	 * Default constructor
	 */
	public PropMetadata()
	{}

	/**
	 * Constructor with all fields
	 */
	public PropMetadata( final String identifier, final String dtype, final Boolean varlength, final String unit, final String name, final String description )
	{
		this.identifier = identifier;
		this.dtype = dtype;
		this.varlength = varlength;
		this.unit = unit;
		this.name = name;
		this.description = description;
	}

	/**
	 * Creates a {@code PropMetadata} from the raw map found in the props
	 * metadata attribute of a geff file. Known keys are parsed into the typed
	 * fields of this class; unknown keys are preserved in
	 * {@link #getUnknownAttributes()}.
	 *
	 * @param map
	 *            the raw property metadata map, or {@code null} for an empty
	 *            instance
	 * @param fallbackIdentifier
	 *            the identifier to use when the map does not define one,
	 *            normally the map key of this property in the props metadata
	 *            attribute
	 * @return a new {@code PropMetadata}
	 */
	public static PropMetadata fromMap( final Map< String, Object > map, final String fallbackIdentifier )
	{
		final PropMetadata metadata = new PropMetadata();
		if ( map == null )
		{
			metadata.identifier = fallbackIdentifier;
			return metadata;
		}
		for ( final Map.Entry< String, Object > entry : map.entrySet() )
		{
			switch ( entry.getKey() )
			{
			case "identifier":
				metadata.identifier = cast( entry.getValue(), String.class, "identifier" );
				break;
			case "dtype":
				metadata.dtype = cast( entry.getValue(), String.class, "dtype" );
				break;
			case "varlength":
				metadata.varlength = cast( entry.getValue(), Boolean.class, "varlength" );
				break;
			case "unit":
				metadata.unit = cast( entry.getValue(), String.class, "unit" );
				break;
			case "name":
				metadata.name = cast( entry.getValue(), String.class, "name" );
				break;
			case "description":
				metadata.description = cast( entry.getValue(), String.class, "description" );
				break;
			default:
				metadata.unknownAttributes.put( entry.getKey(), entry.getValue() );
				break;
			}
		}
		if ( metadata.identifier == null )
			metadata.identifier = fallbackIdentifier;
		return metadata;
	}

	private static < T > T cast( final Object value, final Class< T > type, final String key )
	{
		if ( value == null )
			return null;
		if ( type.isInstance( value ) )
			return type.cast( value );
		if ( value instanceof Number && type == Double.class )
			return type.cast( ( ( Number ) value ).doubleValue() );
		if ( value instanceof Number && type == Integer.class )
			return type.cast( Integer.valueOf( ( ( Number ) value ).intValue() ) );
		return null; // unconvertible; report as absent
	}

	/**
	 * Returns this metadata as a raw map suitable for the props metadata
	 * attribute of a geff file. Known keys are written in specification order,
	 * with {@code null} values omitted, followed by the unknown keys preserved
	 * from {@link #fromMap(Map, String) reading}.
	 *
	 * @return a new ordered map
	 */
	public Map< String, Object > toMap()
	{
		final Map< String, Object > map = new LinkedHashMap<>();
		if ( identifier != null )
			map.put( "identifier", identifier );
		if ( dtype != null )
			map.put( "dtype", dtype );
		if ( varlength != null )
			map.put( "varlength", varlength );
		if ( unit != null )
			map.put( "unit", unit );
		if ( name != null )
			map.put( "name", name );
		if ( description != null )
			map.put( "description", description );
		map.putAll( unknownAttributes );
		return map;
	}

	// Getters and Setters
	public String getIdentifier()
	{
		return identifier;
	}

	public void setIdentifier( final String identifier )
	{
		this.identifier = identifier;
	}

	public String getDtype()
	{
		return dtype;
	}

	public void setDtype( final String dtype )
	{
		this.dtype = dtype;
	}

	public Boolean getVarlength()
	{
		return varlength;
	}

	public void setVarlength( final Boolean varlength )
	{
		this.varlength = varlength;
	}

	public String getUnit()
	{
		return unit;
	}

	public void setUnit( final String unit )
	{
		this.unit = unit;
	}

	public String getName()
	{
		return name;
	}

	public void setName( final String name )
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription( final String description )
	{
		this.description = description;
	}

	/**
	 * Returns the attributes of the property metadata map that this class does
	 * not know about (i.e. attributes introduced by a version of the
	 * specification newer than this library). They are preserved on read by
	 * {@link #fromMap(Map, String)} and written back unchanged by
	 * {@link #toMap()}. The returned map is writable: entries added there are
	 * written by {@link #toMap()}.
	 *
	 * @return the unknown attributes, never {@code null}.
	 */
	public Map< String, Object > getUnknownAttributes()
	{
		return unknownAttributes;
	}

	@Override
	public String toString()
	{
		return String.format(
				"PropMetadata{identifier='%s', dtype='%s', varlength=%s, unit='%s', name='%s', description='%s', unknownAttributes=%s}",
				identifier, dtype, varlength, unit, name, description, unknownAttributes );
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;
		final PropMetadata that = ( PropMetadata ) o;
		return Objects.equals( identifier, that.identifier ) &&
				Objects.equals( dtype, that.dtype ) &&
				Objects.equals( varlength, that.varlength ) &&
				Objects.equals( unit, that.unit ) &&
				Objects.equals( name, that.name ) &&
				Objects.equals( description, that.description ) &&
				Objects.equals( unknownAttributes, that.unknownAttributes );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( identifier, dtype, varlength, unit, name, description, unknownAttributes );
	}
}
