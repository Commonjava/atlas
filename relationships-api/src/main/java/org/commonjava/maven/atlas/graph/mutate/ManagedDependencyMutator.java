package org.commonjava.maven.atlas.graph.mutate;

import static org.apache.commons.lang.StringUtils.join;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectNet;
import org.commonjava.maven.atlas.graph.model.GraphView;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedDependencyMutator
    extends AbstractVersionManagerMutator
    implements GraphMutator
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final boolean accumulate;

    public ManagedDependencyMutator( final GraphView view, final boolean accumulate )
    {
        super( view );
        this.accumulate = accumulate;
    }

    public ManagedDependencyMutator( final EProjectNet net, final boolean accumulate )
    {
        super( net );
        this.accumulate = accumulate;
    }

    public ManagedDependencyMutator( final GraphView view, final VersionManager versions, final boolean accumulate )
    {
        super( view, versions );
        this.accumulate = accumulate;
    }

    public ManagedDependencyMutator( final EProjectNet net, final VersionManager versions, final boolean accumulate )
    {
        super( net, versions );
        this.accumulate = accumulate;
    }

    @Override
    public ProjectRelationship<?> selectFor( final ProjectRelationship<?> rel )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY )
        {
            logger.info( "No selections for relationships of type: {}", rel.getType() );
            return rel;
        }

        final ProjectRelationship<?> mutated = super.selectFor( rel );

        if ( mutated != null && mutated != rel )
        {
            logger.info( "Mutated. Was:\n  {}\n\nNow:\n  {}\n\n", rel, mutated );
        }

        return mutated;
    }

    @Override
    public GraphMutator getMutatorFor( final ProjectRelationship<?> rel )
    {
        if ( rel.getType() != RelationshipType.DEPENDENCY )
        {
            logger.info( "Return 'this' for non-dependency relationship: {}", rel );
            return this;
        }

        if ( accumulate )
        {
            final ProjectVersionRef declaring = rel.getDeclaring()
                                                   .asProjectVersionRef();

            logger.info( "Retrieving managed dependency relationships for: {}", declaring );

            Set<ProjectRelationship<?>> rels =
                view.getDatabase()
                    .getDirectRelationshipsFrom( new GraphView( view.getWorkspace(), view.getRoots() ), declaring, true, false,
                                                 RelationshipType.DEPENDENCY );

            if ( rels != null )
            {
                final Set<ProjectRelationship<?>> processed = new HashSet<ProjectRelationship<?>>( rels.size() );
                for ( final ProjectRelationship<?> r : rels )
                {
                    if ( !( ( r instanceof DependencyRelationship ) && ( (DependencyRelationship) r ).isBOM() ) )
                    {
                        processed.add( r );
                    }
                    else
                    {
                        logger.info( "DETECTED BOM IMPORT: {}; skipping for managed deps in mutator for: {}", r.getTarget(), declaring );
                    }
                }

                rels = processed;
            }

            if ( rels != null && !rels.isEmpty() )
            {
                logger.info( "Incorporating {} new managed deps from: {}", rels.size(), declaring );

                final Map<ProjectRef, ProjectVersionRef> existing = versions.getSelections();

                ProjectRef pr;
                boolean construct = false;
                for ( final ProjectRelationship<?> r : rels )
                {
                    pr = r.getTarget()
                          .asProjectRef();
                    if ( !existing.containsKey( pr ) )
                    {
                        construct = true;
                        break;
                    }
                }

                if ( construct )
                {
                    final Map<ProjectRef, ProjectVersionRef> mapping = VersionManager.createMapping( RelationshipUtils.targets( rels ) );
                    final ManagedDependencyMutator result = new ManagedDependencyMutator( view, new VersionManager( versions, mapping ), accumulate );

                    logger.info( "Managed dependencies for children of: {} are:\n  {}\n\n", declaring, join( result.versions.getSelections()
                                                                                                                            .entrySet(), "\n  " ) );
                    return result;
                }
            }
            else
            {
                logger.info( "No managed deps for target: {}", declaring );
            }
        }

        return this;
    }
}
