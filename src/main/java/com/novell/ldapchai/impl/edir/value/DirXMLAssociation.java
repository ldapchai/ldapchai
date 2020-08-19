/*
 * LDAP Chai API
 * Copyright (c) 2006-2017 Novell, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.novell.ldapchai.impl.edir.value;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <p>An immutable representation of an eDirectory DirXML-Attribute value.</p>
 *
 * <p><b>Example value in ldap representation:</b></p>
 * <pre>
 * cn=driver1,cn=DriverSet,ou=orgUnit,o=org#1#USER_ID=1,table=USERS,schema=IDM
 *                 |                        |                |
 *              driverDN                  state            value
 * </pre>
 *
 * @author Jason D. Rivard
 */
public class DirXMLAssociation implements Serializable
{

    /**
     * ASN value of the <i>DirXML-Association</i> attribute.
     */
    public static final String ASN_VALUE = "2.16.840.1.113719.1.14.4.1.4";

    /**
     * State of the association, stored as an integer.
     */
    public enum State
    {
        /**
         * 0.
         */
        DISABLED( 0 ),

        /**
         * 1.
         */
        PROCESSED( 1 ),

        /**
         * 2.
         */
        PENDING( 2 ),

        /**
         * 3.
         */
        MANUAL( 3 ),

        /**
         * 4.
         */
        MIGRATE( 4 ),;

        private final int numValue;

        State( final int numValue )
        {
            this.numValue = numValue;
        }

        /**
         * Returns the numerical value of the state.
         *
         * @return numerical value of the state
         */
        public int getNumValue()
        {
            return numValue;
        }

        public static State forIntValue( final int numValue )
        {
            for ( final State loopEnum : values() )
            {
                if ( loopEnum.getNumValue() == numValue )
                {
                    return loopEnum;
                }
            }
            throw new IllegalArgumentException( "unknown state for " + DirXMLAssociation.class.getSimpleName() + " (" + numValue + ")" );
        }
    }


    private static final String SEPERATOR = "#";

    private final String driverDN;
    private final State state;
    private final String value;

    public static DirXMLAssociation forStoredValue( final String storedValue )
    {
        return new DirXMLAssociation( storedValue );
    }

    public static Set<DirXMLAssociation> forStoredValues( final Collection<String> values )
    {
        return forStoredValues( values.toArray( new String[0] ) );
    }

    public static Set<DirXMLAssociation> forStoredValues( final String... values )
    {
        final Set<DirXMLAssociation> returnSet = new HashSet<>();
        for ( final String value : values )
        {
            final DirXMLAssociation assocValue = new DirXMLAssociation( value );
            returnSet.add( assocValue );
        }
        return returnSet;
    }

    private DirXMLAssociation( final String storedValue )
    {
        if ( storedValue == null || storedValue.length() < 1 )
        {
            throw new NullPointerException( "missing value" );
        }

        final StringTokenizer st = new StringTokenizer( storedValue, SEPERATOR );

        try
        {
            this.driverDN = st.nextToken();

            {
                final String stateString = st.nextToken();
                final int stateNumber = Integer.parseInt( stateString );
                this.state = State.forIntValue( stateNumber );
            }

            if ( st.hasMoreTokens() )
            {
                this.value = st.nextToken();
            }
            else
            {
                this.value = "";
            }
        }
        catch ( NoSuchElementException e )
        {
            throw new IllegalArgumentException( "malformed value" );
        }
    }

    public DirXMLAssociation( final String driverDN, final State state, final String value )
    {
        this.driverDN = driverDN;
        this.state = state;
        if ( value == null )
        {
            this.value = "";
        }
        else
        {
            this.value = value;
        }
    }

    /**
     * DN of the driver the association is referring to.
     *
     * @return String in ldap DN format
     */
    public String getDriverDN()
    {
        return driverDN;
    }

    /**
     * State of the association.
     *
     * @return state of the association
     */
    public State getState()
    {
        return state;
    }

    /**
     * Value of the association.  Syntax, format, and the actual value is driver specific.
     *
     * @return string value of the association
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Output the value of the association, in ldap format.  The results
     * are suitable for writing to ldap.
     *
     * @return ldap format of the association value.
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( this.getDriverDN() );
        sb.append( '#' );
        sb.append( this.getState().getNumValue() );
        sb.append( '#' );
        sb.append( this.getValue() );
        return sb.toString();
    }
}
