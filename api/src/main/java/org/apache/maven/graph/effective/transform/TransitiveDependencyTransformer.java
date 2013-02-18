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
package org.apache.maven.graph.effective.transform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.VersionlessArtifactRef;
import org.apache.maven.graph.effective.filter.DependencyFilter;
import org.apache.maven.graph.effective.filter.OrFilter;
import org.apache.maven.graph.effective.filter.ParentFilter;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.TraversalType;

public class TransitiveDependencyTransformer
    extends FilteringGraphTransformer
{

    private final Set<VersionlessArtifactRef> seen = new HashSet<VersionlessArtifactRef>();

    public TransitiveDependencyTransformer()
    {
        this( DependencyScope.runtime );
    }

    public TransitiveDependencyTransformer( final DependencyScope scope )
    {
        super( new OrFilter( new DependencyFilter( scope ), new ParentFilter() ) );
    }

    public TransitiveDependencyTransformer( final EProjectKey key )
    {
        this( DependencyScope.runtime, key );
    }

    public TransitiveDependencyTransformer( final DependencyScope scope, final EProjectKey key )
    {
        super( new OrFilter( new DependencyFilter( scope ), new ParentFilter() ), key );
    }

    @Override
    public TraversalType getType( final int pass )
    {
        return TraversalType.breadth_first;
    }

    @Override
    protected boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                          final List<ProjectRelationship<?>> path, final int pass )
    {
        if ( super.shouldTraverseEdge( relationship, path, pass ) )
        {
            if ( relationship instanceof DependencyRelationship )
            {
                final ArtifactRef target = (ArtifactRef) relationship.getTarget();
                final VersionlessArtifactRef versionlessTarget = new VersionlessArtifactRef( target );

                if ( seen.contains( versionlessTarget ) )
                {
                    removeRelationship( relationship );
                }
                else
                {
                    seen.add( versionlessTarget );
                    return true;
                }
            }
            else if ( relationship instanceof ParentRelationship )
            {
                // TODO: What are we supposed to do with these??
                return true;
            }
        }

        return false;
    }

}
