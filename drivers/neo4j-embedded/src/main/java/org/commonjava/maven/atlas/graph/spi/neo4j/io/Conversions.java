/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.maven.atlas.graph.spi.neo4j.io;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.POM_ROOT_URI;
import static org.commonjava.maven.atlas.graph.util.RelationshipUtils.UNKNOWN_SOURCE_URI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commonjava.maven.atlas.graph.model.GraphPathInfo;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphAdmin;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.NodeType;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.CyclePath;
import org.commonjava.maven.atlas.graph.spi.neo4j.model.Neo4jGraphPath;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Conversions
{

    private static final Logger LOGGER = LoggerFactory.getLogger( Conversions.class );

    public static final String RELATIONSHIP_ID = "relationship_id";

    public static final String GROUP_ID = "groupId";

    public static final String ARTIFACT_ID = "artifactId";

    public static final String VERSION = "version";

    public static final String GAV = "gav";

    public static final String GA = "ga";

    public static final String INDEX = "index";

    public static final String IS_REPORTING_PLUGIN = "reporting";

    public static final String IS_MANAGED = "managed";

    public static final String PLUGIN_GROUP_ID = "plugin_groupId";

    public static final String PLUGIN_ARTIFACT_ID = "plugin_artifactId";

    public static final String TYPE = "type";

    public static final String CLASSIFIER = "classifier";

    public static final String SCOPE = "scope";

    public static final String OPTIONAL = "optional";

    public static final String EXCLUDES = "excludes";

    public static final String CYCLE_ID = "cycle_id";

    public static final String CYCLE_RELATIONSHIPS = "relationship_participants";

    public static final String CYCLE_PROJECTS = "project_participants";

    private static final String METADATA_PREFIX = "_metadata_";

    public static final String NODE_TYPE = "_node_type";

    public static final String CYCLE_MEMBERSHIP = "cycle_membership";

    public static final String VARIABLE = "_variable";

    public static final String CONNECTED = "_connected";

    public static final String CYCLE_INJECTION = "_cycle_injection";

    public static final String CYCLES_INJECTED = "_cycles";

    public static final String SOURCE_URI = "source_uri";

    public static final String POM_LOCATION_URI = "pom_location_uri";

    public static final String LAST_ACCESS_DATE = "last_access";

    public static final String SELECTION = "_selection";

    // graph-level configuration.

    public static final String LAST_ACCESS = "last_access";

    public static final String ACTIVE_POM_LOCATIONS = "active-pom-locations";

    public static final String ACTIVE_SOURCES = "active-pom-sources";

    public static final String CONFIG_PROPERTY_PREFIX = "_p_";

    public static final String VIEW_SHORT_ID = "view_sid";

    private static final String VIEW_DATA = "view_data";

    // cached path tracking...ONLY handled by Conversions, since the info is inlined.

    private static final String PATH = "path";

    private static final String PATH_INFO_DATA = "path_info_data";

    // handled by other things, like updaters.

    public static final String CACHED_PATH_RELATIONSHIP = "cached_path_relationship";

    public static final String CACHED_PATH_CONTAINS_NODE = "cached_path_contains_node";

    public static final String CACHED_PATH_CONTAINS_REL = "cached_path_contains_rel";

    public static final String CACHED_PATH_TARGETS = "cached_path_targets";

    public static final String RID = "rel_id";

    public static final String NID = "node_id";

    public static final String CONFIG_ID = "config_id";

    public static final String VIEW_ID = "view_id";

    public static final String CYCLE_DETECTION_PENDING = "cycle_detect_pending";

    private Conversions()
    {
    }

    public static void storeConfig( final Node node, final GraphWorkspaceConfiguration config )
    {
        node.setProperty( LAST_ACCESS, config.getLastAccess() );
        node.setProperty( ACTIVE_POM_LOCATIONS, toStringArray( config.getActivePomLocations() ) );
        node.setProperty( ACTIVE_SOURCES, toStringArray( config.getActiveSources() ) );

        final Map<String, String> properties = config.getProperties();
        if ( properties != null )
        {
            for ( final Entry<String, String> entry : properties.entrySet() )
            {
                final String key = entry.getKey();
                final String value = entry.getValue();

                node.setProperty( CONFIG_PROPERTY_PREFIX + key, value );
            }
        }
    }

    public static int countArrayElements( final String property, final PropertyContainer container )
    {
        if ( !container.hasProperty( property ) )
        {
            return -1;
        }

        final Object value = container.getProperty( property );
        if ( value.getClass()
                  .isArray() )
        {
            final Object[] elements = (Object[]) value;
            return elements.length;
        }

        return 1;
    }

    public static List<ProjectVersionRef> convertToProjects( final Iterable<Node> nodes, final ConversionCache cache )
    {
        final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
        for ( final Node node : nodes )
        {
            if ( node.getId() == 0 )
            {
                continue;
            }

            if ( !Conversions.isType( node, NodeType.PROJECT ) )
            {
                continue;
            }

            refs.add( Conversions.toProjectVersionRef( node, cache ) );
        }

        return refs;
    }

    public static List<ProjectRelationship<?>> convertToRelationships( final Iterable<Relationship> relationships, final ConversionCache cache )
    {
        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        for ( final Relationship relationship : relationships )
        {
            final ProjectRelationship<?> rel = Conversions.toProjectRelationship( relationship, cache );
            if ( rel != null )
            {
                rels.add( rel );
            }
        }

        return rels;
    }

    public static void toNodeProperties( final ProjectVersionRef ref, final Node node, final boolean connected )
    {
        final String g = ref.getGroupId();
        final String a = ref.getArtifactId();
        final String v = ref.getVersionString();

        if ( empty( g ) || empty( a ) || empty( v ) )
        {
            throw new IllegalArgumentException( String.format( "GAV cannot contain nulls: %s:%s:%s", g, a, v ) );
        }

        node.setProperty( NODE_TYPE, NodeType.PROJECT.name() );
        node.setProperty( ARTIFACT_ID, a );
        node.setProperty( GROUP_ID, g );
        node.setProperty( VERSION, v );
        node.setProperty( GAV, ref.toString() );

        if ( ref.isVariableVersion() )
        {
            LOGGER.debug( "Marking: {} as variable.", ref );
            node.setProperty( VARIABLE, true );
        }

        markConnected( node, connected );
    }

    public static boolean isAtlasType( final Relationship rel )
    {
        return GraphRelType.valueOf( rel.getType()
                                        .name() )
                           .isAtlasRelationship();
    }

    public static boolean isType( final Node node, final NodeType type )
    {
        final String nt = getStringProperty( NODE_TYPE, node );
        return nt != null && type == NodeType.valueOf( nt );
    }

    //    public static ProjectVersionRef toProjectVersionRef( final Node node )
    //    {
    //        return toProjectVersionRef( node, null );
    //    }

    public static ProjectVersionRef toProjectVersionRef( final Node node, final ConversionCache cache )
    {
        if ( node == null )
        {
            return null;
        }

        if ( cache != null )
        {
            final ProjectVersionRef ref = cache.getProjectVersionRef( node );
            if ( ref != null )
            {
                return ref;
            }
        }

        if ( !isType( node, NodeType.PROJECT ) )
        {
            throw new IllegalArgumentException( "Node " + node.getId() + " is not a project reference." );
        }

        final String g = getStringProperty( GROUP_ID, node );
        final String a = getStringProperty( ARTIFACT_ID, node );
        final String v = getStringProperty( VERSION, node );

        if ( empty( g ) || empty( a ) || empty( v ) )
        {
            throw new IllegalArgumentException( String.format( "GAV cannot contain nulls: %s:%s:%s", g, a, v ) );
        }

        final ProjectVersionRef result = new ProjectVersionRef( g, a, v );
        if ( cache != null )
        {
            cache.cache( node, result );
        }

        return result;
    }

    private static boolean empty( final String val )
    {
        return val == null || val.trim()
                                 .length() < 1;
    }

    @SuppressWarnings( "incomplete-switch" )
    public static void toRelationshipProperties( final ProjectRelationship<?> rel, final Relationship relationship )
    {
        relationship.setProperty( INDEX, rel.getIndex() );
        relationship.setProperty( SOURCE_URI, toStringArray( rel.getSources() ) );
        relationship.setProperty( POM_LOCATION_URI, rel.getPomLocation()
                                                       .toString() );

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

                        sb.append( exclude.getGroupId() )
                          .append( ":" )
                          .append( exclude.getArtifactId() );
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

    public static String[] toStringArray( final Collection<?> sources )
    {
        final Set<String> result = new LinkedHashSet<String>( sources.size() );
        for ( final Object object : sources )
        {
            if ( object == null )
            {
                continue;
            }

            result.add( object.toString() );
        }

        return result.toArray( new String[result.size()] );
    }

    //    public static ProjectRelationship<?> toProjectRelationship( final Relationship rel )
    //    {
    //        return toProjectRelationship( rel, null );
    //    }

    public static ProjectRelationship<?> toProjectRelationship( final Relationship rel, final ConversionCache cache )
    {
        if ( rel == null )
        {
            return null;
        }

        if ( cache != null )
        {
            final ProjectRelationship<?> r = cache.getRelationship( rel );
            if ( r != null )
            {
                return r;
            }
        }

        final GraphRelType mapper = GraphRelType.valueOf( rel.getType()
                                                             .name() );

        //        LOGGER.debug( "Converting relationship of type: {} (atlas type: {})", mapper,
        //                                              mapper.atlasType() );

        if ( !mapper.isAtlasRelationship() )
        {
            return null;
        }

        if ( rel.getStartNode() == null || rel.getEndNode() == null || !isType( rel.getStartNode(), NodeType.PROJECT )
            || !isType( rel.getEndNode(), NodeType.PROJECT ) )
        {
            return null;
        }

        final ProjectVersionRef from = toProjectVersionRef( rel.getStartNode(), cache );
        final ProjectVersionRef to = toProjectVersionRef( rel.getEndNode(), cache );
        final int index = getIntegerProperty( INDEX, rel );
        final Set<URI> source = getURISetProperty( SOURCE_URI, rel, UNKNOWN_SOURCE_URI );
        final URI pomLocation = getURIProperty( POM_LOCATION_URI, rel, POM_ROOT_URI );

        ProjectRelationship<?> result = null;
        switch ( mapper.atlasType() )
        {
            case DEPENDENCY:
            {
                final ArtifactRef artifact = toArtifactRef( to, rel );
                final boolean managed = getBooleanProperty( IS_MANAGED, rel );
                final String scopeStr = getStringProperty( SCOPE, rel );
                final DependencyScope scope = DependencyScope.getScope( scopeStr );

                final String excludeStr = getStringProperty( EXCLUDES, rel );
                final Set<ProjectRef> excludes = new HashSet<ProjectRef>();
                if ( excludeStr != null )
                {
                    final String[] e = excludeStr.split( "\\s*,\\s*" );
                    for ( final String ex : e )
                    {
                        final String[] parts = ex.split( ":" );
                        if ( parts.length != 2 )
                        {
                            LOGGER.error( "In: {} -> {} skipping invalid exclude specification: '{}'", from, artifact, ex );
                        }
                        else
                        {
                            excludes.add( new ProjectRef( parts[0], parts[1] ) );
                        }
                    }
                }

                result =
                    new DependencyRelationship( source, pomLocation, from, artifact, scope, index, managed, excludes.toArray( new ProjectRef[] {} ) );
                break;
            }
            case PLUGIN_DEP:
            {
                final ArtifactRef artifact = toArtifactRef( to, rel );
                final String pa = getStringProperty( PLUGIN_ARTIFACT_ID, rel );
                final String pg = getStringProperty( PLUGIN_GROUP_ID, rel );
                final boolean managed = getBooleanProperty( IS_MANAGED, rel );

                result = new PluginDependencyRelationship( source, pomLocation, from, new ProjectRef( pg, pa ), artifact, index, managed );
                break;
            }
            case PLUGIN:
            {
                final boolean managed = getBooleanProperty( IS_MANAGED, rel );
                final boolean reporting = getBooleanProperty( IS_REPORTING_PLUGIN, rel );

                result = new PluginRelationship( source, pomLocation, from, to, index, managed, reporting );
                break;
            }
            case EXTENSION:
            {
                result = new ExtensionRelationship( source, from, to, index );
                break;
            }
            case PARENT:
            {
                result = new ParentRelationship( source, from, to );
                break;
            }
            default:
            {
            }
        }

        if ( result != null && cache != null )
        {
            cache.cache( rel, result );
        }

        //        LOGGER.debug( "Returning project relationship: {}", result );
        return result;
    }

    public static String id( final ProjectRelationship<?> rel )
    {
        return DigestUtils.shaHex( rel.toString() );
    }

    private static ArtifactRef toArtifactRef( final ProjectVersionRef ref, final Relationship rel )
    {
        if ( ref == null )
        {
            return null;
        }

        final String type = getStringProperty( TYPE, rel );
        final String classifier = getStringProperty( CLASSIFIER, rel );
        final boolean optional = getBooleanProperty( OPTIONAL, rel );

        return new ArtifactRef( ref, type, classifier, optional );
    }

    private static void toRelationshipProperties( final ArtifactRef target, final Relationship relationship )
    {
        relationship.setProperty( OPTIONAL, target.isOptional() );
        relationship.setProperty( TYPE, target.getType() );
        if ( target.getClassifier() != null )
        {
            relationship.setProperty( CLASSIFIER, target.getClassifier() );
        }
    }

    public static String getStringProperty( final String prop, final PropertyContainer container )
    {
        if ( container.hasProperty( prop ) )
        {
            return (String) container.getProperty( prop );
        }
        return null;
    }

    public static Set<URI> getURISetProperty( final String prop, final PropertyContainer container, final URI defaultValue )
    {
        final Set<URI> result = new HashSet<URI>();

        if ( container.hasProperty( prop ) )
        {
            final String[] uris = (String[]) container.getProperty( prop );
            for ( final String uri : uris )
            {
                try
                {
                    final URI u = new URI( uri );
                    if ( !result.contains( u ) )
                    {
                        result.add( u );
                    }
                }
                catch ( final URISyntaxException e )
                {
                }
            }
        }

        if ( defaultValue != null && result.isEmpty() )
        {
            result.add( defaultValue );
        }

        return result;
    }

    public static void addToURISetProperty( final Collection<URI> uris, final String prop, final PropertyContainer container )
    {
        if ( uris == null || uris.isEmpty() )
        {
            return;
        }

        final Set<URI> existing = getURISetProperty( prop, container, null );
        for ( final URI uri : uris )
        {
            existing.add( uri );
        }

        container.setProperty( prop, toStringArray( existing ) );
    }

    public static void removeFromURISetProperty( final Collection<URI> uris, final String prop, final PropertyContainer container )
    {
        if ( uris == null || uris.isEmpty() || !container.hasProperty( prop ) )
        {
            return;
        }

        final Set<URI> existing = getURISetProperty( prop, container, null );
        for ( final URI uri : uris )
        {
            existing.remove( uri );
        }

        if ( existing.isEmpty() )
        {
            container.removeProperty( prop );
        }
        else
        {
            container.setProperty( prop, toStringArray( existing ) );
        }
    }

    public static URI getURIProperty( final String prop, final PropertyContainer container, final URI defaultValue )
    {
        URI result = defaultValue;

        if ( container.hasProperty( prop ) )
        {
            try
            {
                result = new URI( (String) container.getProperty( prop ) );
            }
            catch ( final URISyntaxException e )
            {
            }
        }

        return result;
    }

    public static Boolean getBooleanProperty( final String prop, final PropertyContainer container )
    {
        if ( container.hasProperty( prop ) )
        {
            return (Boolean) container.getProperty( prop );
        }
        return null;
    }

    public static Boolean getBooleanProperty( final String prop, final PropertyContainer container, final Boolean defaultValue )
    {
        if ( container.hasProperty( prop ) )
        {
            return (Boolean) container.getProperty( prop );
        }

        return defaultValue;
    }

    public static Integer getIntegerProperty( final String prop, final PropertyContainer container )
    {
        if ( container.hasProperty( prop ) )
        {
            return (Integer) container.getProperty( prop );
        }
        return null;
    }

    public static Long getLongProperty( final String prop, final PropertyContainer container, final long defaultValue )
    {
        if ( container.hasProperty( prop ) )
        {
            return (Long) container.getProperty( prop );
        }

        return defaultValue;
    }

    public static String setConfigProperty( final String key, final String value, final PropertyContainer container )
    {
        final String pkey = CONFIG_PROPERTY_PREFIX + key;
        final String old = container.hasProperty( pkey ) ? (String) container.getProperty( pkey ) : null;

        container.setProperty( pkey, value );

        return old;
    }

    public static String removeConfigProperty( final String key, final PropertyContainer container )
    {
        final String pkey = CONFIG_PROPERTY_PREFIX + key;
        String old = null;
        if ( container.hasProperty( pkey ) )
        {
            old = (String) container.getProperty( pkey );

            container.removeProperty( pkey );
        }

        return old;
    }

    public static String getConfigProperty( final String key, final PropertyContainer container, final String defaultValue )
    {
        final String result = getStringProperty( CONFIG_PROPERTY_PREFIX + key, container );

        return result == null ? defaultValue : result;
    }

    public static void setMetadata( final String key, final String value, final PropertyContainer container )
    {
        container.setProperty( METADATA_PREFIX + key, value );
    }

    public static void setMetadata( final Map<String, String> metadata, final PropertyContainer container )
    {
        final Map<String, String> metadataMap = getMetadataMap( container );

        if ( metadataMap != null )
        {
            for ( final String key : metadataMap.keySet() )
            {
                container.removeProperty( key );
            }
        }

        for ( final Map.Entry<String, String> entry : metadata.entrySet() )
        {
            container.setProperty( METADATA_PREFIX + entry.getKey(), entry.getValue() );
        }
    }

    public static Map<String, String> getMetadataMap( final PropertyContainer container )
    {
        return getMetadataMap( container, null );
    }

    public static Map<String, String> getMetadataMap( final PropertyContainer container, final Set<String> matching )
    {
        final Iterable<String> keys = container.getPropertyKeys();
        final Map<String, String> md = new HashMap<String, String>();
        for ( final String key : keys )
        {
            if ( !key.startsWith( METADATA_PREFIX ) )
            {
                continue;
            }

            final String k = key.substring( METADATA_PREFIX.length() );
            if ( matching != null && !matching.contains( k ) )
            {
                continue;
            }

            final String value = getStringProperty( key, container );

            md.put( k, value );
        }

        return md.isEmpty() ? null : md;
    }

    public static String getMetadata( final String key, final PropertyContainer container )
    {
        return getStringProperty( METADATA_PREFIX + key, container );
    }

    public static void toNodeProperties( final String cycleId, final String rawCycleId, final Set<ProjectVersionRef> refs, final Node node )
    {
        node.setProperty( NODE_TYPE, NodeType.CYCLE.name() );
        node.setProperty( CYCLE_ID, cycleId );
        node.setProperty( CYCLE_RELATIONSHIPS, rawCycleId );
        node.setProperty( CYCLE_PROJECTS, join( refs, "," ) );
    }

    public static boolean isConnected( final Node node )
    {
        return getBooleanProperty( CONNECTED, node );
    }

    public static void markConnected( final Node node, final boolean connected )
    {
        //        LOGGER.info( "Marking as connected (non-missing): {}", node.getProperty( GAV ) );
        node.setProperty( CONNECTED, connected );
    }

    public static void markCycleInjection( final Relationship relationship, final Set<List<Relationship>> cycles )
    {
        relationship.setProperty( CYCLE_INJECTION, true );
        final List<Long> collapsed = new ArrayList<Long>();
        final Set<List<Long>> existing = getInjectedCycles( relationship );
        if ( existing != null && !existing.isEmpty() )
        {
            for ( final List<Long> cycle : existing )
            {
                if ( !collapsed.isEmpty() )
                {
                    collapsed.add( -1L );
                }

                collapsed.addAll( cycle );
            }
        }

        for ( final List<Relationship> cycle : cycles )
        {
            if ( existing.contains( cycle ) )
            {
                continue;
            }

            if ( !collapsed.isEmpty() )
            {
                collapsed.add( -1L );
            }

            boolean containsGivenRelationship = false;
            for ( final Relationship r : cycle )
            {
                final long rid = r.getId();

                collapsed.add( rid );
                if ( rid == relationship.getId() )
                {
                    containsGivenRelationship = true;
                }
            }

            if ( !containsGivenRelationship )
            {
                collapsed.add( relationship.getId() );
            }
        }

        final long[] arry = new long[collapsed.size()];
        int i = 0;
        for ( final Long l : collapsed )
        {
            arry[i] = l;
            i++;
        }

        relationship.setProperty( CYCLES_INJECTED, arry );
    }

    public static Set<List<Long>> getInjectedCycles( final Relationship relationship )
    {
        final Set<List<Long>> cycles = new HashSet<List<Long>>();

        if ( relationship.hasProperty( CYCLES_INJECTED ) )
        {
            final long[] collapsed = (long[]) relationship.getProperty( CYCLES_INJECTED );

            List<Long> currentCycle = new ArrayList<Long>();
            for ( final long id : collapsed )
            {
                if ( id == -1 )
                {
                    if ( !currentCycle.isEmpty() )
                    {
                        cycles.add( currentCycle );
                        currentCycle = new ArrayList<Long>();
                    }
                }
                else
                {
                    currentCycle.add( id );
                }
            }

            if ( !currentCycle.isEmpty() )
            {
                cycles.add( currentCycle );
            }
        }

        return cycles;
    }

    public static void removeProperty( final String key, final PropertyContainer container )
    {
        if ( container.hasProperty( key ) )
        {
            container.removeProperty( key );
        }
    }

    public static <T, P> Set<P> toProjectedSet( final Iterable<T> src, final Projector<T, P> projector )
    {
        final Set<P> set = new HashSet<P>();
        for ( final T t : src )
        {
            set.add( projector.project( t ) );
        }

        return set;
    }

    public static <T> Set<T> toSet( final Iterable<T> src )
    {
        final Set<T> set = new HashSet<T>();
        for ( final T t : src )
        {
            set.add( t );
        }

        return set;
    }

    public static <T> List<T> toList( final Iterable<T> src )
    {
        final List<T> set = new ArrayList<T>();
        for ( final T t : src )
        {
            set.add( t );
        }

        return set;
    }

    public static void cloneRelationshipProperties( final Relationship from, final Relationship to )
    {
        final Iterable<String> keys = from.getPropertyKeys();
        for ( final String key : keys )
        {
            to.setProperty( key, from.getProperty( key ) );
        }
    }

    public static Neo4jGraphPath getCachedPath( final Relationship rel )
    {
        if ( !rel.hasProperty( PATH ) )
        {
            throw new IllegalArgumentException( "Relationship " + rel + " is not a cached-path relationship!" );
        }

        final long[] ids = (long[]) rel.getProperty( PATH );
        return new Neo4jGraphPath( ids );
    }

    public static CyclePath getCachedCyclePath( final Relationship rel )
    {
        if ( !rel.hasProperty( PATH ) )
        {
            throw new IllegalArgumentException( "Relationship " + rel + " is not a cached-path relationship!" );
        }

        final long[] ids = (long[]) rel.getProperty( PATH );
        return new CyclePath( ids );
    }

    public static long getLastCachedPathRelationship( final Relationship rel )
    {
        if ( !rel.hasProperty( PATH ) )
        {
            throw new IllegalArgumentException( "Relationship " + rel + " is not a cached-path relationship!" );
        }

        final long[] ids = (long[]) rel.getProperty( PATH );
        return ids.length < 1 ? -1 : ids[ids.length - 1];
    }

    //    public static GraphPathInfo getCachedPathInfo( final Relationship rel, final AbstractNeo4JEGraphDriver driver )
    //    {
    //        return getCachedPathInfo( rel, null, driver );
    //    }

    public static GraphPathInfo getCachedPathInfo( final Relationship rel, final ConversionCache cache, final GraphAdmin maint )
    {
        if ( !rel.hasProperty( PATH_INFO_DATA ) )
        {
            return null;
        }

        final byte[] data = (byte[]) rel.getProperty( PATH_INFO_DATA );

        if ( cache != null )
        {
            final GraphPathInfo pathInfo = cache.getSerializedObject( data, GraphPathInfo.class );
            if ( pathInfo != null )
            {
                return pathInfo;
            }
        }

        ObjectInputStream ois = null;
        try
        {
            ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
            final GraphPathInfo pathInfo = (GraphPathInfo) ois.readObject();
            pathInfo.reattach( maint.getDriver() );

            if ( cache != null )
            {
                cache.cache( data, pathInfo );
            }

            return pathInfo;
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( "Cannot construct ObjectInputStream to wrap ByteArrayInputStream containing " + data.length + " bytes!",
                                             e );
        }
        catch ( final ClassNotFoundException e )
        {
            throw new IllegalStateException( "Cannot read GraphView. A class was missing: " + e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( ois );
        }
    }

    public static void storeCachedPath( final Neo4jGraphPath path, final GraphPathInfo pathInfo, final Relationship rel )
    {
        rel.setProperty( PATH, path.getRelationshipIds() );

        if ( pathInfo == null )
        {
            return;
        }

        ObjectOutputStream oos = null;
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            oos = new ObjectOutputStream( baos );
            oos.writeObject( pathInfo );

            rel.setProperty( PATH_INFO_DATA, baos.toByteArray() );
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( "Cannot construct ObjectOutputStream to wrap ByteArrayOutputStream!", e );
        }
        finally
        {
            IOUtils.closeQuietly( oos );
        }
    }

    public static void storeView( final GraphView view, final Node viewNode )
    {
        viewNode.setProperty( Conversions.VIEW_SHORT_ID, view.getShortId() );

        ObjectOutputStream oos = null;
        try
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            oos = new ObjectOutputStream( baos );
            oos.writeObject( view );

            viewNode.setProperty( VIEW_DATA, baos.toByteArray() );
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( "Cannot construct ObjectOutputStream to wrap ByteArrayOutputStream!", e );
        }
        finally
        {
            IOUtils.closeQuietly( oos );
        }
    }

    //    public static GraphView retrieveView( final Node viewNode, final AbstractNeo4JEGraphDriver driver )
    //    {
    //        return retrieveView( viewNode, null, driver );
    //    }

    public static GraphView retrieveView( final Node viewNode, final ConversionCache cache, final GraphAdmin maint )
    {
        if ( !viewNode.hasProperty( VIEW_DATA ) )
        {
            return null;
        }

        final byte[] data = (byte[]) viewNode.getProperty( VIEW_DATA );

        if ( cache != null )
        {
            final GraphView view = cache.getSerializedObject( data, GraphView.class );
            if ( view != null )
            {
                return view;
            }
        }

        ObjectInputStream ois = null;
        try
        {
            ois = new ObjectInputStream( new ByteArrayInputStream( data ) );
            final GraphView view = (GraphView) ois.readObject();
            view.reattach( maint.getDriver() );

            if ( cache != null )
            {
                cache.cache( data, view );
            }

            return view;
        }
        catch ( final IOException e )
        {
            throw new IllegalStateException( "Cannot construct ObjectInputStream to wrap ByteArrayInputStream containing " + data.length + " bytes!",
                                             e );
        }
        catch ( final ClassNotFoundException e )
        {
            throw new IllegalStateException( "Cannot read GraphView. A class was missing: " + e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( ois );
        }
    }

    public static boolean isCycleDetectionPending( final Node viewNode )
    {
        return getBooleanProperty( CYCLE_DETECTION_PENDING, viewNode, Boolean.FALSE );
    }

    public static void setCycleDetectionPending( final Node viewNode, final boolean pending )
    {
        viewNode.setProperty( CYCLE_DETECTION_PENDING, pending );
    }

}
