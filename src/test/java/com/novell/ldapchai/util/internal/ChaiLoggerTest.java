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

package com.novell.ldapchai.util.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

class ChaiLoggerTest
{
    @Test
    void testFormatDuration()
    {
        final Duration testDuration = Duration.parse( "PT11.22886S" );
        final String formatted = ChaiLogger.format( testDuration );
        Assertions.assertEquals( "PT11.228S", formatted );
    }

    @Test
    void testFormatInstance()
    {
        final Instant testInstant = Instant.parse( "2000-01-01T13:11:44.404946Z" );
        final String formatted = ChaiLogger.format( testInstant );
        Assertions.assertEquals( "2000-01-01T13:11:44.404Z", formatted );
    }
}
