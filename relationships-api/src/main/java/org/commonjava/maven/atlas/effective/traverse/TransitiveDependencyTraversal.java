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
package org.commonjava.maven.atlas.effective.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.common.DependencyScope;
import org.commonjava.maven.atlas.common.ref.ArtifactRef;
import org.commonjava.maven.atlas.common.ref.VersionlessArtifactRef;
import org.commonjava.maven.atlas.effective.filter.DependencyFilter;
import org.commonjava.maven.atlas.effective.filter.OrFilter;
import org.commonjava.maven.atlas.effective.filter.ParentFilter;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.ParentRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

public class TransitiveDependencyTraversal
    extends AbstractFilteringTraversal
{

    private final List<ArtifactRef> artifacts = new ArrayList<ArtifactRef>();

    private final Set<VersionlessArtifactRef> seen = new HashSet<VersionlessArtifactRef>();

    public TransitiveDependencyTraversal()
    {
        this( DependencyScope.runtime );
    }

    public TransitiveDependencyTraversal( final ProjectRelationshipFilter filter )
    {
        super( filter );
    }

    public TransitiveDependencyTraversal( final DependencyScope scope )
    {
        super( new OrFilter( new DependencyFilter( scope ), new ParentFilter( false ) ) );
    }

    @Override
    public TraversalType getType( final int pass )
    {
        return TraversalType.breadth_first;
    }

    public List<ArtifactRef> getArtifacts()
    {
        return Collections.unmodifiableList( artifacts );
    }

    @Override
    public boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                       final List<ProjectRelationship<?>> path, final int pass )
    {
        boolean result = false;
        if ( relationship instanceof DependencyRelationship )
        {
            final ArtifactRef target = (ArtifactRef) relationship.getTarget();
            final VersionlessArtifactRef versionlessTarget = new VersionlessArtifactRef( target );

            if ( !seen.contains( versionlessTarget ) )
            {
                artifacts.add( target );
                seen.add( versionlessTarget );
                result = true;
            }
        }
        else if ( relationship instanceof ParentRelationship )
        {
            result = true;
        }

        return result;
    }
}
