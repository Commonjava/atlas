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

public enum VersionPartSeparator
{

    BLANK( "" ), DASH( "-" ), UNDERSCORE( "_" ), DOT( "." );

    private String rendered;

    private VersionPartSeparator( final String rendered )
    {
        this.rendered = rendered;
    }

    public String getRenderedString()
    {
        return rendered;
    }

    public static VersionPartSeparator find( final String literal )
    {
        for ( final VersionPartSeparator vps : values() )
        {
            if ( vps.rendered.equals( literal ) )
            {
                return vps;
            }
        }

        return null;
    }

}
