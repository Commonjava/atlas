/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.traverse.print;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class TargetRefPrinter
    implements StructureRelationshipPrinter
{

    @Override
    public void print( final ProjectRelationship<?> relationship, final ProjectVersionRef selectedTarget,
                       final PrintWriter writer,
                       final Map<String, Set<ProjectVersionRef>> labels, final int depth, final String indent )
    {
        for ( int i = 0; i < depth; i++ )
        {
            writer.print( indent );
        }

        final ProjectVersionRef originalTarget = relationship.getTarget()
                                                             .asProjectVersionRef();
        final ProjectVersionRef target = selectedTarget == null ? originalTarget : selectedTarget;

        printProjectVersionRef( target, writer, null, labels, null );

        if ( target != originalTarget )
        {
            writer.print( " [was: " );
            writer.print( originalTarget );
            writer.print( "]" );
        }
    }

    @Override
    public void printProjectVersionRef( final ProjectVersionRef target, final PrintWriter writer,
                                        final String targetSuffix,
                                        final Map<String, Set<ProjectVersionRef>> labels, final Set<String> localLabels )
    {
        writer.print( target );
        if ( targetSuffix != null )
        {
            writer.print( targetSuffix );
        }

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
                    writer.print( " (" );
                }
                else
                {
                    writer.print( ", " );
                }

                writer.print( label );
            }

        }

        if ( hasLabel )
        {
            writer.print( ')' );
        }
    }

}
