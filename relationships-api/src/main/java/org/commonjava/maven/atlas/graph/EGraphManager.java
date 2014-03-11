/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.graph;

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
import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.mutate.GraphMutator;
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
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EGraphManager
    extends AbstractGraphWorkspaceListener
    implements Closeable
{

    private static final String TEMP_WS = "is-temporary";

    private static final String GROUP_ID = "groupId";

    private static final String ARTIFACT_ID = "artifactId";

    private static final String VERSION = "version";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<String, GraphWorkspace> loadedWorkspaces = new HashMap<String, GraphWorkspace>();

    private final GraphWorkspaceFactory workspaceFactory;

    public EGraphManager( final GraphWorkspaceFactory workspaceFactory )
    {
        this.workspaceFactory = workspaceFactory;
    }

    public Set<ProjectRelationship<?>> storeRelationships( final GraphWorkspace workspace, final ProjectRelationship<?>... rels )
        throws GraphDriverException
    {
        logger.debug( "Storing relationships for: {}\n\n  {}", workspace.getId(), new JoinString( "\n  ", rels ) );
        return workspace.getDatabase()
                        .addRelationships( rels );
    }

    public Set<ProjectRelationship<?>> storeRelationships( final GraphWorkspace workspace, final Collection<ProjectRelationship<?>> rels )
        throws GraphDriverException
    {
        logger.debug( "Storing relationships for: {}\n\n  {}", workspace.getId(), new JoinString( "\n  ", rels ) );
        return workspace.getDatabase()
                        .addRelationships( rels.toArray( new ProjectRelationship<?>[rels.size()] ) );
    }

    public EProjectGraph createGraph( final GraphWorkspace workspace, final EProjectDirectRelationships rels )
        throws GraphDriverException
    {
        final ProjectVersionRef project = rels.getKey()
                                              .getProject();

        workspace.getDatabase()
                 .addRelationships( rels.getExactAllRelationships()
                                        .toArray( new ProjectRelationship[] {} ) );

        return getGraph( workspace, null, null, project );
    }

    public EProjectGraph createGraph( final GraphView view, final EProjectDirectRelationships rels )
        throws GraphDriverException
    {
        view.getDatabase()
            .addRelationships( rels.getExactAllRelationships()
                                   .toArray( new ProjectRelationship[] {} ) );

        return getGraph( view );
    }

    public EProjectGraph getGraph( final GraphWorkspace workspace, final ProjectVersionRef project )
    {
        return getGraph( workspace, null, null, project );
    }

    public EProjectGraph getGraph( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef project )
    {
        return getGraph( workspace, filter, null, project );
    }

    public EProjectGraph getGraph( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                                   final ProjectVersionRef project )
    {
        final GraphDatabaseDriver dbDriver = workspace.getDatabase();
        final GraphView view = new GraphView( workspace );
        if ( !dbDriver.containsProject( view, project ) || dbDriver.isMissing( view, project ) )
        {
            return null;
        }

        return new EProjectGraph( workspace, filter, mutator, project );
    }

    public EProjectGraph getGraph( final GraphView view )
    {
        final GraphDatabaseDriver dbDriver = view.getWorkspace()
                                                 .getDatabase();
        final ProjectVersionRef root = view.getRoots()
                                           .iterator()
                                           .next();
        if ( !dbDriver.containsProject( view, root ) || dbDriver.isMissing( view, root ) )
        {
            return null;
        }

        return new EProjectGraph( view );
    }

    public EProjectNet getWeb( final GraphWorkspace workspace, final Collection<ProjectVersionRef> refs )
    {
        return getWeb( workspace, null, null, refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectNet getWeb( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final Collection<ProjectVersionRef> refs )
    {
        return getWeb( workspace, null, null, refs == null ? new ProjectVersionRef[0] : refs.toArray( new ProjectVersionRef[refs.size()] ) );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final ProjectVersionRef... refs )
    {
        return getWeb( workspace, null, null, refs );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final ProjectVersionRef... refs )
    {
        return getWeb( workspace, filter, null, refs );
    }

    public EProjectWeb getWeb( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                               final ProjectVersionRef... refs )
    {
        final GraphDatabaseDriver dbDriver = workspace.getDatabase();
        final GraphView view = new GraphView( workspace );
        for ( final ProjectVersionRef ref : refs )
        {
            if ( !dbDriver.containsProject( view, ref ) || dbDriver.isMissing( view, ref ) )
            {
                return null;
            }
        }

        return new EProjectWeb( workspace, filter, mutator, refs );
    }

    public EProjectWeb getWeb( final GraphView view )
    {
        final GraphDatabaseDriver dbDriver = view.getWorkspace()
                                                 .getDatabase();
        for ( final ProjectVersionRef ref : view.getRoots() )
        {
            if ( !dbDriver.containsProject( view, ref ) || dbDriver.isMissing( view, ref ) )
            {
                return null;
            }
        }

        return new EProjectWeb( view );
    }

    public synchronized GraphWorkspace createWorkspace( final String id, final GraphWorkspaceConfiguration config )
        throws GraphDriverException
    {
        final GraphWorkspace ws = workspaceFactory.createWorkspace( id, config )
                                                  .addListener( this );

        loadedWorkspaces.put( ws.getId(), ws );
        return ws;
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

            // try to wait for file handles to close?
            try
            {
                Thread.sleep( 2000 );
            }
            catch ( final InterruptedException e )
            {
            }
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

            loadedWorkspaces.put( id, ws );
        }

        return ws;
    }

    public Set<GraphWorkspace> getAllWorkspaces()
    {
        final Set<GraphWorkspace> result = new HashSet<GraphWorkspace>();
        result.addAll( loadedWorkspaces.values() );
        result.addAll( workspaceFactory.loadAllWorkspaces( loadedWorkspaces.keySet() ) );

        for ( final GraphWorkspace ws : result )
        {
            loadedWorkspaces.put( ws.getId(), ws );
        }

        return result;
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
        return containsGraph( workspace, filter, null, ref );
    }

    public boolean containsGraph( final GraphWorkspace workspace, final ProjectRelationshipFilter filter, final GraphMutator mutator,
                                  final ProjectVersionRef ref )
    {
        return containsGraph( new GraphView( workspace, filter, mutator ), ref );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                    final boolean includeManagedInfo, final RelationshipType... types )
    {
        return view.getDatabase()
                   .getDirectRelationshipsFrom( view, from, includeManagedInfo, true, types );
    }

    public Set<ProjectRelationship<?>> findDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                  final RelationshipType... types )
    {
        return view.getDatabase()
                   .getDirectRelationshipsTo( view, to, includeManagedInfo, true, types );
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

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final GraphView view )
    {
        return view.getDatabase()
                   .getMissingProjects( view );
    }

    public Set<ProjectVersionRef> getAllIncompleteSubgraphs( final GraphWorkspace workspace )
    {
        return getAllIncompleteSubgraphs( new GraphView( workspace ) );
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

    public Map<String, String> getMetadata( final GraphWorkspace workspace, final ProjectVersionRef ref )
    {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, String> metadata = workspace.getDatabase()
                                                      .getMetadata( ref );
        if ( metadata != null )
        {
            result.putAll( metadata );
        }

        result.put( GROUP_ID, ref.getGroupId() );
        result.put( ARTIFACT_ID, ref.getArtifactId() );
        result.put( VERSION, ref.getVersionString() );

        return result;
    }

    public Map<String, String> getMetadata( final GraphWorkspace workspace, final ProjectVersionRef ref, final Set<String> keys )
    {
        final Map<String, String> result = new HashMap<String, String>();
        final Map<String, String> metadata = workspace.getDatabase()
                                                      .getMetadata( ref, keys );
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

    public Map<Map<String, String>, Set<ProjectVersionRef>> collateByMetadata( final GraphWorkspace workspace, final Set<ProjectVersionRef> refs,
                                                                               final Set<String> keys )
    {
        final Map<Map<String, String>, Set<ProjectVersionRef>> result = new HashMap<Map<String, String>, Set<ProjectVersionRef>>();
        for ( final ProjectVersionRef ref : refs )
        {
            final Map<String, String> metadata = getMetadata( workspace, ref, keys );
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

    public void addMetadata( final GraphWorkspace workspace, final EProjectKey key, final String name, final String value )
        throws GraphDriverException
    {
        workspace.getDatabase()
                 .addMetadata( key.getProject(), name, value );
    }

    public void setMetadata( final GraphWorkspace workspace, final EProjectKey key, final Map<String, String> metadata )
        throws GraphDriverException
    {
        workspace.getDatabase()
                 .setMetadata( key.getProject(), metadata );
    }

    public void addMetadata( final GraphWorkspace workspace, final ProjectVersionRef project, final String name, final String value )
        throws GraphDriverException
    {
        workspace.getDatabase()
                 .addMetadata( project, name, value );
    }

    public void setMetadata( final GraphWorkspace workspace, final ProjectVersionRef project, final Map<String, String> metadata )
        throws GraphDriverException
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
        throws GraphDriverException
    {
        workspace.getDatabase()
                 .addDisconnectedProject( ref );
    }

    public void deleteRelationshipsDeclaredBy( final GraphView view, final ProjectVersionRef ref )
        throws GraphDriverException
    {
        view.getDatabase()
            .deleteRelationshipsDeclaredBy( ref );
    }

    public void deleteRelationshipsDeclaredBy( final GraphWorkspace workspace, final ProjectVersionRef ref )
        throws GraphDriverException
    {
        workspace.getDatabase()
                 .deleteRelationshipsDeclaredBy( ref );
    }

    @Override
    public synchronized void close()
        throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        StringWriter sw = new StringWriter();

        for ( final Entry<String, GraphWorkspace> entry : new HashMap<String, GraphWorkspace>( loadedWorkspaces ).entrySet() )
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
                new GraphDriverException( "Failed to close workspace: {}.", e, wsid ).printStackTrace( new PrintWriter( sw ) );
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
                logger.error( String.format( "Failed to store workspace %s on detach. Reason: %s", ws.getId(), e.getMessage() ), e );
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
                logger.error( String.format( "Failed to delete temporary workspace: %s. Reason: %s", workspace.getId(), e.getMessage() ), e );
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
                logger.error( String.format( "Failed to store updates for workspace: %s. Reason: %s", workspace, e.getMessage() ), e );
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
