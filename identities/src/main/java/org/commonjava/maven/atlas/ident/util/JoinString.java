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
package org.commonjava.maven.atlas.ident.util;

import static org.apache.commons.lang.StringUtils.join;

import java.util.Arrays;
import java.util.Collection;

public class JoinString
{

    private final String joint;

    private final Collection<?> items;

    public JoinString( final String joint, final Collection<?> items )
    {
        this.joint = joint;
        this.items = items;
    }

    public JoinString( final String joint, final Object[] items )
    {
        this.joint = joint;
        this.items = Arrays.asList( items );
    }

    @Override
    public String toString()
    {
        return join( items, joint );
    }

}
