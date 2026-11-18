package org.litt.geff;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Display hints for GEFF.
 * <p>
 * Metadata indicating how spatiotemporal axes are displayed by a viewer,
 * corresponding to the {@code geff/display_hints} attribute of the GEFF
 * specification.
 * <p>
 * The hints reference axes by {@link GeffAxis#getName() name}, e.g.
 *
 * <pre>
 * "display_hints": {
 *     "display_horizontal": "x",
 *     "display_vertical": "y",
 *     "display_depth": "z",
 *     "display_time": "t"
 * }
 * </pre>
 *
 * {@code display_horizontal} and {@code display_vertical} are required,
 * {@code display_depth} and {@code display_time} are optional.
 *
 * @see <a href=
 *      "https://liveimagetrackingtools.org/geff/latest/specification/">GEFF
 *      specification</a>
 * @author Jean-Yves Tinevez
 * @author Ko Sugawara
 */
public class GeffDisplayHints
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
	public GeffDisplayHints()
	{}

	/**
	 * Constructor with the required fields
	 */
	public GeffDisplayHints( final String displayHorizontal, final String displayVertical )
	{
		this.displayHorizontal = displayHorizontal;
		this.displayVertical = displayVertical;
	}

	/**
	 * Constructor with all fields
	 */
	public GeffDisplayHints( final String displayHorizontal, final String displayVertical, final String displayDepth, final String displayTime )
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

	public void setDisplayHorizontal( final String displayHorizontal )
	{
		this.displayHorizontal = displayHorizontal;
	}

	public String getDisplayVertical()
	{
		return displayVertical;
	}

	public void setDisplayVertical( final String displayVertical )
	{
		this.displayVertical = displayVertical;
	}

	public String getDisplayDepth()
	{
		return displayDepth;
	}

	public void setDisplayDepth( final String displayDepth )
	{
		this.displayDepth = displayDepth;
	}

	public String getDisplayTime()
	{
		return displayTime;
	}

	public void setDisplayTime( final String displayTime )
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
		final StringBuilder sb = new StringBuilder();
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
	public boolean equals( final Object obj )
	{
		if ( this == obj )
			return true;
		if ( obj == null || getClass() != obj.getClass() )
			return false;

		final GeffDisplayHints that = ( GeffDisplayHints ) obj;

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
