package org.commonjava.maven.atlas.spi.jung.effective;

import org.apache.maven.graph.effective.session.EGraphSession;
import org.apache.maven.graph.effective.session.EGraphSessionConfiguration;

public class JungEGraphSession
    extends EGraphSession
{

    JungEGraphSession( final JungEGraphDriver driver, final EGraphSessionConfiguration config )
    {
        super( Long.toString( System.currentTimeMillis() ), driver, config );
    }

}
