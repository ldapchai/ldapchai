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

import com.novell.ldapchai.util.internal.ChaiLogger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

public class TimeFormatConverterFactory
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( TimeFormatConverterFactory.class );

    public static TimeFormatConverter adTimeFormatConverter()
    {
        return new AdTimeFormatConverter();
    }

    public static TimeFormatConverter simplePatternFormatConverter( final String pattern )
    {
        return new SimpleParsingTimeFormatConverter( pattern );
    }

    public static TimeFormatConverter custom( final DateTimeFormatter parseConverter, final DateTimeFormatter outputConverter )
    {
        return new SimpleParsingTimeFormatConverter( parseConverter, outputConverter );
    }

    public static TimeFormatConverter isoConverter()
    {
        return new SimpleParsingTimeFormatConverter( DateTimeFormatter.ISO_OFFSET_DATE_TIME, DateTimeFormatter.ISO_OFFSET_DATE_TIME  );
    }

    private static class SimpleParsingTimeFormatConverter implements TimeFormatConverter
    {
        private final DateTimeFormatter parseConverter;
        private final DateTimeFormatter outputConverter;

        SimpleParsingTimeFormatConverter( final DateTimeFormatter parseConverter, final DateTimeFormatter outputConverter )
        {
            this.parseConverter = Objects.requireNonNull( parseConverter );
            this.outputConverter = Objects.requireNonNull( outputConverter );
        }

        SimpleParsingTimeFormatConverter( final String patternFormat )
        {
            Objects.requireNonNull( patternFormat );
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern( patternFormat ).withZone( ZoneOffset.UTC );
            this.parseConverter = dateTimeFormatter;
            this.outputConverter = dateTimeFormatter;
        }

        @Override
        public String outputInstantToString( final Instant instant )
        {
            Objects.requireNonNull( instant );

            try
            {
                return outputConverter.format( instant.atZone( ZoneOffset.UTC ) );
            }
            catch ( DateTimeParseException e )
            {
                throw new IllegalArgumentException( "unable to format zulu time-string: " + e.getMessage() );
            }
        }

        @Override
        public Optional<Instant> parseStringToInstant( final String input )
        {
            Objects.requireNonNull( input );

            try
            {
                return Optional.of( Instant.from( parseConverter.parse( input ) ) );
            }
            catch ( DateTimeParseException e )
            {
                throw new IllegalArgumentException( "unable to parse time-string: " + e.getMessage() );
            }
        }
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
        public String outputInstantToString( final Instant instant )
        {
            Objects.requireNonNull( instant, "date must be non-null" );

            final long inputAsMs = instant.toEpochMilli();
            final long inputAsADMs = inputAsMs - AD_EPOCH_OFFSET_MS;
            final long inputAsADNs = inputAsADMs * 10000;

            return String.valueOf( inputAsADNs );
        }

        @Override
        public Optional<Instant> parseStringToInstant( final String input )
        {
            if ( input == null )
            {
                return Optional.empty();
            }

            if ( "0".equals( input ) )
            {
                return Optional.empty();
            }

            final long timestampAsNs;
            try
            {
                timestampAsNs = Long.parseLong( input );
                if ( timestampAsNs <= 0 )
                {
                    return Optional.empty();
                }
            }
            catch ( final NumberFormatException e )
            {
                LOGGER.trace( () -> "error parsing expected AD time format value: " + e.getMessage() );
                return Optional.empty();
            }

            final long timestampAsMs = timestampAsNs / 10000;
            final long timestampAsJavaMs = timestampAsMs + AD_EPOCH_OFFSET_MS;

            //magic future date timestamp that also means no date. (long)
            if ( timestampAsJavaMs >= 910692730085477L )
            {
                return Optional.empty();
            }

            try
            {
                return Optional.of( Instant.ofEpochMilli( timestampAsJavaMs ) );
            }
            catch ( final DateTimeParseException e )
            {
                LOGGER.trace( () -> "error parsing expected AD time format value: " + e.getMessage() );
                return Optional.empty();
            }
        }
    }


}
