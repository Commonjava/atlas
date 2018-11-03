package org.commonjava.atlas.npm.ident.ref;

import java.io.Serializable;

/**
 * Created by ruhan on 11/3/18.
 */
public class NpmProjectRef implements Serializable
{
    protected String name;

    public NpmProjectRef()
    {
    }

    public NpmProjectRef( String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;

        NpmProjectRef that = (NpmProjectRef) o;

        return name.equals( that.name );

    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return name;
    }
}
