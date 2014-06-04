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
package org.commonjava.maven.atlas.ident.version;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.RangeVersionSpec;
import org.junit.Test;

public class VersionRangeTest
{

    @Test
    public void identialRangeEquality()
        throws InvalidVersionSpecificationException
    {
        final RangeVersionSpec r1 = VersionUtils.createRange( "[1.1.1-baz-1,1.1.1-baz-2]" );
        final RangeVersionSpec r2 = VersionUtils.createRange( "[1.1.1-baz-1,1.1.1-baz-2]" );

        assertThat( r1.renderStandard(), equalTo( r2.renderStandard() ) );
        assertThat( r1.hashCode(), equalTo( r2.hashCode() ) );
        assertThat( r1, equalTo( r2 ) );
        assertThat( r2, equalTo( r1 ) );
    }

}
