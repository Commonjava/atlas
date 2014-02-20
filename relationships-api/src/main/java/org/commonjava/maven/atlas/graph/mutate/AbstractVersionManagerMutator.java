package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractVersionManagerMutator
    implements GraphMutator
{

    protected final VersionManager versions;

    protected final GraphView view;

    protected AbstractVersionManagerMutator( final GraphView view )
    {
        this.view = view;
        VersionManager vm = view.getSelections();
        if ( vm == null )
        {
            vm = new VersionManager();
        }
        versions = vm;
    }

    protected AbstractVersionManagerMutator( final EProjectNet net )
    {
        this.view = net.getView();
        VersionManager vm = view.getSelections();
        if ( vm == null )
        {
            vm = new VersionManager();
        }
        versions = vm;
    }

    protected AbstractVersionManagerMutator( final GraphView view, final VersionManager versions )
    {
        this.view = view;
        this.versions = versions;
    }

    protected AbstractVersionManagerMutator( final EProjectNet net, final VersionManager versions )
    {
        this.versions = versions;
        this.view = net.getView();
    }

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel )
    {
        final ProjectRef target = rel.getTarget()
                                     .asProjectRef();

        final ProjectVersionRef ref = versions.getSelected( target );
        if ( ref == null )
        {
            return rel;
        }

        return rel.selectTarget( ref );
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel )
    {
        return this;
    }

    protected VersionManager getVersions()
    {
        return versions;
    }

    protected GraphView getView()
    {
        return view;
    }

}
