package org.commonjava.maven.atlas.effective.traverse.print;

import org.commonjava.maven.atlas.common.RelationshipType;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

public class DependencyTreeRelationshipPrinter
    implements StructureRelationshipPrinter
{

    public void print( final ProjectRelationship<?> relationship, final StringBuilder builder )
    {
        builder.append( relationship.getTargetArtifact() );
        if ( relationship.getType() == RelationshipType.DEPENDENCY )
        {
            builder.append( ':' )
                   .append( ( (DependencyRelationship) relationship ).getScope()
                                                                     .realName() );
        }
        else if ( relationship.getType() == RelationshipType.PARENT )
        {
            builder.append( ":PARENT" );
        }
    }

}
