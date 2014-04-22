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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ListPrinter
{

    //    private final Logger logger = new Logger( getClass() );

    private final StructureRelationshipPrinter relationshipPrinter;

    //    private final Set<ProjectRef> seen = new HashSet<ProjectRef>();

    public ListPrinter()
    {
        this.relationshipPrinter = new TargetRefPrinter();
    }

    public ListPrinter( final StructureRelationshipPrinter relationshipPrinter )
    {
        this.relationshipPrinter = relationshipPrinter;
    }

    public String printStructure( final ProjectVersionRef from, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                                  final Map<String, Set<ProjectVersionRef>> labels )
    {
        return printStructure( from, links, null, null, labels );
    }

    public String printStructure( final ProjectVersionRef from, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                                  final String header, final String footer, final Map<String, Set<ProjectVersionRef>> labels )
    {
        final StringBuilder builder = new StringBuilder();
        if ( header != null )
        {
            builder.append( header );
        }

        builder.append( "  " );
        relationshipPrinter.printProjectVersionRef( from, builder, null, labels, null );
        //      builder.append( from );

        final Set<String> lines = new LinkedHashSet<String>();

        printLinks( from, lines, links, labels, new HashSet<ProjectRef>() );

        final List<String> sorted = new ArrayList<String>( lines );
        Collections.sort( sorted );

        for ( final String line : sorted )
        {
            builder.append( "\n  " )
                   .append( line );
        }
        builder.append( "\n" );

        if ( footer != null )
        {
            builder.append( footer );
        }

        return builder.toString();
    }

    private void printLinks( final ProjectVersionRef from, final Set<String> lines, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                             final Map<String, Set<ProjectVersionRef>> labels, final Set<ProjectRef> excluded )
    {

        final List<ProjectRelationship<?>> outbound = links.get( from );
        if ( outbound != null )
        {
            final StringBuilder builder = new StringBuilder();
            for ( final ProjectRelationship<?> out : outbound )
            {
                if ( excluded.contains( out.getTarget()
                                           .asProjectVersionRef() ) )
                {
                    continue;
                }
                // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
                //                else if ( !seen.add( out.getTarget()
                //                                        .asProjectRef() ) )
                //                {
                //                    return;
                //                }

                builder.setLength( 0 );

                relationshipPrinter.print( out, null, builder, labels, 0, "" );
                lines.add( builder.toString() );

                if ( !from.equals( out.getTarget()
                                      .asProjectRef() ) )
                {
                    Set<ProjectRef> newExcluded = null;
                    if ( out instanceof DependencyRelationship )
                    {
                        final Set<ProjectRef> excludes = ( (DependencyRelationship) out ).getExcludes();
                        if ( excludes != null && !excludes.isEmpty() )
                        {
                            newExcluded = new HashSet<ProjectRef>();
                            for ( final ProjectRef ref : excludes )
                            {
                                if ( !RelationshipUtils.isExcluded( ref, excluded ) )
                                {
                                    newExcluded.add( ref );
                                    excluded.add( ref );
                                }
                            }
                        }
                    }

                    printLinks( out.getTarget()
                                   .asProjectVersionRef(), lines, links, labels, excluded );

                    if ( newExcluded != null && !newExcluded.isEmpty() )
                    {
                        excluded.removeAll( newExcluded );
                    }
                }
            }
        }
    }

}
