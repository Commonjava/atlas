package org.apache.maven.pgraph.version.part;

import java.util.ArrayList;
import java.util.List;

public class StringPart
    implements VersionPart<String>
{

    // FIXME: How do we want to do this??
    private static final List<String> MARKER_ORDER = new ArrayList<String>()
    {
        private static final long serialVersionUID = 1L;

        {
            add( "PRE" );
            add( "PREVIEW" );
            add( "M" );
            add( "MILESTONE" );
            add( "ALPHA" );
            add( "BETA" );
            add( "CR" );
            add( "RC" );
            add( "" );
            add( "SP" );
            add( "CP" );
        }
    };

    private final String value;

    public StringPart( final String value )
    {
        this.value = value;
    }

    public String renderStandard()
    {
        return value;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.format( "STR[%s]", value );
    }

    // FIXME: Need to account for 1.1.M1 < 1.1 == 1.1.GA < 1.1.SP1
    public int compareTo( final VersionPart<?> part )
    {
        if ( part instanceof StringPart )
        {
            return value.compareTo( ( (StringPart) part ).getValue() );
        }
        // 1.2-foo > 1.2-SNAPSHOT
        else if ( part instanceof SnapshotPart )
        {
            return 1;
        }
        // 1.2.2 > 1.2.GA, 1.2.1 > 1.2.M1
        else if ( part instanceof NumericPart )
        {
            if ( ( (NumericPart) part ).getValue() == 0 )
            {
                // 1.2.0 == 1.2.GA, 1.2.0 < 1.2.SP1, 1.2.0 > 1.2.M1
            }
            return -1;
        }

        // punt...shouldn't happen.
        return 0;
    }

}
