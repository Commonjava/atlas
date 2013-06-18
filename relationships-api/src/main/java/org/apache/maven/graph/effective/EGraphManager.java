package org.apache.maven.graph.effective;

import static org.apache.maven.graph.spi.effective.EProjectNetView.GLOBAL;

import java.util.Collection;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.SingleVersion;
import org.apache.maven.graph.effective.filter.ProjectRelationshipFilter;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.session.EGraphSession;
import org.apache.maven.graph.effective.session.EGraphSessionConfiguration;
import org.apache.maven.graph.spi.GraphDriverException;
import org.apache.maven.graph.spi.effective.EGraphDriver;

public class EGraphManager
{

    private final EGraphDriver rootDriver;

    public EGraphManager( final EGraphDriver rootDriver )
    {
        this.rootDriver = rootDriver;
    }

    public void storeRelationships( final ProjectRelationship<?>... rels )
    {
        rootDriver.addRelationships( rels );
    }

    public void storeRelationships( final Collection<ProjectRelationship<?>> rels )
    {
        rootDriver.addRelationships( rels.toArray( new ProjectRelationship<?>[rels.size()] ) );
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

    public EProjectWeb getWeb( final EGraphSession session, final ProjectVersionRef... refs )
        throws GraphDriverException
    {
        return getWeb( session, null, refs );
    }

    public EProjectWeb getWeb( final EGraphSession session, final ProjectRelationshipFilter filter,
                               final ProjectVersionRef... refs )
        throws GraphDriverException
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
        private final EGraphManager manager;

        public EGraphSessionImpl( final String id, final EGraphSessionConfiguration config, final EGraphManager manager )
        {
            super( id, config );
            this.manager = manager;
        }

        @Override
        protected void selectionAdded( final ProjectVersionRef ref, final SingleVersion version )
            throws GraphDriverException
        {
            manager.selectVersionFor( ref, version, getId() );
        }

        @Override
        protected void sessionClosed()
        {
            manager.deleteSession( getId() );
        }

        @Override
        protected void selectionsCleared()
        {
            manager.clearSelectedVersions( getId() );
        }
    }

    private void selectVersionFor( final ProjectVersionRef ref, final SingleVersion version, final String id )
    {
        rootDriver.selectVersionFor( ref, version, id );
    }

    private void deleteSession( final String id )
    {
        rootDriver.deRegisterSession( id );
    }

    private void clearSelectedVersions( final String id )
    {
        rootDriver.clearSelectedVersionsFor( id );
    }

}
