package org.commonjava.maven.atlas.graph.spi.jung;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.jung.model.JungGraphPath;
import org.commonjava.maven.atlas.graph.traverse.AbstractTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

final class PathDetectionTraversal
    extends AbstractTraversal
{
    //        private final Logger logger = new Logger( getClass() );

    private final Set<ProjectVersionRef> to;

    private final Map<JungGraphPath, GraphPathInfo> pathMap = new HashMap<JungGraphPath, GraphPathInfo>();

    private final Set<JungGraphPath> paths = new HashSet<JungGraphPath>();

    private final GraphView view;

    PathDetectionTraversal( final GraphView view, final ProjectVersionRef[] refs )
    {
        this.view = view;
        this.to = new HashSet<ProjectVersionRef>( Arrays.asList( refs ) );
    }

    public PathDetectionTraversal( final GraphView view, final Set<ProjectVersionRef> refs )
    {
        this.view = view;
        this.to = refs;
    }

    public Map<JungGraphPath, GraphPathInfo> getPathMap()
    {
        return pathMap;
    }

    public Set<JungGraphPath> getPaths()
    {
        return paths;
    }

    @Override
    public boolean preCheck( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path, final int pass )
    {
        JungGraphPath jpath;
        GraphPathInfo pathInfo;
        if ( path.isEmpty() )
        {
            jpath = new JungGraphPath( relationship.getDeclaring() );
            pathInfo = new GraphPathInfo( view );
        }
        else
        {
            jpath = new JungGraphPath( path );
            pathInfo = pathMap.get( jpath );
        }

        if ( pathInfo == null )
        {
            return false;
        }

        final ProjectRelationship<?> selected = pathInfo.selectRelationship( relationship, jpath );
        if ( selected == null )
        {
            return false;
        }

        jpath = new JungGraphPath( jpath, selected );
        pathInfo = pathInfo.getChildPathInfo( relationship );

        pathMap.put( jpath, pathInfo );

        final ProjectVersionRef target = selected.getTarget()
                                                 .asProjectVersionRef();

        // logger.info( "Checking path: %s to see if target: %s is in endpoints: %s", join( path, "," ), target, join( to, ", " ) );
        boolean found = false;
        for ( final ProjectVersionRef t : to )
        {
            if ( t.equals( target ) )
            {
                paths.add( jpath );
                // logger.info( "+= %s", join( path, ", " ) );
                found = true;
            }
        }

        return !found;
    }
}