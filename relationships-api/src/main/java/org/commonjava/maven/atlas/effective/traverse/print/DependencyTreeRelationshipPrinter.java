package org.commonjava.maven.atlas.effective.traverse.print;

import org.commonjava.maven.atlas.common.RelationshipType;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

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
