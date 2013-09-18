package org.commonjava.maven.atlas.graph;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.graph.model.GraphView.GLOBAL;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.spi.GraphWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.AbstractGraphWorkspaceListener;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;

public class EGraphManager
    extends AbstractGraphWorkspaceListener
    implements Closeable
{

    private static final String TEMP_WS = "is-temporary";

    private final Logger logger = new Logger( getClass() );

    private final Map<String, GraphWorkspace> loadedWorkspaces = new HashMap<>();

    private final GraphWorkspaceFactory workspaceFactory;

    public EGraphManager( final GraphWorkspaceFactory workspaceFactory )
    {
        this.workspaceFactory = workspaceFactory;
    }

    public Set<ProjectRelationship<?>> storeRelationships( final GraphWorkspace workspace, final ProjectRelationship<?>... rels )
    {
        logger.info( "Storing relationships for: %s\n\n  %s", workspace.getId(), join( rels, "\n  " ) );
        return workspace.getDatabase()
                        .addRelationships( rels );
    }

    public Set<ProjectRelationship<?>> storeRelationships( final GraphWorkspace workspace, final Collection<ProjectRelationship<?>> rels )
    {
        logger.info( "Storing relationships for: %s\n\n  %s", workspace.getId(), join( rels, "\n  " ) );
        return workspace.getDatabase()
                        .addRelationships( rels.toArray( new ProjectRelationship<?>[rels.size()] ) );
    }

    public EProjectGraph createGraph( final GraphWorkspace workspace, final EProjectDirectRelationships rels )
    {
        final ProjectVersionRef project = rels.getKey()
                                              .getProject();

        workspace.getDatabase()
                 .addRelationships( rels.getExactAllRelationships()
                                        .toArray( new ProjectRelationship[] {} ) );

        return getGraph( workspace, null, project );
    }

    public EProjectGraph getGraph( final GraphWorkspace workspace, final ProjectVersionRef project )
    {
        return getGraph( workspace, null, project );
    }

    public EProjectGraph getGraph( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef project )
    {
        final GraphDatabaseDriver dbDriver = workspace.getDatabase();
        if ( !dbDriver.containsProject( GLOBAL, project ) || dbDriver.isMissing( GLOBAL, project ) )
        {
            return null;
        }

        return new EProjectGraph( workspace, filter, project );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final Collection<ProjectVersionRef> refs )
    {
        return getWeb( workspace, null, refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final Collection<ProjectVersionRef> refs )
    {
        return getWeb( workspace, null, refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final ProjectVersionRef... refs )
    {
        return getWeb( workspace, null, refs );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef... refs )
    {
        final GraphDatabaseDriver dbDriver = workspace.getDatabase();
        for ( final ProjectVersionRef ref : refs )
        {
            if ( !dbDriver.containsProject( GLOBAL, ref ) || dbDriver.isMissing( GLOBAL, ref ) )
            {
                return null;
            }
        }

        return new EProjectWeb( workspace, filter, refs );
    }

    public synchronized GraphWorkspace createWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final GraphWorkspace ws = workspaceFactory.createWorkspace( config )
                                                  .addListener( this );

        loadedWorkspaces.put( ws.getId(), ws );
        return ws;
    }

    public synchronized boolean deleteWorkspace( final String id )
        throws IOException
    {
        final GraphWorkspace workspace = loadedWorkspaces.remove( id );
        if ( workspace != null )
        {
            workspace.close();
        }

        return workspaceFactory.deleteWorkspace( id );
    }

    public synchronized GraphWorkspace getWorkspace( final String id )
        throws GraphDriverException
    {
        GraphWorkspace ws = loadedWorkspaces.get( id );
        if ( ws != null )
        {
            return ws;
        }

        ws = workspaceFactory.loadWorkspace( id );
        if ( ws != null )
        {
            ws.addListener( this );
        }

        loadedWorkspaces.put( id, ws );

        return ws;
    }

    public Set<GraphWorkspace> getAllWorkspaces()
    {
        final Set<GraphWorkspace> result = new HashSet<>();
        result.addAll( loadedWorkspaces.values() );
        result.addAll( workspaceFactory.loadAllWorkspaces( loadedWorkspaces.keySet() ) );

        for ( final GraphWorkspace ws : result )
        {
            loadedWorkspaces.put( ws.getId(), ws );
        }

        return result;
    }

    public boolean containsGraph( final ProjectVersionRef ref )
    {
        return containsGraph( GLOBAL, ref );
    }

    public boolean containsGraph( final GraphView view, final ProjectVersionRef ref )
    {
        final GraphDatabaseDriver dbDriver = view.getDatabase();
        return dbDriver.containsProject( view, ref ) && !dbDriver.isMissing( view, ref );
    }

    public boolean containsGraph( final GraphWorkspace workspace, final ProjectVersionRef ref )
    {
        return containsGraph( new GraphView( workspace ), ref );
    }

    public boolean containsGraph( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef ref )
    {
        return containsGraph( new GraphView( workspace, filter ), ref );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo, final RelationshipType... types )
    {
        return view.getDatabase()
                   .getDirectRelationshipsFrom( view, from, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return view.getDatabase()
                   .getDirectRelationshipsTo( view, to, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final GraphWorkspace workspace, final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo, final RelationshipType... types )
    {
        return findDirectRelationshipsFrom( new GraphView( workspace ), from, includeManagedInfo, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final GraphWorkspace workspace, final ProjectVersionRef to,
                                                                  final boolean includeManagedInfo, final RelationshipType... types )
    {
        return findDirectRelationshipsTo( new GraphView( workspace ), to, includeManagedInfo, types );
    }

    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        return view.getDatabase()
                   .getAllProjects( view );
    }

    public Set<ProjectVersionRef> getAllProjects( final GraphWorkspace workspace )
    {
        return getAllProjects( new GraphView( workspace ) );
    }

    public Set<ProjectVersionRef> getAllProjects()
    {
        return getAllProjects( GraphView.GLOBAL );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final GraphView view )
    {
        return view.getDatabase()
                   .getMissingProjects( view );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final GraphWorkspace workspace )
    {
        return getAllIncompleteSubgraphs( new GraphView( workspace ) );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs()
    {
        return getAllIncompleteSubgraphs( GLOBAL );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs( final GraphView view )
    {
        return view.getDatabase()
                   .getVariableProjects( view );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs( final GraphWorkspace workspace )
    {
        return getAllVariableSubgraphs( new GraphView( workspace ) );
    }

    public Set<ProjectVersionRef> getAllVariableSubgraphs()
    {
        return getAllVariableSubgraphs( GLOBAL );
    }

    public Map<String, String> getMetadata( final GraphWorkspace workspace, final ProjectVersionRef ref )
    {
        return workspace.getDatabase()
                        .getMetadata( ref );
    }

    public void addMetadata( final GraphWorkspace workspace, final EProjectKey key, final String name, final String value )
    {
        workspace.getDatabase()
                 .addMetadata( key.getProject(), name, value );
    }

    public void setMetadata( final GraphWorkspace workspace, final EProjectKey key, final Map<String, String> metadata )
    {
        workspace.getDatabase()
                 .setMetadata( key.getProject(), metadata );
    }

    public void addMetadata( final GraphWorkspace workspace, final ProjectVersionRef project, final String name, final String value )
    {
        workspace.getDatabase()
                 .addMetadata( project, name, value );
    }

    public void setMetadata( final GraphWorkspace workspace, final ProjectVersionRef project, final Map<String, String> metadata )
    {
        workspace.getDatabase()
                 .setMetadata( project, metadata );
    }

    public void reindex( final GraphWorkspace workspace )
        throws GraphDriverException
    {
        workspace.getDatabase()
                 .reindex();
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final String key )
    {
        return getProjectsWithMetadata( GLOBAL, key );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphWorkspace workspace, final String key )
    {
        return getProjectsWithMetadata( new GraphView( workspace ), key );
    }

    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
    {
        return view.getDatabase()
                   .getProjectsWithMetadata( view, key );
    }

    public void addDisconnectedProject( final GraphWorkspace workspace, final ProjectVersionRef ref )
    {
        workspace.getDatabase()
                 .addDisconnectedProject( ref );
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        StringWriter sw = new StringWriter();

        for ( final Entry<String, GraphWorkspace> entry : new HashMap<>( loadedWorkspaces ).entrySet() )
        {
            final GraphWorkspace ws = entry.getValue();
            if ( ws == null )
            {
                continue;
            }

            final String wsid = entry.getKey();
            try
            {
                ws.close();
            }
            catch ( final IOException e )
            {
                new GraphDriverException( "Failed to close workspace: %s.", e, wsid ).printStackTrace( new PrintWriter( sw ) );
                if ( sb.length() > 0 )
                {
                    sb.append( "\n\n" );
                }
                sb.append( sw.toString() );
                sw = new StringWriter();
            }
        }

        if ( sb.length() > 0 )
        {
            throw new IOException( sb.toString() );
        }
    }

    @Override
    public void detached( final GraphWorkspace ws )
    {
        if ( ws != null )
        {
            try
            {
                storeWorkspace( ws );
            }
            catch ( final GraphDriverException e )
            {
                logger.error( "Failed to store workspace %s on detach. Reason: %s", e, ws.getId(), e.getMessage() );
            }
        }
    }

    @Override
    public void closed( final GraphWorkspace workspace )
    {
        loadedWorkspaces.remove( workspace.getId() );

        if ( workspace.getProperty( TEMP_WS, Boolean.class, Boolean.FALSE ) )
        {
            try
            {
                workspaceFactory.deleteWorkspace( workspace.getId() );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to delete temporary workspace: %s. Reason: %s", e, workspace.getId(), e.getMessage() );
            }
        }
        else
        {
            try
            {
                workspaceFactory.storeWorkspace( workspace );
            }
            catch ( final GraphDriverException e )
            {
                logger.error( "Failed to store updates for workspace: %s. Reason: %s", e, workspace, e.getMessage() );
            }
        }
    }

    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphWorkspace workspace )
    {
        return workspace.getDatabase()
                        .getProjectsMatching( projectRef, new GraphView( workspace ) );
    }

    public GraphWorkspace createTemporaryWorkspace( final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final GraphWorkspace ws = createWorkspace( config );
        ws.setProperty( TEMP_WS, Boolean.TRUE );

        return ws;
    }

    public void storeWorkspace( final GraphWorkspace ws )
        throws GraphDriverException
    {
        workspaceFactory.storeWorkspace( ws );
    }

}
