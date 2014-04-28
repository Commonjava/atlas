package org.commonjava.maven.atlas.graph;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.commonjava.maven.atlas.graph.spi.RelationshipGraphConnection;

public final class RelationshipGraph
    implements Closeable
{

    private List<RelationshipGraphListener> listeners;

    private final ViewParams params;

    private final RelationshipGraphConnection driver;

    RelationshipGraph( final ViewParams params, final RelationshipGraphConnection driver )
    {
        this.params = params;
        this.driver = driver;
    }

    public ViewParams getParams()
    {
        return params;
    }

    @Override
    public void close()
        throws IOException
    {
        if ( listeners != null )
        {
            for ( final RelationshipGraphListener listener : listeners )
            {
                listener.closing( this );
            }
        }

        try
        {
            driver.close();
        }
        finally
        {
            if ( listeners != null )
            {
                for ( final RelationshipGraphListener listener : listeners )
                {
                    listener.closed( this );
                }
            }
        }
    }

}
