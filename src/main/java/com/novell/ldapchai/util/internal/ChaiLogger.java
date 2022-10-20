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

import org.slf4j.event.Level;

import java.util.function.Supplier;

/**
 * Internal Chai API logging wrapper.  Users of Chai should ignore this class.
 *
 * @author Jason D. Rivard
 */
public class ChaiLogger
{
    private final org.slf4j.Logger logger;

    private ChaiLogger( final Class<?> clazz )
    {
        logger = org.slf4j.LoggerFactory.getLogger( clazz );
    }

    public static ChaiLogger getLogger( final Class<?> clazz )
    {
        return new ChaiLogger( clazz );
    }

    public void debug( final Supplier<String> message )
    {
        doLog( Level.TRACE, message, null );
    }

    public void debug( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.TRACE, message, exception );
    }

    public void error( final Supplier<String> message )
    {
        doLog( Level.ERROR, message, null );
    }

    public void error( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.ERROR, message, exception );
    }

    public void info( final Supplier<String> message )
    {
        doLog( Level.INFO, message, null );
    }

    public void info( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.INFO, message, exception );
    }

    public void trace( final Supplier<String> message )
    {
        doLog( Level.TRACE, message, null );
    }

    public void trace( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.TRACE, message, exception );
    }

    public void warn( final Supplier<String> message )
    {
        doLog( Level.WARN, message, null );
    }

    public void warn( final Supplier<String> message, final Exception exception )
    {
        doLog( Level.WARN, message, exception );
    }

    private void doLog( final Level level, final Supplier<String> message, final Exception exception )
    {
        logger.makeLoggingEventBuilder( level )
                .setCause( exception )
                .log( message );

    }
}
