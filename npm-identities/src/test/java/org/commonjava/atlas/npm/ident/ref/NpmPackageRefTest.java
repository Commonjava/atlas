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
package org.commonjava.atlas.npm.ident.ref;

import com.github.zafarkhaja.semver.Version;
import org.commonjava.atlas.npm.ident.util.NpmPackagePathInfo;
import org.commonjava.atlas.npm.ident.util.NpmVersionUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by ruhan on 10/17/18.
 */
public class NpmPackageRefTest
{
    @Test
    public void pathParseTest()
    {
        String path = "/keycloak-connect/-/keycloak-connect-3.4.1.tgz";

        NpmPackagePathInfo info = NpmPackagePathInfo.parse( path );
        assertTrue( info.getName().equals( "keycloak-connect" ) );
        assertTrue( info.getVersion().toString().equals( "3.4.1" ) );
        assertTrue( info.getFile().equals( "keycloak-connect-3.4.1.tgz" ) );
        assertTrue( info.getFullPath().equals( path ) );
    }

    @Test
    public void scopedPathParseTest()
    {
        String path = "/@hawtio/core-dts/-/core-dts-3.3.2.tgz";

        NpmPackagePathInfo info = NpmPackagePathInfo.parse( path );
        assertTrue( info.getName().equals( "@hawtio/core-dts" ) );
        assertTrue( info.getVersion().toString().equals( "3.3.2" ) );
        assertTrue( info.getFile().equals( "core-dts-3.3.2.tgz" ) );
        assertTrue( info.getFullPath().equals( path ) );
    }

    @Test
    public void versionTest()
    {
        Version v = NpmVersionUtils.valueOf( "1.0.0-rc.1+build.1" );

        int major = v.getMajorVersion(); // 1
        int minor = v.getMinorVersion(); // 0
        int patch = v.getPatchVersion(); // 0

        assertTrue( major == 1 );
        assertTrue( minor == 0 );
        assertTrue( patch == 0 );

        String normal = v.getNormalVersion();     // "1.0.0"
        String preRelease = v.getPreReleaseVersion(); // "rc.1"
        String build = v.getBuildMetadata();     // "build.1"

        assertTrue( normal.equals( "1.0.0" ) );
        assertTrue( preRelease.equals( "rc.1" ) );
        assertTrue( build.equals( "build.1" ) );

        String str = v.toString(); // "1.0.0-rc.1+build.1"
        assertTrue( str.equals( "1.0.0-rc.1+build.1" ) );
    }

    @Test
    public void serializeTest() throws IOException, ClassNotFoundException
    {
        Version v = NpmVersionUtils.valueOf( "1.0.0-rc.1+build.1" );
        NpmPackageRef ref = new NpmPackageRef( "test", v );

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( bos );
        oos.writeObject( ref );
        oos.close();

        byte[] bytes = bos.toByteArray();

        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
        NpmPackageRef ref2 = (NpmPackageRef) ois.readObject();
        ois.close();

        assertEquals( ref, ref2 );
    }
}
