package org.commonjava.maven.atlas.spi.neo4j.io;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.graph.common.DependencyScope;
import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.ProjectRef;
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.spi.GraphDriverException;
import org.commonjava.maven.atlas.spi.neo4j.effective.RelationshipTypeMapper;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public final class Conversions
{

    private static final Logger LOGGER = new Logger( Conversions.class );

    private static final String GROUP_ID = "groupId";

    private static final String ARTIFACT_ID = "artifactId";

    private static final String VERSION = "version";

    private static final String GAV = "gav";

    private static final String INDEX = "index";

    private static final String IS_REPORTING_PLUGIN = "reporting";

    private static final String IS_MANAGED = "managed";

    private static final String PLUGIN_GROUP_ID = "plugin-groupId";

    private static final String PLUGIN_ARTIFACT_ID = "plugin-artifactId";

    private static final String TYPE = "type";

    private static final String CLASSIFIER = "classifier";

    private static final String SCOPE = "scope";

    private static final String OPTIONAL = "optional";

    private static final String EXCLUDES = "excludes";

    private Conversions()
    {
    }

    public static void toNodeProperties( final ProjectVersionRef ref, final Node node )
    {
        node.setProperty( ARTIFACT_ID, ref.getArtifactId() );
        node.setProperty( GROUP_ID, ref.getGroupId() );
        node.setProperty( VERSION, ref.getVersionString() );
        node.setProperty( GAV, ref.toString() );
    }

    public static ProjectVersionRef toProjectVersionRef( final Node node )
    {
        final String g = (String) node.getProperty( GROUP_ID );
        final String a = (String) node.getProperty( ARTIFACT_ID );
        final String v = (String) node.getProperty( VERSION );

        return new ProjectVersionRef( g, a, v );
    }

    @SuppressWarnings( "incomplete-switch" )
    public static void toRelationshipProperties( final ProjectRelationship<?> rel, final Relationship relationship )
    {
        relationship.setProperty( INDEX, rel.getIndex() );
        switch ( rel.getType() )
        {
            case DEPENDENCY:
            {
                final DependencyRelationship specificRel = (DependencyRelationship) rel;
                toRelationshipProperties( (ArtifactRef) rel.getTarget(), relationship );
                relationship.setProperty( IS_MANAGED, specificRel.isManaged() );
                relationship.setProperty( SCOPE, specificRel.getScope()
                                                            .realName() );

                final Set<ProjectRef> excludes = specificRel.getExcludes();
                if ( excludes != null && !excludes.isEmpty() )
                {
                    final StringBuilder sb = new StringBuilder();
                    for ( final ProjectRef exclude : excludes )
                    {
                        if ( sb.length() > 0 )
                        {
                            sb.append( "," );
                        }

                        sb.append( exclude.toString() );
                    }

                    relationship.setProperty( EXCLUDES, sb.toString() );
                }

                break;
            }
            case PLUGIN_DEP:
            {
                toRelationshipProperties( (ArtifactRef) rel.getTarget(), relationship );

                final PluginDependencyRelationship specificRel = (PluginDependencyRelationship) rel;

                final ProjectRef plugin = specificRel.getPlugin();
                relationship.setProperty( PLUGIN_ARTIFACT_ID, plugin.getArtifactId() );
                relationship.setProperty( PLUGIN_GROUP_ID, plugin.getGroupId() );
                relationship.setProperty( IS_MANAGED, specificRel.isManaged() );

                break;
            }
            case PLUGIN:
            {
                final PluginRelationship specificRel = (PluginRelationship) rel;
                relationship.setProperty( IS_MANAGED, specificRel.isManaged() );
                relationship.setProperty( IS_REPORTING_PLUGIN, specificRel.isReporting() );

                break;
            }
        }
    }

    public static ProjectRelationship<?> toProjectRelationship( final Relationship rel )
        throws GraphDriverException
    {
        final RelationshipTypeMapper mapper = (RelationshipTypeMapper) rel.getType();

        final ProjectVersionRef from = toProjectVersionRef( rel.getStartNode() );
        final ProjectVersionRef to = toProjectVersionRef( rel.getEndNode() );
        final int index = (Integer) rel.getProperty( INDEX );

        switch ( mapper.atlasType() )
        {
            case DEPENDENCY:
            {
                final ArtifactRef artifact = toArtifactRef( to, rel );
                final boolean managed = (Boolean) rel.getProperty( IS_MANAGED );
                final String scopeStr = (String) rel.getProperty( SCOPE );
                final DependencyScope scope = DependencyScope.getScope( scopeStr );

                final String excludeStr = (String) rel.getProperty( EXCLUDES );
                final Set<ProjectRef> excludes = new HashSet<ProjectRef>();
                if ( excludeStr != null )
                {
                    final String[] e = excludeStr.split( "\\s*,\\s*" );
                    for ( final String ex : e )
                    {
                        final String[] parts = ex.split( ":" );
                        if ( parts.length != 2 )
                        {
                            LOGGER.error( "In: %s -> %s skipping invalid exclude specification: '%s'", from, artifact,
                                          ex );
                        }
                        else
                        {
                            excludes.add( new ProjectRef( parts[0], parts[1] ) );
                        }
                    }
                }

                return new DependencyRelationship( to, artifact, scope, index, managed,
                                                   excludes.toArray( new ProjectRef[] {} ) );
            }
            case PLUGIN_DEP:
            {
                final ArtifactRef artifact = toArtifactRef( to, rel );
                final String pa = (String) rel.getProperty( PLUGIN_ARTIFACT_ID );
                final String pg = (String) rel.getProperty( PLUGIN_GROUP_ID );
                final boolean managed = (Boolean) rel.getProperty( IS_MANAGED );

                return new PluginDependencyRelationship( from, new ProjectRef( pg, pa ), artifact, index, managed );
            }
            case PLUGIN:
            {
                final boolean managed = (Boolean) rel.getProperty( IS_MANAGED );
                final boolean reporting = (Boolean) rel.getProperty( IS_REPORTING_PLUGIN );

                return new PluginRelationship( from, to, index, managed, reporting );
            }
            case EXTENSION:
            {
                return new ExtensionRelationship( from, to, index );
            }
            case PARENT:
            {
                return new ParentRelationship( from, to );
            }
            default:
            {
                throw new GraphDriverException( "Invalid RelationshipType: %s.", mapper.atlasType() );
            }
        }
    }

    public static String id( final ProjectRelationship<?> rel )
    {
        return DigestUtils.shaHex( rel.toString() );
    }

    private static ArtifactRef toArtifactRef( final ProjectVersionRef ref, final Relationship rel )
    {
        final String type = (String) rel.getProperty( TYPE );
        final String classifier = (String) rel.getProperty( CLASSIFIER );
        final boolean optional = (Boolean) rel.getProperty( OPTIONAL );

        return new ArtifactRef( ref, type, classifier, optional );
    }

    private static void toRelationshipProperties( final ArtifactRef target, final Relationship relationship )
    {
        relationship.setProperty( TYPE, target.getType() );
        relationship.setProperty( CLASSIFIER, target.getClassifier() );
        relationship.setProperty( OPTIONAL, target.isOptional() );
    }

}
