package org.commonjava.maven.atlas.graph.mutate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

/**
 * Manager class used to capture managed/central versioning information and 
 * version-selection decisions in a dependency graph. Accumulation via successive 
 * nodes in the graph containing version-management information is handled via
 * new instances of this class, potentially embedded in {@link GraphMutator} 
 * instances.
 * 
 * @author jdcasey
 */
public class VersionManager
{

    // Recursive structure MIGHT help with duplication in different graph paths 
    // during traverse, which is otherwise incurred when the selections are 
    // copied to the local map (creating all the attendant tracking/state).
    private final VersionManager parent;

    private final Map<ProjectRef, ProjectVersionRef> selections;

    public VersionManager()
    {
        this.parent = null;
        this.selections = new HashMap<ProjectRef, ProjectVersionRef>();
    }

    public VersionManager( final Map<ProjectRef, ProjectVersionRef> selections )
    {
        this.parent = null;
        this.selections = selections == null ? new HashMap<ProjectRef, ProjectVersionRef>() : selections;
    }

    public VersionManager( final VersionManager parent, final Map<ProjectRef, ProjectVersionRef> newSelections )
    {
        this.parent = parent;
        this.selections = newSelections;
    }

    public ProjectVersionRef getSelected( final ProjectRef ref )
    {
        ProjectVersionRef selected = selections.get( ref );
        if ( selected == null )
        {
            selected = selections.get( ref.asProjectRef() );
        }

        if ( selected == null && parent != null )
        {
            selected = parent.getSelected( ref );
        }

        return selected;
    }

    public void select( final ProjectRef ref, final ProjectVersionRef versionRef, final boolean overwrite )
    {
        if ( overwrite || !this.selections.containsKey( ref ) )
        {
            this.selections.put( ref, versionRef );
        }
    }

    public static Map<ProjectRef, ProjectVersionRef> createMapping( final Collection<ProjectVersionRef> refs )
    {
        if ( refs == null )
        {
            return null;
        }

        final Map<ProjectRef, ProjectVersionRef> mapping = new HashMap<ProjectRef, ProjectVersionRef>();
        for ( final ProjectVersionRef ref : refs )
        {
            final ProjectRef pr = ref.asProjectRef();
            if ( !mapping.containsKey( pr ) )
            {
                mapping.put( pr, ref );
            }
        }

        return mapping;
    }

    public boolean hasSelectionFor( final ProjectRef ref )
    {
        return selections.containsKey( ref ) || ( parent != null && parent.hasSelectionFor( ref ) );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( selections == null ) ? 0 : selections.hashCode() );
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
        final VersionManager other = (VersionManager) obj;
        if ( selections == null )
        {
            if ( other.selections != null )
            {
                return false;
            }
        }
        else if ( !selections.equals( other.selections ) )
        {
            return false;
        }
        return true;
    }

    void renderSelections( final StringBuilder sb )
    {

        for ( final Entry<ProjectRef, ProjectVersionRef> entry : selections.entrySet() )
        {
            final ProjectRef key = entry.getKey();
            final ProjectVersionRef value = entry.getValue();

            sb.append( "\n  " )
              .append( key )
              .append( " => " )
              .append( value );
        }

        if ( parent != null )
        {
            parent.renderSelections( sb );
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        renderSelections( sb );

        return "VersionManager: {" + sb + "\n}";
    }

}
