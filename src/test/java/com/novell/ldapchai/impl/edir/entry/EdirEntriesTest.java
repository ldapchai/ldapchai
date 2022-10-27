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

package com.novell.ldapchai.impl.edir.entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;


public class EdirEntriesTest
{

    @Test
    public void shouldReturnExceptionWhenConvertZuluToDateValueIsToShort()
    {
        Assertions.assertThrows( IllegalArgumentException.class, () ->
        {
            EdirEntries.convertZuluToInstant( "01234567890123" );
        } );
    }

    @Test
    public void shouldReturnExceptionWhenConvertZuluToDateValueDoesNotHaveZAtChar14()
    {
        Assertions.assertThrows( IllegalArgumentException.class, () ->
        {
            EdirEntries.convertZuluToInstant( "012345678901234" );
        } );
    }

    @Test
    public void shouldReturnExceptionWhenConvertZuluToDateValueDoesHaveZAtChar14ButIsLonger()
    {
        Assertions.assertThrows( IllegalArgumentException.class, () ->
        {
            EdirEntries.convertZuluToInstant( "20150101000000Z9" );
        } );
    }

    @Test
    public void shouldReturnExceptionWhenConvertZuluToDateValueIsNull()
    {
        Assertions.assertThrows( NullPointerException.class, () ->
        {
            EdirEntries.convertZuluToInstant( null );
        } );
    }

    @Test
    public void shouldReturnInstantWhenConvertZuluToInstantValueIsCorrect()
    {
        final Instant instant = EdirEntries.convertZuluToInstant( "20150402010745Z" );
        Assertions.assertEquals( instant.toEpochMilli(), 1427936865000L );
    }

    @Test
    public void shouldReturnStringWhenConvertInstantToStringIsCorrect()
    {
        final Instant input = Instant.ofEpochMilli( 1427936865000L );
        final String zuluTimestamp = EdirEntries.convertInstantToZulu( input );
        Assertions.assertEquals( zuluTimestamp, "20150402010745Z" );
    }
}
