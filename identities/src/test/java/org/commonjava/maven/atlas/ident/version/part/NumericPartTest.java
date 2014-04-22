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
package org.commonjava.maven.atlas.ident.version.part;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.part.NumericPart;
import org.junit.Test;

public class NumericPartTest
{

    @Test
    public void largeNumericVersionsEqual()
        throws InvalidVersionSpecificationException
    {
        assertThat( new NumericPart( "20050331" ), equalTo( new NumericPart( "20050331" ) ) );
    }

}
