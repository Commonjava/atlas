/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.atlas.ident.version;

import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.junit.Test;

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

}
