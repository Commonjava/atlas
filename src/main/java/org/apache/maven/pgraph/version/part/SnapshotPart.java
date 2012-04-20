package org.apache.maven.pgraph.version.part;

public class SnapshotPart
    implements VersionPart<String>
{

    private final String value;

    public SnapshotPart( final String value )
    {
        this.value = value;
    }

    public boolean isLocalSnapshot()
    {
        return value == null;
    }

    @Override
    public String toString()
    {
        return "SNAP[" + ( value == null ? "local" : "remote;" + value ) + "]";
    }

    public String renderStandard()
    {
        return value == null ? "SNAPSHOT" : value;
    }

    public String getValue()
    {
        return renderStandard();
    }

    public int compareTo( final VersionPart<?> o )
    {
        if ( o instanceof SnapshotPart )
        {
            return 0;
        }
        else
        {
            return -1;
        }
    }

}
