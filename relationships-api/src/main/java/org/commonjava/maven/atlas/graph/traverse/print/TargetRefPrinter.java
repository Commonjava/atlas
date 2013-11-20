package org.commonjava.maven.atlas.graph.traverse.print;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class TargetRefPrinter
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

        final ProjectVersionRef originalTarget = relationship.getTarget()
                                                             .asProjectVersionRef();
        final ProjectVersionRef target = selectedTarget == null ? originalTarget : selectedTarget;
        builder.append( target );

        boolean hasLabel = false;
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

        if ( target != originalTarget )
        {
            builder.append( " [was: " )
                   .append( originalTarget )
                   .append( "]" );
        }
    }

}