/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.atlas.maven.ident.version.part;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.atlas.maven.ident.util.SnapshotUtils;
import org.junit.Test;

public class SnapshotPartTest
{

    @Test
    public void compareTwoRemoteSnapshotsWithDifferentTimestamps_ParsedFromLiterals()
        throws Exception
    {
        final SnapshotPart first = new SnapshotPart( "20140604.124355-1" );
        final SnapshotPart second = new SnapshotPart( "20140604.124350-1" );

        final int result = first.compareTo( second );

        assertThat( result, equalTo( 1 ) );
    }

    @Test
    public void compareLocalVsRemoteSnapshot_LocalSortsLastAsMostRecent()
        throws Exception
    {
        final SnapshotPart first = new SnapshotPart( SnapshotUtils.LOCAL_SNAPSHOT_VERSION_PART );
        final SnapshotPart second = new SnapshotPart( "20140604.124350-1" );

        final int result = first.compareTo( second );

        assertThat( result, equalTo( 1 ) );
    }

}
