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

package com.novell.ldapchai.provider;

import com.google.gson.GsonBuilder;
import com.novell.ldapchai.util.internal.ChaiLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple wire trace provider wrapper.  Adds lots of debugging info to the log4j trace level.
 *
 * @author Jason D. Rivard
 * @see ChaiSetting#WIRETRACE_ENABLE
 */
class WireTraceWrapper extends AbstractWrapper
{
    private static final ChaiLogger LOGGER = ChaiLogger.getLogger( WireTraceWrapper.class );

    private final AtomicLong operationCounter = new AtomicLong( 0 );

    /**
     * Wrap a pre-existing ChaiProvider with a WatchdogWrapper instance.
     *
     * @param chaiProvider a pre-existing {@code ChaiProvider}
     * @return a wrapped {@code ChaiProvider} instance.
     */
    static ChaiProviderImplementor forProvider(
            final ChaiProviderImplementor chaiProvider
    )
    {
        //check to make sure watchdog ise enabled;
        final boolean watchDogEnabled = Boolean.parseBoolean( chaiProvider.getChaiConfiguration().getSetting( ChaiSetting.WIRETRACE_ENABLE ) );
        if ( !watchDogEnabled )
        {
            final String errorStr = "attempt to obtain WireTrace wrapper when watchdog is not enabled in chai config";
            LOGGER.warn( () -> errorStr );
            throw new IllegalStateException( errorStr );
        }

        if ( Proxy.isProxyClass( chaiProvider.getClass() ) && chaiProvider instanceof WireTraceWrapper )
        {
            LOGGER.warn( () -> "attempt to obtain WireTraceWrapper wrapper for already wrapped Provider." );
            return chaiProvider;
        }

        return ( ChaiProviderImplementor ) Proxy.newProxyInstance(
                chaiProvider.getClass().getClassLoader(),
                chaiProvider.getClass().getInterfaces(),
                new WireTraceWrapper( chaiProvider ) );
    }

    WireTraceWrapper(
            final ChaiProviderImplementor realProvider
    )
    {
        this.realProvider = realProvider;
    }

    @Override
    public Object invoke(
            final Object proxy,
            final Method method,
            final Object[] args )
            throws Throwable
    {
        final boolean isLdap = method.getAnnotation( ChaiProviderImplementor.LdapOperation.class ) != null;

        try
        {
            if ( isLdap )
            {
                return traceInvocation( method, args );
            }
            else
            {
                return method.invoke( realProvider, args );
            }
        }
        catch ( InvocationTargetException e )
        {
            throw e.getCause();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "unexpected invocation exception: " + e.getMessage(), e );
        }
    }

    private Object traceInvocation(
            final Method method,
            final Object[] args
    )
            throws Throwable
    {
        final long opNumber = getNextCounter();

        final String messageLabel = "id=" + realProvider.getIdentifier() + ",op#" + opNumber;

        LOGGER.trace( () -> "begin " + messageLabel + " method " + methodToDebugStr( method, args ) );

        final Instant startTime = Instant.now();

        final Object result = method.invoke( realProvider, args );

        final Duration totalTime = Duration.between( startTime, Instant.now() );

        LOGGER.trace( () -> "finish " + messageLabel + " result: "
                + objectToDebugString( result )
                + " (" + ChaiLogger.format( totalTime ) + ")" );

        return result;
    }

    private long getNextCounter()
    {
        return operationCounter.incrementAndGet();
    }

    private String objectToDebugString( final Object object )
    {
        if ( object == null )
        {
            return "[null]";
        }

        try
        {
            return ( new GsonBuilder().disableHtmlEscaping().create() ).toJson( object );
        }
        catch ( Exception e )
        {
            LOGGER.debug( () -> "error converting object to debug string: " + e.getMessage() );
            return "[error:" + e.getMessage() + "]";
        }
    }


    static String methodToDebugStr( final Method theMethod, final Object... parameters )
    {
        final StringBuilder debugStr = new StringBuilder();
        debugStr.append( theMethod.getName() );
        debugStr.append( '(' );
        if ( parameters != null )
        {
            for ( final Iterator<Object> iter = Arrays.asList( parameters ).iterator(); iter.hasNext(); )
            {
                final Object nextValue = iter.next();
                try
                {
                    debugStr.append( parameterToString( nextValue ) );
                }
                catch ( Throwable e )
                {
                    debugStr.append( "<binary>" );
                }
                if ( iter.hasNext() )
                {
                    debugStr.append( ',' );
                }
            }
        }
        debugStr.append( ')' );

        return debugStr.toString();
    }

    private static String parameterToString( final Object nextValue )
    {
        if ( nextValue == null )
        {
            return "null";
        }
        else if ( nextValue.getClass().isArray() )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "[" );
            for ( final Object loopValue : ( Object[] ) nextValue )
            {
                sb.append( parameterToString( loopValue ) );
                sb.append( "," );
            }
            sb.append( "]" );
            return sb.toString();
        }
        else
        {
            return String.valueOf( nextValue );
        }
    }
}
