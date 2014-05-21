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
package org.commonjava.maven.atlas.graph.mutate;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class VersionManagerMutator
    implements GraphMutator
{

    private static final long serialVersionUID = 1L;

    private transient String longId;

    private transient String shortId;

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel, final GraphPath<?> path,
                                             final GraphView view )
    {
        final ProjectRef target = rel.getTarget()
                                     .asProjectRef();

        if ( view != null )
        {
            final ProjectVersionRef ref = view.getSelection( target );
            if ( ref != null )
            {
                return rel.selectTarget( ref );
            }
        }

        return rel;
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel, final GraphView view )
    {
        return this;
    }

    @Override
    public int hashCode()
    {
        return getClass().hashCode() + 1;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        return true;
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            final String abbreviatedPackage = getClass().getPackage()
                                                        .getName()
                                                        .replaceAll( "([a-zA-Z])[a-zA-Z]+", "$1" );

            sb.append( abbreviatedPackage )
              .append( '.' )
              .append( getClass().getSimpleName() );

            longId = sb.toString();
        }

        return longId;
    }

    @Override
    public String getCondensedId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

}
