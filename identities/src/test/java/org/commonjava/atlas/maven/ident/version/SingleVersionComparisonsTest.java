/**
 * Copyright (C) 2012 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.atlas.maven.ident.version;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.apache.commons.lang.StringUtils.join;
import static org.junit.Assert.fail;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.commonjava.atlas.maven.ident.util.VersionUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Test SingleVersion.
 * 
 * @author jdcasey
 */
public class SingleVersionComparisonsTest
{
    private SingleVersion newVersion( final String version )
        throws InvalidVersionSpecificationException
    {
        final SingleVersion v = VersionUtils.createSingleVersion( version );
        System.out.println( "Parsed version: " + version + " to: " + v.getVersionPhrases() );

        return v;
    }

    // ************************************************************************
    // * ORIGINAL FROM MAVEN 3.0.4:
    // *
    // * Basic difference is that 'm' == 'MILESTONE', 'a' == 'ALPHA',
    // * and 'b' == 'BETA'. This is NOT dependent on what precedes it, as long
    // * it's not part of a larger string.
    // *
    // ************************************************************************
    // private static final String[] VERSIONS_QUALIFIER =
    // { "1-alpha2snapshot", "1-alpha2", "1-alpha-123", "1-beta-2", "1-beta123", "1-m2", "1-m11", "1-rc", "1-cr2",
    // "1-rc123", "1-SNAPSHOT", "1", "1-sp", "1-sp2", "1-sp123", "1-abc", "1-def", "1-pom-1", "1-1-snapshot",
    // "1-1", "1-2", "1-123" };
    //
    // private static final String[] VERSIONS_NUMBER =
    // { "2.0", "2-1", "2.0.a", "2.0.0.a", "2.0.2", "2.0.123", "2.1.0", "2.1-a", "2.1b", "2.1-c", "2.1-1", "2.1.0.1",
    // "2.2", "2.123", "11.a2", "11.a11", "11.b2", "11.b11", "11.m2", "11.m11", "11", "11.a", "11b", "11c", "11m" };
    // ************************************************************************

    private static final String[] VERSIONS_QUALIFIER = { "1-m2", "1-m11", "1-alpha2snapshot", "1-alpha2",
        "1-alpha-123", "1-beta-2", "1-beta123", "1-rc", "1-cr2", "1-rc123", "1-SNAPSHOT", "1", "1-sp", "1-sp2",
        "1-sp123", "1-abc", "1-def", "1-pom-1", "1-1-snapshot", "1-1", "1-2", "1-123" };

    private static final String[] VERSIONS_NUMBER = { "2.0.0.a", "2.0", "2-1", "2.0.2", "2.0.123", "2.1-a", "2.1b",
        "2.1.0", "2.1-c", "2.1-1", "2.1.0.1", "2.2", "2.123", "11m", "11.m2", "11.m11", "11.a", "11.a2", "11.a11",
        "11b", "11.b2", "11.b11", "11", "11c" };

    @Rule
    public TestName name = new TestName();

    private final List<String> failed = new ArrayList<String>();

    private final List<String> okay = new ArrayList<String>();

    @After
    public void checkErrors()
    {
        if ( !failed.isEmpty() )
        {
            final NumberFormat fmt = NumberFormat.getPercentInstance();
            fmt.setMaximumFractionDigits( 2 );

            final String message =
                name.getMethodName() + ": "
                    + ( fmt.format( failed.size() / ( (double) failed.size() + (double) okay.size() ) ) ) + " ("
                    + failed.size() + "/" + ( failed.size() + okay.size() ) + ") of version comparisons FAILED.";

            System.out.println( message );

            if ( !okay.isEmpty() )
            {
                System.out.println( "OKAY " + join( okay, "\nOKAY " ) );
            }

            System.out.println( "FAIL " + join( failed, "\nFAIL " ) );
            fail( message );
        }
        else
        {
            System.out.println( name.getMethodName() + ": All tests OKAY" );
        }
    }

