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
