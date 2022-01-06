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

import com.novell.ldapchai.provider.ChaiProvider;

import java.io.Serializable;
import java.util.StringTokenizer;


/**
 * An immutable bean representation of an eDirectory DirXML-EntitlementRef value.
 *
 * @author Jason D. Rivard
 */

public class DirXMLEntitlementRef implements Serializable
{
    //@todo make this class public
    public static final String ASN_VALUE = "2.16.840.1.113719.1.14.4.1.2087";

    public enum State
    {
        REVOKED( 0 ),
        GRANTED( 1 );

        private final int numValue;

        State( final int intValue )
        {
            this.numValue = intValue;
        }

        public int getNumValue()
        {
            return numValue;
        }

        static State forNumValue( final int numValue )
        {
            for ( final State s : values() )
            {
                if ( numValue == s.numValue )
                {
                    return s;
                }
            }
            throw new IllegalArgumentException( "unknown state for " + DirXMLEntitlementRef.class.getSimpleName() + " (" + numValue + ")" );
        }
    }

    private final String entitlementDN;
    private final int state;
    private final String payload;

    public DirXMLEntitlementRef( final String value, final ChaiProvider provider )
    {
        if ( value == null || value.length() < 1 )
        {
            throw new NullPointerException( "missing value" );
        }

        final StringTokenizer st = new StringTokenizer( value, "#" );
        this.entitlementDN = st.nextToken();
        final String stateString = st.nextToken();
        this.state = Integer.parseInt( stateString );
        if ( st.hasMoreTokens() )
        {
            this.payload = st.nextToken();
        }
        else
        {
            this.payload = null;
        }
    }

    public String getEntitlementDN()
    {
        return entitlementDN;
    }

    public State getStateType()
    {
        return State.forNumValue( getState() );
    }

    public int getState()
    {
        return state;
    }

    public String getPayload()
    {
        return payload;
    }
}
