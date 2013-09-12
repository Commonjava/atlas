package org.commonjava.maven.atlas.ident.util;

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;

public class VersionlessArtifactRefComparator
    implements Comparator<VersionlessArtifactRef>
{

    @Override
    public int compare( final VersionlessArtifactRef f, final VersionlessArtifactRef s )
    {
        int comp = f.compareTo( s ); // compare groupId and artifactId ONLY.
        if ( comp == 0 )
        {
            final String fc = f.getClassifier();
            final String sc = s.getClassifier();

            if ( fc == null && sc == null )
            {
                comp = 0;
            }
            else if ( fc == null )
            {
                comp = 1;
            }
            else if ( sc == null )
            {
                comp = -1;
            }
            else
            {
                comp = fc.compareTo( sc );
            }
        }

        if ( comp == 0 )
        {
            final String ft = f.getType(); // never null
            final String st = s.getType(); // never null

            comp = ft.compareTo( st );
        }

        return comp;
    }

}
