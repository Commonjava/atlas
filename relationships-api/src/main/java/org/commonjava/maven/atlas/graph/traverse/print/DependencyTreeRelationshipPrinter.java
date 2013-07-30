package org.commonjava.maven.atlas.graph.traverse.print;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class DependencyTreeRelationshipPrinter
    implements StructureRelationshipPrinter
{

    @Override
    public void print( final ProjectRelationship<?> relationship, final StringBuilder builder )
    {
        final RelationshipType type = relationship.getType();
        builder.append( relationship.getTargetArtifact() );
        if ( type == RelationshipType.DEPENDENCY )
        {
            builder.append( ':' )
                   .append( ( (DependencyRelationship) relationship ).getScope()
                                                                     .realName() );

            builder.append( " (" )
                   .append( relationship.getIndex() )
                   .append( ')' );
        }
        else
        {
            builder.append( ":" )
                   .append( type.name() );
        }
    }
}
