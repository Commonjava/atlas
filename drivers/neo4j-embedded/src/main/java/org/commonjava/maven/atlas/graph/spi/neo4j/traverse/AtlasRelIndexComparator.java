package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.Comparator;

import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Relationship;

public class AtlasRelIndexComparator
    implements Comparator<Relationship>
{

    @Override
    public int compare( final Relationship f, final Relationship s )
    {
        final int fidx = Conversions.getIntegerProperty( Conversions.ATLAS_RELATIONSHIP_INDEX, f, 0 );
        final int sidx = Conversions.getIntegerProperty( Conversions.ATLAS_RELATIONSHIP_INDEX, s, 0 );

        final int comp = fidx - sidx;
        if ( comp < 0 )
        {
            return -1;
        }
        else if ( comp > 0 )
        {
            return 1;
        }

        return comp;
    }

}
