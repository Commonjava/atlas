package org.commonjava.maven.atlas.spi.neo4j.effective;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.effective.EProjectNet;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;
import org.apache.maven.graph.effective.traverse.TraversalType;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.io.Conversions;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.tooling.GlobalGraphOperations;

public class Neo4JEGraphDriver
    implements EGraphDriver, Runnable
{

    //    private final Logger logger = new Logger( getClass() );

    private static final String ALL_RELATIONSHIPS = "all-relationships";

    private static final String ALL_NODES = "all-nodes";

    private static final String UNCONNECTED_NODES = "unconnected-nodes";

    private static final String VARIABLE_NODES = "variable-nodes";

    private GraphDatabaseService graph;

    private final Set<Long> nodeMembership = new HashSet<Long>();

    private final Set<Long> relMembership = new HashSet<Long>();

    private final List<Neo4JEGraphDriver> ancestry = new ArrayList<Neo4JEGraphDriver>();

    public Neo4JEGraphDriver( final File dbPath )
    {
        this( dbPath, true );
    }

    public Neo4JEGraphDriver( final File dbPath, final boolean useShutdownHook )
    {
        graph = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath.getAbsolutePath() );
        if ( useShutdownHook )
        {
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( this ) );
        }
    }

    private Neo4JEGraphDriver( final Neo4JEGraphDriver driver )
    {
        this.graph = driver.graph;
        this.ancestry.addAll( driver.ancestry );
        this.ancestry.add( driver );
    }

    public EGraphDriver newInstance()
        throws GraphDriverException
    {
        return new Neo4JEGraphDriver( this );
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final ProjectVersionRef ref )
    {
        checkClosed();

        if ( ref == null )
        {
            return null;
        }

        final Index<Node> index = graph.index()
                                       .forNodes( ALL_NODES );
        final IndexHits<Node> hits = index.get( Conversions.GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            if ( !nodeMembership.isEmpty() && !nodeMembership.contains( node.getId() ) )
            {
                return null;
            }

            final Iterable<Relationship> relationships = node.getRelationships( Direction.OUTGOING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    private void checkClosed()
    {
        if ( graph == null )
        {
            throw new IllegalStateException( "Graph database has been closed!" );
        }
    }

    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> index = graph.index()
                                       .forNodes( ALL_NODES );
        final IndexHits<Node> hits = index.get( Conversions.GAV, ref.toString() );

        if ( hits.hasNext() )
        {
            final Node node = hits.next();
            final Iterable<Relationship> relationships = node.getRelationships( Direction.INCOMING );
            return convertToRelationships( relationships );
        }

        return null;
    }

    public Collection<ProjectRelationship<?>> getAllRelationships()
    {
        checkClosed();

        return convertToRelationships( GlobalGraphOperations.at( graph )
                                                            .getAllRelationships() );
    }

    public boolean addRelationship( final ProjectRelationship<?> rel )
    {
        checkClosed();

        final Index<Node> index = graph.index()
                                       .forNodes( ALL_NODES );

        final ProjectVersionRef declaring = rel.getDeclaring();
        final ProjectVersionRef target = rel.getTarget()
                                            .asProjectVersionRef();

        final long[] ids = new long[2];
        int i = 0;
        boolean changed = false;
        final Transaction tx = graph.beginTx();
        try
        {
            for ( final ProjectVersionRef ref : new ProjectVersionRef[] { declaring, target } )
            {
                final IndexHits<Node> hits = index.get( Conversions.GAV, ref.toString() );
                if ( !hits.hasNext() )
                {
                    changed = true;
                    final Node node = graph.createNode();
                    ids[i++] = node.getId();
                    Conversions.toNodeProperties( ref, node );
                    index.add( node, Conversions.GAV, ref.toString() );

                    if ( !ref.isRelease() )
                    {
                        graph.index()
                             .forNodes( VARIABLE_NODES )
                             .add( node, Conversions.GAV, ref.toString() );
                    }

                    if ( i > 0 )
                    {
                        graph.index()
                             .forNodes( UNCONNECTED_NODES )
                             .add( node, Conversions.GAV, ref.toString() );
                    }
                }
                else
                {
                    ids[i++] = hits.next()
                                   .getId();
                }

            }

            final RelationshipIndex relIdx = graph.index()
                                                  .forRelationships( ALL_RELATIONSHIPS );

            final String relId = Conversions.id( rel );
            final IndexHits<Relationship> relHits = relIdx.get( Conversions.RELATIONSHIP_ID, relId );
            if ( relHits.size() < 1 )
            {
                changed = true;
                final Node from = graph.getNodeById( ids[0] );
                final Node to = graph.getNodeById( ids[1] );

                final Relationship relationship = from.createRelationshipTo( to, GraphRelType.map( rel.getType() ) );

                Conversions.toRelationshipProperties( rel, relationship );

                graph.index()
                     .forNodes( UNCONNECTED_NODES )
                     .remove( from, Conversions.GAV );

                relIdx.add( relationship, Conversions.RELATIONSHIP_ID, relId );
            }

            tx.success();
        }
        finally
        {
            tx.finish();
        }

        return changed;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        checkClosed();

        return new HashSet<ProjectVersionRef>( convertToProjects( GlobalGraphOperations.at( graph )
                                                                                       .getAllNodes() ) );
    }

    public void traverse( final ProjectNetTraversal traversal, final EProjectNet net, final ProjectVersionRef root )
        throws GraphDriverException
    {
        final Node rootNode = getNode( root );
        if ( rootNode == null )
        {
            throw new GraphDriverException( "Project: %s was not found in graph.", root );
        }
        else if ( !inMembership( rootNode ) )
        {
            throw new GraphDriverException( "Project: %s is not in the restricted membership for this graph.", root );
        }

        for ( int i = 0; i < traversal.getRequiredPasses(); i++ )
        {
            TraversalDescription description = Traversal.description()
                                                        .sort( new PathComparator( this ) );

            if ( traversal.getType( i ) == TraversalType.breadth_first )
            {
                description = description.breadthFirst();
            }
            else
            {
                description = description.depthFirst();
            }

            traversal.startTraverse( i, net );
            description = description.evaluator( new MembershipWrappedTraversalEvaluator( this, traversal, i ) );

            final Traverser traverser = description.traverse( rootNode );
            for ( final Path path : traverser )
            {
                final List<ProjectRelationship<?>> rels = convertToRelationships( path.relationships() );
                if ( rels.isEmpty() )
                {
                    //                    logger.info( "Skipping path with 0 relationships..." );
                    continue;
                }

                //                logger.info( "traversing path with: %d relationships...", rels.size() );
                final ProjectRelationship<?> rel = rels.remove( rels.size() - 1 );

                if ( traversal.traverseEdge( rel, rels, i ) )
                {
                    traversal.edgeTraversed( rel, rels, i );
                }
            }

            traversal.endTraverse( i, net );
        }
    }

    public boolean containsProject( final ProjectVersionRef ref )
    {
        return getNode( ref ) != null;
    }

    public boolean containsRelationship( final ProjectRelationship<?> rel )
    {
        return getRelationship( rel ) != null;
    }

    List<ProjectVersionRef> convertToProjects( final Iterable<Node> nodes )
    {
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
        for ( final Node node : nodes )
        {
            if ( node.getId() == 0 )
            {
                continue;
            }

            if ( !inMembership( node ) )
            {
                continue;
            }

            refs.add( Conversions.toProjectVersionRef( node ) );
        }

        return refs;
    }

    List<ProjectRelationship<?>> convertToRelationships( final Iterable<Relationship> relationships )
    {
        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        for ( final Relationship relationship : relationships )
        {
            if ( !inMembership( relationship ) )
            {
                //                logger.info( "Excluding relationship that's not in the current membership: %d", relationship.getId() );
                continue;
            }

            //            logger.info( "Converting edge: %d to Project relationship.\nDeclaring node id: %d\nTarget node id: %d",
            //                         relationship.getId(), relationship.getStartNode()
            //                                                           .getId(), relationship.getEndNode()
            //                                                                                 .getId() );

            final ProjectRelationship<?> rel = Conversions.toProjectRelationship( relationship );
            if ( rel != null )
            {
                rels.add( rel );
            }
        }

        return rels;
    }

    public void restrictProjectMembership( final Set<ProjectVersionRef> refs )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        for ( final ProjectVersionRef ref : refs )
        {
            final IndexHits<Node> hits = idx.get( Conversions.GAV, ref.toString() );
            while ( hits.hasNext() )
            {
                nodeMembership.add( hits.next()
                                        .getId() );
            }
        }
    }

    public void restrictRelationshipMembership( final Set<ProjectRelationship<?>> rels )
    {
        checkClosed();

        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        for ( final ProjectRelationship<?> rel : rels )
        {
            final IndexHits<Relationship> hits = idx.get( Conversions.RELATIONSHIP_ID, Conversions.id( rel ) );
            while ( hits.hasNext() )
            {
                relMembership.add( hits.next()
                                       .getId() );
            }

            refs.add( rel.getDeclaring() );
            refs.add( rel.getTarget()
                         .asProjectVersionRef() );
        }

        restrictProjectMembership( refs );
    }

    boolean inMembership( final Node node )
    {
        return nodeMembership.isEmpty() || nodeMembership.contains( node.getId() );
    }

    boolean inMembership( final Relationship relationship )
    {
        return relMembership.isEmpty() || relMembership.contains( relationship.getId() );
    }

    Node getNode( final ProjectVersionRef ref )
    {
        checkClosed();

        final Index<Node> idx = graph.index()
                                     .forNodes( ALL_NODES );

        final IndexHits<Node> hits = idx.get( Conversions.GAV, ref.toString() );

        return hits.hasNext() ? hits.next() : null;
    }

    Relationship getRelationship( final ProjectRelationship<?> rel )
    {
        checkClosed();

        final RelationshipIndex idx = graph.index()
                                           .forRelationships( ALL_RELATIONSHIPS );

        final IndexHits<Relationship> hits = idx.get( Conversions.RELATIONSHIP_ID, Conversions.id( rel ) );

        return hits.hasNext() ? hits.next() : null;
    }

    public void selectVersion( final ProjectVersionRef ref, final VersionSpec spec )
    {
        // TODO Auto-generated method stub

    }

    public Set<ProjectVersionRef> getUnconnectedProjectReferences()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<ProjectVersionRef> getVariableProjectReferences()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public synchronized void close()
        throws IOException
    {
        if ( ancestry.isEmpty() )
        {
            if ( graph != null )
            {
                try
                {
                    graph.shutdown();
                    graph = null;
                }
                catch ( final Exception e )
                {
                    throw new IOException( "Failed to shutdown: " + e.getMessage(), e );
                }
            }
        }
        else
        {
            // "close" this derivative driver...
            graph = null;
        }
    }

    public void run()
    {
        try
        {
            close();
        }
        catch ( final IOException e )
        {
            new Logger( getClass() ).error( "Failed to shutdown graph database. Reason: %s", e, e.getMessage() );
        }
    }

}
