package org.mastodon.geff;

class GeffUtil
{
	static void checkSupportedVersion( final String version ) throws IllegalArgumentException
	{
		if ( !( version.startsWith( "0.2" ) || version.startsWith( "0.3" ) ) )
		{
			throw new IllegalArgumentException( "geff_version " + version + " not supported." );
		}
	}

	private GeffUtil()
	{
		// static utility methods. don't instantiate.
	}
}
