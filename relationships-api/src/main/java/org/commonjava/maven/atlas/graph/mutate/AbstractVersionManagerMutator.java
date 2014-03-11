package org.commonjava.maven.atlas.graph.mutate;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.model.GraphPath;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public abstract class AbstractVersionManagerMutator
    implements GraphMutator
{

    protected final GraphView view;

    protected AbstractVersionManagerMutator( final GraphView view )
    {
        this.view = view;
    }

    protected AbstractVersionManagerMutator( final EProjectNet net )
    {
        this.view = net.getView();
    }

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel, final GraphPath<?> path )
    {
        final ProjectRef target = rel.getTarget()
                                     .asProjectRef();

        final VersionManager selections = view.getSelections();
        if ( selections != null )
        {
            final ProjectVersionRef ref = selections.getSelected( target );
            if ( ref != null )
            {
                return rel.selectTarget( ref );
            }
        }

        return rel;
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel )
    {
        return this;
    }

    protected GraphView getView()
    {
        return view;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( view == null ) ? 0 : view.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final AbstractVersionManagerMutator other = (AbstractVersionManagerMutator) obj;
        if ( view == null )
        {
            if ( other.view != null )
            {
                return false;
            }
        }
        else if ( !view.equals( other.view ) )
        {
            return false;
        }
        return true;
    }

}
