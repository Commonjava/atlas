package org.apache.maven.graph.effective.filter;

import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class NoneFilter
    implements ProjectRelationshipFilter
{

    public boolean accept( final ProjectRelationship<?> rel )
    {
        return false;
    }

    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return this;
    }

    public void render( final StringBuilder sb )
    {
        if ( sb.length() > 0 )
        {
            sb.append( " " );
        }
        sb.append( "NONE" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        render( sb );
        return sb.toString();
    }

}
