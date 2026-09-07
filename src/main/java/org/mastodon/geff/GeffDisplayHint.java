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
package org.mastodon.geff;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Metadata indicating how spatiotemporal axes are displayed by a viewer,
 * corresponding to the {@code geff/display_hints} attribute of the GEFF
 * specification.
 * <p>
 * The hints reference axes by {@link GeffAxis#getName() name}, e.g.
 * <pre>
 * "display_hints": {
 *     "display_horizontal": "x",
 *     "display_vertical": "y",
 *     "display_depth": "z",
 *     "display_time": "t"
 * }
 * </pre>
 * {@code display_horizontal} and {@code display_vertical} are required,
 * {@code display_depth} and {@code display_time} are optional.
 *
 * @see <a href=
 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
 *      specification</a>
 */
public class GeffDisplayHint
{
	@SerializedName( "display_horizontal" )
	private String displayHorizontal;

	@SerializedName( "display_vertical" )
	private String displayVertical;

	@SerializedName( "display_depth" )
	private String displayDepth; // Optional - can be null

	@SerializedName( "display_time" )
	private String displayTime; // Optional - can be null

	/**
	 * Default constructor
	 */
	public GeffDisplayHint()
	{}

	/**
	 * Constructor with the required fields
	 */
	public GeffDisplayHint( String displayHorizontal, String displayVertical )
	{
		this.displayHorizontal = displayHorizontal;
		this.displayVertical = displayVertical;
	}

	/**
	 * Constructor with all fields
	 */
	public GeffDisplayHint( String displayHorizontal, String displayVertical, String displayDepth, String displayTime )
	{
		this.displayHorizontal = displayHorizontal;
		this.displayVertical = displayVertical;
		this.displayDepth = displayDepth;
		this.displayTime = displayTime;
	}

	// Getters and Setters
	public String getDisplayHorizontal()
	{
		return displayHorizontal;
	}

	public void setDisplayHorizontal( String displayHorizontal )
	{
		this.displayHorizontal = displayHorizontal;
	}

	public String getDisplayVertical()
	{
		return displayVertical;
	}

	public void setDisplayVertical( String displayVertical )
	{
		this.displayVertical = displayVertical;
	}

	public String getDisplayDepth()
	{
		return displayDepth;
	}

	public void setDisplayDepth( String displayDepth )
	{
		this.displayDepth = displayDepth;
	}

	public String getDisplayTime()
	{
		return displayTime;
	}

	public void setDisplayTime( String displayTime )
	{
		this.displayTime = displayTime;
	}

	/**
	 * Validate the display hints according to GEFF rules
	 */
	public void validate()
	{
		if ( displayHorizontal == null || displayHorizontal.trim().isEmpty() )
		{ throw new IllegalArgumentException( "display_horizontal cannot be null or empty" ); }

		if ( displayVertical == null || displayVertical.trim().isEmpty() )
		{ throw new IllegalArgumentException( "display_vertical cannot be null or empty" ); }
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "GeffDisplayHint{" );
		sb.append( "displayHorizontal='" ).append( displayHorizontal ).append( '\'' );
		sb.append( ", displayVertical='" ).append( displayVertical ).append( '\'' );
		if ( displayDepth != null )
		{
			sb.append( ", displayDepth='" ).append( displayDepth ).append( '\'' );
		}
		if ( displayTime != null )
		{
			sb.append( ", displayTime='" ).append( displayTime ).append( '\'' );
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

		GeffDisplayHint that = ( GeffDisplayHint ) obj;

		return Objects.equals( displayHorizontal, that.displayHorizontal ) &&
				Objects.equals( displayVertical, that.displayVertical ) &&
				Objects.equals( displayDepth, that.displayDepth ) &&
				Objects.equals( displayTime, that.displayTime );
	}

	@Override
	public int hashCode()
	{
		return Objects.hash( displayHorizontal, displayVertical, displayDepth, displayTime );
	}
}
