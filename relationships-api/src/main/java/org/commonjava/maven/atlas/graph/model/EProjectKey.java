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
package org.commonjava.maven.atlas.graph.model;

import java.io.Serializable;
import java.net.URI;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class EProjectKey
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final URI source;

    private final ProjectVersionRef project;

    public EProjectKey( final URI source, final ProjectVersionRef project )
    {
        this.project = project;
        this.source = source;
    }

    public final URI getSource()
    {
        return source;
    }

    public final ProjectVersionRef getProject()
    {
        return project;
    }

    public String renderKey()
    {
        return project.toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( project == null ) ? 0 : project.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final EProjectKey other = (EProjectKey) obj;
        if ( project == null )
        {
            if ( other.project != null )
            {
                return false;
            }
        }
        else if ( !project.equals( other.project ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "EProjectKey [source=%s, project=%s]", source, project );
    }

}
