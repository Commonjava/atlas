/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.traverse.print;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class TreePrinter
{

    //    private final Logger logger = new Logger( getClass() );

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

    public String printStructure( final ProjectVersionRef from, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                                  final Map<String, Set<ProjectVersionRef>> labels )
    {
        return printStructure( from, links, null, null, "  ", labels );
    }

    public String printStructure( final ProjectVersionRef from, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                                  final String indent, final Map<String, Set<ProjectVersionRef>> labels )
    {
        return printStructure( from, links, null, null, indent, labels );
    }

    public String printStructure( final ProjectVersionRef from, final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links,
                                  final String header, final String footer, final String indent, final Map<String, Set<ProjectVersionRef>> labels )
    {
        final StringBuilder builder = new StringBuilder();
        if ( header != null )
        {
            builder.append( header );
        }

        builder.append( "\n" );
        relationshipPrinter.printProjectVersionRef( from, builder, null, labels, null );
        //        builder.append( from );

        printLinks( from, builder, indent, 1, links, labels, new HashSet<ProjectRef>() );
        builder.append( "\n" );

        if ( footer != null )
        {
            builder.append( footer );
        }

        return builder.toString();
    }

    private void printLinks( final ProjectVersionRef from, final StringBuilder builder, final String indent, final int depth,
                             final Map<ProjectVersionRef, List<ProjectRelationship<?>>> links, final Map<String, Set<ProjectVersionRef>> labels,
                             final Set<ProjectRef> excluded )
    {
        selected.put( from.asProjectRef(), from );

        final List<ProjectRelationship<?>> outbound = links.get( from );
        if ( outbound != null )
        {
            for ( final ProjectRelationship<?> out : outbound )
            {
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

                builder.append( "\n" );

                final ProjectVersionRef selection = selected.get( out.getTarget()
                                                                     .asProjectRef() );

                if ( selection == null )
                {
                    selected.put( out.getTarget()
                                     .asProjectRef(), selection );
                }

                relationshipPrinter.print( out, selection, builder, labels, depth, indent );

                if ( ( selection == null || selection.equals( out.getTarget()
                                                                 .asProjectVersionRef() ) ) && !from.equals( out.getTarget()
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
                                   .asProjectVersionRef(), builder, indent, depth + 1, links, labels, excluded );

                    if ( newExcluded != null && !newExcluded.isEmpty() )
                    {
                        excluded.removeAll( newExcluded );
                    }
                }
            }
        }
    }

}
