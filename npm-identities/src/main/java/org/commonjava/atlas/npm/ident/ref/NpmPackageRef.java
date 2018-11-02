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

import com.github.zafarkhaja.semver.Version;
import org.commonjava.atlas.npm.ident.util.NpmVersionUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * NpmPackageRef use jsemver Version object. Ref https://github.com/zafarkhaja/jsemver
 *
 * Created by ruhan on 10/17/18.
 */
public class NpmPackageRef
                implements Externalizable
{
    private String name;

    private Version version;

    public NpmPackageRef()
    {
    }

    public NpmPackageRef( String name, Version version )
    {
        this.name = name;
        this.version = version;
    }

    public String getName()
    {
        return name;
    }

    public Version getVersion()
    {
        return version;
    }

    @Override
    public String toString()
    {
        return name + ":" + version;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;

        NpmPackageRef that = (NpmPackageRef) o;

        if ( !name.equals( that.name ) )
            return false;
        return version.equals( that.version );

    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public void writeExternal( ObjectOutput objectOutput ) throws IOException
    {
        objectOutput.writeObject( name );
        objectOutput.writeObject( version.toString() );
    }

    @Override
    public void readExternal( ObjectInput objectInput ) throws IOException, ClassNotFoundException
    {
        this.name = (String) objectInput.readObject();
        this.version = NpmVersionUtils.valueOf( (String) objectInput.readObject() );
    }
}
