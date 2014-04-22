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
package org.commonjava.maven.atlas.graph.spi.neo4j;

import java.util.Comparator;
import java.util.List;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipPathComparator;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.ConversionCache;
import org.commonjava.maven.atlas.graph.spi.neo4j.io.Conversions;
import org.neo4j.graphdb.Path;

public final class PathComparator
    implements Comparator<Path>
{

    public static final PathComparator INSTANCE = new PathComparator();

    private final RelationshipPathComparator pathComparator = RelationshipPathComparator.INSTANCE;

    private final ConversionCache cache = new ConversionCache();

    private PathComparator()
    {
    }

    @Override
    public int compare( final Path first, final Path second )
    {
        final List<ProjectRelationship<?>> firstRels = Conversions.convertToRelationships( first.relationships(), cache );
        final List<ProjectRelationship<?>> secondRels = Conversions.convertToRelationships( second.relationships(), cache );

        return pathComparator.compare( firstRels, secondRels );
    }

}
