package org.apache.maven.graph.effective.rel;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;

public final class ParentRelationship
    extends AbstractProjectRelationship<ProjectVersionRef>
{

    public ParentRelationship( final ProjectVersionRef declaring, final ProjectVersionRef target )
    {
        super( RelationshipType.PARENT, declaring, target, 0 );
    }

    @Override
    public String toString()
    {
        return String.format( "ParentRelationship [%s => %s]", getDeclaring(), getTarget() );
    }

}
