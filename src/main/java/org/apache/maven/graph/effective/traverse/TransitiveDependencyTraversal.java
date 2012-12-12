/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.VersionlessArtifactRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class TransitiveDependencyTraversal
    extends AncestryTraversal
{

    private final List<ArtifactRef> artifacts = new ArrayList<ArtifactRef>();

    private final Set<VersionlessArtifactRef> seen = new HashSet<VersionlessArtifactRef>();

    private final DependencyScope scope;

    public TransitiveDependencyTraversal()
    {
        this( DependencyScope.runtime );
    }

    public TransitiveDependencyTraversal( final DependencyScope scope )
    {
        this.scope = scope;
    }

    @Override
    public TraversalType getType( final int pass )
    {
        if ( pass == 0 )
        {
            return super.getType( pass );
        }

        return TraversalType.breadth_first;
    }

    public List<ArtifactRef> getArtifacts()
    {
        return Collections.unmodifiableList( artifacts );
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        if ( pass == 0 )
        {
            return super.traverseEdge( relationship, path, pass );
        }
        else
        {
            if ( relationship instanceof DependencyRelationship )
            {
                final ArtifactRef target = (ArtifactRef) relationship.getTarget();
                final VersionlessArtifactRef versionlessTarget = new VersionlessArtifactRef( target );

                if ( !seen.contains( versionlessTarget )
                    && scope.implies( ( (DependencyRelationship) relationship ).getScope() ) )
                {
                    artifacts.add( target );
                    seen.add( versionlessTarget );
                    return true;
                }
            }
            else if ( relationship instanceof ParentRelationship )
            {
                return true;
            }

            return false;
        }
    }

    @Override
    public void startTraverse( final int pass, final EProjectNet network )
    {
        if ( pass == 0 )
        {
            super.startTraverse( pass, network );
        }
    }

    @Override
    public int getRequiredPasses()
    {
        return 2;
    }
}
