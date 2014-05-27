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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

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
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path )
    {
        if ( !preCheck( relationship, path ) )
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

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path )
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
