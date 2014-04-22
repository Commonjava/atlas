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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class DependencyTreeRelationshipPrinter
    implements StructureRelationshipPrinter
{

    private final Set<ProjectVersionRef> missing;

    public DependencyTreeRelationshipPrinter()
    {
        missing = null;
    }

    public DependencyTreeRelationshipPrinter( final Set<ProjectVersionRef> missing )
    {
        this.missing = missing;
    }

    @Override
    public void print( final ProjectRelationship<?> relationship, final ProjectVersionRef selectedTarget, final StringBuilder builder,
                       final Map<String, Set<ProjectVersionRef>> labels, final int depth, final String indent )
    {
        indent( builder, depth, indent );

        final RelationshipType type = relationship.getType();

        final ProjectVersionRef originalTarget = relationship.getTarget()
                                                             .asProjectVersionRef();

        ProjectVersionRef target = null;
        ArtifactRef targetArtifact = relationship.getTargetArtifact();

        if ( selectedTarget == null )
        {
            target = originalTarget;
        }
        else
        {
            target = selectedTarget;
            targetArtifact = selectedTarget.asArtifactRef( targetArtifact.getTypeAndClassifier() );
        }

        final Set<String> localLabels = new HashSet<String>();

        String suffix = null;
        if ( type == RelationshipType.DEPENDENCY )
        {
            final DependencyRelationship dr = (DependencyRelationship) relationship;
            if ( DependencyScope._import == dr.getScope() /*&& dr.isManaged() && "pom".equals( dr.getType() )*/)
            {
                localLabels.add( "BOM" );
            }
            else
            {
                suffix = ":" + dr.getScope()
                                 .name();
            }

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

        printProjectVersionRef( targetArtifact, builder, suffix, labels, localLabels );

        if ( !target.equals( originalTarget ) )
        {
            builder.append( " [was: " )
                   .append( originalTarget )
                   .append( "]" );
        }

        if ( missing != null && missing.contains( target ) )
        {
            builder.append( '\n' );
            indent( builder, depth + 1, indent );
            builder.append( "???" );
        }
    }

    @Override
    public void printProjectVersionRef( final ProjectVersionRef targetArtifact, final StringBuilder builder, final String targetSuffix,
                                        final Map<String, Set<ProjectVersionRef>> labels, final Set<String> localLabels )
    {
        // the original could be an artifact ref!
        final ProjectVersionRef target = targetArtifact.asProjectVersionRef();

        builder.append( targetArtifact );
        if ( targetSuffix != null )
        {
            builder.append( targetSuffix );
        }

        boolean hasLabel = false;
        if ( localLabels != null && !localLabels.isEmpty() )
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
    }

    private void indent( final StringBuilder builder, final int depth, final String indent )
    {
        for ( int i = 0; i < depth; i++ )
        {
            builder.append( indent );
        }
    }
}
