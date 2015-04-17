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
package org.commonjava.maven.atlas.ident.util;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Arrays;
import java.util.Collection;

public class JoinString
{

    private final String joint;

    private final Collection<?> items;

    public JoinString( final String joint, final Collection<?> items )
    {
        this.joint = joint;
        this.items = items;
    }

    public JoinString( final String joint, final Object[] items )
    {
        this.joint = joint;
        this.items = Arrays.asList( items );
    }

    @Override
    public String toString()
    {
        return join( items, joint );
    }

}
