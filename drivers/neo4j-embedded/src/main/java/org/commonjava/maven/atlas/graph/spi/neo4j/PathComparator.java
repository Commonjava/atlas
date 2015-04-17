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
        final List<ProjectRelationship<?>> firstRels =
            Conversions.convertToRelationships( first.relationships(), cache );
        final List<ProjectRelationship<?>> secondRels =
            Conversions.convertToRelationships( second.relationships(), cache );

        return pathComparator.compare( firstRels, secondRels );
    }

}
