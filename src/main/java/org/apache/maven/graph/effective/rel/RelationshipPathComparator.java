package org.apache.maven.graph.effective.rel;

import java.util.Comparator;
import java.util.List;

public class RelationshipPathComparator
    implements Comparator<List<ProjectRelationship<?>>>
{

    private RelationshipComparator comp;

    public int compare( final List<ProjectRelationship<?>> one, final List<ProjectRelationship<?>> two )
    {
        final int commonLen = Math.min( one.size(), two.size() );

        if ( one.size() > commonLen )
        {
            return 1;
        }
        else if ( two.size() > commonLen )
        {
            return -1;
        }

        for ( int i = 0; i < commonLen; i++ )
        {
            final int result = compareRelTypes( one.get( i ), two.get( i ) );
            if ( result != 0 )
            {
                return result;
            }
        }

        for ( int i = 0; i < commonLen; i++ )
        {
            final int result = compareRels( one.get( i ), two.get( i ) );
            if ( result != 0 )
            {
                return result;
            }
        }

        return 0;
    }

    private int compareRels( final ProjectRelationship<?> one, final ProjectRelationship<?> two )
    {
        return comp.compare( one, two );
    }

    private int compareRelTypes( final ProjectRelationship<?> one, final ProjectRelationship<?> two )
    {
        return one.getType()
                  .ordinal() - two.getType()
                                  .ordinal();
    }

}
