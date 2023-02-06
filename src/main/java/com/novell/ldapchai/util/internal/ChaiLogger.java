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


import org.apache.log4j.Level;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * Internal Chai API logging wrapper.  Users of Chai should ignore this class.
 *
 * @author Jason D. Rivard
 */
public final class ChaiLogger
{
    private final org.apache.log4j.Logger logger;

    private ChaiLogger( final Class<?> clazz )
    {
        logger = org.apache.log4j.Logger.getLogger( clazz.getName() );
    }

    public static ChaiLogger getLogger( final Class<?> clazz )
    {
        return new ChaiLogger( clazz );
    }

    public void debug( final Supplier<String> message )
    {
        doLog( Level.TRACE, message, null, null );
    }

    public void debug( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.TRACE, message, null, exception );
    }

    public void debug( final Supplier<String> message, final Duration duration )
    {
        doLog( Level.TRACE, message, duration, null );
    }

    public void error( final Supplier<String> message )
    {
        doLog( Level.ERROR, message, null, null );
    }

    public void error( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.ERROR, message, null, exception );
    }

    public void info( final Supplier<String> message )
    {
        doLog( Level.INFO, message, null, null );
    }

    public void info( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.INFO, message, null, exception );
    }

    public void trace( final Supplier<String> message )
    {
        doLog( Level.TRACE, message, null, null );
    }

    public void trace( final Supplier<String> message, final Duration duration )
    {
        doLog( Level.TRACE, message, duration, null );
    }

    public void trace( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.TRACE, message, null, exception );
    }

    public void warn( final Supplier<String> message )
    {
        doLog( Level.WARN, message, null, null );
    }

    public void warn( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.WARN, message, null, exception );
    }

    private void doLog(
            final Level level,
            final Supplier<String> message,
            final Duration duration,
            final Exception exception
    )
    {
        logger.log( level, appendDurationToMessage( message, duration ), exception );
    }

    private static String appendDurationToMessage(
            final Supplier<String> message,
            final Duration duration
    )
    {
        if ( duration == null )
        {
            return message.get();
        }

        return message.get() + " (" + ChaiLogger.format( duration ) + ")";
    }

    public static String format( final Instant instant )
    {
        return instant == null ? "" : instant.truncatedTo( ChronoUnit.MILLIS ).toString();
    }

    public static String format( final Duration duration )
    {
        if ( duration == null )
        {
            return "";
        }

        final Duration msDuration = Duration.ofMillis( duration.toMillis() );
        return msDuration.toString();
    }
}
