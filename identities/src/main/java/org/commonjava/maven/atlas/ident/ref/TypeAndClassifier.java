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
package org.commonjava.maven.atlas.ident.ref;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class TypeAndClassifier
{
    private final String type;

    private final String classifier;

    public TypeAndClassifier( final String type, final String classifier )
    {
        this.type = type == null ? "jar" : type;
        this.classifier = isEmpty( classifier ) ? null : classifier;
    }

    public TypeAndClassifier( final String type )
    {
        this( type, null );
    }

    public TypeAndClassifier()
    {
        this( null, null );
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    @Override
    public String toString()
    {
        return String.format( "%s%s", type, ( classifier == null ? "" : ":" + classifier ) );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( classifier == null ) ? 0 : classifier.hashCode() );
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
        final TypeAndClassifier other = (TypeAndClassifier) obj;
        if ( classifier == null )
        {
            if ( other.classifier != null )
            {
                return false;
            }
        }
        else if ( !classifier.equals( other.classifier ) )
        {
            return false;
        }
        if ( type == null )
        {
            if ( other.type != null )
            {
                return false;
            }
        }
        else if ( !type.equals( other.type ) )
        {
            return false;
        }
        return true;
    }

}
