/*******************************************************************************
 * Copyright (C) 2013 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.apache.maven.graph.effective.rel;

import static org.apache.maven.graph.effective.util.EGraphUtils.dependency;
import static org.apache.maven.graph.effective.util.EGraphUtils.projectVersion;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.junit.Test;

public class RelationshipPathComparatorTest
{

    @Test
    public void sortParentDependencyPathAheadOfDirectDependency()
        throws InvalidVersionSpecificationException
    {
        final List<List<ProjectRelationship<?>>> paths = new ArrayList<List<ProjectRelationship<?>>>();

        List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();

        final ProjectVersionRef root = projectVersion( "group.id", "my-artifact", "1.0" );

        final ProjectVersionRef dep = projectVersion( "org.group", "dep-1", "1.0" );
        rels.add( dependency( root, dep, 0 ) );
        rels.add( dependency( dep, projectVersion( "org.foo", "bar", "1.0" ), 0 ) );

        paths.add( rels );

        rels = new ArrayList<ProjectRelationship<?>>();

        final ProjectVersionRef parent = projectVersion( "group.id", "parent", "1" );

        rels.add( new ParentRelationship( root, parent ) );
        rels.add( dependency( parent, "org.foo", "bar", "1.1.1", 0 ) );

        paths.add( rels );

        Collections.sort( paths, new RelationshipPathComparator() );

        final List<ProjectRelationship<?>> result = paths.get( 0 );
        final ProjectRelationship<?> firstResult = result.get( 0 );

        assertThat( ( firstResult instanceof ParentRelationship ), equalTo( true ) );
    }

}
