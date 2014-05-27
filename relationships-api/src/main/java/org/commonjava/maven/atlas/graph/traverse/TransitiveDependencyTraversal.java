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

    public List<ArtifactRef> getArtifacts()
    {
        return Collections.unmodifiableList( new ArrayList<ArtifactRef>( artifacts.values() ) );
    }

    @Override
    public boolean shouldTraverseEdge( final ProjectRelationship<?> relationship,
                                       final List<ProjectRelationship<?>> path )
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
