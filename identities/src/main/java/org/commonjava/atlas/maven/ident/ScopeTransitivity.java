/**
 * Copyright (C) 2012 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.ident;

import static org.commonjava.atlas.maven.ident.DependencyScope.embedded;
import static org.commonjava.atlas.maven.ident.DependencyScope.runtime;
import static org.commonjava.atlas.maven.ident.DependencyScope.toolchain;

public enum ScopeTransitivity
{
    maven
    {
        @Override
        public DependencyScope getChildFor( final DependencyScope scope )
        {
            switch ( scope )
            {
                case provided:
                    return null;
                case embedded:
                    return embedded;
                case toolchain:
                    return toolchain;
                default:
                    return runtime;
            }
        }
    },

    all
    {
        @Override
        public DependencyScope getChildFor( final DependencyScope scope )
        {
            return scope;
        }
    };

    public abstract DependencyScope getChildFor( DependencyScope scope );

}
