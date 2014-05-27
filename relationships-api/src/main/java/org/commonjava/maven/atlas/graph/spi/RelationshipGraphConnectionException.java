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
package org.commonjava.maven.atlas.graph.spi;

import org.commonjava.maven.atlas.graph.RelationshipGraphException;

public class RelationshipGraphConnectionException
    extends RelationshipGraphException
{

    private static final long serialVersionUID = 1L;

    public RelationshipGraphConnectionException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

    public RelationshipGraphConnectionException( final String message, final Object... params )
    {
        super( message, params );
    }

}
