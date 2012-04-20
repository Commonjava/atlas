package org.apache.maven.pgraph.version.part;

public enum VersionPartSeparator
{

    DOT( "." ), DASH( "-" ), UNDERSCORE( "_" ), BLANK( "" );

    private String rendered;

    private VersionPartSeparator( final String rendered )
    {
        this.rendered = rendered;
    }

    public String getRenderedString()
    {
        return rendered;
    }

}
