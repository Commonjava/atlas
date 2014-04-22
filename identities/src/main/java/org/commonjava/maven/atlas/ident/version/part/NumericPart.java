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
import java.math.BigInteger;

public class NumericPart
    extends VersionPart
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    public static final NumericPart ZERO = new NumericPart( 0 );

    private final BigInteger value;

    public NumericPart( final String value )
    {
        this.value = BigInteger.valueOf( Long.parseLong( value ) );
    }

    public NumericPart( final long value )
    {
        this.value = BigInteger.valueOf( value );
    }

    @Override
    public String renderStandard()
    {
        return value.toString();
    }

    public BigInteger getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.format( "NUM[%s]", value );
    }

    public int compareTo( final VersionPart part )
    {
        // 1.2.2 > 1.2.GA, 1.2.1 > 1.2.M1
        if ( part instanceof StringPart )
        {
            // Let the StringPart compareTo(..) method do the heavy lifting.
            return -1 * ( (StringPart) part ).compareTo( this );
        }
        // 1.2.1 > 1.2-SNAPSHOT, 1.2[.0] > 1.2-SNAPSHOT
        else if ( part instanceof SnapshotPart )
        {
            return 1;
        }
        else if ( part instanceof NumericPart )
        {
            final BigInteger other = ( (NumericPart) part ).getValue();
            return value.compareTo( other );
        }

        // punt...shouldn't happen.
        return 0;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + value.hashCode();
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
        final NumericPart other = (NumericPart) obj;
        return value.equals( other.value );
    }

}
