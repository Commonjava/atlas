/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

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

    public String printStructure( final ProjectVersionRef from )
    {
        return printStructure( from, null, null, "  " );
    }

    public String printStructure( final ProjectVersionRef from, final String indent )
    {
        return printStructure( from, null, null, indent );
    }

    public String printStructure( final ProjectVersionRef from, final String header, final String footer, final String indent )
    {
        //        final Set<ProjectRelationship<?>> refs = new HashSet<ProjectRelationship<?>>();
        //        for ( final Map.Entry<ProjectVersionRef, List<ProjectRelationship<?>>> entry : outboundLinks.entrySet() )
        //        {
        //            refs.add( entry.getKey() );
        //            final List<ProjectRelationship<?>> list = entry.getValue();
        //            if ( list != null )
        //            {
        //                refs.addAll( list );
        //            }
        //        }

        //        logger.info( "Printing structure for: %s using %d accumulated project references.", from, refs.size() );

        final StringBuilder builder = new StringBuilder();
        if ( header != null )
        {
            builder.append( header );
        }

        builder.append( "\n" );
        builder.append( from );

        printLinks( from, builder, indent, 1 );
        builder.append( "\n" );

        if ( footer != null )
        {
            builder.append( footer );
        }

        return builder.toString();
    }

    private void printLinks( final ProjectVersionRef from, final StringBuilder builder, final String indent, final int depth )
    {
        final List<ProjectRelationship<?>> outbound = outboundLinks.get( from );
        if ( outbound != null )
        {
            for ( final ProjectRelationship<?> out : outbound )
            {
                builder.append( "\n" );

                for ( int i = 0; i < depth; i++ )
                {
                    builder.append( indent );
                }

                relationshipPrinter.print( out, builder );

                if ( !from.equals( out.getTarget()
                                      .asProjectVersionRef() ) )
                {
                    printLinks( out.getTarget()
                                   .asProjectVersionRef(), builder, indent, depth + 1 );
                }
            }
        }
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        return traversal == null || traversal.preCheck( relationship, path, pass );
    }

    @Override
    public void startTraverse( final int pass )
        throws GraphDriverException
    {
        if ( traversal != null )
        {
            traversal.startTraverse( pass );
        }
    }

    @Override
    public void endTraverse( final int pass )
        throws GraphDriverException
    {
        if ( traversal != null )
        {
            traversal.endTraverse( pass );
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

    @Override
    public boolean isStopped()
    {
        return false;
    }

}
