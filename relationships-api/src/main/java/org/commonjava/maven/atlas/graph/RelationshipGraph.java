package org.commonjava.maven.atlas.graph;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
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

    public void storeProjectError( final ProjectVersionRef ref, final Throwable error )
        throws RelationshipGraphException
    {
        connection.addProjectError( ref, error );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.projectError( this, ref, error );
        }
    }

    public Throwable getProjectError( final ProjectVersionRef ref )
    {
        return connection.getProjectError( ref );
    }

    public boolean hasProjectError( final ProjectVersionRef ref )
    {
        return connection.hasProjectError( ref );
    }

    public void clearProjectError( final ProjectVersionRef ref )
        throws RelationshipGraphException
    {
        connection.clearProjectError( ref );
    }

    public Set<ProjectRelationship<?>> storeRelationships( final ProjectRelationship<?>... relationships )
        throws RelationshipGraphException
    {
        final List<ProjectRelationship<?>> rels = Arrays.asList( relationships );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.storing( this, rels );
        }

        final Set<ProjectRelationship<?>> rejected = connection.addRelationships( relationships );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.stored( this, rels, rejected );
        }

        return rejected;
    }

    public Set<ProjectRelationship<?>> storeRelationships( final Collection<? extends ProjectRelationship<?>> relationships )
        throws RelationshipGraphException
    {
        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.storing( this, relationships );
        }

        final Set<ProjectRelationship<?>> rejected =
            connection.addRelationships( relationships.toArray( new ProjectRelationship<?>[relationships.size()] ) );

        for ( final RelationshipGraphListener listener : listeners )
        {
            listener.stored( this, relationships, rejected );
        }

        return rejected;
    }

    public void addListener( final RelationshipGraphListener listener )
    {
        if ( listeners == null )
        {
            listeners = new ArrayList<RelationshipGraphListener>();
        }

        listeners.add( listener );
    }

    public void removeListener( final RelationshipGraphListener listener )
    {
        if ( listeners != null )
        {
            listeners.remove( listener );
        }
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
        throws RelationshipGraphException
    {
        return addAll( rels.getAllRelationships() );
    }

    public boolean add( final ProjectRelationship<?> rel )
        throws RelationshipGraphException
    {
        if ( rel == null )
        {
            return false;
        }

        return connection.addRelationships( rel )
                         .isEmpty();
    }

    public <T extends ProjectRelationship<?>> Set<T> addAll( final Collection<T> rels )
        throws RelationshipGraphException
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
        throws RelationshipGraphException
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
        throws RelationshipGraphException
    {
        connection.traverse( traversal, start, this, type );
    }

    public void traverse( final ProjectVersionRef start, final RelationshipGraphTraversal traversal )
        throws RelationshipGraphException
    {
        traverse( start, traversal, TraversalType.breadth_first );
    }

    public void traverse( final RelationshipGraphTraversal traversal, final TraversalType type )
        throws RelationshipGraphException
    {
        for ( final ProjectVersionRef root : params.getRoots() )
        {
            connection.traverse( traversal, root, this, type );
        }
    }

    public void traverse( final RelationshipGraphTraversal traversal )
        throws RelationshipGraphException
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
        throws RelationshipGraphException
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
        throws RelationshipGraphException
    {
        connection.addMetadata( ref, name, value );
    }

    public void addMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
        throws RelationshipGraphException
    {
        connection.setMetadata( ref, metadata );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return connection.getProjectsWithMetadata( params, key );
    }

    public void reindex()
        throws RelationshipGraphException
    {
        connection.reindex();
    }

    public void reindex( final ProjectVersionRef ref )
        throws RelationshipGraphConnectionException
    {
        connection.reindex( ref );
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
        throws RelationshipGraphException
    {
        connection.addDisconnectedProject( ref );
    }

    public boolean isMissing( final ProjectVersionRef ref )
    {
        return connection.isMissing( params, ref );
    }

    public Set<URI> getSources()
    {
        return params.getActiveSources();
        //        final Set<ProjectRelationship<?>> rels = getAllRelationships();
        //        final Set<URI> sources = new HashSet<URI>();
        //        for ( final ProjectRelationship<?> rel : rels )
        //        {
        //            sources.addAll( rel.getSources() );
        //        }
        //
        //        return sources;
    }

    public GraphMutator getMutator()
    {
        return params.getMutator();
    }

    public ProjectRelationshipFilter getFilter()
    {
        return params.getFilter();
    }

    public ViewParams addActivePomLocation( final URI location )
    {
        return params.addActivePomLocation( location );
    }

    public ViewParams addActivePomLocations( final Collection<URI> locations )
    {
        return params.addActivePomLocations( locations );
    }

    public ViewParams addActivePomLocations( final URI... locations )
    {
        return params.addActivePomLocations( locations );
    }

    public ViewParams addActiveSources( final Collection<URI> sources )
    {
        return params.addActiveSources( sources );
    }

    public ViewParams addActiveSources( final URI... sources )
    {
        return params.addActiveSources( sources );
    }

    public ViewParams addActiveSource( final URI source )
    {
        return params.addActiveSource( source );
    }

    public long getLastAccess()
    {
        return params.getLastAccess();
    }

    public String setProperty( final String key, final String value )
    {
        return params.setProperty( key, value );
    }

    public String getProperty( final String key )
    {
        return params.getProperty( key );
    }

    public String getProperty( final String key, final String def )
    {
        return params.getProperty( key, def );
    }

    public final String getWorkspaceId()
    {
        return params.getWorkspaceId();
    }

    public final Set<URI> getActivePomLocations()
    {
        return params.getActivePomLocations();
    }

    public final Set<URI> getActiveSources()
    {
        return params.getActiveSources();
    }

    public final ProjectVersionRef getSelection( final ProjectRef ref )
    {
        return params.getSelection( ref );
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

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final ProjectVersionRef source,
                                                                    final boolean managed,
                                                                    final RelationshipType... types )
    {
        return connection.getDirectRelationshipsFrom( params, source, managed, true, types );
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
        throws RelationshipGraphException
    {
        connection.setMetadata( project, metadata );
    }

    public void deleteRelationshipsDeclaredBy( final ProjectVersionRef ref )
        throws RelationshipGraphException
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

    @Override
    public String toString()
    {
        return "RelationshipGraph [params=" + params + "]";
    }

}
