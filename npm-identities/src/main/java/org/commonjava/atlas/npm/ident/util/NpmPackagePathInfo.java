package org.commonjava.atlas.npm.ident.util;

import com.github.zafarkhaja.semver.Version;
import org.commonjava.atlas.maven.ident.util.PathInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.commonjava.atlas.npm.ident.util.NpmVersionUtils.valueOf;

/**
 * Created by ruhan on 11/2/18.
 */
public class NpmPackagePathInfo implements PathInfo
{
    private static final String EXT_TGZ = ".tgz";

    private static final String PACKAGE_PATH_REGEX = "/(.+)/-/(.+)\\" + EXT_TGZ;

    private static final int PACKAGE_NAME_GROUP = 1;

    private static final int PACKAGE_NAME_AND_VERSION_GROUP = 2;

    // e.g., /keycloak-connect/-/keycloak-connect-3.4.1.tgz
    public static NpmPackagePathInfo parse( final String path )
    {
        final Matcher matcher = Pattern.compile( PACKAGE_PATH_REGEX ).matcher( path.replace( '\\', '/' ) );
        if ( !matcher.matches() )
        {
            return null;
        }

        final String name = matcher.group( PACKAGE_NAME_GROUP );
        final String nameAndVersion = matcher.group( PACKAGE_NAME_AND_VERSION_GROUP );

        final String version = nameAndVersion.substring( name.length() + 1 );

        return new NpmPackagePathInfo( name, valueOf( version ), nameAndVersion + EXT_TGZ, path );
    }

    private String name;

    private Version version;

    private String file;

    private String fullPath;

    public NpmPackagePathInfo( String name, Version version, String file, String fullPath )
    {
        this.name = name;
        this.version = version;
        this.file = file;
        this.fullPath = fullPath;
    }

    @Override
    public String getFile()
    {
        return file;
    }

    @Override
    public String getFullPath()
    {
        return fullPath;
    }

    public String getName()
    {
        return name;
    }

    public Version getVersion()
    {
        return version;
    }
}
