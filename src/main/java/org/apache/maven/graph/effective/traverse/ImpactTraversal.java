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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class ImpactTraversal
    extends AbstractTraversal
{

    private final Map<ProjectVersionRef, Set<List<ProjectRelationship<?>>>> impactedPaths =
        new HashMap<ProjectVersionRef, Set<List<ProjectRelationship<?>>>>();

    private final boolean includeManagedInfo;

    public ImpactTraversal( final ProjectVersionRef... targets )
    {
        this( false, targets );
    }

    public ImpactTraversal( final boolean includeManagedInfo, final ProjectVersionRef... targets )
    {
        this.includeManagedInfo = includeManagedInfo;
        for ( final ProjectVersionRef target : targets )
        {
            impactedPaths.put( target, new HashSet<List<ProjectRelationship<?>>>() );
        }
    }

    public ImpactTraversal( final Set<ProjectVersionRef> targets )
    {
        this( false, targets );
    }

    public ImpactTraversal( final boolean includeManagedInfo, final Set<ProjectVersionRef> targets )
    {
        this.includeManagedInfo = includeManagedInfo;
        for ( final ProjectVersionRef target : targets )
        {
            impactedPaths.put( target, new HashSet<List<ProjectRelationship<?>>>() );
        }
    }

    public Map<ProjectVersionRef, Set<List<ProjectRelationship<?>>>> getImpactedPaths()
    {
        return impactedPaths;
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        if ( !includeManagedInfo )
        {
            if ( relationship instanceof DependencyRelationship
                && ( (DependencyRelationship) relationship ).isManaged() )
            {
                return false;
            }

            if ( relationship instanceof PluginRelationship && ( (PluginRelationship) relationship ).isManaged() )
            {
                return false;
            }
        }

        ProjectVersionRef target = relationship.getTarget();
        if ( target instanceof ArtifactRef )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        final Set<List<ProjectRelationship<?>>> paths = impactedPaths.get( target );
        if ( paths != null )
        {
            if ( !path.isEmpty() )
            {
                final ArrayList<ProjectRelationship<?>> p = new ArrayList<ProjectRelationship<?>>( path );
                p.add( relationship );

                paths.add( p );
            }

            // we've seen an impact target, we don't need to go further.
            return false;
        }

        // we may yet encounter the impact targets, so allow this traverse to proceed.
        return true;
    }

}
