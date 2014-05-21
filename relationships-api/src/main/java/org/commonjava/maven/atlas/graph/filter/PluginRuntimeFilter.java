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
package org.commonjava.maven.atlas.graph.filter;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;

public class PluginRuntimeFilter
    implements ProjectRelationshipFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PluginRuntimeFilter()
    {
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        return ( rel instanceof PluginRelationship ) && !( (PluginRelationship) rel ).isManaged();
    }

    // TODO: Optimize to minimize new instance creation...
    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        ProjectRelationshipFilter child;
        if ( parent instanceof PluginRelationship )
        {
            final PluginRelationship plugin = (PluginRelationship) parent;

            child =
                new OrFilter( new DependencyFilter( DependencyScope.runtime ),
                              new PluginDependencyFilter( plugin, true, true ), ParentFilter.EXCLUDE_TERMINAL_PARENTS );
        }
        else
        {
            child = NoneFilter.INSTANCE;
        }

        return child;
    }

    @Override
    public String getLongId()
    {
        return "PLUGIN-RUNTIME";
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public String getCondensedId()
    {
        return getLongId();
    }

    @Override
    public boolean includeManagedRelationships()
    {
        return false;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return true;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        final Set<RelationshipType> result = new HashSet<RelationshipType>();
        result.add( RelationshipType.PLUGIN );

        // for descendants
        result.add( RelationshipType.PARENT );
        result.add( RelationshipType.DEPENDENCY );
        result.add( RelationshipType.PLUGIN_DEP );

        return result;
    }

}
