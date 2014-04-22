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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;

public class AnyFilter
    implements ProjectRelationshipFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final AnyFilter INSTANCE = new AnyFilter();

    private AnyFilter()
    {
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        return true;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

    @Override
    public String getLongId()
    {
        return "ANY";
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public boolean equals( final Object obj )
    {
        return obj instanceof AnyFilter;
    }

    @Override
    public int hashCode()
    {
        return AnyFilter.class.hashCode() + 1;
    }

    @Override
    public String getCondensedId()
    {
        return getLongId();
    }

    @Override
    public boolean includeManagedRelationships()
    {
        return true;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return true;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        return new HashSet<RelationshipType>( Arrays.asList( RelationshipType.values() ) );
    }

}
