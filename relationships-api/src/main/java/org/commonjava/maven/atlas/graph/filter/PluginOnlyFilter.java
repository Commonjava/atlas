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

import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class PluginOnlyFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public PluginOnlyFilter()
    {
        this( false, true );
    }

    public PluginOnlyFilter( final boolean includeManaged, final boolean includeConcrete )
    {
        super( RelationshipType.PLUGIN, false, includeManaged, includeConcrete );
    }

    @Override
    public boolean doAccept( final ProjectRelationship<?> rel )
    {
        final PluginRelationship pr = (PluginRelationship) rel;
        if ( includeManagedRelationships() && pr.isManaged() )
        {
            return true;
        }
        else if ( includeConcreteRelationships() && !pr.isManaged() )
        {
            return true;
        }

        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return NoneFilter.INSTANCE;
    }

}
