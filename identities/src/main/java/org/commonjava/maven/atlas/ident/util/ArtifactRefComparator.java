package org.commonjava.maven.atlas.ident.util;

import java.util.Comparator;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

public class ArtifactRefComparator
    implements Comparator<ArtifactRef>
{

    private final ProjectVersionRefComparator pvrComp = new ProjectVersionRefComparator();

    @Override
    public int compare( final ArtifactRef f, final ArtifactRef s )
    {
        int comp = pvrComp.compare( f, s ); // compare groupId, artifactId, and version spec.
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
