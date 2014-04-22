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

import java.util.Collections;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class NoneFilter
    implements ProjectRelationshipFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final NoneFilter INSTANCE = new NoneFilter();

    private NoneFilter()
    {
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        return false;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

    @Override
    public String getLongId()
    {
        return "NONE";
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public boolean equals( final Object obj )
    {
        return obj instanceof NoneFilter;
    }

    @Override
    public int hashCode()
    {
        return NoneFilter.class.hashCode() + 1;
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
        return false;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        return Collections.emptySet();
    }

}
