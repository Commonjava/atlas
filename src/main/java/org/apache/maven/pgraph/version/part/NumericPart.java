package org.apache.maven.pgraph.version.part;

public class NumericPart
    extends VersionPart
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

    @Override
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

    public int compareTo( final VersionPart part )
    {
        // 1.2.2 > 1.2.GA, 1.2.1 > 1.2.M1
        if ( part instanceof StringPart )
        {
            // Let the StringPart compareTo(..) method do the heavy lifting.
            return -1 * ( (StringPart) part ).compareTo( this );
        }
        // 1.2.1 > 1.2-SNAPSHOT, 1.2[.0] > 1.2-SNAPSHOT
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + value;
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
        final NumericPart other = (NumericPart) obj;
        if ( value != other.value )
        {
            return false;
        }
        return true;
    }

}
