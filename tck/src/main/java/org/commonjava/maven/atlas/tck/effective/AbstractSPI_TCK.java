package org.commonjava.maven.atlas.tck.effective;

import org.apache.maven.graph.spi.effective.EGraphDriver;

public abstract class AbstractSPI_TCK
{
    
    protected abstract EGraphDriver newDriverInstance() throws Exception;

}
