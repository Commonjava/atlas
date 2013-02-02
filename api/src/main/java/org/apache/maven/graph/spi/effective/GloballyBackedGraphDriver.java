package org.apache.maven.graph.spi.effective;

import java.util.Collection;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectNet;

public interface GloballyBackedGraphDriver
    extends EGraphDriver
{
    boolean includeGraph( ProjectVersionRef project );

    void restrictToRoots( final Collection<ProjectVersionRef> roots, final EProjectNet net );
}
