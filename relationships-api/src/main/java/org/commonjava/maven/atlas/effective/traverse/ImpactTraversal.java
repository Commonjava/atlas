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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.PluginRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;

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
        if ( !preCheck( relationship, path, pass ) )
        {
            return false;
        }

        final ProjectVersionRef target = relationship.getTarget()
                                                     .asProjectVersionRef();
        final Set<List<ProjectRelationship<?>>> paths = impactedPaths.get( target );
        final ArrayList<ProjectRelationship<?>> p = new ArrayList<ProjectRelationship<?>>( path );
        p.add( relationship );

        paths.add( p );

        // we may yet encounter the impact targets, so allow this traverse to proceed.
        return true;
    }

    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
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

        final Set<List<ProjectRelationship<?>>> paths = impactedPaths.get( relationship.getTarget()
                                                                                       .asProjectVersionRef() );
        if ( paths != null && paths.isEmpty() )
        {
            // we've seen an impact target, we don't need to go further.

            // TODO: huh??
            return false;
        }

        return true;
    }

}
