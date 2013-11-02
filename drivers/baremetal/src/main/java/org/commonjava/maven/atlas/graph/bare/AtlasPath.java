package org.commonjava.maven.atlas.graph.bare;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

// FIXME: Replace static storage of instances with a factory wrapper of some sort.
public class AtlasPath
    implements Iterable<ProjectRelationship<?>>
{

    private final String id;

    private final List<ProjectRelationship<?>> path;

    AtlasPath( final String id, final ProjectRelationship<?>... path )
    {
        this.id = id;
        this.path = Collections.unmodifiableList( Arrays.asList( path ) );
    }

    AtlasPath( final String id, final List<ProjectRelationship<?>> path )
    {
        this.id = id;
        this.path = Collections.unmodifiableList( path );
    }

    public String getId()
    {
        return id;
    }

    public List<ProjectRelationship<?>> getPathElements()
    {
        return path;
    }

    @Override
    public Iterator<ProjectRelationship<?>> iterator()
    {
        return path.iterator();
    }

    public List<ProjectRelationship<?>> getPreviousParts()
    {
        return path.size() > 1 ? path.subList( 0, path.size() - 1 ) : Collections.<ProjectRelationship<?>> emptyList();
    }

    public ProjectRelationship<?> getCurrentPart()
    {
        return path.isEmpty() ? null : path.get( path.size() - 1 );
    }

    public ProjectVersionRef getStartGAV()
    {
        return path.isEmpty() ? null : path.get( 0 )
                                           .getDeclaring();
    }

    public ProjectVersionRef getEndGAV()
    {
        return path.isEmpty() ? null : path.get( path.size() - 1 )
                                           .getTarget()
                                           .asProjectVersionRef();
    }

    public boolean contains( final ProjectVersionRef ref )
    {
        for ( final ProjectRelationship<?> rel : path )
        {
            if ( rel.getDeclaring()
                    .equals( ref ) || rel.getTarget()
                                         .asProjectVersionRef()
                                         .equals( ref ) )
            {
                return true;
            }
        }

        return false;
    }

}
