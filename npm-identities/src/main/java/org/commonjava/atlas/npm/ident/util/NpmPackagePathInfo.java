/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private static final String PACKAGE_PATH_REGEX = "/((?:(.+)/)?(.+))/-/(.+)\\" + EXT_TGZ;

    private static final int SCOPED_PACKAGE_NAME_GROUP = 1;

    private static final int PACKAGE_SCOPE_GROUP = 2;

    private static final int PACKAGE_NAME_GROUP = 3;

    private static final int PACKAGE_NAME_AND_VERSION_GROUP = 4;


    /**
     * Parses an npm package path into fields. The path might be scoped or not. A package metadata path, e.g.
     * &quot;/keycloak-connect&quot;, cannot be parsed by this method.
     *
     * @param path
     *            parsed path starting with '/', e.g. /keycloak-connect/-/keycloak-connect-3.4.1.tgz or
     *            /@hawtio/core-dts/-/core-dts-3.3.2.tgz
     * @return parsed path into an NpmPackagePathInfo instance
     */
    public static NpmPackagePathInfo parse( final String path )
    {
        final Matcher matcher = Pattern.compile( PACKAGE_PATH_REGEX ).matcher( path.replace( '\\', '/' ) );
        if ( !matcher.matches() )
        {
            return null;
        }

        final String scopedName = matcher.group( SCOPED_PACKAGE_NAME_GROUP );
        final String name = matcher.group( PACKAGE_NAME_GROUP );
        final String nameAndVersion = matcher.group( PACKAGE_NAME_AND_VERSION_GROUP );

        final String version = nameAndVersion.substring( name.length() + 1 );

        return new NpmPackagePathInfo( scopedName, valueOf( version ), nameAndVersion + EXT_TGZ, path );
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

    /**
     * @return package name, can be scoped, e.g. &#64;hawtio/core-dts
     */
    public String getName()
    {
        return name;
    }

    public Version getVersion()
    {
        return version;
    }
}
