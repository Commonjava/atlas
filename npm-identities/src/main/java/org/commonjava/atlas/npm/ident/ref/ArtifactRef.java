package org.commonjava.atlas.npm.ident.ref;

import com.github.zafarkhaja.semver.Version;

/**
 * ArtifactRef use jsemver Version object. Ref https://github.com/zafarkhaja/jsemver
 *
 * Created by ruhan on 10/17/18.
 */
public class ArtifactRef
{
    private String name;

    private Version version;

    public ArtifactRef( String name, Version version )
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
        return "ArtifactRef{" + "name='" + name + '\'' + ", version=" + version + '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;

        ArtifactRef that = (ArtifactRef) o;

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
}
