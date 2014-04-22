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

import java.io.Serializable;

public abstract class VersionPart
    implements Comparable<VersionPart>, Serializable
{

    private static final long serialVersionUID = 1L;

    private boolean silent = false;

    public abstract String renderStandard();

    final boolean isSilent()
    {
        return silent;
    }

    final void setSilent( final boolean silent )
    {
        this.silent = silent;
    }

}
