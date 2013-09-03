/*******************************************************************************
 * Copyright (C) 2013 John Casey.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.spi.neo4j.GraphRelType;
import org.commonjava.maven.atlas.graph.spi.neo4j.NodeType;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

public final class Conversions
{

    private static final Logger LOGGER = new Logger( Conversions.class );

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

    public static final String SELECTED_FOR = "_selected_for";

    public static final String DESELECTED_FOR = "_deselected_for";

    public static final String CLONE_OF = "_clone_of";

    public static final String CYCLE_INJECTION = "_cycle_injection";

    public static final String CYCLES_INJECTED = "_cycles";

    public static final String SOURCE_URI = "source_uri";

    public static final String POM_LOCATION_URI = "pom_location_uri";

    public static final String FORCE_VERSION_SELECTIONS = "force_selections";

    public static final String LAST_ACCESS_DATE = "last_access";

    public static final String SELECTION_PREFIX = "rel_selection_";

    private static final int SELECTION_PREFIX_LEN = SELECTION_PREFIX.length();

    public static final String SELECTION_ONLY = "_selection_only";

    public static final String SELECTION = "selection";

    public static final int SELECTION_FOR_PREFIX_LEN = SELECTED_FOR.length();

    public static final String WS_ID = "wsid";

    private Conversions()
    {
    }

    public static void toSelectionNodeProperties( final ProjectRef ref, final ProjectVersionRef select, final Node node )
    {
        node.setProperty( GA, ref.toString() );
        node.setProperty( NODE_TYPE, NodeType.GA_SELECTIONS.name() );
        node.setProperty( SELECTION, select.toString() );
    }

    public static ProjectVersionRef getWorkspaceSelection( final Node node )
    {
        if ( node == null )
        {
            return null;
        }

        if ( !isType( node, NodeType.WORKSPACE ) )
        {
            throw new IllegalArgumentException( "Node " + node.getId() + " is not a ga-selections reference." );
        }

        final Iterable<String> keys = node.getPropertyKeys();
        for ( final String key : keys )
        {
            if ( key.startsWith( SELECTION ) )
            {
                return ProjectVersionRef.parse( (String) node.getProperty( key ) );
            }
        }

        return null;
    }

    public static void toNodeProperties( final GraphWorkspace workspace, final Node node )
    {
        node.setProperty( NODE_TYPE, NodeType.WORKSPACE.name() );

        node.setProperty( LAST_ACCESS_DATE, workspace.getLastAccess() );

        node.setProperty( SOURCE_URI, toStringArray( workspace.getActiveSources() ) );
        node.setProperty( POM_LOCATION_URI, toStringArray( workspace.getActivePomLocations() ) );
        node.setProperty( FORCE_VERSION_SELECTIONS, workspace.isForceVersions() );
    }

    public static List<ProjectVersionRef> convertToProjects( final Iterable<Node> nodes )
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

            refs.add( Conversions.toProjectVersionRef( node ) );
        }

        return refs;
    }

    public static List<ProjectRelationship<?>> convertToRelationships( final Iterable<Relationship> relationships )
    {
        final List<ProjectRelationship<?>> rels = new ArrayList<ProjectRelationship<?>>();
        for ( final Relationship relationship : relationships )
        {
            final ProjectRelationship<?> rel = Conversions.toProjectRelationship( relationship );
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
            LOGGER.debug( "Marking: %s as variable.", ref );
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

    public static ProjectVersionRef toProjectVersionRef( final Node node )
    {
        if ( node == null )
        {
            return null;
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

        return new ProjectVersionRef( g, a, v );
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

    public static ProjectRelationship<?> toProjectRelationship( final Relationship rel )
    {
        if ( rel == null )
        {
            return null;
        }

        final GraphRelType mapper = GraphRelType.valueOf( rel.getType()
                                                             .name() );

        //        LOGGER.debug( "Converting relationship of type: %s (atlas type: %s)", mapper,
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

        final ProjectVersionRef from = toProjectVersionRef( rel.getStartNode() );
        final ProjectVersionRef to = toProjectVersionRef( rel.getEndNode() );
        final int index = getIntegerProperty( INDEX, rel );
        final List<URI> source = getURIListProperty( SOURCE_URI, rel, UNKNOWN_SOURCE_URI );
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
                            LOGGER.error( "In: %s -> %s skipping invalid exclude specification: '%s'", from, artifact, ex );
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

        //        LOGGER.debug( "Returning project relationship: %s", result );
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

    public static List<URI> getURIListProperty( final String prop, final PropertyContainer container, final URI defaultValue )
    {
        final List<URI> result = new ArrayList<URI>();

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

    public static void addToURIListProperty( final Collection<URI> uris, final String prop, final PropertyContainer container )
    {
        final List<URI> existing = getURIListProperty( prop, container, null );
        for ( final URI uri : uris )
        {
            if ( !existing.contains( uri ) )
            {
                existing.add( uri );
            }
        }

        container.setProperty( prop, toStringArray( existing ) );
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

    public static void setMetadata( final String key, final String value, final PropertyContainer container )
    {
        container.setProperty( METADATA_PREFIX + key, value );
    }

    public static void setMetadata( final Map<String, String> metadata, final PropertyContainer container )
    {
        final Map<String, String> metadataMap = getMetadataMap( container );

        for ( final String key : metadataMap.keySet() )
        {
            container.removeProperty( key );
        }

        for ( final Map.Entry<String, String> entry : metadata.entrySet() )
        {
            container.setProperty( METADATA_PREFIX + entry.getKey(), entry.getValue() );
        }
    }

    public static Map<String, String> getMetadataMap( final PropertyContainer container )
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

            List<Long> currentCycle = new ArrayList<>();
            for ( final long id : collapsed )
            {
                if ( id == -1 )
                {
                    if ( !currentCycle.isEmpty() )
                    {
                        cycles.add( currentCycle );
                        currentCycle = new ArrayList<>();
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

    public static void cloneRelationshipProperties( final Relationship from, final Relationship to )
    {
        final Iterable<String> keys = from.getPropertyKeys();
        for ( final String key : keys )
        {
            to.setProperty( key, from.getProperty( key ) );
        }

        to.setProperty( CLONE_OF, from.getId() );
    }

    public static boolean isCloneFor( final Relationship relationship, final Relationship original )
    {
        if ( relationship.hasProperty( CLONE_OF ) )
        {
            final long id = (Long) relationship.getProperty( CLONE_OF );
            return original.getId() == id;
        }

        return false;
    }

    public static void clearCloneStatus( final Relationship relationship )
    {
        if ( relationship.hasProperty( CLONE_OF ) )
        {
            relationship.removeProperty( CLONE_OF );
        }
    }

    public static void markSelectionOnly( final Relationship rel, final boolean value )
    {
        rel.setProperty( SELECTION_ONLY, value );
    }

    public static boolean isSelectionOnly( final Relationship rel )
    {
        return getBooleanProperty( SELECTION_ONLY, rel, false );
    }

    public static void markSelection( final Relationship from, final Relationship to, final Node session )
    {
        LOGGER.debug( "Marking selected: %s for session: %s", getStringProperty( GAV, to.getEndNode() ), session.getId() );

        addToIdListing( session.getId(), SELECTED_FOR, to );
        removeFromIdListing( session.getId(), DESELECTED_FOR, to );

        markDeselected( from, session );

        session.setProperty( SELECTION_PREFIX + from.getId(), to.getId() );
    }

    public static void markDeselected( final Relationship rel, final Node session )
    {
        LOGGER.debug( "Marking de-selected: %s for session: %s", getStringProperty( GAV, rel.getEndNode() ), session.getId() );

        removeFromIdListing( session.getId(), SELECTED_FOR, rel );
        addToIdListing( session.getId(), DESELECTED_FOR, rel );
    }

    public static Map<Long, Long> getSelections( final Node session )
    {
        final Map<Long, Long> result = new HashMap<Long, Long>();
        for ( final String key : session.getPropertyKeys() )
        {
            if ( key.startsWith( SELECTION_PREFIX ) && key.length() > SELECTION_PREFIX_LEN )
            {
                final Long k = Long.parseLong( key.substring( SELECTION_PREFIX_LEN ) );
                final Long v = (Long) session.getProperty( key );

                result.put( k, v );
            }
        }

        return result;
    }

    public static void removeSelectionAnnotationsFor( final Relationship relationship, final Node root )
    {
        LOGGER.debug( "%s: removing ALL selection annotations for: %s", getStringProperty( GAV, relationship.getEndNode() ),
                      getStringProperty( GAV, root ) );

        removeFromIdListing( root.getId(), SELECTED_FOR, relationship );
        removeFromIdListing( root.getId(), DESELECTED_FOR, relationship );
    }

    public static boolean idListingContains( final String property, final Relationship relationship, final long... targets )
    {
        if ( !relationship.hasProperty( property ) )
        {
            return false;
        }

        final long[] ids = (long[]) relationship.getProperty( property, new long[] {} );
        //        LOGGER.info( "Relationship: %s\nValue of property: %s is: %s", relationship, property,
        //                     relationship.getProperty( property ) );

        Arrays.sort( ids );
        for ( final long target : targets )
        {
            if ( Arrays.binarySearch( ids, target ) > -1 )
            {
                return true;
            }
        }

        return false;
    }

    public static boolean idListingContains( final String property, final Relationship relationship, final Collection<Node> targets )
    {
        if ( !relationship.hasProperty( property ) )
        {
            return false;
        }

        final long[] ids = (long[]) relationship.getProperty( property, new long[] {} );
        //        LOGGER.info( "Relationship: %s\nValue of property: %s is: %s", relationship, property,
        //                     relationship.getProperty( property ) );

        Arrays.sort( ids );
        for ( final Node target : targets )
        {
            if ( Arrays.binarySearch( ids, target.getId() ) > -1 )
            {
                return true;
            }
        }

        return false;
    }

    private static void addToIdListing( final long target, final String property, final Relationship relationship )
    {
        final Set<Long> ids = new HashSet<Long>();
        boolean contains = false;
        for ( final long id : (long[]) relationship.getProperty( property, new long[] {} ) )
        {
            if ( id == target )
            {
                contains = true;
            }

            ids.add( id );
        }

        if ( !contains )
        {
            ids.add( target );
            relationship.setProperty( property, ids.toArray( new Long[] {} ) );
        }
    }

    private static void removeFromIdListing( final long target, final String property, final Relationship relationship )
    {
        final Set<Long> ids = new HashSet<Long>();
        boolean changed = false;
        for ( final long id : (long[]) relationship.getProperty( property, new long[] {} ) )
        {
            if ( id == target )
            {
                LOGGER.debug( "Found id: %d in property: %s.", target, property );
                changed = true;
            }
            else
            {
                ids.add( id );
            }
        }

        if ( changed )
        {
            if ( ids.isEmpty() )
            {
                LOGGER.debug( "Removing empty property: %s from: %s", property, relationship );
                relationship.removeProperty( property );
            }
            else
            {
                LOGGER.debug( "Modifying property: %s from: %s", property, relationship );
                relationship.setProperty( property, ids.toArray( new Long[] {} ) );
            }
        }
    }

    public static boolean isDeselectedFor( final Relationship relationship, final Node... roots )
    {
        final long[] ids = new long[roots.length];
        for ( int i = 0; i < roots.length; i++ )
        {
            final Node n = roots[i];
            ids[i] = n.getId();
        }

        return idListingContains( DESELECTED_FOR, relationship, ids );
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

}
