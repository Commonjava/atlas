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
package org.commonjava.maven.atlas.ident;

import static org.commonjava.maven.atlas.ident.DependencyScope.embedded;
import static org.commonjava.maven.atlas.ident.DependencyScope.runtime;
import static org.commonjava.maven.atlas.ident.DependencyScope.toolchain;

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
