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

package com.novell.ldapchai.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

public class TimeFormatConverterFactory
{
    public static TimeFormatConverter adTimeFormatConverter()
    {
        return new AdTimeFormatConverter();
    }

    public static TimeFormatConverter simplePatternFormatConverter( final String pattern )
    {
        return new SimpleParsingTimeFormatConverter( pattern );
    }

    private static class AdTimeFormatConverter implements TimeFormatConverter
    {
        private static final long AD_EPOCH_OFFSET_MS = makeAdEpochOffeset();

        private static long makeAdEpochOffeset()
        {
            final Calendar msEpochCalender = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
            msEpochCalender.clear();
            msEpochCalender.set( 1601, 0, 1, 0, 0 );
            return msEpochCalender.getTime().getTime();
        }

        @Override
        public String convertInstantToZulu( final Instant instant )
        {
            Objects.requireNonNull( instant, "date must be non-null" );

            final long inputAsMs = instant.toEpochMilli();
            final long inputAsADMs = inputAsMs - AD_EPOCH_OFFSET_MS;
            final long inputAsADNs = inputAsADMs * 10000;

            return String.valueOf( inputAsADNs );
        }

        @Override
        public Optional<Instant> convertZuluToInstant( final String input )
        {
            if ( input == null )
            {
                return Optional.empty();
            }

            if ( "0".equals( input ) )
            {
                return Optional.empty();
            }

            final long timestampAsNs = Long.parseLong( input );
            if ( timestampAsNs <= 0 )
            {
                return Optional.empty();
            }

            final long timestampAsMs = timestampAsNs / 10000;
            final long timestampAsJavaMs = timestampAsMs + AD_EPOCH_OFFSET_MS;

            //magic future date timestamp that also means no date. (long)
            if ( timestampAsJavaMs >= 910692730085477L )
            {
                return Optional.empty();
            }

            return Optional.of( Instant.ofEpochMilli( timestampAsJavaMs ) );
        }
    }

    private static class SimpleParsingTimeFormatConverter implements TimeFormatConverter
    {
        private final DateTimeFormatter dateTimeFormatter;

        SimpleParsingTimeFormatConverter( final String patternFormat )
        {
            Objects.requireNonNull( patternFormat );
            dateTimeFormatter = DateTimeFormatter.ofPattern( patternFormat );
        }

        @Override
        public String convertInstantToZulu( final Instant instant )
        {
            Objects.requireNonNull( instant );

            try
            {
                return dateTimeFormatter.format( instant.atZone( ZoneOffset.UTC ) );
            }
            catch ( DateTimeParseException e )
            {
                throw new IllegalArgumentException( "unable to format zulu time-string: " + e.getMessage() );
            }
        }

        /**
         * Convert the commonly used eDirectory zulu time string to java Date object.
         * See the <a href="http://developer.novell.com/documentation/ndslib/schm_enu/data/sdk5701.html">eDirectory Time attribute syntax definition</a> for more details.
         *
         * @param input a date string in the format of "yyyyMMddHHmmss'Z'", for example "19941216103200Z"
         * @return A Date object representing the string date
         * @throws IllegalArgumentException if dateString is incorrectly formatted
         */
        @Override
        public Optional<Instant> convertZuluToInstant( final String input )
        {
            Objects.requireNonNull( input );

            try
            {
                final LocalDateTime localDateTime = LocalDateTime.parse( input, dateTimeFormatter );
                final ZonedDateTime zonedDateTime = localDateTime.atZone( ZoneOffset.UTC );
                return Optional.of( Instant.from( zonedDateTime ) );
            }
            catch ( DateTimeParseException e )
            {
                throw new IllegalArgumentException( "unable to parse zulu time-string: " + e.getMessage() );
            }
        }
    }

}
