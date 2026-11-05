package org.mastodon.geff.imglib2;

public class GeffException extends Exception
{

	private static final long serialVersionUID = 1L;

	public GeffException( final Throwable cause )
	{
		super( cause );
	}

	public GeffException( final String message )
	{
		super( message );
	}

	public GeffException( final String message, final Throwable cause )
	{
		super( message, cause );
	}
}
