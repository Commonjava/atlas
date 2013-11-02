package org.commonjava.maven.atlas.graph.filter;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;

public class BOMFilter
    extends AbstractTypedFilter
{

    public BOMFilter()
    {
        super( RelationshipType.DEPENDENCY, false, true, false );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return null;
    }

    @Override
    public void render( final StringBuilder sb )
    {
        sb.append( "BOMs" );
    }

    @Override
    protected boolean doAccept( final ProjectRelationship<?> rel )
    {
        final DependencyRelationship dr = (DependencyRelationship) rel;
        return "pom".equals( dr.getTarget()
                               .getType() ) && DependencyScope._import.equals( dr.getScope() );
    }

}
