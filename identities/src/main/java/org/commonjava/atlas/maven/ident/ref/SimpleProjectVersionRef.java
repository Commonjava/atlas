/**
 * Copyright (C) 2012-2022 Red Hat, Inc. (nos-devel@redhat.com)
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
package org.commonjava.atlas.maven.ident.ref;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.atlas.maven.ident.util.VersionUtils;
import org.commonjava.atlas.maven.ident.version.InvalidVersionSpecificationException;
import org.commonjava.atlas.maven.ident.version.SingleVersion;
import org.commonjava.atlas.maven.ident.version.VersionSpec;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Reference to a particular release of a project (or module, in terms of Maven builds). A release may contain many artifacts (see {@link SimpleArtifactRef}).
 *
 * @see {@link SimpleProjectRef}
 * @see {@link SimpleArtifactRef}
 *
 * @author jdcasey
 */
public class SimpleProjectVersionRef
    extends SimpleProjectRef
    implements ProjectVersionRef
{

    private static final long serialVersionUID = 1L;

    // NEVER null
    private VersionSpec versionSpec;

    private String versionString;

    public SimpleProjectVersionRef( final ProjectRef ref, final VersionSpec versionSpec )
    {
        this( ref.getGroupId(), ref.getArtifactId(), versionSpec, null );
    }

    public SimpleProjectVersionRef( final ProjectRef ref, final String versionSpec )
        throws InvalidVersionSpecificationException
    {
        this( ref.getGroupId(), ref.getArtifactId(), versionSpec );
    }

    SimpleProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec,
                             final String versionString )
    {
        super( groupId, artifactId );
        if ( versionSpec == null && StringUtils.isEmpty( versionString ) )
        {
            throw new InvalidRefException(
                    "Version spec AND string cannot both be empty for '" + groupId + ":" + artifactId + "'" );
        }

        this.versionString = versionString;
        this.versionSpec = versionSpec;
    }

    public SimpleProjectVersionRef( final String groupId, final String artifactId, final VersionSpec versionSpec )
    {
        this( groupId, artifactId, versionSpec, null );
    }

    public SimpleProjectVersionRef( final String groupId, final String artifactId, final String versionString )
        throws InvalidVersionSpecificationException
    {
        this( groupId, artifactId, null, versionString );
    }

    public <T extends ProjectVersionRef> SimpleProjectVersionRef( final ProjectVersionRef ref )
    {
        super( ref );
        this.versionSpec = ref.getVersionSpecRaw();
        this.versionString = ref.getVersionStringRaw();
    }

    public static ProjectVersionRef parse( final String gav )
    {
        final String[] parts = gav.split( ":" );
        if ( parts.length < 3 || isEmpty( parts[0] ) || isEmpty( parts[1] ) || isEmpty( parts[2] ) )
        {
            throw new InvalidRefException(
                                           "ProjectVersionRef must contain non-empty groupId, artifactId, AND version. (Given: '"
                                               + gav + "')" );
        }

        return new SimpleProjectVersionRef( parts[0], parts[1], parts[2] );
    }

    @Override
    public SimpleProjectVersionRef asProjectVersionRef()
    {
        return SimpleProjectVersionRef.class.equals( getClass() ) ? this : new SimpleProjectVersionRef( getGroupId(),
                                                                                            getArtifactId(),
                                                                                            getVersionSpecRaw(),
                                                                                            getVersionStringRaw() );
    }

    @Override
    public ArtifactRef asPomArtifact()
    {
        return asArtifactRef( "pom", null );
    }

    @Override
    public ArtifactRef asJarArtifact()
    {
        return asArtifactRef( "jar", null );
    }

    @Override
    public ArtifactRef asArtifactRef( final String type, final String classifier )
    {
        return new SimpleArtifactRef( this, type, classifier );
    }

    @Override
    public ArtifactRef asArtifactRef( final TypeAndClassifier tc )
    {
        return new SimpleArtifactRef( this, tc );
    }

    @Override
    public VersionSpec getVersionSpecRaw()
    {
        return versionSpec;
    }

    @Override
    public String getVersionStringRaw()
    {
        return versionString;
    }

    @Override
    public boolean isRelease()
    {
        return getVersionSpec().isRelease();
    }

    @Override
    public boolean isSpecificVersion()
    {
        return getVersionSpec().isSingle();
    }

    @Override
    public boolean matchesVersion( final SingleVersion version )
    {
        return getVersionSpec().contains( version );
    }

    @Override
    public SimpleProjectVersionRef selectVersion( final String version )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single, false );
    }

    @Override
    public SimpleProjectVersionRef selectVersion( final String version, final boolean force )
    {
        final SingleVersion single = VersionUtils.createSingleVersion( version );
        return selectVersion( single, force );
    }

    @Override
    public SimpleProjectVersionRef selectVersion( final SingleVersion version )
    {
        return selectVersion( version, false );
    }

    @Override
    public SimpleProjectVersionRef selectVersion( final SingleVersion version, final boolean force )
    {
        final VersionSpec versionSpec = getVersionSpec();
        if ( versionSpec.equals( version ) )
        {
            return this;
        }

        if ( !force && !versionSpec.contains( version ) )
        {
            throw new IllegalArgumentException( "Specified version: " + version.renderStandard()
                + " is not contained in spec: " + versionSpec.renderStandard() );
        }

        return newRef( getGroupId(), getArtifactId(), version );
    }

    @Override
    public SimpleProjectVersionRef newRef( final String groupId, final String artifactId, final SingleVersion version )
    {
        return new SimpleProjectVersionRef( groupId, artifactId, version );
    }

    @Override
    public VersionSpec getVersionSpec()
    {
        if ( versionSpec == null )
        {
            versionSpec = VersionUtils.createFromSpec( versionString );
        }
        return versionSpec;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( getVersionString() == null ) ? 0 : getVersionString().hashCode() );
        return result;
    }

    @Override
    public boolean versionlessEquals( final ProjectVersionRef other )
    {
        return this == other || super.equals( other );
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( !(obj instanceof ProjectVersionRef) )
        {
            return false;
        }

        final ProjectVersionRef other = (ProjectVersionRef) obj;
        boolean result = true;
        try
        {
            if ( getVersionSpec() == null )
            {
                if ( other.getVersionSpec() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionSpec().equals( other.getVersionSpec() ) )
            {
                result = false;
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            if ( getVersionString() == null )
            {
                if ( other.getVersionString() != null )
                {
                    result = false;
                }
            }
            else if ( !getVersionString().equals( other.getVersionString() ) )
            {
                result = false;
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        return String.format( "%s:%s:%s", getGroupId(), getArtifactId(), getVersionString() );
    }

    @Override
    public boolean isCompound()
    {
        return !getVersionSpec().isSingle();
    }

    @Override
    public boolean isSnapshot()
    {
        return getVersionSpec().isSnapshot();
    }

    @Override
    public String getVersionString()
    {
        if ( versionString == null )
        {
            versionString = versionSpec.renderStandard();
        }

        return versionString;
    }

    @Override
    public boolean isVariableVersion()
    {
        return isCompound() || ( isSpecificVersion() && ( (SingleVersion) getVersionSpec() ).isLocalSnapshot() );
    }

    @Override
    public int compareTo( final ProjectRef o )
    {
        int comp = super.compareTo( o );
        if ( comp == 0 && ( o instanceof ProjectVersionRef ) )
        {
            final ProjectVersionRef or = (ProjectVersionRef) o;
            comp = getVersionString().compareTo( or.getVersionString() );
        }

        return comp;
    }

}
