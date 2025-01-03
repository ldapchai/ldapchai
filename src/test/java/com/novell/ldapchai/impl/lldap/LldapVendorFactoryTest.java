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

package com.novell.ldapchai.impl.lldap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Tests for "LLDAP" vendor (not generic LDAP).  <a href="https://github.com/lldap/lldap">https://github.com/lldap/lldap</a>.
 */
class LldapVendorFactoryTest
{
    @Test
    public void stringToInstantTest()
    {
        final LldapVendorFactory factory = new LldapVendorFactory();
        final Instant instant = factory.stringToInstant( "2022-10-20T21:46:12.000000000+00:00" );
        final Instant expectedValue = Instant.parse(  "2022-10-20T21:46:12Z" );

        Assertions.assertEquals( expectedValue, instant );

    }

    @Test
    public void instantToStringTest()
    {
        final LldapVendorFactory factory = new LldapVendorFactory();
        final Instant testInstant = Instant.parse( "2022-10-20T21:46:12Z" );
        final String string =  factory.instantToString( testInstant );

        Assertions.assertEquals( "2022-10-20T21:46:12.000000000+00:00", string );
    }

}
