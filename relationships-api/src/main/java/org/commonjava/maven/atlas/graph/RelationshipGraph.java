package org.commonjava.maven.atlas.graph;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;
import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnectionException;
import org.commonjava.maven.atlas.graph.traverse.RelationshipGraphTraversal;
import org.commonjava.maven.atlas.graph.traverse.TraversalType;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public final class RelationshipGraph
{

    private static final String GROUP_ID = "groupId";

    private static final String ARTIFACT_ID = "artifactId";

    private static final String VERSION = "version";

    private List<RelationshipGraphListener> listeners;

    private final ViewParams params;

    private final RelationshipGraphConnection connection;

    RelationshipGraph( final ViewParams params, final RelationshipGraphConnection driver )
    {
        this.params = params;
        this.connection = driver;
    }

    public ViewParams getParams()
    {
        return params;
    }

    public Set<ProjectRelationship<?>> storeRelationships( final ProjectRelationship<?>... relationships )
        throws RelationshipGraphException
    {
        return connection.addRelationships( relationships );
    }

    public Set<ProjectRelationship<?>> storeRelationships( final Collection<? extends ProjectRelationship<?>> relationships )
        throws RelationshipGraphException
    {
        return connection.addRelationships( relationships.toArray( new ProjectRelationship<?>[relationships.size()] ) );
    }

    public void addListener( final RelationshipGraphListener listener )
    {
        listeners.add( listener );
    }

    public void removeListener( final RelationshipGraphListener listener )
    {
        listeners.remove( listener );
    }

    public void close()
        throws RelationshipGraphException
    {
        if ( listeners != null )
        {
            for ( final RelationshipGraphListener listener : listeners )
            {
                listener.closing( this );
            }
        }

        try
        {
            connection.close();
        }
        finally
        {
            if ( listeners != null )
            {
                for ( final RelationshipGraphListener listener : listeners )
                {
                    listener.closed( this );
                }
            }
        }
    }

    // +++ IMPORTED FROM EProjectWeb...

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( connection.getAllRelationships( params ) );
    }

    public boolean isComplete()
    {
        return !connection.hasMissingProjects( params );
    }

    public boolean isConcrete()
    {
        return !connection.hasVariableProjects( params );
    }

    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( connection.getMissingProjects( params ) );
    }

    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( connection.getVariableProjects( params ) );
    }

    public Set<ProjectRelationship<?>> add( final EProjectDirectRelationships rels )
        throws RelationshipGraphConnectionException
    {
        return addAll( rels.getAllRelationships() );
    }

    public boolean add( final ProjectRelationship<?> rel )
        throws RelationshipGraphConnectionException
    {
        if ( rel == null )
        {
            return false;
        }

        return connection.addRelationships( rel )
                         .isEmpty();
    }

    public <T extends ProjectRelationship<?>> Set<T> addAll( final Collection<T> rels )
        throws RelationshipGraphConnectionException
    {
        if ( rels == null )
        {
            return null;
        }

        final Set<T> result = new HashSet<T>( rels );

        final Set<ProjectRelationship<?>> rejected =
            connection.addRelationships( rels.toArray( new ProjectRelationship<?>[] {} ) );
        result.removeAll( rejected );

        if ( !result.isEmpty() )
        {
            connection.recomputeIncompleteSubgraphs();
        }

        return result;
    }

    public <T extends ProjectRelationship<?>> Set<T> addAll( final T... rels )
        throws RelationshipGraphConnectionException
    {
        if ( rels == null )
        {
            return null;
        }

        final Set<T> result = new HashSet<T>();
        for ( final T rel : rels )
        {
            if ( add( rel ) )
            {
                result.add( rel );
            }
        }

        connection.recomputeIncompleteSubgraphs();

        return result;
    }

    public void traverse( final ProjectVersionRef start, final RelationshipGraphTraversal traversal,
                          final TraversalType type )
        throws RelationshipGraphConnectionException
    {
        connection.traverse( traversal, start, this, type );
    }

    public void traverse( final ProjectVersionRef start, final RelationshipGraphTraversal traversal )
        throws RelationshipGraphConnectionException
    {
        traverse( start, traversal, TraversalType.breadth_first );
    }

    public void traverse( final RelationshipGraphTraversal traversal, final TraversalType type )
        throws RelationshipGraphConnectionException
    {
        for ( final ProjectVersionRef root : params.getRoots() )
        {
            connection.traverse( traversal, root, this, type );
        }
    }

    public void traverse( final RelationshipGraphTraversal traversal )
        throws RelationshipGraphConnectionException
    {
        traverse( traversal, TraversalType.breadth_first );
    }

    public Set<ProjectRelationship<?>> getUserRelationships( final ProjectVersionRef ref )
    {
        if ( !connection.containsProject( params, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( connection.getRelationshipsTargeting( params, ref ) );
    }

    public Set<ProjectRelationship<?>> getDirectRelationships( final ProjectVersionRef ref )
    {
        if ( !connection.containsProject( params, ref ) )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( connection.getRelationshipsDeclaredBy( params, ref ) );
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return params.getRoots();
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        return getAllRelationships();
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        return connection.isCycleParticipant( params, ref );
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        return connection.isCycleParticipant( params, rel );
    }

    public void addCycle( final EProjectCycle cycle )
        throws RelationshipGraphConnectionException
    {
        connection.addCycle( cycle );
    }

    public Set<EProjectCycle> getCycles()
    {
        return connection.getCycles( params );
    }

    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<? extends ProjectRelationship<?>> rels =
            connection.getRelationshipsTargeting( params, ref.asProjectVersionRef() );
        if ( rels == null )
        {
            return Collections.emptySet();
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    public RelationshipGraphConnection getDatabase()
    {
        return connection;
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return connection.getAllProjects( params );
    }

    public void addMetadata( final ProjectVersionRef ref, final String name, final String value )
        throws RelationshipGraphConnectionException
    {
        connection.addMetadata( ref, name, value );
    }

    public void addMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
        throws RelationshipGraphConnectionException
    {
        connection.setMetadata( ref, metadata );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return connection.getProjectsWithMetadata( params, key );
    }

    public void reindex()
        throws RelationshipGraphConnectionException
    {
        connection.reindex();
    }

    public Set<List<ProjectRelationship<?>>> getPathsTo( final ProjectVersionRef... projectVersionRefs )
    {
        return connection.getAllPathsTo( params, projectVersionRefs );
    }

    public boolean introducesCycle( final ProjectRelationship<?> rel )
    {
        return connection.introducesCycle( params, rel );
    }

    public void addDisconnectedProject( final ProjectVersionRef ref )
        throws RelationshipGraphConnectionException
    {
        connection.addDisconnectedProject( ref );
    }

    public boolean isMissing( final ProjectVersionRef ref )
    {
        return connection.isMissing( params, ref );
    }

    public Set<URI> getSources()
    {
        final Set<ProjectRelationship<?>> rels = getAllRelationships();
        final Set<URI> sources = new HashSet<URI>();
        for ( final ProjectRelationship<?> rel : rels )
        {
            sources.addAll( rel.getSources() );
        }

        return sources;
    }

    // --- IMPORTED FROM EProjectWeb

    // +++ IMPORTED FROM EGraphManager
    public boolean containsGraph( final ProjectVersionRef ref )
    {
        return connection.containsProject( params, ref ) && !connection.isMissing( params, ref );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return connection.getDirectRelationshipsTo( params, to, includeManagedInfo, true, types );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs()
    {
        return connection.getMissingProjects( params );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs()
    {
        return connection.getVariableProjects( params );
    }

    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, String> metadata = connection.getMetadata( ref );
        if ( metadata != null )
        {
            result.putAll( metadata );
        }

        result.put( GROUP_ID, ref.getGroupId() );
        result.put( ARTIFACT_ID, ref.getArtifactId() );
        result.put( VERSION, ref.getVersionString() );

        return result;
    }

    public Map<String, String> getMetadata( final ProjectVersionRef ref, final Set<String> keys )
    {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, String> metadata = connection.getMetadata( ref, keys );
        if ( metadata != null )
        {
            result.putAll( metadata );
        }

        if ( keys.contains( GROUP_ID ) )
        {
            result.put( GROUP_ID, ref.getGroupId() );
        }

        if ( keys.contains( ARTIFACT_ID ) )
        {
            result.put( ARTIFACT_ID, ref.getArtifactId() );
        }

        if ( keys.contains( VERSION ) )
        {
            result.put( VERSION, ref.getVersionString() );
        }

        return result;
    }

    public Map<Map<String, String>, Set<ProjectVersionRef>> collateByMetadata( final Set<ProjectVersionRef> refs,
                                                                               final Set<String> keys )
    {
        final Map<Map<String, String>, Set<ProjectVersionRef>> result =
            new HashMap<Map<String, String>, Set<ProjectVersionRef>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Map<String, String> metadata = getMetadata( ref, keys );
            Set<ProjectVersionRef> collated = result.get( metadata );
            if ( collated == null )
            {
                collated = new HashSet<ProjectVersionRef>();
                result.put( metadata, collated );
            }

            collated.add( ref );
        }

        return result;
    }

    public void setMetadata( final ProjectVersionRef project, final Map<String, String> metadata )
        throws RelationshipGraphConnectionException
    {
        connection.setMetadata( project, metadata );
    }

    public void deleteRelationshipsDeclaredBy( final ProjectVersionRef ref )
        throws RelationshipGraphConnectionException
    {
        connection.deleteRelationshipsDeclaredBy( ref );
    }

    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef )
    {
        return connection.getProjectsMatching( params, projectRef );
    }

    public Map<GraphPath<?>, GraphPathInfo> getPathMapTargeting( final Set<ProjectVersionRef> refs )
    {
        return connection.getPathMapTargeting( params, refs );
    }

    public ProjectVersionRef getPathTargetRef( final GraphPath<?> path )
    {
        return connection.getPathTargetRef( path );
    }

    public GraphPath<?> createPath( final GraphPath<?> parentPath, final ProjectRelationship<?> relationship )
    {
        return connection.createPath( parentPath, relationship );
    }

    public GraphPath<?> createPath( final ProjectRelationship<?>... relationships )
    {
        return connection.createPath( relationships );
    }

    public List<ProjectVersionRef> getPathRefs( final GraphPath<?> path )
    {
        return connection.getPathRefs( params, path );
    }

    RelationshipGraphConnection getConnection()
    {
        return connection;
    }

    // --- IMPORTED FROM EGraphManager
}
