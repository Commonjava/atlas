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
package org.commonjava.maven.atlas.ident.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ArtifactPathInfoTest
{

    @Test
    public void matchSnapshotUIDVersion()
    {
        final String path =
            "/path/to/unsigner-maven-plugin/0.2-SNAPSHOT/unsigner-maven-plugin-0.2-20120307.200227-1.jar";
        assertThat( ArtifactPathInfo.parse( path )
                                    .isSnapshot(), equalTo( true ) );
    }

    @Test
    public void matchSnapshotNonUIDVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin/0.2-SNAPSHOT/unsigner-maven-plugin-0.2-SNAPSHOT.jar";
        assertThat( ArtifactPathInfo.parse( path )
                                    .isSnapshot(), equalTo( true ) );
    }

    @Test
    public void dontMatchNonSnapshotVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin/0.2/unsigner-maven-plugin-0.2.jar";
        assertThat( ArtifactPathInfo.parse( path )
                                    .isSnapshot(), equalTo( false ) );
    }

}
