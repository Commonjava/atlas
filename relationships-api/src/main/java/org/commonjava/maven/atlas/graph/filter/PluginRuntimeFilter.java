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
package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
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
                new OrFilter( new DependencyFilter( DependencyScope.runtime ), new PluginDependencyFilter( plugin, true, true ),
                              ParentFilter.EXCLUDE_TERMINAL_PARENTS );
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

}
