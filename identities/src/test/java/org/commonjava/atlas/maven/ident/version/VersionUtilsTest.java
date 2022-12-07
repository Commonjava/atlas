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
package org.commonjava.atlas.maven.ident.version;

import org.commonjava.atlas.maven.ident.util.VersionUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VersionUtilsTest
{

    @Test
    public void createSingleTimestampVersionFormat()
        throws Exception
    {
        final String spec = "20031129.200437";
        final SingleVersion version = VersionUtils.createSingleVersion( spec );

        System.out.println( version );
    }

    @Test
    public void SingleVersionStringValidationTest() throws Exception
    {
        final String invalid1 = "abc@1";
        final String invalid2 = "abc//1";
        final String valid = "abc123a.";

        assertThat( VersionUtils.isValidSingleVersion( invalid1 ), equalTo( false ) );
        assertThat( VersionUtils.isValidSingleVersion( invalid2 ), equalTo( false ) );
        assertThat( VersionUtils.isValidSingleVersion( valid ), equalTo( true ) );
    }
}
