package org.apache.maven.graph.effective;

import static org.apache.maven.graph.effective.GraphView.GLOBAL;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.RelationshipType;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.workspace.GraphWorkspaceConfiguration;
import org.apache.maven.graph.effective.workspace.GraphWorkspaceListener;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public class EGraphManager
    implements Closeable, GraphWorkspaceListener
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

    public EProjectGraph createGraph( final GraphWorkspace session, final EProjectDirectRelationships rels )
    {
        final ProjectVersionRef project = rels.getKey()
                                              .getProject();

        rootDriver.addRelationships( rels.getExactAllRelationships()
                                         .toArray( new ProjectRelationship[] {} ) );

        return getGraph( session, null, project );
    }

    public EProjectGraph getGraph( final GraphWorkspace session, final ProjectVersionRef project )
    {
        return getGraph( session, null, project );
    }

    public EProjectGraph getGraph( final GraphWorkspace session, final ProjectRelationshipFilter filter,
                                   final ProjectVersionRef project )
    {
        if ( !rootDriver.containsProject( GLOBAL, project ) || rootDriver.isMissing( GLOBAL, project ) )
        {
            return null;
        }

        return new EProjectGraph( session, rootDriver, filter, project );
    }

    public EProjectWeb getWeb( final GraphWorkspace session, final Collection<ProjectVersionRef> refs )
    {
        return getWeb( session, null,
                       refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final GraphWorkspace session, final ProjectRelationshipFilter filter,
                               final Collection<ProjectVersionRef> refs )
    {
        return getWeb( session, null,
                       refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final GraphWorkspace session, final ProjectVersionRef... refs )
    {
        return getWeb( session, null, refs );
    }

    public EProjectWeb getWeb( final GraphWorkspace session, final ProjectRelationshipFilter filter,
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

    public GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        return new GraphWorkspace( rootDriver.registerNewSession( config ), config ).addListener( this );
    }

    public boolean containsGraph( final ProjectVersionRef ref )
    {
        return containsGraph( GLOBAL, ref );
    }

    public boolean containsGraph( final GraphView view, final ProjectVersionRef ref )
    {
        return rootDriver.containsProject( view, ref );
    }

    public boolean containsGraph( final GraphWorkspace session, final ProjectVersionRef ref )
    {
        return containsGraph( new GraphView( session ), ref );
    }

    public boolean containsGraph( final GraphWorkspace session, final ProjectRelationshipFilter filter,
                                  final ProjectVersionRef ref )
    {
        return containsGraph( new GraphView( session, filter ), ref );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo,
                                                                    final RelationshipType... types )
    {
        return rootDriver.getDirectRelationshipsFrom( view, from, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return rootDriver.getDirectRelationshipsTo( view, to, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final GraphWorkspace session,
                                                                    final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo,
                                                                    final RelationshipType... types )
    {
        return findDirectRelationshipsFrom( new GraphView( session ), from, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final GraphWorkspace session,
                                                                  final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return findDirectRelationshipsTo( new GraphView( session ), to, includeManagedInfo, types );
    }

    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        return rootDriver.getAllProjects( view );
    }

    public Set<ProjectVersionRef> getAllProjects( final GraphWorkspace session )
    {
        return getAllProjects( new GraphView( session ) );
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return getAllProjects( GraphView.GLOBAL );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final GraphView view )
    {
        return rootDriver.getMissingProjects( view );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final GraphWorkspace session )
    {
        return getAllIncompleteSubgraphs( new GraphView( session ) );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs()
    {
        return getAllIncompleteSubgraphs( GLOBAL );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs( final GraphView view )
    {
        return rootDriver.getVariableProjects( view );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs( final GraphWorkspace session )
    {
        return getAllVariableSubgraphs( new GraphView( session ) );
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

    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphWorkspace session, final String key )
    {
        return getProjectsWithMetadata( new GraphView( session ), key );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
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
    public void selectionAdded( final GraphWorkspace session, final ProjectVersionRef ref, final SingleVersion version )
        throws GraphDriverException
    {
        rootDriver.selectVersionFor( ref, version, session.getId() );
    }

    @Override
    public void sessionClosed( final GraphWorkspace session )
        throws GraphDriverException
    {
        rootDriver.deRegisterSession( session.getId() );
    }

    @Override
    public void selectionsCleared( final GraphWorkspace session )
        throws GraphDriverException
    {
        rootDriver.clearSelectedVersionsFor( session.getId() );
    }

    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphWorkspace session )
    {
        return rootDriver.getProjectsMatching( projectRef, new GraphView( session ) );
    }

}
