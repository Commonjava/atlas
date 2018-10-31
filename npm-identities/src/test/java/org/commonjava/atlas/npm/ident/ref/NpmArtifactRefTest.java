package org.commonjava.atlas.npm.ident.ref;

import com.github.zafarkhaja.semver.Version;
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
public class NpmArtifactRefTest
{
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
        NpmArtifactRef ref = new NpmArtifactRef( "test", v );

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( bos );
        oos.writeObject( ref );
        oos.close();

        byte[] bytes = bos.toByteArray();

        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
        NpmArtifactRef ref2 = (NpmArtifactRef) ois.readObject();
        ois.close();

        assertEquals( ref, ref2 );
    }
}
