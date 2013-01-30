package org.apache.maven.graph.spi;

import java.util.IllegalFormatException;

public class GraphDriverException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    private final Object[] params;

    public GraphDriverException( final String message, final Throwable error, final Object... params )
    {
        super( message, error );
        this.params = params;
    }

    public GraphDriverException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    @Override
    public String getLocalizedMessage()
    {
        return getMessage();
    }

    @Override
    public String getMessage()
    {
        String message = super.getMessage();
        if ( params != null && params.length > 0 )
        {
            try
            {
                final String formatted = String.format( message, params );
                message = formatted;
            }
            catch ( final IllegalFormatException e )
            {
            }
        }

        return message;
    }
}
