package org.apache.maven.pgraph.version.part;

public class NumericPart
    implements VersionPart<Integer>
{

    public static final NumericPart ZERO = new NumericPart( 0 );

    private final int value;

    public NumericPart( final String value )
    {
        this.value = Integer.parseInt( value );
    }

    public NumericPart( final int value )
    {
        this.value = value;
    }

    public String renderStandard()
    {
        return Integer.toString( value );
    }

    public Integer getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.format( "NUM[%s]", value );
    }

    public int compareTo( final VersionPart<?> part )
    {
        // 1.2.2 > 1.2.GA, 1.2.1 > 1.2.M1
        if ( part instanceof StringPart )
        {
            return 1;
        }
        // 1.2.1 > 1.2-SNAPSHOT
        else if ( part instanceof SnapshotPart )
        {
            return 1;
        }
        else if ( part instanceof NumericPart )
        {
            final int other = ( (NumericPart) part ).getValue();
            if ( value < other )
            {
                return -1;
            }
            else if ( value > other )
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }

        // punt...shouldn't happen.
        return 0;
    }

}
