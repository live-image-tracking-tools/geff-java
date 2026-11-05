package org.mastodon.geff.imglib2;

/**
 * Thrown when binding a builder to a {@link GeffProperties} collection fails:
 * e.g. no or multiple constructor methods, an unsupported parameter type, or a
 * property that cannot be matched to a constructor parameter.
 */
public class GeffBindError extends GeffException
{

	private static final long serialVersionUID = 1L;

	public GeffBindError( final String message )
	{
		super( message );
	}

	public GeffBindError( final String message, final Throwable cause )
	{
		super( message, cause );
	}
}
