package org.apache.maven.graph.effective.traverse.print;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

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
