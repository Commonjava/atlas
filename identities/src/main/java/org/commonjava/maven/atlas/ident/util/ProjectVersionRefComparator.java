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
