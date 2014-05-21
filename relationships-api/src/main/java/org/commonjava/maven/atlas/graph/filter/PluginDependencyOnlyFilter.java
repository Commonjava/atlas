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

import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public class PluginDependencyOnlyFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final ProjectRef plugin;

    public PluginDependencyOnlyFilter( final PluginRelationship plugin )
    {
        this( plugin, false, true );
    }

    public PluginDependencyOnlyFilter( final PluginRelationship plugin, final boolean includeManaged,
                                       final boolean includeConcrete )
    {
        super( RelationshipType.PLUGIN_DEP, false, includeManaged, includeConcrete );

        this.plugin = plugin.getTarget()
                            .asProjectRef();
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        final PluginDependencyRelationship pdr = (PluginDependencyRelationship) rel;
        if ( plugin.equals( pdr.getPlugin() ) )
        {
            if ( includeManagedRelationships() && pdr.isManaged() )
            {
                return true;
            }
            else if ( includeConcreteRelationships() && !pdr.isManaged() )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return NoneFilter.INSTANCE;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( plugin == null ) ? 0 : plugin.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final PluginDependencyOnlyFilter other = (PluginDependencyOnlyFilter) obj;
        if ( plugin == null )
        {
            if ( other.plugin != null )
            {
                return false;
            }
        }
        else if ( !plugin.equals( other.plugin ) )
        {
            return false;
        }
        return true;
    }

    @Override
    protected void renderIdAttributes( final StringBuilder sb )
    {
        sb.append( ",plugin:" )
          .append( plugin );
    }

}
