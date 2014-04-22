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
package org.commonjava.maven.atlas.graph.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class DependencyFilterTest
{

    @Rule
    public TestName naming = new TestName();

    private URI testURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    @Test
    public void rejectTestScopeForRuntimeFilter()
        throws Exception
    {
        final DependencyFilter filter = new DependencyFilter( DependencyScope.runtime );
        final DependencyRelationship rel =
            new DependencyRelationship( testURI(), new ProjectVersionRef( "g", "a", "1" ),
                                        new ArtifactRef( "g", "b", "2", "jar", null, false ), DependencyScope.test, 0,
                                        false );

        assertThat( filter.accept( rel ), equalTo( false ) );
    }

}
