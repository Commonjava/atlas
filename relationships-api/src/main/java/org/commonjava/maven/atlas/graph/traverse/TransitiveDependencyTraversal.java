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
package org.commonjava.maven.atlas.graph.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ParentFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitiveDependencyTraversal
    extends AbstractFilteringTraversal
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<VersionlessArtifactRef, ArtifactRef> artifacts = new HashMap<VersionlessArtifactRef, ArtifactRef>();

    private final Map<VersionlessArtifactRef, Integer> seenArtifacts = new HashMap<VersionlessArtifactRef, Integer>();

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
        super( new OrFilter( new DependencyFilter( scope ), ParentFilter.EXCLUDE_TERMINAL_PARENTS ) );
    }

    @Override
    public TraversalType getType( final int pass )
    {
        return TraversalType.breadth_first;
    }

    public List<ArtifactRef> getArtifacts()
    {
        return Collections.unmodifiableList( new ArrayList<ArtifactRef>( artifacts.values() ) );
    }

    @Override
    public boolean shouldTraverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        boolean result = false;
        if ( relationship instanceof DependencyRelationship )
        {
            final ArtifactRef target = (ArtifactRef) relationship.getTarget();
            final VersionlessArtifactRef versionlessTarget = new VersionlessArtifactRef( target );

            logger.debug( "Checking for seen versionless GA[TC]: {}", versionlessTarget );
            final Integer distance = seenArtifacts.get( versionlessTarget );
            if ( distance == null || distance > path.size() )
            {
                logger.debug( "Adding: {} ({})", target, versionlessTarget );
                artifacts.put( versionlessTarget, target );
                seenArtifacts.put( versionlessTarget, path.size() );
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
