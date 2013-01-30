package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4JEGraphDriver
    implements EGraphDriver
{

    private static final String ALL_RELATIONSHIPS = "all-relationships";

    private static final String RELATIONSHIP_ID = "relationship-id";

    private static final String ALL_NODES = "all-nodes";

    private static final String GAV = "gav";

    private final GraphDatabaseService graph;

    private final File dbPath;

    public Neo4JEGraphDriver( final File dbPath )
    {
        this.dbPath = dbPath;
        graph = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath.getAbsolutePath() );
        // FIXME: shutdown hook...
    }

    public EGraphDriver newInstance()
        throws GraphDriverException
    {
        final String fname = dbPath.getName();
        try
        {
            final File newDbPath = File.createTempFile( fname, "-atlas" );

            return new Neo4JEGraphDriver( newDbPath );
        }
        catch ( final IOException e )
        {
            throw new GraphDriverException( "Failed to create temp file to hold new graph variant of: %s. Reason: %s",
                                            e, fname, e.getMessage() );
        }
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
        throws GraphDriverException
    {
        return convertToRelationships( GlobalGraphOperations.at( graph )
                                                            .getAllRelationships() );
    }

    public boolean addRelationship( final ProjectRelationship<?> rel )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return convertToProjects( GlobalGraphOperations.at( graph )
                                                       .getAllNodes() );
    }

    public void traverse( final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
    {
        // TODO Auto-generated method stub

    }

    public boolean containsProject( final ProjectVersionRef ref )
    {
        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );
        final IndexHits<Node> hits = idx.get( GAV, ref.toString() );

        return hits.size() > 0;
    }

    public boolean containsRelationship( final ProjectRelationship<?> rel )
    {
        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );
        final IndexHits<Relationship> hits = idx.get( RELATIONSHIP_ID, Conversions.id( rel ) );

        return hits.size() > 0;
    }

    private Set<ProjectVersionRef> convertToProjects( final Iterable<Node> nodes )
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();
        for ( final Node node : nodes )
        {
            refs.add( Conversions.toProjectVersionRef( node ) );
        }

        return refs;
    }

    private Collection<ProjectRelationship<?>> convertToRelationships( final Iterable<Relationship> relationships )
        throws GraphDriverException
    {
        final Set<ProjectRelationship<?>> rels = new HashSet<ProjectRelationship<?>>();
        for ( final Relationship relationship : relationships )
        {
            rels.add( Conversions.toProjectRelationship( relationship ) );
        }

        return rels;
    }

}
