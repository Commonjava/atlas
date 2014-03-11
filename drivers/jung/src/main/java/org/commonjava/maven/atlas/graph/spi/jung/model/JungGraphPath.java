package org.commonjava.maven.atlas.graph.spi.jung.model;

import java.util.Arrays;
import java.util.Iterator;

import org.commonjava.maven.atlas.graph.spi.model.GraphPath;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class JungGraphPath
    implements GraphPath<ProjectVersionRef>
{

    private final ProjectVersionRef[] nodes;

    public JungGraphPath( final ProjectVersionRef... nodes )
    {
        this.nodes = nodes;
    }

    public JungGraphPath( final JungGraphPath parent, final ProjectVersionRef child )
    {
        if ( parent == null )
        {
            nodes = new ProjectVersionRef[] { child };
        }
        else
        {
            final int parentLen = parent.nodes.length;
            this.nodes = new ProjectVersionRef[parentLen + 1];
            System.arraycopy( parent.nodes, 0, this.nodes, 0, parentLen );
            this.nodes[parentLen] = child;
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( nodes );
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
        final JungGraphPath other = (JungGraphPath) obj;
        if ( !Arrays.equals( nodes, other.nodes ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public Iterator<ProjectVersionRef> iterator()
    {
        return new Iterator<ProjectVersionRef>()
        {
            private int next = 0;

            @Override
            public boolean hasNext()
            {
                return nodes.length > next;
            }

            @Override
            public ProjectVersionRef next()
            {
                return nodes[next++];
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException( "Immutable array of GAV's. Remove not supported." );
            }
        };
    }

}
