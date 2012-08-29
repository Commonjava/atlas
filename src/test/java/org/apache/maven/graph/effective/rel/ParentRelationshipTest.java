package org.apache.maven.graph.effective.rel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.apache.maven.graph.common.version.VersionUtils;
import org.apache.maven.graph.effective.rel.ParentRelationship;
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
