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

import java.util.Objects;

/**
 * Represents metadata for a property in the GEFF format. This class corresponds
 * to the PropMetadata schema in the GEFF v1 specification.
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
	 * Default constructor
	 */
	public PropMetadata()
	{}

	/**
	 * Constructor with all fields
	 */
	public PropMetadata( String identifier, String dtype, Boolean varlength, String unit, String name, String description )
	{
		this.identifier = identifier;
		this.dtype = dtype;
		this.varlength = varlength;
		this.unit = unit;
		this.name = name;
		this.description = description;
	}

	// Getters and Setters
	public String getIdentifier()
	{
		return identifier;
	}

	public void setIdentifier( String identifier )
	{
		this.identifier = identifier;
	}

	public String getDtype()
	{
		return dtype;
	}

	public void setDtype( String dtype )
	{
		this.dtype = dtype;
	}

	public Boolean getVarlength()
	{
		return varlength;
	}

	public void setVarlength( Boolean varlength )
	{
		this.varlength = varlength;
	}

	public String getUnit()
	{
		return unit;
	}

	public void setUnit( String unit )
	{
		this.unit = unit;
	}

	public String getName()
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription( String description )
	{
		this.description = description;
	}

	@Override
	public String toString()
	{
		return String.format(
				"PropMetadata{identifier='%s', dtype='%s', varlength=%s, unit='%s', name='%s', description='%s'}",
				identifier, dtype, varlength, unit, name, description );
	}

	@Override
	public boolean equals( Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;
		PropMetadata that = ( PropMetadata ) o;
		return Objects.equals( identifier, that.identifier ) &&
				Objects.equals( dtype, that.dtype ) &&
				Objects.equals( varlength, that.varlength ) &&
				Objects.equals( unit, that.unit ) &&
				Objects.equals( name, that.name ) &&
				Objects.equals( description, that.description );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( identifier, dtype, varlength, unit, name, description );
	}
}
