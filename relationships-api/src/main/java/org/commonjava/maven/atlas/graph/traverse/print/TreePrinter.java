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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreePrinter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final StructureRelationshipPrinter relationshipPrinter;

    //    private final boolean collapseTransitives;

    //    private final Set<ProjectRef> seen = new HashSet<ProjectRef>();

    private final Map<ProjectRef, ProjectVersionRef> selected = new HashMap<ProjectRef, ProjectVersionRef>();

    public TreePrinter()
    {
        this.relationshipPrinter = new TargetRefPrinter();
        //        this.collapseTransitives = true;
    }

    public TreePrinter( final StructureRelationshipPrinter relationshipPrinter )
    {
        this.relationshipPrinter = relationshipPrinter;
    }

    // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
    //    public TreePrinter( final StructureRelationshipPrinter relationshipPrinter, final boolean collapseTransitives )
    //    {
    //        this.relationshipPrinter = relationshipPrinter;
    //        this.collapseTransitives = collapseTransitives;
    //    }

    public void printStructure( final ProjectVersionRef from,
                                final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                                final Map<String, Set<ProjectVersionRef>> labels, PrintWriter writer )
    {
        printStructure( from, links, null, null, "  ", labels, writer );
    }

    public void printStructure( final ProjectVersionRef from,
                                final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links, final String indent,
                                final Map<String, Set<ProjectVersionRef>> labels, PrintWriter writer )
    {
        printStructure( from, links, null, null, indent, labels, writer );
    }

    public void printStructure( final ProjectVersionRef from,
                                final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links, final String header,
                                final String footer, final String indent,
                                final Map<String, Set<ProjectVersionRef>> labels, PrintWriter writer )
    {
        if ( header != null )
        {
            writer.print( header );
        }

        writer.print( "\n" );
        relationshipPrinter.printProjectVersionRef( from, writer, null, labels, null );
        //        writer.print( from );

        printLinks( from, writer, indent, 1, links, labels, new HashSet<ProjectRef>(), new Stack<ProjectVersionRef>() );
        writer.print( "\n" );

        if ( footer != null )
        {
            writer.print( footer );
        }
    }

    private void printLinks( final ProjectVersionRef from, final PrintWriter writer, final String indent,
                             final int depth, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                             final Map<String, Set<ProjectVersionRef>> labels, final Set<ProjectRef> excluded,
                             final Stack<ProjectVersionRef> inPath )
    {
        inPath.push( from );
        selected.put( from.asProjectRef(), from );

        final List<ProjectRelationship<?>> outbound = links.get( from );
        if ( outbound != null )
        {
            for ( final ProjectRelationship<?> out : outbound )
            {
                ProjectVersionRef outRef = out.getTarget()
                                              .asProjectVersionRef();
                if ( inPath.contains( outRef ) )
                {
                    continue;
                }

                if ( excluded.contains( out.getTarget()
                                           .asProjectVersionRef() ) )
                {
                    continue;
                }

                // TODO: Reinstate transitive collapse IF we can find a way to make output consistent.
                //                else if ( collapseTransitives && !seen.add( out.getTarget()
                //                                                               .asProjectRef() ) )
                //                {
                //                    return;
                //                }

                writer.append( "\n" );

                final ProjectVersionRef selection = selected.get( out.getTarget()
                                                                     .asProjectRef() );

                if ( selection == null )
                {
                    selected.put( out.getTarget()
                                     .asProjectRef(), selection );
                }

                relationshipPrinter.print( out, selection, writer, labels, depth, indent );

                if ( ( selection == null || selection.equals( out.getTarget()
                                                                 .asProjectVersionRef() ) )
                    && !from.equals( out.getTarget()
                                        .asProjectVersionRef() ) )
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
                                   .asProjectVersionRef(), writer, indent, depth + 1, links, labels, excluded, inPath );

                    if ( newExcluded != null && !newExcluded.isEmpty() )
                    {
                        excluded.removeAll( newExcluded );
                    }
                }
            }
        }

        inPath.pop();
    }

}
