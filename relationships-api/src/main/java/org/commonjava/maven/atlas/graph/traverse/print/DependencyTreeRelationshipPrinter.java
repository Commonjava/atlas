package org.commonjava.maven.atlas.graph.traverse.print;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class DependencyTreeRelationshipPrinter
    implements StructureRelationshipPrinter
{

    @Override
    public void print( final ProjectRelationship<?> relationship, final ProjectVersionRef selectedTarget, final StringBuilder builder,
                       final Map<String, Set<ProjectVersionRef>> labels, final int depth, final String indent )
    {
        for ( int i = 0; i < depth; i++ )
        {
            builder.append( indent );
        }

        final RelationshipType type = relationship.getType();

        ArtifactRef targetArtifact = relationship.getTargetArtifact();
        ProjectVersionRef target = selectedTarget;
        final ProjectVersionRef originalTargetGAV = targetArtifact.asProjectVersionRef();

        if ( selectedTarget == null )
        {
            target = originalTargetGAV;
        }
        else
        {
            targetArtifact = selectedTarget.asArtifactRef( targetArtifact.getTypeAndClassifier() );
        }

        builder.append( targetArtifact );

        final Set<String> localLabels = new HashSet<String>();

        if ( type == RelationshipType.DEPENDENCY )
        {
            final DependencyRelationship dr = (DependencyRelationship) relationship;
            builder.append( ':' )
                   .append( dr.getScope()
                              .name() );

            if ( dr.getTargetArtifact()
                   .isOptional() )
            {
                localLabels.add( "OPTIONAL" );
            }

            //            builder.append( " [idx: " )
            //                   .append( relationship.getIndex() )
            //                   .append( ']' );
        }
        else
        {
            localLabels.add( type.name() );
        }

        boolean hasLabel = false;
        if ( !localLabels.isEmpty() )
        {
            hasLabel = true;
            builder.append( " (" );

            boolean first = true;
            for ( final String label : localLabels )
            {
                if ( first )
                {
                    first = false;
                }
                else
                {
                    builder.append( ", " );
                }

                builder.append( label );
            }
        }

        for ( final Entry<String, Set<ProjectVersionRef>> entry : labels.entrySet() )
        {
            final String label = entry.getKey();
            final Set<ProjectVersionRef> refs = entry.getValue();

            if ( refs.contains( target ) )
            {
                if ( !hasLabel )
                {
                    hasLabel = true;
                    builder.append( " (" );
                }
                else
                {
                    builder.append( ", " );
                }

                builder.append( label );
            }

        }

        if ( hasLabel )
        {
            builder.append( ')' );
        }

        if ( originalTargetGAV != target )
        {
            builder.append( " [was: " )
                   .append( originalTargetGAV )
                   .append( "]" );
        }
    }
}