    private void checkVersionsOrder( final String[] versions )
        throws InvalidVersionSpecificationException
    {
        final SingleVersion[] c = new SingleVersion[versions.length];
        for ( int i = 0; i < versions.length; i++ )
        {
            c[i] = newVersion( versions[i] );
        }

        for ( int i = 1; i < versions.length; i++ )
        {
            final String lowver = versions[i - 1];
            final SingleVersion low = c[i - 1];
            for ( int j = i; j < versions.length; j++ )
            {
                final String hiver = versions[j];
                final SingleVersion high = c[j];

                int comp = low.compareTo( high );
                checkTrue( "expected < 0: " + lowver + " < " + hiver + " (got: " + comp + ")", comp < 0 );

                comp = high.compareTo( low );
                checkTrue( "expected > 0: " + hiver + " > " + lowver + " (got: " + comp + ")", comp > 0 );
            }
        }
    }

    private void checkTrue( final String message, final boolean check )
    {
        if ( check )
        {
            okay.add( message );
        }
        else
        {
            failed.add( message );
        }
    }

    private void checkVersionsEqual( final String v1, final String v2 )
        throws InvalidVersionSpecificationException
    {
        final SingleVersion c1 = newVersion( v1 );
        final SingleVersion c2 = newVersion( v2 );

        int comp = c1.compareTo( c2 );
        checkTrue( "expected 0: " + v1 + " compareTo " + v2 + " (got: " + comp + ")", comp == 0 );

        comp = c2.compareTo( c1 );
        checkTrue( "expected 0: " + v2 + " compareTo " + v1 + " (got: " + comp + ")", comp == 0 );

        checkTrue( "expected same hashcode for " + v1 + " (" + c1.hashCode() + ") and " + v2 + " (" + c2.hashCode()
            + ")", c1.hashCode() == c2.hashCode() );

        checkTrue( "expected " + v1 + ".equals( " + v2 + " )", c1.equals( c2 ) );

        checkTrue( "expected " + v2 + ".equals( " + v1 + " )", c2.equals( c1 ) );
    }

    private void checkVersionsOrder( final String v1, final String v2 )
        throws InvalidVersionSpecificationException
    {
        final SingleVersion c1 = newVersion( v1 );
        final SingleVersion c2 = newVersion( v2 );

        System.out.println( "first version: '" + v1 + "' parsed to: " + c1 );
        System.out.println( "second version: '" + v2 + "' parsed to: " + c2 );
        System.out.println();

        int comp = c1.compareTo( c2 );
        checkTrue( "expected < 0: " + v1 + " < " + v2 + " (got: " + comp + ")", comp < 0 );

        comp = c2.compareTo( c1 );
        checkTrue( "expected > 0: " + v2 + " > " + v1 + " (got: " + comp + ")", comp > 0 );
    }

    @Test
    public void testZeroFill()
    {
        checkVersionsEqual( "7", "7.0.0" );
    }

