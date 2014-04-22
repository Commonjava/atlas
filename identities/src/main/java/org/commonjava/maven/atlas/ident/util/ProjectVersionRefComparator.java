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

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.VersionSpec;

public class ProjectVersionRefComparator
    implements Comparator<ProjectVersionRef>
{

    @Override
    public int compare( final ProjectVersionRef f, final ProjectVersionRef s )
    {
        final int comp = f.compareTo( s ); // this compares groupId and artifactId ONLY
        if ( comp == 0 )
        {
            final VersionSpec fs = f.getVersionSpec();
            final VersionSpec ss = s.getVersionSpec();

            return fs.compareTo( ss );
        }

        return comp;
    }

}
