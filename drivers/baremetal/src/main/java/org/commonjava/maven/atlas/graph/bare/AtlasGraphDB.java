package org.commonjava.maven.atlas.graph.bare;

import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.util.logging.Logger;

public class AtlasGraphDB
{

    private final Logger logger = new Logger( getClass() );

    private final Map<ProjectVersionRef, SortedSet<ProjectRelationship<?>>> inEdges = new HashMap<>();

    private final Map<ProjectVersionRef, SortedSet<ProjectRelationship<?>>> outEdges = new HashMap<>();

    private final Map<ProjectVersionRef, ProjectVersionRef> canonicalGAVs = new HashMap<>();

    private final Map<ArtifactRef, ArtifactRef> canonicalArtifacts = new HashMap<>();

    private final Map<ProjectRef, ProjectRef> canonicalGAs = new HashMap<>();

    private final Map<ProjectRelationship<?>, ProjectRelationship<?>> canonicalRelationships = new HashMap<>();

    private final Set<ProjectVersionRef> incomplete = new HashSet<>();

    private final Set<ProjectVersionRef> variable = new HashSet<>();

    public ProjectRelationship<?> add( ProjectRelationship<?> rel )
    {
        final ProjectVersionRef target = normalizeGAV( rel.getTarget()
                                                          .asProjectVersionRef() );
        try
        {
            if ( !target.getVersionSpec()
                        .isSingle() )
            {
                logger.info( "Adding variable target: %s", target );
                variable.add( target );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Failed to determine variable-version status of: %s. Reason: %s", e, target, e.getMessage() );
            return null;
        }

        rel = normalize( rel );

        if ( !outEdges.containsKey( target ) )
        {
            logger.info( "Adding incomplete target: %s", target );
            incomplete.add( target );
        }

        final ProjectVersionRef declaring = rel.getDeclaring();

        addEdge( rel, declaring, outEdges );
        addEdge( rel, target, inEdges );

        logger.info( "removing from incomplete status: %s", declaring );
        incomplete.remove( declaring );

        final Set<AtlasPath> paths = getPathsEndingWith( declaring );
        if ( paths != null )
        {
            for ( final AtlasPath path : paths )
            {
                extendPath( path, rel );
            }
        }

        return rel;
    }

    private void addEdge( final ProjectRelationship<?> rel, final ProjectVersionRef ref,
                          final Map<ProjectVersionRef, SortedSet<ProjectRelationship<?>>> edges )
    {
        SortedSet<ProjectRelationship<?>> set = edges.get( ref );
        if ( set == null )
        {
            set = new TreeSet<>();
            edges.put( ref, set );
        }

        set.add( rel );
    }

    public ProjectRelationship<?> normalize( final ProjectRelationship<?> rel )
    {
        ProjectRelationship<?> result = canonicalRelationships.get( rel );
        if ( result == null )
        {
            final ProjectVersionRef d = normalizeGAV( rel.getDeclaring() );

            switch ( rel.getType() )
            {
                case DEPENDENCY:
                {
                    final ArtifactRef t = normalizeArtifact( rel.getTargetArtifact() );
                    final DependencyRelationship dr = (DependencyRelationship) rel;
                    result =
                        new DependencyRelationship( rel.getSources(), rel.getPomLocation(), d, t, dr.getScope(), rel.getIndex(), rel.isManaged(),
                                                    normalizeAll( dr.getExcludes() ) );
                    break;
                }
                case EXTENSION:
                {
                    final ProjectVersionRef t = normalizeGAV( rel.getTarget() );
                    result = new ExtensionRelationship( rel.getSources(), rel.getPomLocation(), d, t, rel.getIndex() );
                    break;
                }
                case PARENT:
                {
                    final ProjectVersionRef t = normalizeGAV( rel.getTarget() );
                    result = new ParentRelationship( rel.getSources(), d, t );
                    break;
                }
                case PLUGIN:
                {
                    final ProjectVersionRef t = normalizeGAV( rel.getTarget() );
                    result =
                        new PluginRelationship( rel.getSources(), rel.getPomLocation(), d, t, rel.getIndex(), rel.isManaged(),
                                                ( (PluginRelationship) rel ).isReporting() );
                    break;
                }
                case PLUGIN_DEP:
                {
                    final ArtifactRef t = normalizeArtifact( rel.getTargetArtifact() );
                    final ProjectRef plugin = normalizeGA( ( (PluginDependencyRelationship) rel ).getPlugin() );
                    result = new PluginDependencyRelationship( rel.getSources(), rel.getPomLocation(), d, plugin, t, rel.getIndex(), rel.isManaged() );
                    break;
                }
            }

            canonicalRelationships.put( result, result );
        }
        else
        {
            result.addSources( rel.getSources() );
        }

        return result;
    }

    public ProjectRef[] normalizeAll( final Collection<ProjectRef> refs )
    {
        final ProjectRef[] result = new ProjectRef[refs.size()];
        int i = 0;
        for ( final ProjectRef ref : refs )
        {
            result[i++] = normalizeGA( ref );
        }

        return result;
    }

    public ProjectRef normalizeGA( final ProjectRef ref )
    {
        ProjectRef result = canonicalGAs.get( ref );
        if ( result == null )
        {
            result = ref;
            canonicalGAs.put( ref, ref );
        }

        return result;
    }

    public ProjectVersionRef normalizeGAV( final ProjectVersionRef ref )
    {
        ProjectVersionRef result = canonicalGAVs.get( ref );
        if ( result == null )
        {
            result = ref;
            canonicalGAs.put( ref, ref );
        }

        normalizeGA( ref.asProjectRef() );

        return result;
    }

    public ArtifactRef normalizeArtifact( final ArtifactRef ref )
    {
        ArtifactRef result = canonicalArtifacts.get( ref );
        if ( result == null )
        {
            result = ref;
            canonicalGAs.put( ref, ref );
        }

        normalizeGAV( result.asProjectVersionRef() );
        normalizeGA( result.asProjectRef() );

        return result;
    }

    private final Map<String, AtlasPath> pathsById = new HashMap<>();

    private final Map<ProjectVersionRef, Set<String>> pathStartGAV = new HashMap<>();

    private final Map<ProjectVersionRef, Set<String>> pathEndGAV = new HashMap<>();

    private final Map<ProjectVersionRef, Set<String>> pathsContainingGAV = new HashMap<>();

    private final Map<ProjectRelationship<?>, Set<String>> pathsContainingRelationship = new HashMap<>();

    private final Map<GraphView, GraphVisibility> visibility = new WeakHashMap<>();

    //    private AtlasPath getPath( final ProjectRelationship<?>... path )
    //    {
    //        final String id = shaHex( join( path, ">" ) );
    //        AtlasPath result = pathsById.get( id );
    //        if ( result == null )
    //        {
    //            result = new AtlasPath( id, path );
    //            storePath( result );
    //        }
    //
    //        return result;
    //    }
    //
    private AtlasPath getPath( final List<ProjectRelationship<?>> path )
    {
        final String id = shaHex( join( path, ">" ) );
        AtlasPath result = pathsById.get( id );
        if ( result == null )
        {
            result = new AtlasPath( id, path );
            storePath( result );
        }

        return result;
    }

    private void storePath( final AtlasPath path )
    {
        pathsById.put( path.getId(), path );
        boolean first = true;
        for ( final Iterator<ProjectRelationship<?>> it = path.iterator(); it.hasNext(); )
        {
            final ProjectRelationship<?> r = it.next();

            final ProjectVersionRef d = r.getDeclaring();
            mapPathToProject( d, path, null );

            final ProjectVersionRef t = r.getTarget()
                                         .asProjectVersionRef();
            mapPathToProject( t, path, null );

            if ( first )
            {
                first = false;
                mapPathToProject( d, path, Boolean.TRUE );
            }

            if ( !it.hasNext() )
            {
                mapPathToProject( t, path, Boolean.FALSE );
            }

            Set<String> pathIds = pathsContainingRelationship.get( r );
            if ( pathIds == null )
            {
                pathIds = new HashSet<>();
                pathsContainingRelationship.put( r, pathIds );
            }

            pathIds.add( path.getId() );
        }
    }

    private void mapPathToProject( final ProjectVersionRef ref, final AtlasPath path, final Boolean endpointMarker )
    {
        Map<ProjectVersionRef, Set<String>> map;
        if ( endpointMarker == null )
        {
            map = pathsContainingGAV;
        }
        else if ( endpointMarker )
        {
            map = pathStartGAV;
        }
        else
        {
            map = pathEndGAV;
        }

        Set<String> pathIds = map.get( ref );
        if ( pathIds == null )
        {
            pathIds = new HashSet<>();
            map.put( ref, pathIds );
        }

        pathIds.add( path.getId() );
    }

    private Set<AtlasPath> getPathsEndingWith( final ProjectVersionRef ref )
    {
        final Set<String> ids = pathEndGAV.get( ref );
        if ( ids == null )
        {
            return null;
        }

        final Set<AtlasPath> paths = new HashSet<>();
        for ( final String id : ids )
        {
            paths.add( pathsById.get( id ) );
        }

        return paths;
    }

    //    private AtlasPath getPreviousPath( final AtlasPath path )
    //    {
    //        return getPath( path.getPreviousParts() );
    //    }

    private AtlasPath extendPath( final AtlasPath path, final ProjectRelationship<?> next )
    {
        final List<ProjectRelationship<?>> nextList = new ArrayList<>( path.getPathElements() );
        nextList.add( next );
        final AtlasPath result = getPath( nextList );

        for ( final GraphVisibility vis : visibility.values() )
        {
            vis.addPathIfVisible( result );
        }

        return result;
    }

}
