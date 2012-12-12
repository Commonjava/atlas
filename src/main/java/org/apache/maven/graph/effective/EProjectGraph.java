/*******************************************************************************
 * Copyright 2012 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.maven.graph.effective;

import static org.apache.maven.graph.effective.util.EGraphUtils.filterTerminalParents;

import java.io.IOException;
import java.io.Serializable;
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
import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.ref.EGraphFacts;
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
import org.apache.maven.graph.effective.traverse.ProjectNetTraversal;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Graphs;

public class EProjectGraph
    implements EProjectNet, KeyedProjectRelationshipCollection, Serializable
{

    private static final long serialVersionUID = 1L;

    private final EProjectKey key;

    private transient Set<ProjectVersionRef> incompleteSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> connectedProjects = new HashSet<ProjectVersionRef>();

    private transient Set<ProjectVersionRef> variableSubgraphs = new HashSet<ProjectVersionRef>();

    private transient Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

    private final DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> graph =
        new DirectedSparseMultigraph<ProjectVersionRef, ProjectRelationship<?>>();

    public EProjectGraph( final EProjectRelationships relationships )
    {
        this.key = relationships.getKey();
        add( relationships );
    }

    public EProjectGraph( final EProjectKey key, final Collection<ProjectRelationship<?>> relationships,
                          final Collection<EProjectRelationships> projectRelationships, final Set<EProjectCycle> cycles )
    {
        // NOTE: It does make sense to allow analysis of snapshots...it just requires different standards for mutability.
        //        final VersionSpec version = key.getProject()
        //                        .getVersionSpec();
        //
        //        if ( !version.isConcrete() )
        //        {
        //            throw new IllegalArgumentException(
        //                                                "Cannot build project graph rooted on non-concrete version of a project! Version is: "
        //                                                    + version );
        //        }

        this.key = key;
        if ( cycles != null )
        {
            this.cycles = cycles;
        }

        addAll( relationships );
        for ( final EProjectRelationships project : projectRelationships )
        {
            add( project );
        }
    }

    public EProjectKey getKey()
    {
        return key;
    }

    public EGraphFacts getFacts()
    {
        return key.getFacts();
    }

    public Set<ProjectRelationship<?>> getFirstOrderRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactFirstOrderRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public Set<ProjectRelationship<?>> getExactFirstOrderRelationships()
    {
        return new HashSet<ProjectRelationship<?>>( graph.getOutEdges( getRoot() ) );
    }

    public Set<ProjectRelationship<?>> getExactAllRelationships()
    {
        final Collection<ProjectRelationship<?>> rels = graph.getEdges();
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }

    public Set<ProjectRelationship<?>> getAllRelationships()
    {
        final Set<ProjectRelationship<?>> rels = getExactAllRelationships();
        filterTerminalParents( rels );

        return rels;
    }

    public DirectedGraph<ProjectVersionRef, ProjectRelationship<?>> getRawGraph()
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

    public Set<ProjectVersionRef> getIncompleteSubgraphs()
    {
        return Collections.unmodifiableSet( incompleteSubgraphs );
    }

    public Set<ProjectVersionRef> getVariableSubgraphs()
    {
        return Collections.unmodifiableSet( variableSubgraphs );
    }

    public static final class Builder
    {
        private final EProjectKey key;

        private final Set<ProjectRelationship<?>> relationships = new HashSet<ProjectRelationship<?>>();

        private final Set<EProjectRelationships> projects = new HashSet<EProjectRelationships>();

        private Set<EProjectCycle> cycles = new HashSet<EProjectCycle>();

        public Builder( final EProjectRelationships rels )
        {
            this.key = rels.getKey();
            addFromDirectRelationships( rels );
        }

        public Builder( final ProjectVersionRef projectRef, final String... activeProfiles )
        {
            this.key = new EProjectKey( projectRef, new EGraphFacts( activeProfiles ) );
        }

        public Builder( final EProjectKey key )
        {
            this.key = key;
        }

        public Builder withParent( final ProjectVersionRef parent )
        {
            relationships.add( new ParentRelationship( key.getProject(), parent ) );
            return this;
        }

        public Builder withParent( final ProjectRelationship<ProjectVersionRef> parent )
        {
            if ( parent.getDeclaring()
                       .equals( key.getProject() ) )
            {
                relationships.add( parent );
            }
            else
            {
                relationships.add( parent.cloneFor( key.getProject() ) );
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
            this.relationships.clear();
            this.relationships.add( relationships.getParent() );
            this.relationships.addAll( relationships.getDependencies() );
            this.relationships.addAll( relationships.getManagedDependencies() );

            this.relationships.addAll( relationships.getPlugins() );
            this.relationships.addAll( relationships.getManagedPlugins() );

            this.relationships.addAll( relationships.getExtensions() );

            if ( relationships.getPluginDependencies() != null )
            {
                for ( final Map.Entry<PluginRelationship, List<PluginDependencyRelationship>> entry : relationships.getPluginDependencies()
                                                                                                                   .entrySet() )
                {
                    if ( entry.getValue() != null )
                    {
                        this.relationships.addAll( entry.getValue() );
                    }
                }
            }
        }

        public Builder withDependencies( final List<DependencyRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withDependencies( final DependencyRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withPlugins( final Collection<PluginRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withPlugins( final PluginRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withPluginLevelDependencies( final Collection<PluginDependencyRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withPluginLevelDependencies( final PluginDependencyRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withExtensions( final Collection<ExtensionRelationship> rels )
        {
            this.relationships.addAll( rels );
            return this;
        }

        public Builder withExtensions( final ExtensionRelationship... rels )
        {
            this.relationships.addAll( Arrays.asList( rels ) );
            return this;
        }

        public Builder withExactRelationships( final Collection<ProjectRelationship<?>> relationships )
        {
            this.relationships.addAll( relationships );
            return this;
        }

        public Builder withExactRelationships( final ProjectRelationship<?>... relationships )
        {
            this.relationships.addAll( Arrays.asList( relationships ) );
            return this;
        }

        public Builder withRelationships( final Collection<ProjectRelationship<?>> relationships )
        {
            final Set<PluginDependencyRelationship> pluginDepRels = new HashSet<PluginDependencyRelationship>();
            for ( final ProjectRelationship<?> rel : relationships )
            {
                switch ( rel.getType() )
                {
                    case DEPENDENCY:
                    {
                        final DependencyRelationship dr = (DependencyRelationship) rel;
                        withDependencies( dr );

                        break;
                    }
                    case PLUGIN:
                    {
                        final PluginRelationship pr = (PluginRelationship) rel;
                        withPlugins( pr );

                        break;
                    }
                    case EXTENSION:
                    {
                        withExtensions( (ExtensionRelationship) rel );
                        break;
                    }
                    case PLUGIN_DEP:
                    {
                        // load all plugin relationships first.
                        pluginDepRels.add( (PluginDependencyRelationship) rel );
                        break;
                    }
                    case PARENT:
                    {
                        withParent( (ParentRelationship) rel );
                        break;
                    }
                }
            }

            withPluginLevelDependencies( pluginDepRels );

            return this;
        }

        public EProjectGraph build()
        {
            boolean foundParent = false;
            for ( final ProjectRelationship<?> rel : relationships )
            {
                if ( rel instanceof ParentRelationship && rel.getDeclaring()
                                                             .equals( key.getProject() ) )
                {
                    foundParent = true;
                    break;
                }
            }

            if ( !foundParent )
            {
                relationships.add( new ParentRelationship( key.getProject(), key.getProject() ) );
            }

            return new EProjectGraph( key, relationships, projects, cycles );
        }

        public Builder withCycles( final Set<EProjectCycle> cycles )
        {
            if ( cycles != null )
            {
                this.cycles = cycles;
            }

            return this;
        }

    }

    public void add( final EProjectRelationships rels )
    {
        if ( incompleteSubgraphs.contains( rels.getProjectRef() ) )
        {
            incompleteSubgraphs.remove( rels.getProjectRef() );
        }

        connectedProjects.add( rels.getProjectRef() );

        addAll( rels.getExactAllRelationships() );
    }

    private <T extends ProjectRelationship<?>> void add( final T rel )
    {
        if ( rel == null )
        {
            return;
        }

        incompleteSubgraphs.remove( rel.getDeclaring() );

        ProjectVersionRef target = rel.getTarget();
        if ( rel instanceof DependencyRelationship )
        {
            target = ( (ArtifactRef) target ).asProjectVersionRef();
        }

        if ( !graph.containsVertex( target ) )
        {
            graph.addVertex( target );
        }

        graph.addEdge( rel, rel.getDeclaring(), target );

        if ( !target.getVersionSpec()
                    .isSingle() )
        {
            variableSubgraphs.add( target );
        }
        else if ( !connectedProjects.contains( target ) )
        {
            incompleteSubgraphs.add( target );
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

        recomputeIncompleteSubgraphs();
    }

    public void connect( final EProjectGraph subGraph )
    {
        if ( incompleteSubgraphs.contains( subGraph.getRoot() ) )
        {
            incompleteSubgraphs.remove( subGraph.getRoot() );
        }

        connectedProjects.add( subGraph.getRoot() );

        this.connectedProjects.addAll( subGraph.connectedProjects );
        addAll( subGraph.getAllRelationships() );
    }

    public ProjectVersionRef getRoot()
    {
        return key.getProject();
    }

    public void traverse( final ProjectNetTraversal traversal )
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
    private void dfsTraverse( final ProjectNetTraversal traversal, final int pass )
    {
        dfsIterate( getRoot(), traversal, new LinkedList<ProjectRelationship<?>>(), pass );
    }

    private void dfsIterate( final ProjectVersionRef node, final ProjectNetTraversal traversal,
                             final LinkedList<ProjectRelationship<?>> path, final int pass )
    {
        final List<ProjectRelationship<?>> edges = getSortedOutEdges( node );
        if ( edges != null )
        {
            for ( final ProjectRelationship<?> edge : edges )
            {
                if ( traversal.traverseEdge( edge, path, pass ) )
                {
                    ProjectVersionRef target = edge.getTarget();
                    if ( target instanceof ArtifactRef )
                    {
                        target = ( (ArtifactRef) target ).asProjectVersionRef();
                    }

                    // FIXME: Are there cases where a traversal needs to see cycles??
                    boolean cycle = false;
                    for ( final ProjectRelationship<?> item : path )
                    {
                        if ( item.getDeclaring()
                                 .equals( target ) )
                        {
                            cycle = true;
                            break;
                        }
                    }

                    if ( !cycle )
                    {
                        path.addLast( edge );
                        dfsIterate( target, traversal, path, pass );
                        path.removeLast();
                    }

                    traversal.edgeTraversed( edge, path, pass );
                }
            }
        }
    }

    // TODO: Implement without recursion.
    private void bfsTraverse( final ProjectNetTraversal traversal, final int pass )
    {
        final List<ProjectRelationship<?>> path = new ArrayList<ProjectRelationship<?>>();
        path.add( new SelfEdge( getRoot() ) );

        bfsIterate( Collections.singletonList( path ), traversal, pass );
    }

    private void bfsIterate( final List<List<ProjectRelationship<?>>> thisLayer, final ProjectNetTraversal traversal,
                             final int pass )
    {
        final List<List<ProjectRelationship<?>>> nextLayer = new ArrayList<List<ProjectRelationship<?>>>();

        for ( final List<ProjectRelationship<?>> path : thisLayer )
        {
            ProjectVersionRef node = path.get( path.size() - 1 )
                                         .getTarget();
            if ( node instanceof ArtifactRef )
            {
                node = ( (ArtifactRef) node ).asProjectVersionRef();
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

                        // FIXME: How do we avoid cycle traversal here??
                        nextPath.add( edge );
                        nextLayer.add( nextPath );

                        traversal.edgeTraversed( edge, path, pass );
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

    private List<ProjectRelationship<?>> getSortedOutEdges( final ProjectVersionRef node )
    {
        Collection<ProjectRelationship<?>> unsorted = graph.getOutEdges( node );
        if ( unsorted == null )
        {
            return null;
        }

        unsorted = new ArrayList<ProjectRelationship<?>>( unsorted );

        filterTerminalParents( unsorted );

        final List<ProjectRelationship<?>> sorted = new ArrayList<ProjectRelationship<?>>( unsorted );
        Collections.sort( sorted, new RelationshipComparator() );

        return sorted;
    }

    private static final class SelfEdge
        extends AbstractProjectRelationship<ProjectVersionRef>
    {

        private static final long serialVersionUID = 1L;

        SelfEdge( final ProjectVersionRef ref )
        {
            super( null, ref, ref, 0 );
        }

        @Override
        public ArtifactRef getTargetArtifact()
        {
            return new ArtifactRef( getTarget(), "pom", null, false );
        }

    }

    private void readObject( final java.io.ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        incompleteSubgraphs = new HashSet<ProjectVersionRef>();
        connectedProjects = new HashSet<ProjectVersionRef>();
        variableSubgraphs = new HashSet<ProjectVersionRef>();
        cycles = new HashSet<EProjectCycle>();
    }

    public void recomputeIncompleteSubgraphs()
    {
        for ( final ProjectVersionRef vertex : graph.getVertices() )
        {
            final Collection<ProjectRelationship<?>> outEdges = graph.getOutEdges( vertex );
            if ( outEdges != null && !outEdges.isEmpty() )
            {
                incompleteSubgraphs.remove( vertex );
            }
        }
    }

    public boolean isCycleParticipant( final ProjectVersionRef ref )
    {
        for ( final EProjectCycle cycle : cycles )
        {
            if ( cycle.contains( ref ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean isCycleParticipant( final ProjectRelationship<?> rel )
    {
        for ( final EProjectCycle cycle : cycles )
        {
            if ( cycle.contains( rel ) )
            {
                return true;
            }
        }

        return false;
    }

    public void addCycle( final EProjectCycle cycle )
    {
        this.cycles.add( cycle );

        for ( final ProjectRelationship<?> rel : cycle )
        {
            incompleteSubgraphs.remove( rel.getDeclaring() );
            connectedProjects.add( rel.getDeclaring() );
        }
    }

    public Set<EProjectCycle> getCycles()
    {
        return new HashSet<EProjectCycle>( cycles );
    }

    public Set<ProjectRelationship<?>> getRelationshipsTargeting( final ProjectVersionRef ref )
    {
        final Collection<ProjectRelationship<?>> rels = graph.getInEdges( ref );
        if ( rels == null )
        {
            return null;
        }

        return new HashSet<ProjectRelationship<?>>( rels );
    }
}
