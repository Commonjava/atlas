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

    private final Map<ProjectRef, ProjectVersionRef> selections;

    public VersionManager()
    {
        this.selections = new HashMap<ProjectRef, ProjectVersionRef>();
    }

    public VersionManager( final Map<ProjectRef, ProjectVersionRef> selections )
    {
        this.selections = selections == null ? new HashMap<ProjectRef, ProjectVersionRef>() : selections;
    }

    public VersionManager( final VersionManager parent, final Map<ProjectRef, ProjectVersionRef> newSelections )
    {
        this.selections = new HashMap<ProjectRef, ProjectVersionRef>( parent.selections );

        for ( final Entry<ProjectRef, ProjectVersionRef> entry : newSelections.entrySet() )
        {
            final ProjectRef key = entry.getKey();
            final ProjectVersionRef value = entry.getValue();

            if ( !this.selections.containsKey( key ) )
            {
                this.selections.put( key, value );
            }
        }
    }

    public ProjectVersionRef getSelected( final ProjectRef ref )
    {
        ProjectVersionRef selected = selections.get( ref );
        if ( selected == null )
        {
            selected = selections.get( ref.asProjectRef() );
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
        return selections.containsKey( ref );
    }

    public Map<ProjectRef, ProjectVersionRef> getSelections()
    {
        return selections;
    }

}
