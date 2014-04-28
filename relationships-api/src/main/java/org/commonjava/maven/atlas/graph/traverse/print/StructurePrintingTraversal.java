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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

// TODO: Replace with getAllRelationships(), map to declaring GAV, and then printStructure based on that.
// Letting the filter in the graph view shape what's in getAllRelationships()...
public class StructurePrintingTraversal
    implements ProjectNetTraversal
{

    //    private final Logger logger = new Logger( getClass() );

    private final ProjectNetTraversal traversal;

    private final StructureRelationshipPrinter relationshipPrinter;

    private final Map<ProjectVersionRef, List<ProjectRelationship<?>>> outboundLinks = new HashMap<ProjectVersionRef, List<ProjectRelationship<?>>>();

    public StructurePrintingTraversal()
    {
        this.traversal = null;
        this.relationshipPrinter = new TargetRefPrinter();
    }

    public StructurePrintingTraversal( final ProjectNetTraversal traversal )
    {
        this.traversal = traversal;
        this.relationshipPrinter = new TargetRefPrinter();
    }

    public StructurePrintingTraversal( final StructureRelationshipPrinter relationshipPrinter )
    {
        this.traversal = null;
        this.relationshipPrinter = relationshipPrinter;
    }

    public StructurePrintingTraversal( final ProjectNetTraversal traversal, final StructureRelationshipPrinter relationshipPrinter )
    {
        this.traversal = traversal;
        this.relationshipPrinter = relationshipPrinter;
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( traversal == null || traversal.traverseEdge( relationship, path, pass ) )
        {
            List<ProjectRelationship<?>> outbound = outboundLinks.get( relationship.getDeclaring() );
            if ( outbound == null )
            {
                outbound = new ArrayList<ProjectRelationship<?>>();
                outboundLinks.put( relationship.getDeclaring(), outbound );
            }

            if ( !outbound.contains( relationship ) )
            {
                outbound.add( relationship );
            }

            return true;
        }

        return false;
    }

    public void printStructure( final ProjectVersionRef from, final Map<String, Set<ProjectVersionRef>> labels,
                                final PrintWriter writer )
    {
        printStructure( from, null, null, "  ", labels, writer );
    }

    public void printStructure( final ProjectVersionRef from, final String indent,
                                final Map<String, Set<ProjectVersionRef>> labels, final PrintWriter writer )
    {
        printStructure( from, null, null, indent, labels, writer );
    }

    public void printStructure( final ProjectVersionRef from, final String header, final String footer,
                                final String indent, final Map<String, Set<ProjectVersionRef>> labels,
                                final PrintWriter writer )
    {
        if ( header != null )
        {
            writer.print( header );
        }

        writer.print( "\n" );
        writer.print( from );

        printLinks( from, writer, indent, 1, labels, new HashSet<ProjectRef>(), new Stack<ProjectVersionRef>() );
        writer.print( "\n" );

        if ( footer != null )
        {
            writer.print( footer );
        }
    }

    private void printLinks( final ProjectVersionRef from, final PrintWriter writer, final String indent,
                             final int depth, final Map<String, Set<ProjectVersionRef>> labels,
                             final Set<ProjectRef> excluded, final Stack<ProjectVersionRef> inPath )
    {
        inPath.push( from );
        final List<ProjectRelationship<?>> outbound = outboundLinks.get( from );
        if ( outbound != null )
        {
            for ( final ProjectRelationship<?> out : outbound )
            {
                final ProjectVersionRef outRef = out.getTarget()
                                              .asProjectVersionRef();
                if ( inPath.contains( outRef ) )
                {
                    continue;
                }

                if ( excluded.contains( outRef ) )
                {
                    continue;
                }

                writer.append( "\n" );

                relationshipPrinter.print( out, null, writer, labels, depth, indent );

                if ( !from.equals( out.getTarget()
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
                                   .asProjectVersionRef(), writer, indent, depth + 1, labels, excluded, inPath );

                    if ( newExcluded != null && !newExcluded.isEmpty() )
                    {
                        excluded.removeAll( newExcluded );
                    }
                }
            }
        }
        inPath.pop();
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        return traversal == null || traversal.preCheck( relationship, path, pass );
    }

    @Override
    public void startTraverse( final int pass, final EProjectNet network )
        throws RelationshipGraphConnectionException
    {
        if ( traversal != null )
        {
            traversal.startTraverse( pass, network );
        }
    }

    @Override
    public void endTraverse( final int pass, final EProjectNet network )
        throws RelationshipGraphConnectionException
    {
        if ( traversal != null )
        {
            traversal.endTraverse( pass, network );
        }
    }

    @Override
    public void edgeTraversed( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( traversal != null )
        {
            traversal.edgeTraversed( relationship, path, pass );
        }
    }

    @Override
    public TraversalType getType( final int pass )
    {
        return traversal == null ? TraversalType.depth_first : traversal.getType( pass );
    }

    @Override
    public int getRequiredPasses()
    {
        return traversal == null ? 1 : traversal.getRequiredPasses();
    }

    @Override
    public TraversalType[] getTraversalTypes()
    {
        return traversal == null ? new TraversalType[] { TraversalType.depth_first } : traversal.getTraversalTypes();
    }

}
