package org.mastodon.geff.geom;

import org.mastodon.geff.ZarrEntity;

public class GeffSerializableVertex implements ZarrEntity
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
