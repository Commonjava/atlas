/*******************************************************************************
 * Copyright (C) 2014 John Casey.
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
package org.commonjava.maven.atlas.graph.rel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.VersionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class ParentRelationshipTest
{

    @Rule
    public TestName naming = new TestName();

    private URI testURI()
        throws URISyntaxException
    {
        return new URI( "test:repo:" + naming.getMethodName() );
    }

    @Test
    public void cloneToDifferentProject()
        throws InvalidVersionSpecificationException, URISyntaxException
    {
        final ProjectVersionRef projectRef =
            new ProjectVersionRef( "org.foo", "foobar", VersionUtils.createSingleVersion( "1.0" ) );

        final ProjectVersionRef project2Ref =
            new ProjectVersionRef( "org.foo", "footoo", VersionUtils.createSingleVersion( "1.0" ) );

        final ProjectVersionRef parentRef =
            new ProjectVersionRef( "org.foo", "foobar-parent", VersionUtils.createSingleVersion( "1" ) );

        final URI source = testURI();
        final ParentRelationship pr = new ParentRelationship( source, projectRef, parentRef );
        final ParentRelationship pr2 = (ParentRelationship) pr.cloneFor( project2Ref );

        assertThat( pr.getDeclaring(), equalTo( projectRef ) );
        assertThat( pr2.getDeclaring(), equalTo( project2Ref ) );
        assertThat( pr.getTarget(), equalTo( parentRef ) );
        assertThat( pr2.getTarget(), equalTo( parentRef ) );
    }

}
