package org.commonjava.maven.atlas.effective.traverse.print;

import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

public final class TargetRefPrinter
    implements StructureRelationshipPrinter
{

    public void print( final ProjectRelationship<?> relationship, final StringBuilder builder )
    {
        builder.append( relationship.getTarget()
                                    .asProjectVersionRef() );
    }

}