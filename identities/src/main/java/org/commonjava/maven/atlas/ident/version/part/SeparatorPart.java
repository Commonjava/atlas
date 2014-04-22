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
package org.commonjava.maven.atlas.ident.version.part;

import java.io.Serializable;

public class SeparatorPart
    extends VersionPart
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final VersionPartSeparator type;

    public SeparatorPart( final VersionPartSeparator type )
    {
        this.type = type;
    }

    @Override
    public String renderStandard()
    {
        return type.getRenderedString();
    }

    public VersionPartSeparator getValue()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return String.format( "SEP[%s]", type.getRenderedString() );
    }

    public int compareTo( final VersionPart o )
    {
        return 0;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
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
        final SeparatorPart other = (SeparatorPart) obj;
        if ( type != other.type )
        {
            return false;
        }
        return true;
    }

}
