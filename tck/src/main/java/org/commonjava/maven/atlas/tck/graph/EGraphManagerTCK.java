/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.atlas.tck.graph;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;

public abstract class EGraphManagerTCK
    extends AbstractSPI_TCK
{

    @Test
    public void createPath_ReturnNullWhenTargetVersionIsAnExpression()
        throws Exception
    {
        final ProjectVersionRef from = new ProjectVersionRef( "org.from", "project", "1.0" );
        final ProjectVersionRef to = new ProjectVersionRef( "org.to", "artifact", "${version.target}" );

        final URI src = new URI( "test:source-uri" );
        final ProjectRelationship<?> rel = new DependencyRelationship( src, from, to.asArtifactRef( "jar", null ), DependencyScope.compile, 0, false );

        final GraphView view = new GraphView( simpleWorkspace(), from );
        final GraphPath<?> path = getManager().createPath( view, rel );

        assertThat( path, nullValue() );
    }

}
