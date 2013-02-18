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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.VersionUtils;
import org.junit.Test;

public class ParentRelationshipTest
{

    @Test
    public void cloneToDifferentProject()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef projectRef =
            new ProjectVersionRef( "org.foo", "foobar", VersionUtils.createSingleVersion( "1.0" ) );

        final ProjectVersionRef project2Ref =
            new ProjectVersionRef( "org.foo", "footoo", VersionUtils.createSingleVersion( "1.0" ) );

        final ProjectVersionRef parentRef =
            new ProjectVersionRef( "org.foo", "foobar-parent", VersionUtils.createSingleVersion( "1" ) );

        final ParentRelationship pr = new ParentRelationship( projectRef, parentRef );
        final ParentRelationship pr2 = (ParentRelationship) pr.cloneFor( project2Ref );

        assertThat( pr.getDeclaring(), equalTo( projectRef ) );
        assertThat( pr2.getDeclaring(), equalTo( project2Ref ) );
        assertThat( pr.getTarget(), equalTo( parentRef ) );
        assertThat( pr2.getTarget(), equalTo( parentRef ) );
    }

}
