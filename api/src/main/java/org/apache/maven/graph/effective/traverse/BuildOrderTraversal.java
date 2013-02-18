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
package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectCycle;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.filter.OrFilter;
import org.apache.maven.graph.effective.filter.ParentFilter;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.model.BuildOrder;
import org.apache.maven.graph.spi.GraphDriverException;

public class BuildOrderTraversal
    extends AbstractFilteringTraversal
{

    private final List<ProjectRef> order = new ArrayList<ProjectRef>();

    private Set<EProjectCycle> cycles;

    public BuildOrderTraversal()
    {
    }

    public BuildOrderTraversal( final ProjectRelationshipFilter filter )
    {
        super( new OrFilter( filter, new ParentFilter( false ) ) );
    }

    public BuildOrder getBuildOrder()
    {
        return new BuildOrder( order, cycles );
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
        final ProjectVersionRef decl = relationship.getDeclaring();

        ProjectVersionRef target = relationship.getTarget();
        if ( target instanceof ArtifactRef )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        final ProjectRef baseDecl = new ProjectRef( decl.getGroupId(), decl.getArtifactId() );
        final ProjectRef baseTgt = new ProjectRef( target.getGroupId(), target.getArtifactId() );

        int declIdx = order.indexOf( baseDecl );
        final int tgtIdx = order.indexOf( baseTgt );
        if ( declIdx < 0 )
        {
            declIdx = order.size();
            order.add( baseDecl );
        }

        if ( tgtIdx < 0 )
        {
            order.add( declIdx, baseTgt );
        }

        return true;
    }

    @Override
    public void endTraverse( final int pass, final EProjectNet network )
        throws GraphDriverException
    {
        super.endTraverse( pass, network );

        Set<EProjectCycle> cycles = network.getCycles();
        if ( cycles != null )
        {
            cycles = new HashSet<EProjectCycle>( cycles );
            for ( final Iterator<EProjectCycle> iterator = cycles.iterator(); iterator.hasNext(); )
            {
                final EProjectCycle eProjectCycle = iterator.next();
                ProjectRelationshipFilter filter = getRootFilter();

                boolean include = true;
                for ( final ProjectRelationship<?> rel : eProjectCycle )
                {
                    if ( !filter.accept( rel ) )
                    {
                        include = false;
                        break;
                    }

                    filter = filter.getChildFilter( rel );
                }

                if ( !include )
                {
                    iterator.remove();
                }
            }

        }

        this.cycles = cycles;
    }

}
