package org.mastodon.geff.bind;

import org.mastodon.geff.props.GeffPropertyType;

/**
 * Thrown when a property cannot be converted to the type expected by a
 * constructor parameter: e.g. an optional source property bound to a
 * non-optional target type, or a dimensionality / var-length mismatch.
 */
public class GeffConvertError extends GeffBindError
{

	private static final long serialVersionUID = 1L;

	public GeffConvertError( final GeffPropertyType sourceType, final GeffPropertyType targetType, final String explanation )
	{
		super( "Cannot convert " + sourceType + " to " + targetType + ": " + explanation );
	}
}
