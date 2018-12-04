/**
 * Copyright (C) 2012 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.npm.ident.ref;

import java.io.Serializable;

/**
 * Created by ruhan on 11/3/18.
 */
public class NpmProjectRef implements Serializable
{
    private static final long serialVersionUID = -1799733486053972932L;

    protected String name;

    public NpmProjectRef()
    {
    }

    public NpmProjectRef( final String name )
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public NpmProjectRef asNpmProjectRef()
    {
        return NpmProjectRef.class.equals( getClass() ) ? this : new NpmProjectRef( getName() );
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

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
