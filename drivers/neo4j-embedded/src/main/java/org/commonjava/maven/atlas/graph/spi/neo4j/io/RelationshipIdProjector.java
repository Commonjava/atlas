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
package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import org.neo4j.graphdb.Relationship;

public class RelationshipIdProjector
    implements Projector<Relationship, Long>
{

    public Long project( final Relationship item )
    {
        return item.getId();
    }

}
