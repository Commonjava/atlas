package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootedRelationshipsVisitor
    extends AbstractTraverseVisitor
    implements Iterable<ProjectRelationship<?>>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<ProjectRelationship<?>> found = new HashSet<ProjectRelationship<?>>();

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return found;
    }

    @Override
    public Iterator<ProjectRelationship<?>> iterator()
    {
        return found.iterator();
    }

    @Override
    public boolean includeChildren( final Path path, final Neo4jGraphPath graphPath, final GraphPathInfo pathInfo )
    {
        for ( final Relationship r : path.relationships() )
        {
            if ( r.getType() == GraphRelType.PARENT && r.getStartNode()
                                                        .getId() == r.getEndNode()
                                                                     .getId() )
            {
                logger.debug( "Skipping self-referential parent relationship to: {}", r.getStartNode()
                                                                                       .getProperty( Conversions.GAV ) );
            }
            else
            {
                found.add( Conversions.toProjectRelationship( r, getConversionCache() ) );
            }
        }

        return true;
    }

}
