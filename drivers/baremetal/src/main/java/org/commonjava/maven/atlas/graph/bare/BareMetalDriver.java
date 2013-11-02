package org.commonjava.maven.atlas.graph.bare;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.spi.GraphDatabaseDriver;
import org.commonjava.maven.atlas.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.graph.traverse.ProjectNetTraversal;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class BareMetalDriver
    implements GraphDatabaseDriver
{

    private final AtlasGraphDB graph;

    public BareMetalDriver()
    {
        this.graph = new AtlasGraphDB();
    }

    @Override
    public void close()
        throws IOException
    {
    }

    @Override
    public boolean addCycle( final EProjectCycle cycle )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addDisconnectedProject( final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addMetadata( final ProjectVersionRef ref, final String key, final String value )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMetadata( final ProjectVersionRef ref, final Map<String, String> metadata )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<ProjectRelationship<?>> addRelationships( final ProjectRelationship<?>... rel )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean clearSelectedVersions()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void reindex()
        throws GraphDriverException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void selectVersionFor( final ProjectVersionRef ref, final ProjectVersionRef selected )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void selectVersionForAll( final ProjectRef ref, final ProjectVersionRef selected )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsDeclaredBy( final GraphView view, final ProjectVersionRef root )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<? extends ProjectRelationship<?>> getRelationshipsTargeting( final GraphView view, final ProjectVersionRef root )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ProjectRelationship<?>> getAllRelationships( final GraphView view )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<List<ProjectRelationship<?>>> getAllPathsTo( final GraphView view, final ProjectVersionRef... projectVersionRefs )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean introducesCycle( final GraphView view, final ProjectRelationship<?> rel )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<ProjectVersionRef> getAllProjects( final GraphView view )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void traverse( final GraphView view, final ProjectNetTraversal traversal, final ProjectVersionRef... root )
        throws GraphDriverException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean containsProject( final GraphView view, final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsRelationship( final GraphView view, final ProjectRelationship<?> rel )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isMissing( final GraphView view, final ProjectVersionRef project )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasMissingProjects( final GraphView view )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<ProjectVersionRef> getMissingProjects( final GraphView view )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasVariableProjects( final GraphView view )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<ProjectVersionRef> getVariableProjects( final GraphView view )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<EProjectCycle> getCycles( final GraphView view )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectRelationship<?> rel )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCycleParticipant( final GraphView view, final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getMetadata( final ProjectVersionRef ref, final Set<String> keys )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ProjectVersionRef> getProjectsWithMetadata( final GraphView view, final String key )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsFrom( final GraphView view, final ProjectVersionRef from,
                                                                   final boolean includeManagedInfo, final RelationshipType... types )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ProjectRelationship<?>> getDirectRelationshipsTo( final GraphView view, final ProjectVersionRef to, final boolean includeManagedInfo,
                                                                 final RelationshipType... types )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ProjectVersionRef> getProjectsMatching( final ProjectRef projectRef, final GraphView view )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectVersionRef getSelectedFor( final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ProjectVersionRef, ProjectVersionRef> getSelections()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ProjectRef, ProjectVersionRef> getWildcardSelections()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasSelectionFor( final ProjectVersionRef ref )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasSelectionForAll( final ProjectRef ref )
    {
        // TODO Auto-generated method stub
        return false;
    }

}
