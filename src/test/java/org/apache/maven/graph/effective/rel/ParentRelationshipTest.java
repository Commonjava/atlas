package org.apache.maven.graph.effective.rel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.maven.graph.common.ref.VersionedProjectRef;
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
        final VersionedProjectRef projectRef =
            new VersionedProjectRef( "org.foo", "foobar", VersionUtils.createSingleVersion( "1.0" ) );

        final VersionedProjectRef project2Ref =
            new VersionedProjectRef( "org.foo", "footoo", VersionUtils.createSingleVersion( "1.0" ) );

        final VersionedProjectRef parentRef =
            new VersionedProjectRef( "org.foo", "foobar-parent", VersionUtils.createSingleVersion( "1" ) );

        final ParentRelationship pr = new ParentRelationship( projectRef, parentRef );
        final ParentRelationship pr2 = (ParentRelationship) pr.cloneFor( project2Ref );

        assertThat( pr.getDeclaring(), equalTo( projectRef ) );
        assertThat( pr2.getDeclaring(), equalTo( project2Ref ) );
        assertThat( pr.getTarget(), equalTo( parentRef ) );
        assertThat( pr2.getTarget(), equalTo( parentRef ) );
    }

}
