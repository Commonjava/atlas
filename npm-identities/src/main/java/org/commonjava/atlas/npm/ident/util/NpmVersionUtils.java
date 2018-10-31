package org.commonjava.atlas.npm.ident.util;

import com.github.zafarkhaja.semver.Version;

/**
 * Created by ruhan on 10/17/18.
 */
public class NpmVersionUtils
{
    public static Version valueOf( String ver )
    {
        return Version.valueOf( ver );
    }
}
