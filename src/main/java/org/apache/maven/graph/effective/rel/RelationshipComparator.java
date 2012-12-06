package org.apache.maven.graph.effective.rel;

import java.util.Comparator;

public class RelationshipComparator
    implements Comparator<ProjectRelationship>
{

    public int compare( final ProjectRelationship one, final ProjectRelationship two )
    {
        if ( one.getType() == two.getType() )
        {
            return one.getIndex() - two.getIndex();
        }
        else
        {
            return one.getType()
                      .ordinal() - two.getType()
                                      .ordinal();
        }
    }

}
