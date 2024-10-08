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

package com.novell.ldapchai.impl.openldap.entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class OpenLDAPVendorFactoryTest
{
    @Test
    public void stringToInstantTest()
    {
        final OpenLDAPVendorFactory factory = new OpenLDAPVendorFactory();
        Assertions.assertEquals( Instant.parse( "2022-10-19T19:57:31Z" ), factory.stringToInstant( "20221019195731Z" ) );
    }

    @Test
    public void magicValueStringToInstantTest()
    {
        final OpenLDAPVendorFactory factory = new OpenLDAPVendorFactory();
        Assertions.assertEquals( Instant.parse( "0000-01-01T00:00:00Z" ), factory.stringToInstant( "000001010000Z" ) );
        Assertions.assertEquals( "00010101000000Z", factory.instantToString( Instant.parse( "0000-01-01T00:00:00Z" ) ) );
    }

    @Test
    public void instantToStringTest()
    {
        final OpenLDAPVendorFactory factory = new OpenLDAPVendorFactory();
        Assertions.assertEquals( "20221020214612Z", factory.instantToString( Instant.parse( "2022-10-20T21:46:12Z" ) ) );
    }

}
