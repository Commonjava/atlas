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
package org.apache.maven.graph.effective.filter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.junit.Test;

public class DependencyFilterTest
{

    @Test
    public void rejectTestScopeForRuntimeFilter()
        throws Exception
    {
        final DependencyFilter filter = new DependencyFilter( DependencyScope.runtime );
        final DependencyRelationship rel =
            new DependencyRelationship( new ProjectVersionRef( "g", "a", "1" ), new ArtifactRef( "g", "b", "2", "jar",
                                                                                                 null, false ),
                                        DependencyScope.test, 0, false );

        assertThat( filter.accept( rel ), equalTo( false ) );
    }

}
