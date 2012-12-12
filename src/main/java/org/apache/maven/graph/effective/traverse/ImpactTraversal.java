package org.apache.maven.graph.effective.traverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;

public class ImpactTraversal
    extends AbstractTraversal
{

    private final Map<ProjectVersionRef, Set<List<ProjectRelationship<?>>>> impactedPaths =
        new HashMap<ProjectVersionRef, Set<List<ProjectRelationship<?>>>>();

    private final boolean includeManagedInfo;

    public ImpactTraversal( final ProjectVersionRef... targets )
    {
        this( false, targets );
    }

    public ImpactTraversal( final boolean includeManagedInfo, final ProjectVersionRef... targets )
    {
        this.includeManagedInfo = includeManagedInfo;
        for ( final ProjectVersionRef target : targets )
        {
            impactedPaths.put( target, new HashSet<List<ProjectRelationship<?>>>() );
        }
    }

    public ImpactTraversal( final Set<ProjectVersionRef> targets )
    {
        this( false, targets );
    }

    public ImpactTraversal( final boolean includeManagedInfo, final Set<ProjectVersionRef> targets )
    {
        this.includeManagedInfo = includeManagedInfo;
        for ( final ProjectVersionRef target : targets )
        {
            impactedPaths.put( target, new HashSet<List<ProjectRelationship<?>>>() );
        }
    }

    public Map<ProjectVersionRef, Set<List<ProjectRelationship<?>>>> getImpactedPaths()
    {
        return impactedPaths;
    }

    @Override
    public boolean traverseEdge( final ProjectRelationship<?> relationship, final List<ProjectRelationship<?>> path,
                                 final int pass )
    {
        if ( !includeManagedInfo )
        {
            if ( relationship instanceof DependencyRelationship
                && ( (DependencyRelationship) relationship ).isManaged() )
            {
                return false;
            }

            if ( relationship instanceof PluginRelationship && ( (PluginRelationship) relationship ).isManaged() )
            {
                return false;
            }
        }

        ProjectVersionRef target = relationship.getTarget();
        if ( target instanceof ArtifactRef )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        final Set<List<ProjectRelationship<?>>> paths = impactedPaths.get( target );
        if ( paths != null )
        {
            if ( !path.isEmpty() )
            {
                paths.add( new ArrayList<ProjectRelationship<?>>( path ) );
            }

            // we've seen an impact target, we don't need to go further.
            return false;
        }

        // we may yet encounter the impact targets, so allow this traverse to proceed.
        return true;
    }

}
