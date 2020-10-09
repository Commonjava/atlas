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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.commonjava.atlas.npm.ident.util.NpmVersionUtils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * NpmPackageRef use jsemver Version object. Ref https://github.com/zafarkhaja/jsemver
 *
 * Created by ruhan on 10/17/18.
 */
public class NpmPackageRef extends NpmProjectRef
                implements Externalizable
{
    private Version version;

    private String versionString;

    public NpmPackageRef()
    {
    }

    public NpmPackageRef( final String name, final Version version )
    {
        this( name, version, null );
    }

    public NpmPackageRef( final String name, final String versionString )
    {
        this( name, null, versionString );
    }

    NpmPackageRef( final String name, final Version version, final String versionString )
    {
        super( name );
        this.version = version;
        this.versionString = versionString;
    }

    /**
     * Parses new instance from a string. The expected format is "[name]:[version]".
     *
     * @param nv the string to be parsed
     * @return parsed package ref
     * @throws InvalidNpmRefException when the given string doesn't match the expected format
     */
    public static NpmPackageRef parse( final String nv )
    {
        final String[] parts = nv.split( ":" );
        if ( ( parts.length < 2 ) || isEmpty( parts[0] ) || isEmpty( parts[1] ) )
        {
            throw new InvalidNpmRefException( "NpmPackageRef must contain non-empty name AND version. (Given: '" + nv
                                              + "')" );
        }

        return new NpmPackageRef( parts[0], parts[1] );
    }

    public Version getVersionRaw()
    {
        return version;
    }

    public Version getVersion()
    {
        if ( ( version == null ) && ( versionString != null ) )
        {
            version = NpmVersionUtils.valueOf( versionString );
        }
        return version;
    }

    public String getVersionString()
    {
        if ( ( versionString == null ) && ( version != null ) )
        {
            versionString = version.toString();
        }
        return versionString;
    }

    public String getVersionStringRaw()
    {
        return versionString;
    }

    public NpmPackageRef selectVersion( final String versionString )
    {
        Version version = NpmVersionUtils.valueOf( versionString );
        return selectVersion( version );
    }

    public NpmPackageRef selectVersion( final Version newVersion )
    {
        final Version version = getVersion();
        if ( version.equals( newVersion ) )
        {
            return this;
        }
        return new NpmPackageRef( name, newVersion );
    }


    @Override
    public String toString()
    {
        return getName() + ":" + version;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( ( o == null ) || ( getClass() != o.getClass() ) )
        {
            return false;
        }
        if ( !super.equals( o ) )
        {
            return false;
        }

        boolean result = true;
        NpmPackageRef other = (NpmPackageRef) o;
        try
        {
            if ( getVersion() == null )
            {
                if ( other.getVersion() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersion().equals( other.getVersion() ) )
            {
                result = false;
            }
        }
        catch ( final ParseException e )
        {
            if ( getVersionString() == null )
            {
                if ( other.getVersionString() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionString().equals( other.getVersionString() ) )
            {
                result = false;
            }
        }

        return result;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = ( 31 * result ) + ( ( getVersionString() == null ) ? 0 : getVersionString().hashCode() );
        return result;
    }

    @Override
    public void writeExternal( final ObjectOutput objectOutput ) throws IOException
    {
        objectOutput.writeObject( getName() );
        objectOutput.writeObject( version.toString() );
    }

    @Override
    public void readExternal( final ObjectInput objectInput ) throws IOException, ClassNotFoundException
    {
        this.name = (String) objectInput.readObject();
        this.version = NpmVersionUtils.valueOf( (String) objectInput.readObject() );
    }
}
