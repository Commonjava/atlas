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

import org.commonjava.maven.atlas.graph.model.GraphPath;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ManagedDependencyMutator
    extends VersionManagerMutator
    implements GraphMutator
{

    private static final long serialVersionUID = 1L;

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel, final GraphPath<?> path, final GraphView view )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY )
        {
            //            logger.debug( "No selections for relationships of type: {}", rel.getType() );
            return rel;
        }

        ProjectRelationship<?> mutated = super.selectFor( rel, path, view );
        if ( mutated == null || mutated == rel )
        {
            final ProjectVersionRef managed = view.getDatabase()
                                                  .getManagedTargetFor( rel.getTarget(), path, RelationshipType.DEPENDENCY );
            if ( managed != null )
            {
                mutated = rel.selectTarget( managed );
            }
        }

        return mutated == null ? rel : mutated;
    }
}
