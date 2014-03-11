package org.commonjava.maven.atlas.graph.spi.neo4j.traverse;

import static org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions.toProjectRelationship;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.neo4j.graphdb.Relationship;

public class GraphPathInfo
{

    private final ProjectRelationshipFilter filter;

    private final GraphMutator mutator;

    public GraphPathInfo( final GraphView view )
    {
        filter = view.getFilter();
        mutator = view.getMutator();
    }

    public GraphPathInfo( final ProjectRelationshipFilter filter, final GraphMutator mutator )
    {
        this.filter = filter;
        this.mutator = mutator;
    }

    public ProjectRelationshipFilter getFilter()
    {
        return filter;
    }

    public GraphMutator getMutator()
    {
        return mutator;
    }

    public ProjectRelationship<?> selectRelationship( ProjectRelationship<?> next, final Neo4jGraphPath path )
    {
        if ( filter != null && !filter.accept( next ) )
        {
            return null;
        }

        if ( mutator != null )
        {
            next = mutator.selectFor( next, path );
        }

        return next;
    }

    public GraphPathInfo getChildPathInfo( final Relationship r )
    {
        final ProjectRelationship<?> rel = toProjectRelationship( r );
        final ProjectRelationshipFilter nextFilter = filter == null ? null : filter.getChildFilter( rel );
        final GraphMutator nextMutator = mutator == null ? null : mutator.getMutatorFor( rel );
        if ( nextFilter == filter && nextMutator == mutator )
        {
            return this;
        }

        return new GraphPathInfo( nextFilter, nextMutator );
    }
}
