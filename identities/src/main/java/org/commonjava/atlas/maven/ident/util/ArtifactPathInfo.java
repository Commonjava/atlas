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
package org.commonjava.atlas.maven.ident.util;

import org.commonjava.atlas.maven.ident.ref.ArtifactRef;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.ref.SimpleArtifactRef;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectVersionRef;
import org.commonjava.atlas.maven.ident.version.part.SnapshotPart;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactPathInfo implements PathInfo
{
    private static final String GROUP_REGEX = "(([^/]+/)*[^/]+)"; // group 1~2

    private static final String ARTIFACT_REGEX = "([^/]+)"; // group 3

    private static final String VERSION_RAW_REGEX = "(([^/]+)(-SNAPSHOT)?)"; // group 4~6

    private static final String CLASSIFIER_AND_TYPE = "-?(.+)";

    // regex developed at: http://fiddle.re/tvk5
    private static final String ARTIFACT_PATH_REGEX =
            "/?" + GROUP_REGEX + "/" + ARTIFACT_REGEX + "/" + VERSION_RAW_REGEX + "/(\\3-((\\4)|(\\5-"
                    + SnapshotUtils.RAW_REMOTE_SNAPSHOT_PART_PATTERN + "))" + CLASSIFIER_AND_TYPE + ")";

    private static final int GROUP_ID_GROUP = 1;

    private static final int ARTIFACT_ID_GROUP = 3;

    private static final int VERSION_RAW_GROUP = 4;

    private static final int FILE_GROUP = 7;

    private static final int VERSION_GROUP = 8;

    private static final String TAR_GZ = "tar.gz";

    public static ArtifactPathInfo parse( final String path )
    {
        if ( path == null || path.length() < 1 )
        {
            return null;
        }

        final Matcher matcher = Pattern.compile( ARTIFACT_PATH_REGEX )
                                       .matcher( path.replace( '\\', '/' ) );
        if ( !matcher.matches() )
        {
            return null;
        }

        final int groupCount = matcher.groupCount();
        final String g = matcher.group( GROUP_ID_GROUP )
                                .replace( '/', '.' );
        final String a = matcher.group( ARTIFACT_ID_GROUP );
        final String v = matcher.group( VERSION_GROUP );

        String c = "";
        String t;

        String left = matcher.group( groupCount );
        
        // The classifier can contain dots or hyphens, it is hard to separate it from type. e.g,
        // wildfly8.1.3.jar, project-sources.tar.gz, etc. We don't have a very solid pattern to match the classifier.
        // Here we use the best guess.
        if ( left.endsWith( TAR_GZ ) )
        {
            t = TAR_GZ;
        }
        else
        {
            t = left.substring( left.lastIndexOf( "." ) + 1 ); // Otherwise, use the simple file ext as type
        }
        int extLen = t.length() + 1; // plus len of "."
        int leftLen = left.length();
        if ( leftLen > extLen )
        {
            c = left.substring( 0, leftLen - extLen );
        }

        final String f = matcher.group( FILE_GROUP );

        return new ArtifactPathInfo( g, a, v, c, t, f, path );
    }

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String classifier;

    private final String type;

    private final String file;

    private final String fullPath;

    private final boolean isSnapshot;

    private final String releaseVersion;

    public ArtifactPathInfo( final String groupId, final String artifactId, final String version, final String file,
                             final String fullPath )
    {
        this( groupId, artifactId, version, null, "jar", file, fullPath );
    }

    public ArtifactPathInfo( final String groupId, final String artifactId, final String version,
                             final String classifier, final String type, final String file, final String fullPath )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
        this.file = file;
        this.fullPath = fullPath;
        this.isSnapshot = SnapshotUtils.isSnapshotVersion( version );
        this.releaseVersion = calcReleaseVersion( version );
    }

    private String calcReleaseVersion( String version )
    {
        int index = version.indexOf( "-" );
        if ( index > 0 )
        {
            return version.substring( 0, index );
        }
        return version;
    }

    public String getReleaseVersion()
    {
        return releaseVersion;
    }

    public boolean isSnapshot()
    {
        return isSnapshot;
    }

    public synchronized SnapshotPart getSnapshotInfo()
    {
        return SnapshotUtils.extractSnapshotVersionPart(version);
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getType()
    {
        return type;
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

    @Override
    public String toString()
    {
        return String.format( "ArtifactPathInfo [groupId=%s, artifactId=%s, version=%s, file=%s]", groupId, artifactId,
                              version, file );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = prime * result + ( ( file == null ) ? 0 : file.hashCode() );
        result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactPathInfo other = (ArtifactPathInfo) obj;
        if ( artifactId == null )
        {
            if ( other.artifactId != null )
            {
                return false;
            }
        }
        else if ( !artifactId.equals( other.artifactId ) )
        {
            return false;
        }
        if ( file == null )
        {
            if ( other.file != null )
            {
                return false;
            }
        }
        else if ( !file.equals( other.file ) )
        {
            return false;
        }
        if ( groupId == null )
        {
            if ( other.groupId != null )
            {
                return false;
            }
        }
        else if ( !groupId.equals( other.groupId ) )
        {
            return false;
        }
        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }
        return true;
    }

    public ProjectVersionRef getProjectId()
    {
        return new SimpleProjectVersionRef( getGroupId(), getArtifactId(), getVersion() );
    }

    public ArtifactRef getArtifact()
    {
        return new SimpleArtifactRef( getGroupId(), getArtifactId(), getVersion(), getType(), getClassifier() );
    }

}
