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
package org.commonjava.maven.atlas.ident.ref;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.VersionSpec;
import org.junit.Test;

public class ProjectVersionRefTest
{

    @Test
    public void constructWithStringVersionAndRenderStandardSpecMatches()
        throws InvalidVersionSpecificationException
    {
        final String ver = "2.1.1.Final";
        final ProjectVersionRef ref = new ProjectVersionRef( "g", "a", ver );
        final VersionSpec spec = ref.getVersionSpec();

        assertThat( spec.renderStandard(), equalTo( ver ) );
    }

    @Test
    public void hashCodeEquality()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1.hashCode(), equalTo( ref2.hashCode() ) );
    }

    @Test
    public void objectEquality()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1, equalTo( ref2 ) );
    }

    @Test
    public void addTwoIdenticalRefsToASetAndVerifyThatOnlyOneIsAdded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "1.1.1-baz-1" );

        assertThat( ref1, equalTo( ref2 ) );

        final Set<ProjectVersionRef> set = new HashSet<ProjectVersionRef>();
        assertThat( set.add( ref1 ), equalTo( true ) );
        assertThat( set.add( ref2 ), equalTo( false ) );
    }

    @Test
    public void addTwoIdenticalCompoundRefsToASetAndVerifyThatOnlyOneIsAdded()
        throws InvalidVersionSpecificationException
    {
        final ProjectVersionRef ref1 = new ProjectVersionRef( "org.foo", "bar", "[1.1.1-baz-1,1.1.1-baz-2]" );
        final ProjectVersionRef ref2 = new ProjectVersionRef( "org.foo", "bar", "[1.1.1-baz-1,1.1.1-baz-2]" );

        assertThat( ref1, equalTo( ref2 ) );

        final Set<ProjectVersionRef> set = new HashSet<ProjectVersionRef>();
        assertThat( set.add( ref1 ), equalTo( true ) );
        assertThat( set.add( ref2 ), equalTo( false ) );
    }

}
