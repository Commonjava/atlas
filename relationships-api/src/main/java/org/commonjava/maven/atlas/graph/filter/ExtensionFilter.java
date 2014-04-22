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

import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;

// TODO: Do we need to consider excludes in the extensions?
public class ExtensionFilter
    extends AbstractTypedFilter
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ExtensionFilter()
    {
        super( RelationshipType.EXTENSION, RelationshipType.DEPENDENCY, false, true );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        if ( parent instanceof ExtensionRelationship )
        {
            return new OrFilter( new DependencyFilter( DependencyScope.runtime ), ParentFilter.EXCLUDE_TERMINAL_PARENTS );
        }
        else
        {
            return NoneFilter.INSTANCE;
        }
    }

}
