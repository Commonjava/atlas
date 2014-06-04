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

import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.junit.Test;

public class VersionUtilsTest
{

    @Test
    public void createSingleTimestampVersionFormat()
        throws Exception
    {
        final String spec = "20031129.200437";
        final SingleVersion version = VersionUtils.createSingleVersion( spec );

        System.out.println( version );
    }

}
