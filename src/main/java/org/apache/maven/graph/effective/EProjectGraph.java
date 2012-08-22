package org.apache.maven.graph.effective;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.graph.common.ref.ArtifactRef;
import org.apache.maven.graph.common.ref.VersionedProjectRef;
import org.apache.maven.graph.common.version.VersionSpec;
import org.apache.maven.graph.effective.ref.EProjectFacts;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.apache.maven.graph.effective.rel.AbstractProjectRelationship;
import org.apache.maven.graph.effective.rel.DependencyRelationship;
import org.apache.maven.graph.effective.rel.ExtensionRelationship;
import org.apache.maven.graph.effective.rel.ParentRelationship;
import org.apache.maven.graph.effective.rel.PluginDependencyRelationship;
import org.apache.maven.graph.effective.rel.PluginRelationship;
import org.apache.maven.graph.effective.rel.ProjectRelationship;
import org.apache.maven.graph.effective.rel.RelationshipComparator;
import org.apache.maven.graph.effective.rel.RelationshipPathComparator;
import org.apache.maven.graph.effective.traverse.ProjectGraphTraversal;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Graphs;

public class EProjectGraph
{

    private final EProjectKey key;

    private transient final Set<VersionedProjectRef> incompleteSubgraphs = new HashSet<VersionedProjectRef>();

    private transient final Set<VersionedProjectRef> variableSubgraphs = new HashSet<VersionedProjectRef>();

    private final DirectedGraph<VersionedProjectRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<VersionedProjectRef, ProjectRelationship<?>>();

    public EProjectGraph( final EProjectRelationships relationships )
    {
        this.key = relationships.getKey();
        add( relationships );
    }

