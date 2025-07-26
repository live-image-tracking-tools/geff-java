package org.mastodon.geff.geom;

public class GeffSerializableVertex
{
    final double x;

    final double y;

    public GeffSerializableVertex( final double x, final double y )
    {
        this.x = x;
        this.y = y;
    }

    public double[] getCoordinates()
    {
        return new double[] { x, y };
    }
}
