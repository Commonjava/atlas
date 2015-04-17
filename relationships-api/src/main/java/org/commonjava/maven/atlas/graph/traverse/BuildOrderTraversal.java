/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.atlas.graph.traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

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
        super( new OrFilter( filter, ParentFilter.EXCLUDE_TERMINAL_PARENTS ) );
    }

    public BuildOrder getBuildOrder()
    {
        return new BuildOrder( order, cycles );
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path )
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
    public void endTraverse( final RelationshipGraph graph )
        throws RelationshipGraphConnectionException
    {
        super.endTraverse( graph );

        Set<EProjectCycle> cycles = graph.getCycles();
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