    public EProjectGraph( final EProjectKey key, final ProjectRelationship<VersionedProjectRef> parent,
                          final Collection<DependencyRelationship> dependencies,
                          final Collection<PluginRelationship> plugins,
                          final Collection<PluginDependencyRelationship> pluginLevelDeps,
                          final Collection<ExtensionRelationship> extensions,
                          final Collection<EProjectRelationships> projectRelationships )
    {
        final VersionSpec version = key.getProject()
                                       .getVersionSpec();
        if ( !version.isConcrete() )
        {
            throw new IllegalArgumentException(
                                                "Cannot build project graph rooted on non-concrete version of a project! Version is: "
                                                    + version );
        }

        this.key = key;

        add( parent );
        addAll( dependencies );
        addAll( plugins );
        addAll( pluginLevelDeps );
        addAll( extensions );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }
    }

    public EProjectKey getKey()
    {
        return key;
    }

    public EProjectFacts getFacts()
    {
        return key.getFacts();
    }

    public Set<ProjectRelationship<?>> getFirstOrderRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( graph.getOutEdges( getRoot() ) );
    }

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( graph.getEdges() );
    }

    public DirectedGraph<VersionedProjectRef, ProjectRelationship<?>> getRawGraph()
    {
        return Graphs.unmodifiableDirectedGraph( graph );
    }

    public boolean isComplete()
    {
        return incompleteSubgraphs.isEmpty();
    }

    public boolean isConcrete()
    {
        return variableSubgraphs.isEmpty();
    }

    public Set<VersionedProjectRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( incompleteSubgraphs );
    }

    public Set<VersionedProjectRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( variableSubgraphs );
    }

    public static final class Builder
    {
        private final EProjectKey key;

        private ProjectRelationship<VersionedProjectRef> parent;

        private final Set<DependencyRelationship> dependencies = new HashSet<DependencyRelationship>();

        private final Set<PluginRelationship> plugins = new HashSet<PluginRelationship>();

        private final Set<PluginDependencyRelationship> pluginLevelDeps = new HashSet<PluginDependencyRelationship>();

        private final Set<EProjectRelationships> projects = new HashSet<EProjectRelationships>();

        private final Set<ExtensionRelationship> extensions = new HashSet<ExtensionRelationship>();

        public Builder( final EProjectRelationships rels )
        {
            this.key = rels.getKey();
            addFromDirectRelationships( rels );
        }

        public Builder( final VersionedProjectRef projectRef, final String... activeProfiles )
        {
            this.key = new EProjectKey( projectRef, new EProjectFacts( activeProfiles ) );
        }

        public Builder withParent( final VersionedProjectRef parent )
        {
            this.parent = new ParentRelationship( key.getProject(), parent );
            return this;
        }

        public Builder withParent( final ProjectRelationship<VersionedProjectRef> parent )
        {
            if ( parent.getDeclaring()
                       .equals( key.getProject() ) )
            {
                this.parent = parent;
            }
            else
            {
                this.parent = parent.cloneFor( key.getProject() );
            }
            return this;
        }

        public Builder withDirectProjectRelationships( final EProjectRelationships... rels )
        {
            return withDirectProjectRelationships( Arrays.asList( rels ) );
        }

        public Builder withDirectProjectRelationships( final Collection<EProjectRelationships> rels )
        {
            for ( final EProjectRelationships relationships : rels )
            {
                if ( relationships.getKey()
                                  .equals( key ) )
                {
                    addFromDirectRelationships( relationships );
                }
                else
                {
                    this.projects.add( relationships );
                }
            }

            return this;
        }

        private void addFromDirectRelationships( final EProjectRelationships relationships )
        {
            this.parent = relationships.getParent();
            this.dependencies.clear();
            this.dependencies.addAll( relationships.getDependencies() );
            this.dependencies.addAll( relationships.getManagedDependencies() );

            this.plugins.clear();
            this.plugins.addAll( relationships.getPlugins() );
            this.plugins.addAll( relationships.getManagedPlugins() );

            this.extensions.clear();
            this.extensions.addAll( relationships.getExtensions() );

            this.pluginLevelDeps.clear();
            if ( relationships.getPluginDependencies() != null )
            {
                for ( final Map.Entry<PluginRelationship, List<PluginDependencyRelationship>> entry : relationships.getPluginDependencies()
                                                                                                                   .entrySet() )
                {
                    if ( entry.getValue() != null )
                    {
                        this.pluginLevelDeps.addAll( entry.getValue() );
                    }
                }
            }
        }

        public Builder withDependencies( final List<DependencyRelationship> rels )
        {
            this.dependencies.addAll( rels );
            return this;
        }

        public Builder withDependencies( final DependencyRelationship... rels )
        {
            this.dependencies.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withPlugins( final Collection<PluginRelationship> rels )
        {
            this.plugins.addAll( rels );
            return this;
        }

        public Builder withPlugins( final PluginRelationship... rels )
        {
            this.plugins.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withPluginLevelDependencies( final Collection<PluginDependencyRelationship> rels )
        {
            this.pluginLevelDeps.addAll( rels );
            return this;
        }

        public Builder withPluginLevelDependencies( final PluginDependencyRelationship... rels )
        {
            this.pluginLevelDeps.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withExtensions( final Collection<ExtensionRelationship> rels )
        {
            this.extensions.addAll( rels );
            return this;
        }

        public Builder withExtensions( final ExtensionRelationship... rels )
        {
            this.extensions.addAll( Arrays.asList( rels ) );
            return this;
        }

        public EProjectGraph build()
        {
            return new EProjectGraph( key, parent, dependencies, plugins, pluginLevelDeps, extensions, projects );
        }

    }

    public void add( final EProjectRelationships rels )
    {
        if ( incompleteSubgraphs.contains( rels.getProjectRef() ) )
        {
            incompleteSubgraphs.remove( rels.getProjectRef() );
        }

        addAll( rels.getAll() );
    }

    private <T extends ProjectRelationship<?>> void add( final T rel )
    {
        if ( rel == null )
        {
            return;
        }

        VersionedProjectRef target = rel.getTarget();
        if ( rel instanceof DependencyRelationship )
        {
            target = ( (ArtifactRef) target ).asVersionedProjectRef();
        }

        if ( !graph.containsVertex( rel.getTarget() ) )
        {
            graph.addVertex( rel.getTarget() );
        }

        graph.addEdge( rel, rel.getDeclaring(), rel.getTarget() );

        if ( !rel.getTarget()
                 .getVersionSpec()
                 .isSingle() )
        {
            variableSubgraphs.add( rel.getTarget() );
        }
        else
        {
            incompleteSubgraphs.add( rel.getTarget() );
        }
    }

    private <T extends ProjectRelationship<?>> void addAll( final Collection<T> rels )
    {
        if ( rels == null )
        {
            return;
        }

        for ( final T rel : rels )
        {
            add( rel );
        }
    }

    public VersionedProjectRef getRoot()
    {
        return key.getProject();
    }

    public void traverse( final ProjectGraphTraversal traversal )
    {
        final int passes = traversal.getRequiredPasses();
        for ( int i = 0; i < passes; i++ )
        {
            traversal.startTraverse( i, this );

            switch ( traversal.getType( i ) )
            {
                case breadth_first:
                {
                    bfsTraverse( traversal, i );
                    break;
                }
                case depth_first:
                {
                    dfsTraverse( traversal, i );
                    break;
                }
            }

            traversal.endTraverse( i, this );
        }
    }

    // TODO: Implement without recursion.
    private void dfsTraverse( final ProjectGraphTraversal traversal, final int pass )
    {
        dfsIterate( getRoot(), traversal, new LinkedList<ProjectRelationship<?>>(), pass );
    }

    private void dfsIterate( final VersionedProjectRef node, final ProjectGraphTraversal traversal,
                             final LinkedList<ProjectRelationship<?>> path, final int pass )
    {
        final List<ProjectRelationship<?>> edges = getSortedOutEdges( node );
        if ( edges != null )
        {
            for ( final ProjectRelationship<?> edge : edges )
            {
                if ( traversal.traverseEdge( edge, path, pass ) )
                {
                    path.addLast( edge );
                    dfsIterate( edge.getTarget(), traversal, path, pass );
                    path.removeLast();
                }
            }
        }
    }

    private List<ProjectRelationship<?>> getSortedOutEdges( final VersionedProjectRef node )
    {
        final Collection<ProjectRelationship<?>> unsorted = graph.getOutEdges( node );
        if ( unsorted != null )
        {
            final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( unsorted );
            Collections.sort( sorted, new RelationshipComparator() );

            return sorted;
        }

        return null;
    }

    // TODO: Implement without recursion.
    private void bfsTraverse( final ProjectGraphTraversal traversal, final int pass )
    {
        final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
        path.add( new SelfEdge( getRoot() ) );

        bfsIterate( Collections.singletonList( path ), traversal, pass );
    }

    private void bfsIterate( final List<List<ProjectRelationship<?>>> thisLayer, final ProjectGraphTraversal traversal,
                             final int pass )
    {
        final List<List<ProjectRelationship<?>>> nextLayer = new ArrayList<List<ProjectRelationship<?>>>();

        for ( final List<ProjectRelationship<?>> path : thisLayer )
        {
            VersionedProjectRef node = path.get( path.size() - 1 )
                                           .getTarget();
            if ( node instanceof ArtifactRef )
            {
                node = ( (ArtifactRef) node ).asVersionedProjectRef();
            }

            if ( !path.isEmpty() && ( path.get( 0 ) instanceof SelfEdge ) )
            {
                path.remove( 0 );
            }

            final List<ProjectRelationship<?>> edges = getSortedOutEdges( node );
            if ( edges != null )
            {
                for ( final ProjectRelationship<?> edge : edges )
                {
                    if ( ( edge instanceof SelfEdge ) || traversal.traverseEdge( edge, path, pass ) )
                    {
                        final List<ProjectRelationship<?>> nextPath = new ArrayList<ProjectRelationship<?>>( path );

                        nextPath.add( edge );
                        nextLayer.add( nextPath );
                    }
                }
            }
        }

        if ( !nextLayer.isEmpty() )
        {
            Collections.sort( nextLayer, new RelationshipPathComparator() );
            bfsIterate( nextLayer, traversal, pass );
        }
    }

    private static final class SelfEdge
        extends AbstractProjectRelationship<VersionedProjectRef>
    {

        SelfEdge( final VersionedProjectRef ref )
        {
            super( null, ref, ref, 0 );
        }

    }

}