    @Test
    public void testVersionsQualifier()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( SingleVersionComparisonsTest.VERSIONS_QUALIFIER );
    }

    @Test
    public void testVersionsNumber()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( SingleVersionComparisonsTest.VERSIONS_NUMBER );
    }

    @Test
    public void jumbledAlphaSnapSortsAfterBareSnap()
        throws InvalidVersionSpecificationException
    {
        final String s1 = "1-alpha2snapshot";
        final String s2 = "1-SNAPSHOT";

        final SingleVersion v1 = newVersion( s1 );
        final SingleVersion v2 = newVersion( s2 );

        checkTrue( "Expected: " + s1 + " < " + s2, v1.compareTo( v2 ) < 0 );
    }

    @Test
    public void largeNumericVersionsEqual()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "20050331", "20050331" );
    }

    @Test
    public void testVersionsEqual()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1", "1" );
        checkVersionsEqual( "1", "1.0" );
        checkVersionsEqual( "1", "1.0.0" );
        checkVersionsEqual( "1.0", "1.0.0" );
        checkVersionsEqual( "1", "1-0" );
        checkVersionsEqual( "1", "1.0-0" );
        checkVersionsEqual( "1.0", "1.0-0" );
        // no separator between number and character
        checkVersionsEqual( "1a", "1.a" );
        checkVersionsEqual( "1a", "1-a" );
        checkVersionsEqual( "1a", "1.0-a" );
        checkVersionsEqual( "1a", "1.0.0-a" );
        checkVersionsEqual( "1.0a", "1.0.a" );
        checkVersionsEqual( "1.0.0a", "1.0.0.a" );
        checkVersionsEqual( "1x", "1.x" );
        checkVersionsEqual( "1x", "1-x" );
        checkVersionsEqual( "1x", "1.0-x" );
        checkVersionsEqual( "1x", "1.0.0-x" );
        checkVersionsEqual( "1.0x", "1.0.x" );
        checkVersionsEqual( "1.0.0x", "1.0.0.x" );

        // aliases
        checkVersionsEqual( "1ga", "1" );
        checkVersionsEqual( "1final", "1" );
        checkVersionsEqual( "1cr", "1rc" );

        // special "aliases" a, b and m for alpha, beta and milestone
        checkVersionsEqual( "1a1", "1alpha1" );
        checkVersionsEqual( "1b2", "1beta2" );
        checkVersionsEqual( "1m3", "1milestone3" );

        // case insensitive
        checkVersionsEqual( "1X", "1x" );
        checkVersionsEqual( "1A", "1a" );
        checkVersionsEqual( "1B", "1b" );
        checkVersionsEqual( "1M", "1m" );
        checkVersionsEqual( "1Ga", "1" );
        checkVersionsEqual( "1GA", "1" );
        checkVersionsEqual( "1Final", "1" );
        checkVersionsEqual( "1FinaL", "1" );
        checkVersionsEqual( "1FINAL", "1" );
        checkVersionsEqual( "1Cr", "1Rc" );
        checkVersionsEqual( "1cR", "1rC" );
        checkVersionsEqual( "1m3", "1Milestone3" );
        checkVersionsEqual( "1m3", "1MileStone3" );
        checkVersionsEqual( "1m3", "1MILESTONE3" );
    }

    @Test
    public void testVersionComparing()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1", "2" );
        checkVersionsOrder( "1.5", "2" );
        checkVersionsOrder( "1", "2.5" );
        checkVersionsOrder( "1.0", "1.1" );
        checkVersionsOrder( "1.1", "1.2" );
        checkVersionsOrder( "1.0.0", "1.1" );
        checkVersionsOrder( "1.0.1", "1.1" );
        checkVersionsOrder( "1.1", "1.2.0" );

        checkVersionsOrder( "1.0-alpha-1", "1.0" );
        checkVersionsOrder( "1.0-alpha-1", "1.0-alpha-2" );
        checkVersionsOrder( "1.0-alpha-1", "1.0-beta-1" );

        checkVersionsOrder( "1.0-beta-1", "1.0-SNAPSHOT" );
        checkVersionsOrder( "1.0-SNAPSHOT", "1.0" );
        checkVersionsOrder( "1.0-alpha-1-SNAPSHOT", "1.0-alpha-1" );

        checkVersionsOrder( "1.0", "1.0-1" );
        checkVersionsOrder( "1.0-1", "1.0-2" );
        checkVersionsOrder( "1.0.0", "1.0-1" );

        checkVersionsOrder( "2.0-1", "2.0.1" );
        checkVersionsOrder( "2.0.1-klm", "2.0.1-lmn" );
        checkVersionsOrder( "2.0.1", "2.0.1-xyz" );

        checkVersionsOrder( "2.0.1", "2.0.1-123" );
        checkVersionsOrder( "2.0.1-xyz", "2.0.1-123" );
    }

    @Test
    public void testLocaleIndependent()
        throws InvalidVersionSpecificationException
    {
        final Locale orig = Locale.getDefault();
        final Locale[] locales = { Locale.ENGLISH, new Locale( "tr" ), Locale.getDefault() };
        try
        {
            for ( final Locale locale : locales )
            {
                Locale.setDefault( locale );
                checkVersionsEqual( "1-abcdefghijklmnopqrstuvwxyz", "1-ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
            }
        }
        finally
        {
            Locale.setDefault( orig );
        }
    }

    @Test
    public void dashSeparatorSortsAfterDotSeparator()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "2.0", "2-1" );
    }

    @Test
    public void dontIgnoreIntermediateZeros()
        throws InvalidVersionSpecificationException
    {
        markIncompatibility();
        checkVersionsEqual( "2.0.a", "2.0.0.a" );
    }

    @Test
    public void milestoneMarkerSortsBeforeAlphaMarker()
        throws InvalidVersionSpecificationException
    {
        markIncompatibility();
        checkVersionsOrder( "11.m2", "11.a2" );
    }

    @Test
    public void alphaMarkerSortsBeforeBetaMarker()
        throws InvalidVersionSpecificationException
    {
        markIncompatibility();
        checkVersionsOrder( "11.alpha2", "11.beta1" );
        checkVersionsOrder( "11.a2", "11.b1" );
    }

    @Test
    public void alphaMarkerSortsBeforeFinalRelease()
        throws InvalidVersionSpecificationException
    {
        markIncompatibility();
        checkVersionsOrder( "11.alpha2", "11" );
        checkVersionsOrder( "11.a2", "11" );
    }

    @Test
    public void randomStringSortsAfterRelease()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1.0", "1.0.z" );
    }

    @Test
    public void randomStringSortsAfterAlpha()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1.0.a", "1.0.z" );
    }

    @Test
    public void randomStringSortsAfterRelease_2()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1.0", "1.0z" );
    }

    @Test
    public void randomStringSortsAfterAlpha_2()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1.0.a", "1.0z" );
    }

    @Test
    public void servicePackSortsBeforeRandomString()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1.sp", "1.abc" );
    }

    @Test
    public void rebuildSortsAfterServicePack()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1.sp", "1-1" );
    }

    @Test
    public void rebuildSortsAfterRandomString()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1-abc", "1-1" );
    }

    @Test
    public void rebuildSnapshotSortsAfterRandomString()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1-abc", "1-1-SNAPSHOT" );
    }

    @Test
    public void snapshotSortsBeforeServicePack()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1-SNAPSHOT", "1-sp" );
    }

    @Test
    public void snapshotSortsAfterBeta()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1-beta", "1-SNAPSHOT" );
    }

    @Test
    public void snapshotSortsBeforeRelease()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1-SNAPSHOT", "1" );
    }

    @Test
    public void servicePackSortsAfterRelease()
        throws InvalidVersionSpecificationException
    {
        checkVersionsOrder( "1", "1-sp1" );
    }

    @Test
    public void equalsIgnoresTrailingZeros()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1", "1.0.0" );
    }

    @Test
    public void equalsIgnoresTrailingZerosInComplexVersion()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1-1", "1-1.0.0" );
    }

    @Test
    public void disregardSeparatorForEquality()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1a", "1.a" );
    }

    @Test
    public void randomStringsComparedCaseInsensitively()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1X", "1x" );
    }

    @Test
    public void disregardSeparatorForEquality2()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1.0a", "1.0.a" );
    }

    @Test
    public void disregardSeparatorForEquality3()
        throws InvalidVersionSpecificationException
    {
        checkVersionsEqual( "1a", "1.0-a" );
    }

    private void markIncompatibility()
    {
        System.out.println( name.getMethodName() + ": This is an INCOMPATIBILITY with maven-artifact" );
    }

}
