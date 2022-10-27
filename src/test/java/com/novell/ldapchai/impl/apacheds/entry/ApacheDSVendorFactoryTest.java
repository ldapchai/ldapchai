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

package com.novell.ldapchai.impl.apacheds.entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

public class ApacheDSVendorFactoryTest
{
    @Test
    public void stringToInstantTest()
    {
        final ApacheDSVendorFactory factory = new ApacheDSVendorFactory();
        Assertions.assertEquals( Instant.parse( "2022-10-20T21:46:12.316Z" ), factory.stringToInstant( "20221020214612.316Z" ) );
    }


    @Test
    public void instantToStringTest()
    {
        final ApacheDSVendorFactory factory = new ApacheDSVendorFactory();
        Assertions.assertEquals( "20221020214612.316Z", factory.instantToString( Instant.parse( "2022-10-20T21:46:12.316Z" ) ) );
    }
}
