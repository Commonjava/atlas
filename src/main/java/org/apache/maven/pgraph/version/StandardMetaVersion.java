package org.apache.maven.pgraph.version;

public enum StandardMetaVersion
{

    SNAPSHOT;

    private String versionString;

    private StandardMetaVersion()
    {
    }

    private StandardMetaVersion( final String versionString )
    {
        this.versionString = versionString;
    }

    public String versionString()
    {
        return versionString == null ? name() : versionString;
    }

}
