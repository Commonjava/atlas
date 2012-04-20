package org.apache.maven.pgraph.version.part;

public class SeparatorPart
    implements VersionPart<VersionPartSeparator>
{

    private final VersionPartSeparator type;

    public SeparatorPart( final VersionPartSeparator type )
    {
        this.type = type;
    }

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

    public int compareTo( final VersionPart<?> o )
    {
        if ( o instanceof SeparatorPart )
        {
            return type.compareTo( ( (SeparatorPart) o ).getValue() );
        }

        // punt...shouldn't happen.
        return 0;
    }

}
