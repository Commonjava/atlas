package org.apache.maven.graph.effective.traverse.print;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public final class TargetRefPrinter
    implements StructureRelationshipPrinter
{

    public void print( final ProjectRelationship<?> relationship, final StringBuilder builder )
    {
        builder.append( relationship.getTarget()
                                    .asProjectVersionRef() );
    }

}