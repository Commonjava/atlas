package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubPathsCollectingVisitor
    extends AbstractTraverseVisitor
    implements Iterable<Neo4jGraphPath>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<Long> viaNodes;

    private final Set<Neo4jGraphPath> paths = new HashSet<Neo4jGraphPath>();

    private final GraphAdmin admin;

    public SubPathsCollectingVisitor( final Set<Long> viaNodes, final GraphAdmin admin )
    {
        this.viaNodes = viaNodes;
        this.admin = admin;
    }

    @Override
    public Iterator<Neo4jGraphPath> iterator()
    {
        return paths.iterator();
    }

    public Set<Neo4jGraphPath> getSubPaths()
    {
        return paths;
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        if ( path.length() > 1 )
        {
            final Neo4jGraphPath gp = new Neo4jGraphPath( path );
            final long[] allRids = gp.getRelationshipIds();
            int i = 0;
            for ( ; i < allRids.length; i++ )
            {
                final Relationship r = admin.getRelationship( allRids[i] );
                if ( viaNodes.contains( r.getEndNode()
                                         .getId() ) )
                {
                    logger.debug( "found via-node ending: {}", r );
                    break;
                }
            }

            if ( i < allRids.length - 1 )
            {
                final long[] rids = new long[allRids.length - i];
                System.arraycopy( allRids, i, rids, 0, rids.length );
                paths.add( new Neo4jGraphPath( rids ) );
            }
        }

        return true;
    }

    @Override
    public void configure( final AtlasCollector<?> collector )
    {
        collector.setUseSelections( false );
    }

}
