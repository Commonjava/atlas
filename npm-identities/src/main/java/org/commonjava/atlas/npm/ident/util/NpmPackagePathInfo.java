package org.commonjava.atlas.npm.ident.util;

import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.commonjava.atlas.npm.ident.util.NpmVersionUtils.valueOf;

/**
 * Created by ruhan on 11/2/18.
 */
public class NpmPackagePathInfo
{
    private static final String PACKAGE_PATH_REGEX = "/(.+)/-/(.+)\\.tgz";

    private static final int PACKAGE_NAME_GROUP = 1;

    private static final int PACKAGE_NAME_AND_VERSION_GROUP = 2;

    // e.g., /keycloak-connect/-/keycloak-connect-3.4.1.tgz
    public static NpmPackageRef parse( final String path )
    {
        final Matcher matcher = Pattern.compile( PACKAGE_PATH_REGEX ).matcher( path.replace( '\\', '/' ) );
        if ( !matcher.matches() )
        {
            return null;
        }

        final String name = matcher.group( PACKAGE_NAME_GROUP );
        final String nameAndVersion = matcher.group( PACKAGE_NAME_AND_VERSION_GROUP );

        final String version = nameAndVersion.substring( name.length() + 1 );

        return new NpmPackageRef( name, valueOf( version ) );
    }
}
