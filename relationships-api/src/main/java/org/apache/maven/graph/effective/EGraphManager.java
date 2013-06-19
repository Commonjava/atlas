package org.apache.maven.graph.effective;

import static org.apache.maven.graph.spi.effective.EProjectNetView.GLOBAL;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.session.EGraphSession;
import org.apache.maven.graph.effective.session.EGraphSessionConfiguration;
import org.apache.maven.graph.effective.session.GraphSessionListener;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;
import org.apache.maven.graph.spi.effective.EProjectNetView;

public class EGraphManager
    implements Closeable, GraphSessionListener
{

    private final EGraphDriver rootDriver;

    public EGraphManager( final EGraphDriver rootDriver )
    {
        this.rootDriver = rootDriver;
    }

    public Set<ProjectRelationship<?>> storeRelationships( final ProjectRelationship<?>... rels )
    {
        return rootDriver.addRelationships( rels );
    }

    public Set<ProjectRelationship<?>> storeRelationships( final Collection<ProjectRelationship<?>> rels )
    {
        return rootDriver.addRelationships( rels.toArray( new ProjectRelationship<?>[rels.size()] ) );
    }

    public EProjectGraph createGraph( final EGraphSession session, final EProjectDirectRelationships rels )
    {
        final ProjectVersionRef project = rels.getKey()
                                              .getProject();

        rootDriver.addRelationships( rels.getExactAllRelationships()
                                         .toArray( new ProjectRelationship[] {} ) );

        return getGraph( session, null, project );
    }

    public EProjectGraph getGraph( final EGraphSession session, final ProjectVersionRef project )
    {
        return getGraph( session, null, project );
    }

    public EProjectGraph getGraph( final EGraphSession session, final ProjectRelationshipFilter filter,
                                   final ProjectVersionRef project )
    {
        if ( !rootDriver.containsProject( GLOBAL, project ) || rootDriver.isMissing( GLOBAL, project ) )
        {
            return null;
        }

        return new EProjectGraph( session, rootDriver, filter, project );
    }

    public EProjectWeb getWeb( final EGraphSession session, final Collection<ProjectVersionRef> refs )
    {
        return getWeb( session, null,
                       refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final EGraphSession session, final ProjectRelationshipFilter filter,
                               final Collection<ProjectVersionRef> refs )
    {
        return getWeb( session, null,
                       refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final EGraphSession session, final ProjectVersionRef... refs )
    {
        return getWeb( session, null, refs );
    }

    public EProjectWeb getWeb( final EGraphSession session, final ProjectRelationshipFilter filter,
                               final ProjectVersionRef... refs )
    {
        for ( final ProjectVersionRef ref : refs )
        {
            if ( !rootDriver.containsProject( GLOBAL, ref ) || rootDriver.isMissing( GLOBAL, ref ) )
            {
                return null;
            }
        }

        return new EProjectWeb( session, rootDriver, filter, refs );
    }

    public EGraphSession createSession( final EGraphSessionConfiguration config )
        throws GraphDriverException
    {
        return new EGraphSessionImpl( rootDriver.registerNewSession( config ), config, this );
    }

    private static final class EGraphSessionImpl
        extends EGraphSession
    {
        public EGraphSessionImpl( final String id, final EGraphSessionConfiguration config, final EGraphManager manager )
        {
            super( id, config );
            addListener( manager );
        }
    }

    public boolean containsGraph( final ProjectVersionRef ref )
    {
        return containsGraph( GLOBAL, ref );
    }

    public boolean containsGraph( final EProjectNetView view, final ProjectVersionRef ref )
    {
        return rootDriver.containsProject( view, ref );
    }

    public boolean containsGraph( final EGraphSession session, final ProjectVersionRef ref )
    {
        return containsGraph( new EProjectNetView( session ), ref );
    }

    public boolean containsGraph( final EGraphSession session, final ProjectRelationshipFilter filter,
                                  final ProjectVersionRef ref )
    {
        return containsGraph( new EProjectNetView( session, filter ), ref );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final EProjectNetView view,
                                                                    final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo,
                                                                    final RelationshipType... types )
    {
        return rootDriver.getDirectRelationshipsFrom( view, from, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final EProjectNetView view,
                                                                  final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return rootDriver.getDirectRelationshipsTo( view, to, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final EGraphSession session,
                                                                    final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo,
                                                                    final RelationshipType... types )
    {
        return findDirectRelationshipsFrom( new EProjectNetView( session ), from, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final EGraphSession session,
                                                                  final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return findDirectRelationshipsTo( new EProjectNetView( session ), to, includeManagedInfo, types );
    }

    public Set<ProjectVersionRef> getAllProjects( final EProjectNetView view )
    {
        return rootDriver.getAllProjects( view );
    }

    public Set<ProjectVersionRef> getAllProjects( final EGraphSession session )
    {
        return getAllProjects( new EProjectNetView( session ) );
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return getAllProjects( EProjectNetView.GLOBAL );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final EProjectNetView view )
    {
        return rootDriver.getMissingProjects( view );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final EGraphSession session )
    {
        return getAllIncompleteSubgraphs( new EProjectNetView( session ) );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs()
    {
        return getAllIncompleteSubgraphs( GLOBAL );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs( final EProjectNetView view )
    {
        return rootDriver.getVariableProjects( view );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs( final EGraphSession session )
    {
        return getAllVariableSubgraphs( new EProjectNetView( session ) );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs()
    {
        return getAllVariableSubgraphs( GLOBAL );
    }

    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        return rootDriver.getMetadata( ref );
    }

    public void addMetadata( final EProjectKey key, final String name, final String value )
    {
        rootDriver.addMetadata( key.getProject(), name, value );
    }

    public void addMetadata( final EProjectKey key, final Map<String, String> metadata )
    {
        rootDriver.addMetadata( key.getProject(), metadata );
    }

    public void reindex()
        throws GraphDriverException
    {
        rootDriver.reindex();
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return getProjectsWithMetadata( GLOBAL, key );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final EGraphSession session, final String key )
    {
        return getProjectsWithMetadata( new EProjectNetView( session ), key );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final EProjectNetView view, final String key )
    {
        return rootDriver.getProjectsWithMetadata( view, key );
    }

    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        rootDriver.addDisconnectedProject( ref );
    }

    @Override
    public void close()
        throws IOException
    {
        rootDriver.close();
    }

    @Override
    public void selectionAdded( final EGraphSession session, final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        rootDriver.selectVersionFor( ref, version, session.getId() );
    }

    @Override
    public void sessionClosed( final EGraphSession session )
        throws GraphDriverException
    {
        rootDriver.deRegisterSession( session.getId() );
    }

    @Override
    public void selectionsCleared( final EGraphSession session )
        throws GraphDriverException
    {
        rootDriver.clearSelectedVersionsFor( session.getId() );
    }

}
