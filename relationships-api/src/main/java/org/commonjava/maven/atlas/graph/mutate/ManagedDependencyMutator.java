package org.commonjava.maven.atlas.graph.mutate;

import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ManagedDependencyMutator
    extends AbstractVersionManagerMutator
    implements GraphMutator
{

    private final boolean accumulate;

    public ManagedDependencyMutator( final GraphView view, final boolean accumulate )
    {
        super( view );
        this.accumulate = accumulate;
    }

    public ManagedDependencyMutator( final EProjectNet net, final boolean accumulate )
    {
        super( net );
        this.accumulate = accumulate;
    }

    public ManagedDependencyMutator( final GraphView view, final VersionManager versions, final boolean accumulate )
    {
        super( view, versions );
        this.accumulate = accumulate;
    }

    public ManagedDependencyMutator( final EProjectNet net, final VersionManager versions, final boolean accumulate )
    {
        super( net, versions );
        this.accumulate = accumulate;
    }

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY )
        {
            return rel;
        }

        return super.selectFor( rel );
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY )
        {
            return this;
        }

        if ( accumulate )
        {
            final Set<ProjectRelationship<?>> rels =
                view.getDatabase()
                    .getDirectRelationshipsFrom( view, rel.getTarget()
                                                          .asProjectVersionRef(), true, false, RelationshipType.DEPENDENCY );

            final Map<ProjectRef, ProjectVersionRef> mapping = VersionManager.createMapping( RelationshipUtils.targets( rels ) );

            if ( mapping != null && !mapping.isEmpty() )
            {
                return new ManagedDependencyMutator( view, new VersionManager( versions, mapping ), accumulate );
            }
        }

        return this;
    }

}
